/*******************************************************************************
 * Copyright 2013 Yoann Mikami <yoann@ymkm.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ymkm.lib.automaton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import org.ymkm.lib.automaton.IntentAutomaton.Runner.RunnerContext;

/**
 * Defines an finite-state-machine (FSM) with transitions triggered by Intents
 * 
 * <p>
 * A FSM is composed of States and Transitions.<br>
 * A Transition links two states in the FSM, the source state and the target
 * state.<br>
 * Two special states are typically found in a FSM : The initial state and the
 * final state(s).<br>
 * While there can be multiple final states, only one initial state is
 * permitted.
 * </p>
 * <p>
 * An {@code IntentAutomaton} is an abstract representation of a FSM, but does
 * not do anything by itself; it needs a {@link IntentAutomaton#Runner}, which
 * represents a running instance of a given FSM.<br>
 * In other words, multiple Runner may run simultaneously on the same FSM; each
 * will store the current state, and any other contextual data (see
 * {@link IntentAutomaton.Runner.RunnerContext}), while the
 * {@code IntentAutomaton} itself is mostly stateless.
 * </p>
 * <p>
 * A State is defined by at least a state ID (UNIQUE in a given automaton), a
 * name (For readability/debugging).<br>
 * Optionally, a {@link StateAction} can be supplied for either or both onEnter
 * and onExit events;<br>
 * onEnter event will run the specified StateAction whenever a transition leads
 * to it, while onExit will run the specified StateAction whenever a transition
 * leaves from it to another state.
 * </p>
 * <p>
 * A Transition is defined by at least a from state ID X, a to state ID Y and an
 * intent qualifier.<br>
 * This tells the automaton that whenever the specified intent is broadcast in
 * the system, moves to state Y if any Runner's current state is X.<br>
 * Therefore, multiple Runner may transition at the same time should they react
 * on the same intent and are in the same state.<br>
 * Optionally a {@link TransitionAction} may be supplied to a given Transition :
 * <br>
 * This will be called whenever the transition it is attached to is taken,
 * irrespective of from/to states.<br>
 * In addition, a {@link TransitionGuard} may be applied to a Transition :<br>
 * This prevents the Runner to take the transition matching the from state and
 * intent if the {@link TransitionGuard#guarded} function returns {@code true}.
 * </p>
 * <p>
 * A given state can have multiple transitions linking to several states. While
 * guards set on transitions should ideally be mutually exclusive, the system
 * does not enforce this, and will take the first transition it finds that it
 * can take.<br>
 * <strong>Note :</strong> Incidentally, it is possible to create a FSM in which
 * a Runner gets stuck into if all transitions from current state have
 * TransitionGuard that all return true.<br>
 * Special care must thus be taken when designing FSM with
 * Transition/TransitionGuard leaving from the same state.
 * </p>
 * <p>
 * Multiple {@link Runner} instances can process the same
 * {@code IntentAutomaton} ; Each new instance will yield a new
 * {@link RunnerContext} which gets passed to each {@link StateAction},
 * {@link TransitionAction}, {@link TransitionGuard} callback methods; all
 * instance-specific states that needs to be propagated throughout the whole run
 * can be stored in the context using a somple key-value storage.<br>
 * </p>
 * 
 * @author mikami-yoann
 * 
 */
@SuppressLint("UseSparseArrays")
public class IntentAutomaton {

	/*
	 * INTERNAL How it works : Each new intent assigned to each transition will
	 * be dynamically added to an IntentFilter then monitored by a
	 * BroadcastReceiver created by the Runner when the latter is started
	 * (start()). It merely delegates the work to a Handler running in the
	 * Runner context. The Handler checks the current state, finds transitions
	 * assigned, then checks against the return value of their TransitionGuard
	 * until one returns false. If all of them return true, the transition does
	 * not occur. Otherwise, fromState.onExit, transition.action(),
	 * toState.onEnter(), in this order, are executed.
	 */

	/**
	 * Intent sent to the system when a transition occurs
	 * 
	 * The application may register this intent in a broadcast to react to state
	 * changes.<br/>
	 * The following keys are defined as intent extra data :
	 * <dl>
	 * <dt>{@link KEY_FROM_STATE}</dt>
	 * <dd>The state name the Runner is exiting from</dd>
	 * <dt>{@link KEY_TO_STATE}</dt>
	 * <dd>The state name the Runner is entering to</dd>
	 * <dt>{@link KEY_INTENT_TRANSITION}</dt>
	 * <dd>The intent that triggered the transition</dd>
	 * </dl>
	 */
	public final static String INTENT_AUTOMATON_STATE_CHANGE = "org.ymkm.lib.automaton.AUTOMATON_STATE_CHANGE";

	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : From state name
	 */
	public final static String KEY_FROM_STATE = "KEY_FROM_STATE";
	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : To state name
	 */
	public final static String KEY_TO_STATE = "KEY_TO_STATE";
	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : Intent that
	 * triggered transition
	 */
	public final static String KEY_INTENT_TRANSITION = "KEY_INTENT_TRANSITION";

	/**
	 * State flag for initial state. An IntentAutomaton must have exactly one
	 * state with this flag defined
	 */
	private static final int INITIAL_STATE = 0x0;
	/**
	 * State flag for final state. An IntentAutomaton must have AT LEAST one
	 * state with this flag defined
	 */
	private static final int FINAL_STATE = 0x1;
	/**
	 * State flag for default states. States that are neither initial nor final
	 * states gets this flag
	 */
	private static final int DEFAULT_STATE = 0x2;
	/**
	 * Maps State ID to State objects
	 */
	private Map<Integer, State> mStates;
	/**
	 * Maps State ID to List of Transitions
	 */
	private SparseArray<Map<Integer, List<Transition>>> mTransitions;
	/**
	 * Set of all intents declared in Transitions defined for this
	 * IntentAutomaton
	 */
	private Set<String> mIntents;

	private final static String TAG = IntentAutomaton.class.getCanonicalName();
	
	
	/**
	 * Represents a state in the automaton
	 * 
	 * States have a unique ID, a name and an optional enter/exit action.<br>
	 * <br>
	 * They can take either of these 3 types : INITIAL, FINAL or DEFAULT.
	 * <p>
	 * To circumvent the realtime, multithreaded nature of Android, notify
	 * actions may also be specified to perform some processing on a given state
	 * whenever the Runner gets paused/resumed.
	 * </p>
	 * <p>
	 * StateAction supplied to enter/exit/notify events may have a delay during
	 * their creation.<br>
	 * In that case, the action will be performed upon
	 * entering/exiting/notifying after the specified amount of time has
	 * elapsed.
	 * </p>
	 * 
	 * @author mikami-yoann
	 */
	private final static class State {
		private int mStateId;
		private String mName;
		private int mType;
		private StateAction mOnEnterAction;
		private StateAction mOnExitAction;

		/**
		 * Runnable that is ran in onEnter/onExit of a state
		 * 
		 * Stores the RunnerContext and the StateAction
		 * 
		 * @author mikami-yoann
		 */
		private final static class ActionRunnable implements Runnable {
			StateAction mStateAction;
			Runner.RunnerContext mContext;

			public ActionRunnable(final StateAction saction,
					Runner.RunnerContext context) {
				mStateAction = saction;
				mContext = context;
			}

			/**
			 * The run method wraps the StateAction's run method, but the latter
			 * needs a context, which cannot be given through normal Runnable.
			 */
			@Override
			public void run() {
				mStateAction.run(mContext);
			}
		}

		/**
		 * Creates a new state using given automaton as its parent, with
		 * specified ID and name
		 * 
		 * <p>
		 * It creates a normal state. To set this state as an initial state,
		 * {@code setInitialState(id)} must be called.
		 * </p>
		 * <p>
		 * Created state will have no enter / exit actions.
		 * </p>
		 * 
		 * @param parent
		 *            the automaton it gets attached to
		 * @param stateId
		 *            the ID. Must be unique
		 * @param name
		 *            the state name
		 */
		State(IntentAutomaton parent, int stateId, String name) {
			mStateId = stateId;
			mName = name;
			mType = DEFAULT_STATE;
		}

		/**
		 * Creates a new state using given automaton as its parent, with
		 * specified ID, name and enter action
		 * 
		 * <p>
		 * It creates a normal state. To set this state as an initial state,
		 * {@code setInitialState(id)} must be called.
		 * </p>
		 * <p>
		 * Specified enter action will be ran whenever this state is entered by
		 * a Runner.
		 * </p>
		 * <p>
		 * Created state will have no exit actions.
		 * </p>
		 * 
		 * @param parent
		 *            the automaton it gets attached to
		 * @param stateId
		 *            the ID. Must be unique
		 * @param name
		 *            the state name
		 * @param onEnterAction
		 *            the action to perform on enter
		 */
		State(IntentAutomaton parent, int stateId, String name,
				StateAction onEnterAction) {
			this(parent, stateId, name);
			mOnEnterAction = onEnterAction;
		}

		/**
		 * Creates a new state using given automaton as its parent, with
		 * specified ID, name and enter action
		 * 
		 * <p>
		 * It creates a normal state. To set this state as an initial state,
		 * {@code setInitialState(id)} must be called.
		 * </p>
		 * <p>
		 * Specified enter action will be ran whenever this state is entered by
		 * a Runner.<br>
		 * Specified exit action will be ran whenever this state is left by a
		 * Runner.
		 * </p>
		 * 
		 * @param parent
		 *            the automaton it gets attached to
		 * @param stateId
		 *            the ID. Must be unique
		 * @param name
		 *            the state name
		 * @param onEnterAction
		 *            the action to perform on enter
		 * @param onExitAction
		 *            the action to perform on exit
		 */
		State(IntentAutomaton parent, int stateId, String name,
				StateAction onEnterAction, StateAction onExitAction) {
			this(parent, stateId, name, onEnterAction);
			mOnExitAction = onExitAction;
		}

		/**
		 * Returns this state's ID
		 * 
		 * @return the state ID
		 */
		public int getStateId() {
			return mStateId;
		}

		/**
		 * Returns this state name
		 * 
		 * @return the state name
		 */
		public String getName() {
			return mName;
		}

		/**
		 * Returns this state type
		 * 
		 * @return INITIAL_STATE|FINAL_STATE|DEFAULT
		 */
		public int getType() {
			return mType;
		}

		/**
		 * Sets this state type
		 * 
		 * @param type
		 *            INITIAL_STATE|FINAL_STATE|DEFAULT
		 */
		public void setType(int type) {
			mType = type;
		}
		
		/**
		 * Returns the enter action, or null if none
		 * 
		 * @return StateAction on enter, or null
		 */
		public StateAction getEnterAction() {
			return mOnEnterAction;
		}

		/**
		 * Returns the exit action, or null if none
		 * 
		 * @return StateAction on exit, or null
		 */
		@SuppressWarnings("unused")
		public StateAction getExitAction() {
			return mOnExitAction;
		}
		
		/**
		 * Enter action of a state
		 * 
		 * <p>
		 * Called by a running {@link Runner} instance, passes in its
		 * {@link Handler} and the current {@link RunnerContext}.
		 * </p>
		 * 
		 * @param handler
		 *            the running Runner Handler
		 * @param rContext
		 *            the RunnerContext
		 * @return return value of {@link Handler#post}
		 */
		public boolean enter(Handler handler, Runner.RunnerContext rContext) {
			boolean ret = false;
			if (null != mOnEnterAction) {
				if (mOnEnterAction.getDelay() > 0) {
					ret = handler.postDelayed(new ActionRunnable(
							mOnEnterAction, rContext), mOnEnterAction
							.getDelay());
				} else {
					ret = handler.post(new ActionRunnable(mOnEnterAction,
							rContext));
				}
			}
			return ret;
		}

		/**
		 * Exit action of a state
		 * 
		 * <p>
		 * Called by a running {@link Runner} instance, passes in its
		 * {@link Handler} and the current {@link RunnerContext}.
		 * </p>
		 * 
		 * @param handler
		 *            the running Runner Handler
		 * @param rContext
		 *            the RunnerContext
		 * @return return value of {@link Handler#post}
		 */
		public boolean exit(Handler handler, Runner.RunnerContext rContext) {
			boolean ret = false;
			if (null != mOnExitAction) {
				if (mOnEnterAction.getDelay() > 0) {
					ret = handler.postDelayed(new ActionRunnable(mOnExitAction,
							rContext), mOnExitAction.getDelay());
				} else {
					ret = handler.post(new ActionRunnable(mOnExitAction,
							rContext));
				}
			}
			return ret;
		}
	}

	/**
	 * Represents a transition in the automaton
	 * 
	 * @author mikami-yoann
	 */
	private final static class Transition {
		private TransitionAction mTransitionAction;
		private TransitionGuard mGuard;
		private String mIntent;

		/**
		 * Runnable that is ran in action of a transition
		 * 
		 * Stores the RunnerContext, the Intent and the TransitionAction
		 * 
		 * @author mikami-yoann
		 */
		private final static class ActionRunnable implements Runnable {
			TransitionAction mTransitionAction;
			Intent mIntent;
			Runner.RunnerContext mContext;

			public ActionRunnable(final TransitionAction taction,
					final Intent intent, Runner.RunnerContext context) {
				mTransitionAction = taction;
				mIntent = intent;
				mContext = context;
			}

			/**
			 * The run method wraps the TransitionAction's run method, but the
			 * latter needs a context, which cannot be given through normal
			 * Runnable.
			 */
			@Override
			public void run() {
				mTransitionAction.run(mIntent, mContext);
			}
		}

		/**
		 * Creates a new Transaction reacting to given Intent and triggering
		 * specified TransitionAction
		 * 
		 * This transition is not guarded, ie. it will be taken whenever a
		 * matching intent will be broadcast.<br>
		 * Internally, it uses a {@link NullTransitionGuard} as its
		 * {@link TransitionGuard}.
		 * 
		 * @param intent
		 *            The intent to react to
		 * @param a
		 *            the action to perform on transition
		 */
		Transition(String intent, TransitionAction a) {
			mTransitionAction = a;
			mIntent = intent;
		}

		/**
		 * Creates a new Transaction reacting to given Intent and triggering
		 * specified TransitionAction if specified TransitionGuard is not
		 * guarded.
		 * 
		 * A transition will be taken if {@link TransitionGuard#guarded} returns
		 * <strong>false</strong>.
		 * 
		 * @param intent
		 *            The intent to react to
		 * @param a
		 *            the action to perform on transition
		 * @param g
		 *            the guard condition. Returns true if transition should not
		 *            be taken
		 */
		Transition(String intent, TransitionAction a, TransitionGuard g) {
			this(intent, a);
			mGuard = g;
		}

		/**
		 * Returns the intent registered with current transition
		 * 
		 * @return the intent
		 */
		public String getIntent() {
			return mIntent;
		}

		/**
		 * Returns true to guard the transition (i.e. transition not taken)
		 * 
		 * @param intent
		 *            Intent that wants to trigger the transition
		 * @param context
		 *            the runner context
		 * @return true if the transition is guarded (ie. should not be taken),
		 *         false otherwise
		 */
		public boolean guarded(Intent intent, Runner.RunnerContext context) {
			return null != mGuard && mGuard.predicate(intent, context);
		}

		/**
		 * Action of a transition
		 * 
		 * <p>
		 * Called by a running {@link Runner} instance, passes in its
		 * {@link Handler} and the current {@link RunnerContext}.
		 * </p>
		 * 
		 * @param intent
		 *            the intent that wants to trigger current transition
		 * @param handler
		 *            the running Runner Handler
		 * @param rContext
		 *            the RunnerContext
		 * @return return value of {@link Handler#post}
		 */
		public boolean action(Intent intent, Handler handler,
				Runner.RunnerContext rContext) {
			if (null == mTransitionAction) { return false; }
			if (mTransitionAction.getDelay() > 0) {
				return handler.postDelayed(new ActionRunnable(
						mTransitionAction, intent, rContext), mTransitionAction
						.getDelay());
			} else {
				return handler.post(new ActionRunnable(mTransitionAction,
						intent, rContext));
			}
		}
	}

	/**
	 * Defines an action to perform when entering/exiting a state
	 * 
	 * @author mikami-yoann
	 */
	public static abstract class StateAction {

		/* Delay in ms to activate this StateAction */
		private int mDelayInMs = 0;

		/**
		 * Creates a new StateAction, with no delay
		 */
		public StateAction() {
		}

		/**
		 * Creates a new StateAction with given delay
		 * 
		 * @param delay
		 *            in ms, the delay after which the action is performed
		 */
		public StateAction(int delay) {
			mDelayInMs = delay;
		}

		/**
		 * Returns the delay
		 * 
		 * @return in ms, the delay
		 */
		public int getDelay() {
			return mDelayInMs;
		}

		/**
		 * Subclasses must implement this, the action to perform on state action
		 * 
		 * @param context
		 *            the current runner context
		 */
		public abstract void run(Runner.RunnerContext context);
	};

	/**
	 * Defines an action to perform when traversing this transition
	 * 
	 * @author mikami-yoann
	 */
	public static abstract class TransitionAction {

		/* Delay in ms to activate this StateAction */
		private int mDelayInMs = 0;

		/**
		 * Creates a new TransitionAction, with no delay
		 */
		public TransitionAction() {
		}

		/**
		 * Creates a new TransitionAction with given delay
		 * 
		 * @param delay
		 *            in ms, the delay after which the action is performed
		 */
		public TransitionAction(int delay) {
			mDelayInMs = delay;
		}

		/**
		 * Returns the delay
		 * 
		 * @return in ms, the delay
		 */
		public int getDelay() {
			return mDelayInMs;
		}

		/**
		 * Subclasses must implement this, the action to perform on transition
		 * action
		 * 
		 * @param context
		 *            the current runner context
		 */
		public abstract void run(Intent intent, Runner.RunnerContext context);
	};

	/**
	 * Defines a guard condition for transitions
	 * 
	 * Guard : prevents from traversing the transition if the predicate returns
	 * true.
	 */
	public static abstract class TransitionGuard {

		/**
		 * Function that should check the guard conditions when taking the
		 * transition
		 * 
		 * @param intent
		 *            the intent that triggered the transition
		 * @param context
		 *            current runner context
		 * @return true if transition should not be taken, false otherwise
		 */
		public final boolean predicate(Intent intent,
				Runner.RunnerContext context) {
			return doPredicate(intent, context);
		}

		/**
		 * Subclasses must implement this
		 * 
		 * @see TransitionGuard#predicate
		 */
		protected abstract boolean doPredicate(Intent intent,
				Runner.RunnerContext context);
	}

	/**
	 * Default null transition guard (ie. does not have any guard constraints)
	 * 
	 * @author mikami-yoann
	 */
	public final static class NullTransitionGuard extends TransitionGuard {
		@Override
		protected boolean doPredicate(Intent intent,
				Runner.RunnerContext context) {
			return false;
		}
	};

	/**
	 * Transition guard that returns true if the decorated guard returns false
	 * (Inverts)
	 * 
	 * @author mikami-yoann
	 */
	public final static class NotTransitionGuard extends TransitionGuard {

		private TransitionGuard mGuard;

		NotTransitionGuard(TransitionGuard g) {
			mGuard = g;
		}

		@Override
		protected boolean doPredicate(Intent intent,
				Runner.RunnerContext context) {
			return !mGuard.predicate(intent, context);
		}
	}

	/**
	 * Transition guard that returns true if both decorated guard return true
	 * 
	 * @author mikami-yoann
	 */
	public final static class AndTransitionGuard extends TransitionGuard {

		private TransitionGuard mGuard1;
		private TransitionGuard mGuard2;

		public AndTransitionGuard(TransitionGuard g1, TransitionGuard g2) {
			mGuard1 = g1;
			mGuard2 = g2;
		}

		@Override
		protected boolean doPredicate(Intent intent,
				Runner.RunnerContext context) {
			return (mGuard1.predicate(intent, context) || mGuard2.predicate(
					intent, context));
		}
	}

	/**
	 * Base IntentAutomaton exception
	 * 
	 * @author mikami-yoann
	 */
	public abstract static class IntentAutomatonException extends Exception {
		private static final long serialVersionUID = -2972101806117136770L;

		public IntentAutomatonException(String string) {
			super(string);
		}
	};

	/**
	 * IntentAutomaton exception for state errors (static errors)
	 * 
	 * @author mikami-yoann
	 */
	public final static class IntentAutomatonStateException extends
			IntentAutomatonException {
		private static final long serialVersionUID = -7043422581164886882L;

		public IntentAutomatonStateException(String string) {
			super(string);
		}
	};

	/**
	 * IntentAutomaton exception related to a runner (dynamic errors)
	 * 
	 * @author mikami-yoann
	 */
	public final static class IntentAutomatonRunnerException extends
			IntentAutomatonException {
		private static final long serialVersionUID = 4715439559246329424L;

		public IntentAutomatonRunnerException(String string) {
			super(string);
		}
	};

	/**
	 * Defines a runner that can walk through an automaton
	 * 
	 * An IntentAutomaton is stateless; a Runner is stateful. Thus, the same
	 * IntentAutomaton can be walked through using different Runners
	 * independently and concurrently, all managing their own states.
	 * <p>
	 * Each runner runs in its own thread, is managed using a Handler, and
	 * registers a BroadcastReceiver for all Intents listened by
	 * {@link Transition}s defined in managed {@link IntentAutomaton}.
	 * </p>
	 * <p>
	 * Each Runner can independently be started/paused/stopped/restarted, thus
	 * making them usable inside Activities, Fragments, or any other Android
	 * module that has such lifecycle.
	 * </p>
	 * <p>
	 * Runner can also save their own states into supplied Bundles
	 * (save/restoreInstanceState), provided the latter are called by the
	 * Activity/Fragment that manages it.<br>
	 * Current state can thus be restored at a later stage even if
	 * Activity/Fragment is destroyed/recreated.
	 * </p>
	 * 
	 * @author mikami-yoann
	 */
	public final static class Runner {

		/**
		 * RunnerContext provides with a shared storage among all states of an
		 * automaton
		 * 
		 * Each runner instance holds a RunnerContext instance, which gets
		 * passed to every state/transition action of the automaton it is
		 * running onto.<br>
		 * This way, it is possible to keep track of data shared among different
		 * states/transitions (e.g. in {@link TransitionGuard}, one could handle
		 * different cases based on values inside the context).
		 * 
		 * @author mikami-yoann
		 * 
		 */
		public final static class RunnerContext {
			private Map<String, Object> mContextValues;

			public RunnerContext() {
				mContextValues = new ConcurrentHashMap<String, Object>();
			}

			public void setInt(String key, int value) {
				mContextValues.put(key, Integer.valueOf(value));
			}

			public void setLong(String key, long value) {
				mContextValues.put(key, Long.valueOf(value));
			}

			public void setString(String key, String value) {
				mContextValues.put(key, value);
			}

			public void setFloat(String key, float value) {
				mContextValues.put(key, Float.valueOf(value));
			}

			public void setBoolean(String key, boolean value) {
				mContextValues.put(key, Boolean.valueOf(value));
			}

			public void setObject(String key, Object value) {
				mContextValues.put(key, value);
			}

			public int getInt(String key, int defValue) {
				if (mContextValues.containsKey(key)) {
					return (Integer) mContextValues.get(key);
				}
				return defValue;
			}

			public long getLong(String key, long defValue) {
				if (mContextValues.containsKey(key)) {
					return (Long) mContextValues.get(key);
				}
				return defValue;
			}

			public String getString(String key, String defValue) {
				if (mContextValues.containsKey(key)) {
					return (String) mContextValues.get(key);
				}
				return defValue;
			}

			public float getFloat(String key, float defValue) {
				if (mContextValues.containsKey(key)) {
					return (Float) mContextValues.get(key);
				}
				return defValue;
			}

			public boolean getBoolean(String key, boolean defValue) {
				if (mContextValues.containsKey(key)) {
					return (Boolean) mContextValues.get(key);
				}
				return defValue;
			}

			public Object getObject(String key, Object defValue) {
				if (mContextValues.containsKey(key)) {
					return mContextValues.get(key);
				}
				return defValue;
			}

			public void remove(String key) {
				if (mContextValues.containsKey(key)) {
					mContextValues.remove(key);
				}
			}
		};

		// The only message handled by the Runner Handler.
		private final static int MSG_INTENT_RECEIVED = 0x10;

		private IntentAutomaton mIntentAutomatonInstance;
		private int mCurrentState;
		private String mAutomatonName;
		private boolean mStarted = false;
		private boolean mPaused = true;
		private Context mContext;
		private Handler mIntentAutomatonHandler;
		private HandlerThread _thread;

		/**
		 * RunnerContext for this Runner
		 */
		public RunnerContext runnerContext;

		/**
		 * Creates a new Runner on specified {@link IntentAutomaton} given
		 * specified {@link Context}
		 * 
		 * <p>
		 * It performs a check for initial state on given automaton, and will
		 * throw an exception if there's none, or if multiple initial states are
		 * defined.
		 * </p>
		 * 
		 * @param context
		 *            the context to register BroadcastReceiver to
		 * @param g
		 *            the automaton to manage
		 * @throws IntentAutomatonStateException
		 *             if errors are found in the automaton
		 */
		private Runner(Context context, IntentAutomaton g)
				throws IntentAutomatonStateException {
			mIntentAutomatonInstance = g;
			mCurrentState = -1;
			mContext = context;
			runnerContext = new RunnerContext();
			init();
		}

		/**
		 * Creates a new Runner on specified {@link IntentAutomaton} given
		 * specified {@link Context}, using given RunnerContext
		 * 
		 * <p>
		 * It performs a check for initial state on given automaton, and will
		 * throw an exception if there's none, or if multiple initial states are
		 * defined.
		 * </p>
		 * 
		 * @param context
		 *            the context to register BroadcastReceiver to
		 * @param g
		 *            the automaton to manage
		 * @throws IntentAutomatonStateException
		 *             if errors are found in the automaton
		 */
		private Runner(Context context, IntentAutomaton g,
				RunnerContext rContext) throws IntentAutomatonStateException {
			mIntentAutomatonInstance = g;
			mCurrentState = -1;
			mContext = context;
			runnerContext = rContext;
			init();
		}

		// Checks for initial state on automaton
		private void init() throws IntentAutomatonStateException {
			boolean initialStateFound = false;
			State initialState = null;
			for (Entry<Integer, State> s : mIntentAutomatonInstance.mStates
					.entrySet()) {
				if (INITIAL_STATE == s.getValue().getType()) {
					if (initialStateFound) {
						throw new IntentAutomatonStateException(
								"IntentAutomaton cannot have more than one initial state");
					}

					initialStateFound = true;
					initialState = s.getValue();
				}
			}
			if (!initialStateFound) {
				throw new IntentAutomatonStateException(
						"IntentAutomaton must have an initial state");
			}
			mCurrentState = initialState.getStateId();
		}

		/**
		 * Sets runner name
		 * 
		 * @param name
		 *            the name to set for current Runner
		 */
		public void setName(String name) {
			mAutomatonName = name;
		}

		/**
		 * Restores internal state of current Runner
		 * 
		 * <p>
		 * May be called with a valid Bundle previously passed to
		 * {@link Runner#saveInstanceState}
		 * </p>
		 * 
		 * @param savedInstanceState
		 */
		public void restoreInstanceState(Bundle savedInstanceState) {
			if (null != savedInstanceState) {
				mCurrentState = savedInstanceState.getInt("mAutomatonRunner_"
						+ mAutomatonName + "_mCurrentState");
				mStarted = savedInstanceState.getBoolean("mAutomatonRunner_"
						+ mAutomatonName + "_mStarted");
				mPaused = savedInstanceState.getBoolean("mAutomatonRunner_"
						+ mAutomatonName + "_mPaused");
			}
		}

		/**
		 * Saves current Runner state to specified Bundle
		 * 
		 * <p>
		 * May be called by Activities or Fragments in the equivalent methods to
		 * react to lifecycle events.
		 * </p>
		 * 
		 * @param outState
		 */
		public void saveInstanceState(Bundle outState) {
			outState.putInt("mAutomatonRunner_" + mAutomatonName
					+ "_mCurrentState", mCurrentState);
			outState.putBoolean("mAutomatonRunner_" + mAutomatonName
					+ "_mStarted", mStarted);
			outState.putBoolean("mAutomatonRunner_" + mAutomatonName
					+ "_mPaused", mPaused);
		}

		/**
		 * If not started, starts the current Runner and enters initial state of
		 * the automaton
		 * 
		 * <p>
		 * A new thread is spawned, under which Runner's Handler gets its Looper
		 * from.
		 * </p>
		 */
		public void start() {
			synchronized (this) {
				if (!mStarted) {
					_thread = new HandlerThread(
							"IntentAutomaton runner instance");
					_thread.start();
					mIntentAutomatonHandler = new Handler(_thread.getLooper(),
							_callback);
				}
				restart();
				if (!mStarted) {
					mIntentAutomatonInstance.mStates.get(mCurrentState).enter(
							mIntentAutomatonHandler, runnerContext);
				}
				mStarted = true;
			}
		}

		/**
		 * Is current Runner started
		 * 
		 * @return true if started, false otherwise
		 */
		public boolean isStarted() {
			return mStarted;
		}

		/**
		 * Is current Runner paused
		 * 
		 * @return true if not started, or if {@link Runner#pause()} was
		 *         previously called
		 */
		public boolean isPaused() {
			return mStarted && mPaused;
		}

		/**
		 * Is current Runner running
		 * 
		 * @return true if started and not paused
		 */
		public boolean isRunning() {
			return mStarted && !mPaused;
		}

		/**
		 * Is current Runner stopped
		 * 
		 * @return true if not started and paused (Runner{@link #stop()} was
		 *         called)
		 */
		public boolean isStopped() {
			return !mStarted && mPaused;
		}

		/**
		 * Forces enter action to be reran on current state
		 */
		public void reenter() {
			mIntentAutomatonInstance.mStates.get(mCurrentState).enter(
					mIntentAutomatonHandler, runnerContext);
		}

		/**
		 * Restarts current Runner
		 * 
		 * <p>
		 * can Resume a Runner after a previous call to {@link Runner#pause()},
		 * or during {@link Runner#start()} to start the BroadcastReceiver.
		 * </p>
		 */
		public void restart() {
			synchronized (this) {
				IntentFilter ifilters = new IntentFilter();
				for (String intent : mIntentAutomatonInstance.mIntents) {
					ifilters.addAction(intent);
				}
				if (mPaused) {
					mContext.registerReceiver(_receiver, ifilters);
					mPaused = false;
				}
			}
		}

		/**
		 * Stops current Runner
		 * 
		 * <p>
		 * Does nothing is Runner was not started.<br>
		 * Stopping a Runner will kill its internal thread, and resets all
		 * internal states.<br>
		 * <strong>A stopped Runner cannot be resumed!!</strong><br>
		 * A stopped Runner can be started again; in this case, current state
		 * will be reset to the initial state.<br>
		 * RunnerContext will not be reset.
		 * </p>
		 */
		public void stop() {
			synchronized (this) {
				pause();
				if (mStarted) {
					HandlerThread ht = _thread;
					_thread = null;
					if (null != ht) {
						ht.quit();
						ht.interrupt();
					}
				}
				mStarted = false;
				mPaused = true;
			}
		}

		/**
		 * Pauses current Runner
		 * 
		 * <p>
		 * Pausing a running Runner will unregister its BroadcastReceiver, until
		 * {@link Runner#restart()} is called.<br>
		 * Calling pause on a non running, non started Runner has no effect.
		 * </p>
		 */
		public void pause() {
			synchronized (this) {
				if (!mPaused) {
					mContext.unregisterReceiver(_receiver);
				}
				mPaused = true;
			}
		}

		/**
		 * Transitions manually.
		 * 
		 * @param to state ID to transition to
		 */
		public void transition(Intent intent) {
			mIntentAutomatonHandler
					.obtainMessage(MSG_INTENT_RECEIVED, intent)
					.sendToTarget();

		}

		// BroadcastReceiver : intercepts Intents watched by Transitions,
		// then delegate to internal Handler.
		private BroadcastReceiver _receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				mIntentAutomatonHandler
						.obtainMessage(MSG_INTENT_RECEIVED, arg1)
						.sendToTarget();
			}
		};

		// This is the core of the Runner process :
		// Each time an intent is intercepted by the BroadcastReceiver, a new
		// message
		// is posted to this Handler.
		// It compares it to Intents registered by outgoing Transitions of
		// current state
		// and tries to traverse the first one it finds whose TransitionGuard
		// returns false.
		private class IntentAutomatonRunnerHandler implements Handler.Callback {
			@Override
			public boolean handleMessage(Message msg) {

				switch (msg.what) {
				case MSG_INTENT_RECEIVED:
					Intent intent = (Intent) msg.obj;
					Log.d(TAG, "Intent received : " + intent.getAction());
					Map<Integer, List<Transition>> trList = mIntentAutomatonInstance.mTransitions
							.get(mCurrentState);

					if (null != trList && 0 < trList.size()) {
						for (Entry<Integer, List<Transition>> trs : trList
								.entrySet()) {
							for (Transition tr : trs.getValue()) {
								// Received intent matches expected intent for
								// this transition
								// Plus, the guard returned false : we perform
								// the transition
								if (intent.getAction().equals(tr.getIntent())) {
									State from = mIntentAutomatonInstance.mStates
											.get(mCurrentState);
									State to = mIntentAutomatonInstance.mStates
											.get(trs.getKey());

									if (null == from || null == to) {
										return false;
									}

									Log.d(TAG, "> Checking if transition " + from.getName() + " -> " + to.getName() + " is guarded");

									if (tr.guarded(intent, runnerContext)) {
										Log.d(TAG, ">> Transition guarded. Checking next");
										continue;
									}

									// Transition while not in final state
									if (FINAL_STATE != from.getType()) {
										
										Log.d(TAG, ">> Transition not guarded, applying " + from.getName() + " -> " + to.getName());
										
										from.exit(mIntentAutomatonHandler,
												runnerContext);
										tr.action(intent,
												mIntentAutomatonHandler,
												runnerContext);
										to.enter(mIntentAutomatonHandler,
												runnerContext);
										mCurrentState = to.getStateId();

										Intent i = new Intent(
												INTENT_AUTOMATON_STATE_CHANGE);
										i.putExtra(KEY_FROM_STATE,
												from.getName());
										i.putExtra(KEY_TO_STATE, to.getName());
										i.putExtra(KEY_INTENT_TRANSITION,
												tr.getIntent());
										mContext.sendBroadcast(i);
									}

									if (FINAL_STATE == to.getType()) {
										mIntentAutomatonHandler
												.postDelayed(new Runnable() {
													@Override
													public void run() {
														stop();
													}
												}, (null != to.getEnterAction())?(to.getEnterAction().getDelay()+1):0);
									}

									return true;
								}
							}
						}
					}

					Log.d(TAG, "Automaton state after intent handling : " + mIntentAutomatonInstance.mStates.get(mCurrentState));
				}

				return false;
			}
		};

		private final IntentAutomatonRunnerHandler _callback = new IntentAutomatonRunnerHandler();
	};

	/**
	 * Creates a new Runner to walk through this IntentAutomaton
	 * 
	 * @see Runner#Runner(Context, IntentAutomaton)
	 * @param context
	 * @param g
	 * @return The new Runner instance
	 * @throws IntentAutomatonStateException
	 */
	public static Runner instantiate(Context context, IntentAutomaton g)
			throws IntentAutomatonStateException {
		return new Runner(context, g);
	}

	/**
	 * Creates a new Runner to walk through this IntentAutomaton, by supplying
	 * the RunnerContext it will use instead of letting it create its own.
	 * 
	 * @see Runner#Runner(Context, IntentAutomaton,
	 *      org.ymkm.lib.automaton.IntentAutomaton.Runner.RunnerContext)
	 * @param context
	 * @param g
	 * @param rContext
	 * @return The new Runner instance
	 * @throws IntentAutomatonStateException
	 */
	public static Runner instantiate(Context context, IntentAutomaton g,
			Runner.RunnerContext rContext) throws IntentAutomatonStateException {
		return new Runner(context, g, rContext);
	}

	/**
	 * Creates a new IntentAutomaton
	 */
	public IntentAutomaton() {
		mStates = new HashMap<Integer, State>();
		mTransitions = new SparseArray<Map<Integer, List<Transition>>>();
		mIntents = new TreeSet<String>();
	}

	/*
	 * Public API to define states/transitions on the automaton
	 */

	/**
	 * Adds a state with specified ID and name. No enter/exit state action
	 * 
	 * @see IntentAutomaton#addState(int, String, StateAction, StateAction)
	 * @param stateId
	 *            the ID uniquely defining this state in current automaton
	 * @param name
	 *            the automaton name
	 * @throws IntentAutomatonStateException
	 *             if a state with the same ID was previously added
	 */
	public void addState(int stateId, String name)
			throws IntentAutomatonStateException {
		addState(stateId, name, null, null);
	}

	/**
	 * Adds a state with specified ID and name. No exit state action
	 * 
	 * @see IntentAutomaton#addState(int, String, StateAction, StateAction)
	 * @param stateId
	 *            the ID uniquely defining this state in current automaton
	 * @param name
	 *            the automaton name
	 * @throws IntentAutomatonStateException
	 *             if a state with the same ID was previously added
	 */
	public void addState(int stateId, String name, StateAction onEnterAction)
			throws IntentAutomatonStateException {
		addState(stateId, name, onEnterAction, null);
	}

	/**
	 * Adds a state with specified ID and name. Defines enter/exit action
	 * 
	 * @param stateId
	 *            the ID uniquely defining this state in current automaton
	 * @param name
	 *            the automaton name
	 * @throws IntentAutomatonStateException
	 *             if a state with the same ID was previously added
	 */
	public void addState(int stateId, String name, StateAction onEnterAction,
			StateAction onExitAction) throws IntentAutomatonStateException {
		State s;

		if (mStates.containsKey(stateId)) {
			throw new IntentAutomatonStateException("Error while adding state "
					+ name + " : ID " + stateId + " already defined as name + "
					+ mStates.get(stateId).getName());
		}

		if (null != onEnterAction && null != onExitAction) {
			s = new State(this, stateId, name, onEnterAction, onExitAction);
		} else if (null != onEnterAction) {
			s = new State(this, stateId, name, onEnterAction);
		} else {
			s = new State(this, stateId, name);
		}
		mStates.put(stateId, s);
	}

	/**
	 * Defines a new transition linking "from" state to "to" state with given
	 * TransitionAction, reacting to specified Intent
	 * 
	 * <p>
	 * No {@link TransitionGuard} is defined for this Transition.<br>
	 * Internally, it translates to a {@link NullTransitionGuard}.
	 * </p>
	 * 
	 * @see IntentAutomaton#addTransition(int, int, String, TransitionAction,
	 *      TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param intent
	 *            the Intent the Transition reacts to
	 * @param onTransitionAction
	 *            the TransitionAction to perform during transition
	 */
	public void addTransition(int from, int to, String intent,
			TransitionAction onTransitionAction) {
		addTransition(from, to, intent, onTransitionAction,
				new NullTransitionGuard());
	}
	
	/**
	 * Defines a new transition linking "from" state to "to" state,
	 * reacting to specified Intent. Defines a TransitionGuard.
	 * No action.
	 * 
	 * <p>
	 * The transition will not be taken by the Runner if specified
	 * TransitionGuard returns true at the time of calling.
	 * </p>
	 * 
	 * @see IntentAutomaton#addTransition(int, int, String, TransitionAction,
	 *      TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param intent
	 *            the Intent the Transition reacts to
	 * @param g
	 *            the TransitionGuard to check before traversing
	 */
	public void addTransition(int from, int to, String intent, TransitionGuard g) {
		Map<Integer, List<Transition>> m;
		List<Transition> l;
		if (mTransitions.indexOfKey(from) < 0) {
			l = new ArrayList<Transition>();
			m = new HashMap<Integer, List<Transition>>();
			m.put(to, l);
			mTransitions.put(from, m);
		} else {
			m = mTransitions.get(from);
			if (!m.containsKey(to)) {
				l = new ArrayList<Transition>();
				m.put(to, l);
			} else {
				l = m.get(to);
			}
		}
		l.add(new Transition(intent, null, g));

		// Add this intent to the list of intents to listen to
		mIntents.add(intent);
	}

	/**
	 * Defines a new transition linking "from" state to "to" state with given
	 * TransitionAction, reacting to specified Intent. Defines a
	 * TransitionGuard.
	 * 
	 * <p>
	 * The transition will not be taken by the Runner if specified
	 * TransitionGuard returns true at the time of calling.
	 * </p>
	 * 
	 * @see IntentAutomaton#addTransition(int, int, String, TransitionAction,
	 *      TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param intent
	 *            the Intent the Transition reacts to
	 * @param onTransitionAction
	 *            the TransitionAction to perform during transition
	 * @param g
	 *            the TransitionGuard to check before traversing
	 */
	public void addTransition(int from, int to, String intent,
			TransitionAction onTransitionAction, TransitionGuard g) {
		Map<Integer, List<Transition>> m;
		List<Transition> l;
		if (mTransitions.indexOfKey(from) < 0) {
			l = new ArrayList<Transition>();
			m = new HashMap<Integer, List<Transition>>();
			m.put(to, l);
			mTransitions.put(from, m);
		} else {
			m = mTransitions.get(from);
			if (!m.containsKey(to)) {
				l = new ArrayList<Transition>();
				m.put(to, l);
			} else {
				l = m.get(to);
			}
		}
		l.add(new Transition(intent, onTransitionAction, g));

		// Add this intent to the list of intents to listen to
		mIntents.add(intent);
	}

	/**
	 * Defines a new transition linking "from" state to "to" state,
	 * reacting to specified Intent. No TransitionGuard. No action.
	 * 
	 * @see IntentAutomaton#addTransition(int, int, String, TransitionAction,
	 *      TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param intent
	 *            the Intent the Transition reacts to
	 */
	public void addTransition(int from, int to, String intent) {
		Map<Integer, List<Transition>> m;
		List<Transition> l;
		if (mTransitions.indexOfKey(from) < 0) {
			l = new ArrayList<Transition>();
			m = new HashMap<Integer, List<Transition>>();
			m.put(to, l);
			mTransitions.put(from, m);
		} else {
			m = mTransitions.get(from);
			if (!m.containsKey(to)) {
				l = new ArrayList<Transition>();
				m.put(to, l);
			} else {
				l = m.get(to);
			}
		}
		l.add(new Transition(intent, null, null));

		// Add this intent to the list of intents to listen to
		mIntents.add(intent);
	}
	
	/**
	 * Sets the initial state of current automaton. Must have one and only one!
	 * 
	 * <p>
	 * The specified ID must be previously added to the automaton before this
	 * method is called.
	 * </p>
	 * 
	 * @param stateId
	 *            the state ID of the state to set as initial state.
	 */
	public void setInitialState(int stateId) {
		if (mStates.containsKey(stateId)) {
			mStates.get(stateId).setType(INITIAL_STATE);
		}
	}

	/**
	 * Sets a final state on this Automaton. Can have multiple or none.
	 * 
	 * <p>
	 * The specified ID must be previously added to the automaton before this
	 * method is called.
	 * </p>
	 * 
	 * @param stateId
	 *            the state ID of the state to set as a final state.
	 */
	public void setFinalState(int stateId) {
		if (mStates.containsKey(stateId)) {
			mStates.get(stateId).setType(FINAL_STATE);
		}
	}
}

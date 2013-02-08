package org.ymkm.lib.automaton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.ymkm.lib.automaton.Automaton.Runner.RunnerContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

/**
 * Defines a simple finite-state-machine (FSM) with transitions triggered by messages defined as integers.
 * 
 * <p>
 * A FSM is composed of States and Transitions.<br>
 * A Transition links two states in the FSM, the source state and the target state.<br>
 * Two special states are typically found in a FSM : The initial state and the final state(s).<br>
 * While there can be multiple final states, only one initial state is permitted.
 * </p>
 * <p>
 * An {@code Automaton} is an abstract representation of a FSM, but does not do anything by itself; it needs a
 * {@link Automaton#Runner}, which represents a running instance of a given FSM.<br>
 * In other words, multiple Runner may run simultaneously on the same FSM; each will store the current state, and any
 * other contextual data (see {@link Automaton.Runner.RunnerContext}), while the {@code Automaton} itself is mostly
 * stateless.
 * </p>
 * <p>
 * A State is defined by at least a state ID (UNIQUE in a given automaton), a name (For readability/debugging).<br>
 * Optionally, a {@link StateAction} can be supplied for either or both onEnter and onExit events;<br>
 * onEnter event will run the specified StateAction whenever a transition leads to it, while onExit will run the
 * specified StateAction whenever a transition leaves from it to another state.
 * </p>
 * <p>
 * A Transition is defined by at least a from state ID X, a to state ID Y and a message ID as a long it will react to.<br>
 * This tells the automaton that whenever the message long is sent to it, it needs t move to state Y if the runner's
 * current state is X.<br>
 * Optionally a {@link TransitionAction} may be supplied to a given Transition : <br>
 * This will be called whenever the transition it is attached to is taken, irrespective of from/to states.<br>
 * In addition, a {@link TransitionGuard} may be applied to a Transition :<br>
 * This prevents the Runner to take the transition matching the from state and message if the
 * {@link TransitionGuard#guarded} function returns {@code true}.
 * </p>
 * <p>
 * A given state can have multiple transitions linking to several states. While guards set on transitions should ideally
 * be mutually exclusive, the system does not enforce this, and will take the first transition it finds that it can
 * take.<br>
 * <p>
 * Transitions are triggered through the {@linkplain Runner#send(long)} method, which sends a message to a given runner
 * instance.<br>
 * Messages are sent to Runners, which means that should multiple runners reacting to the same message exist, each
 * should be called one by one.
 * </p>
 * <p>
 * <strong>Note :</strong> Incidentally, it is possible to create a FSM in which a Runner gets stuck into if all
 * transitions from current state have TransitionGuard that all return true.<br>
 * Special care must thus be taken when designing FSM with Transition/TransitionGuard leaving from the same state.
 * </p>
 * <p>
 * Multiple {@link Runner} instances can process the same {@code Automaton} ; Each new instance will yield a new
 * {@link RunnerContext} which gets passed to each {@link StateAction}, {@link TransitionAction},
 * {@link TransitionGuard} callback methods; all instance-specific states that needs to be propagated throughout the
 * whole run can be stored in the context using a simple key-value storage.<br>
 * </p>
 * 
 * @author yoann@ymkm.org
 * 
 */
@SuppressLint("UseSparseArrays")
public class Automaton {

	/**
	 * Intent sent to the system when a transition occurs
	 * 
	 * The application may register this intent in a broadcast to react to state changes.<br/>
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
	public final static String AUTOMATON_STATE_CHANGE = "org.ymkm.lib.automaton.AUTOMATON_STATE_CHANGE";

	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : From state name
	 */
	public final static String KEY_FROM_STATE = "KEY_FROM_STATE";
	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : To state name
	 */
	public final static String KEY_TO_STATE = "KEY_TO_STATE";
	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : message ID (long) that triggered transition
	 */
	public final static String KEY_MESSAGE_ID = "KEY_MESSAGE_ID";
	/**
	 * Extra data for {@link INTENT_AUTOMATON_STATE_CHANGE} : runner instance ID that triggered transition
	 */
	public final static String KEY_RUNNER_INSTANCE_ID = "KEY_RUNNER_INSTANCE_ID";

	/**
	 * State flag for initial state. An Automaton must have exactly one state with this flag defined
	 */
	private static final int INITIAL_STATE = 0x0;
	/**
	 * State flag for final state. An Automaton must have AT LEAST one state with this flag defined
	 */
	private static final int FINAL_STATE = 0x1;
	/**
	 * State flag for default states. States that are neither initial nor final states gets this flag
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
	 * Set of all message IDs declared in Transitions defined for this Automaton
	 */
	private Set<Long> mMessages;

	private final static String TAG = Automaton.class.getCanonicalName();

	/**
	 * Represents a state in the automaton
	 * 
	 * States have a unique ID, a name and an optional enter/exit action.<br>
	 * <br>
	 * They can take either of these 3 types : INITIAL, FINAL or DEFAULT.
	 * <p>
	 * StateAction supplied to enter/exit/notify events may have a delay during their creation.<br>
	 * In that case, the action will be performed upon entering/exiting/notifying after the specified amount of time has
	 * elapsed.
	 * </p>
	 * 
	 * @author yoann@ymkm.org
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
		 */
		private final static class ActionRunnable implements Runnable {

			StateAction mStateAction;
			Runner.RunnerContext mContext;

			public ActionRunnable(final StateAction saction, Runner.RunnerContext context) {
				mStateAction = saction;
				mContext = context;
			}

			/**
			 * The run method wraps the StateAction's run method, but the latter needs a context, which cannot be given
			 * through normal Runnable.
			 */
			@Override
			public void run() {
				mStateAction.run(mContext);
			}
		}

		/**
		 * Creates a new state using given automaton as its parent, with specified ID and name
		 * 
		 * <p>
		 * It creates a normal state. To set this state as an initial state, {@code setInitialState(id)} must be called.
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
		State(Automaton parent, int stateId, String name) {
			mStateId = stateId;
			mName = name;
			mType = DEFAULT_STATE;
		}

		/**
		 * Creates a new state using given automaton as its parent, with specified ID, name and enter action
		 * 
		 * <p>
		 * It creates a normal state. To set this state as an initial state, {@code setInitialState(id)} must be called.
		 * </p>
		 * <p>
		 * Specified enter action will be ran whenever this state is entered by a Runner.
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
		State(Automaton parent, int stateId, String name, StateAction onEnterAction) {
			this(parent, stateId, name);
			mOnEnterAction = onEnterAction;
		}

		/**
		 * Creates a new state using given automaton as its parent, with specified ID, name and enter action
		 * 
		 * <p>
		 * It creates a normal state. To set this state as an initial state, {@code setInitialState(id)} must be called.
		 * </p>
		 * <p>
		 * Specified enter action will be ran whenever this state is entered by a Runner.<br>
		 * Specified exit action will be ran whenever this state is left by a Runner.
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
		State(Automaton parent, int stateId, String name, StateAction onEnterAction, StateAction onExitAction) {
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
		 * Called by a running {@link Runner} instance, passes in its {@link Handler} and the current
		 * {@link RunnerContext}.
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
					ret = handler.postDelayed(new ActionRunnable(mOnEnterAction, rContext), mOnEnterAction.getDelay());
				}
				else {
					ret = handler.post(new ActionRunnable(mOnEnterAction, rContext));
				}
			}
			return ret;
		}

		/**
		 * Exit action of a state
		 * 
		 * <p>
		 * Called by a running {@link Runner} instance, passes in its {@link Handler} and the current
		 * {@link RunnerContext}.
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
					ret = handler.postDelayed(new ActionRunnable(mOnExitAction, rContext), mOnExitAction.getDelay());
				}
				else {
					ret = handler.post(new ActionRunnable(mOnExitAction, rContext));
				}
			}
			return ret;
		}
	}

	/**
	 * Represents a transition in the automaton
	 * 
	 * @author yoann@ymkm.org
	 */
	private final static class Transition {

		private TransitionAction mTransitionAction;
		private TransitionGuard mGuard;
		private long mMessage;

		/**
		 * Runnable that is ran in action of a transition
		 * 
		 * Stores the RunnerContext, the message ID and the TransitionAction
		 */
		private final static class ActionRunnable implements Runnable {

			TransitionAction mTransitionAction;
			long mMessage;
			Runner.RunnerContext mContext;

			public ActionRunnable(final TransitionAction taction, final long message, Runner.RunnerContext context) {
				mTransitionAction = taction;
				mMessage = message;
				mContext = context;
			}

			/**
			 * The run method wraps the TransitionAction's run method, but the latter needs a context, which cannot be
			 * given through normal Runnable.
			 */
			@Override
			public void run() {
				mTransitionAction.run(mMessage, mContext);
			}
		}

		/**
		 * Creates a new Transaction reacting to given message long and triggering specified TransitionAction
		 * 
		 * This transition is not guarded, i.e. it will be taken whenever a matching long is sent to the automaton.<br>
		 * Internally, it uses a {@link NullTransitionGuard} as its {@link TransitionGuard}.
		 * 
		 * @param message
		 *            The message to react to
		 * @param a
		 *            the action to perform on transition
		 */
		Transition(long message, TransitionAction a) {
			mTransitionAction = a;
			mMessage = message;
		}

		/**
		 * Creates a new Transaction reacting to given message long and triggering specified TransitionAction if
		 * specified TransitionGuard is not guarded.
		 * 
		 * A transition will be taken if {@link TransitionGuard#guarded} returns <strong>false</strong>.
		 * 
		 * @param message
		 *            The message to react to
		 * @param a
		 *            the action to perform on transition
		 * @param g
		 *            the guard condition. Returns true if transition should not be taken
		 */
		Transition(long message, TransitionAction a, TransitionGuard g) {
			this(message, a);
			mGuard = g;
		}

		/**
		 * Returns the message long registered with current transition
		 * 
		 * @return the message ID
		 */
		public long getMessage() {
			return mMessage;
		}

		/**
		 * Returns true to guard the transition (i.e. transition not taken)
		 * 
		 * @param message
		 *            Message that wants to trigger the transition
		 * @param context
		 *            the runner context
		 * @return true if the transition is guarded (i.e. should not be taken), false otherwise
		 */
		public boolean guarded(long message, Runner.RunnerContext context) {
			return null != mGuard && mGuard.predicate(message, context);
		}

		/**
		 * Action of a transition
		 * 
		 * <p>
		 * Called by a running {@link Runner} instance, passes in its {@link Handler} and the current
		 * {@link RunnerContext}.
		 * </p>
		 * 
		 * @param message
		 *            the message that wants to trigger current transition
		 * @param handler
		 *            the running Runner Handler
		 * @param rContext
		 *            the RunnerContext
		 * @return return value of {@link Handler#post}
		 */
		public boolean action(long message, Handler handler, Runner.RunnerContext rContext) {
			if (null == mTransitionAction) { return false; }
			if (mTransitionAction.getDelay() > 0) {
				return handler.postDelayed(new ActionRunnable(mTransitionAction, message, rContext),
						mTransitionAction.getDelay());
			}
			else {
				return handler.post(new ActionRunnable(mTransitionAction, message, rContext));
			}
		}
	}

	/**
	 * Defines an action to perform when entering/exiting a state
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
		 * Subclasses must implement this, the action to perform on transition action
		 * 
		 * @param message
		 *            the message that triggers the action
		 * @param context
		 *            the current runner context
		 */
		public abstract void run(long message, Runner.RunnerContext context);
	};

	/**
	 * Defines a guard condition for transitions
	 * 
	 * Guard : prevents from traversing the transition if the predicate returns true.
	 */
	public static abstract class TransitionGuard {

		/**
		 * Function that should check the guard conditions when taking the transition
		 * 
		 * @param message
		 *            the message that triggered the transition
		 * @param context
		 *            current runner context
		 * @return true if transition should not be taken, false otherwise
		 */
		public final boolean predicate(long message, Runner.RunnerContext context) {
			return doPredicate(message, context);
		}

		/**
		 * Subclasses must implement this
		 * 
		 * @see TransitionGuard#predicate
		 */
		protected abstract boolean doPredicate(long message, Runner.RunnerContext context);
	}

	/**
	 * Default null transition guard (i.e. does not have any guard constraints)
	 */
	public final static class NullTransitionGuard extends TransitionGuard {

		@Override
		protected boolean doPredicate(long message, Runner.RunnerContext context) {
			return false;
		}
	};

	/**
	 * Transition guard that returns true if the decorated guard returns false (Inverts)
	 */
	public final static class NotTransitionGuard extends TransitionGuard {

		private TransitionGuard mGuard;

		NotTransitionGuard(TransitionGuard g) {
			mGuard = g;
		}

		@Override
		protected boolean doPredicate(long message, Runner.RunnerContext context) {
			return !mGuard.predicate(message, context);
		}
	}

	/**
	 * Transition guard that returns true if both decorated guard return true
	 */
	public final static class AndTransitionGuard extends TransitionGuard {

		private TransitionGuard mGuard1;
		private TransitionGuard mGuard2;

		public AndTransitionGuard(TransitionGuard g1, TransitionGuard g2) {
			mGuard1 = g1;
			mGuard2 = g2;
		}

		@Override
		protected boolean doPredicate(long message, Runner.RunnerContext context) {
			return (mGuard1.predicate(message, context) || mGuard2.predicate(message, context));
		}
	}

	/**
	 * Base Automaton exception
	 */
	public abstract static class AutomatonException extends Exception {

		private static final long serialVersionUID = -2972101806117136770L;

		public AutomatonException(String string) {
			super(string);
		}
	};

	/**
	 * Automaton exception for state errors (static errors)
	 */
	public final static class AutomatonStateException extends AutomatonException {

		private static final long serialVersionUID = -7043422581164886882L;

		public AutomatonStateException(String string) {
			super(string);
		}
	};

	/**
	 * Automaton exception related to a runner (dynamic errors)
	 */
	public final static class AutomatonRunnerException extends AutomatonException {

		private static final long serialVersionUID = 4715439559246329424L;

		public AutomatonRunnerException(String string) {
			super(string);
		}
	};

	/**
	 * Defines a runner that can walk through an automaton
	 * 
	 * An Automaton is stateless; a Runner is stateful. Thus, the same Automaton can be walked through using different
	 * Runners independently and concurrently, all managing their own states.
	 * <p>
	 * Each runner runs in its own thread, and is managed using a Handler.
	 * </p>
	 * <p>
	 * Each Runner can independently be started/paused/stopped/restarted, thus making them usable inside Activities,
	 * Fragments, or any other Android module that has such lifecycle.
	 * </p>
	 * <p>
	 * Runner can also save their own states into supplied Bundles (save/restoreInstanceState), provided the latter are
	 * called by the Activity/Fragment that manages it.<br>
	 * Current state can thus be restored at a later stage even if Activity/Fragment is destroyed/recreated.
	 * </p>
	 * 
	 * @author yoann@ymkm.org
	 */
	public final static class Runner {

		/**
		 * RunnerContext provides with a shared storage among all states of an automaton
		 * 
		 * Each runner instance holds a RunnerContext instance, which gets passed to every state/transition action of
		 * the automaton it is running onto.<br>
		 * This way, it is possible to keep track of data shared among different states/transitions (e.g. in
		 * {@link TransitionGuard}, one could handle different cases based on values inside the context).
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
				if (mContextValues.containsKey(key)) { return (Integer) mContextValues.get(key); }
				return defValue;
			}

			public long getLong(String key, long defValue) {
				if (mContextValues.containsKey(key)) { return (Long) mContextValues.get(key); }
				return defValue;
			}

			public String getString(String key, String defValue) {
				if (mContextValues.containsKey(key)) { return (String) mContextValues.get(key); }
				return defValue;
			}

			public float getFloat(String key, float defValue) {
				if (mContextValues.containsKey(key)) { return (Float) mContextValues.get(key); }
				return defValue;
			}

			public boolean getBoolean(String key, boolean defValue) {
				if (mContextValues.containsKey(key)) { return (Boolean) mContextValues.get(key); }
				return defValue;
			}

			public Object getObject(String key, Object defValue) {
				if (mContextValues.containsKey(key)) { return mContextValues.get(key); }
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

		private Automaton mAutomatonInstance;
		private int mCurrentState;
		private long mInstanceID;
		private boolean mStarted = false;
		private boolean mPaused = true;
		private Context mContext;
		private Handler mAutomatonHandler;
		private HandlerThread _thread;

		/**
		 * RunnerContext for this Runner
		 */
		public RunnerContext runnerContext;

		/**
		 * Creates a new Runner on specified {@link Automaton} given specified {@link Context}
		 * 
		 * <p>
		 * It performs a check for initial state on given automaton, and will throw an exception if there's none, or if
		 * multiple initial states are defined.
		 * </p>
		 * 
		 * @param context
		 *            the context (used to send broadcasts)
		 * @param g
		 *            the automaton to manage
		 * @throws AutomatonStateException
		 *             if errors are found in the automaton
		 */
		private Runner(Context context, Automaton g) throws AutomatonStateException {
			this(context, g, new RunnerContext());
		}

		/**
		 * Creates a new Runner on specified {@link Automaton} given specified {@link Context} and instance ID.
		 * 
		 * <p>
		 * It performs a check for initial state on given automaton, and will throw an exception if there's none, or if
		 * multiple initial states are defined.
		 * </p>
		 * 
		 * @param context
		 *            the context (used to send broadcasts)
		 * @param g
		 *            the automaton to manage
		 * @param instanceID
		 *            the instance ID to set
		 * @throws AutomatonStateException
		 *             if errors are found in the automaton
		 */
		private Runner(Context context, Automaton g, long instanceID) throws AutomatonStateException {
			this(context, g, instanceID, new RunnerContext());
		}

		/**
		 * Creates a new Runner on specified {@link Automaton} given specified {@link Context}, using given
		 * RunnerContext
		 * 
		 * <p>
		 * It performs a check for initial state on given automaton, and will throw an exception if there's none, or if
		 * multiple initial states are defined.
		 * </p>
		 * 
		 * @param context
		 *            the context (used to send broadcasts)
		 * @param g
		 *            the automaton to manage
		 * @throws AutomatonStateException
		 *             if errors are found in the automaton
		 */
		private Runner(Context context, Automaton g, RunnerContext rContext) throws AutomatonStateException {
			this(context, g, 0, rContext);
			mInstanceID = hashCode();
		}

		/**
		 * Creates a new Runner on specified {@link Automaton} given specified {@link Context}, using given
		 * RunnerContext and instance ID.
		 * 
		 * <p>
		 * It performs a check for initial state on given automaton, and will throw an exception if there's none, or if
		 * multiple initial states are defined.
		 * </p>
		 * 
		 * @param context
		 *            the context (used to send broadcasts)
		 * @param g
		 *            the automaton to manage
		 * @param instanceID
		 *            the instance ID to set
		 * @param rContext
		 *            the {@linkplain RunnerContext} to use
		 * @throws AutomatonStateException
		 *             if errors are found in the automaton
		 */
		private Runner(Context context, Automaton g, long instanceID, RunnerContext rContext)
				throws AutomatonStateException {
			mAutomatonInstance = g;
			mInstanceID = instanceID;
			mCurrentState = -1;
			mContext = context;
			runnerContext = rContext;
			init();
		}

		// Checks for initial state on automaton
		private void init() throws AutomatonStateException {
			boolean initialStateFound = false;
			State initialState = null;
			for (Entry<Integer, State> s : mAutomatonInstance.mStates.entrySet()) {
				if (INITIAL_STATE == s.getValue().getType()) {
					if (initialStateFound) { throw new AutomatonStateException(
							"Automaton cannot have more than one initial state"); }

					initialStateFound = true;
					initialState = s.getValue();
				}
			}
			if (!initialStateFound) { throw new AutomatonStateException("Automaton must have an initial state"); }
			mCurrentState = initialState.getStateId();
		}

		/**
		 * Restores internal state of current Runner
		 * 
		 * <p>
		 * May be called with a valid Bundle previously passed to {@link Runner#saveInstanceState}
		 * </p>
		 * 
		 * @param savedInstanceState
		 */
		public void restoreInstanceState(Bundle savedInstanceState) {
			if (null != savedInstanceState) {
				mCurrentState = savedInstanceState.getInt("mAutomatonRunner_" + mInstanceID + "_mCurrentState");
				mStarted = savedInstanceState.getBoolean("mAutomatonRunner_" + mInstanceID + "_mStarted");
				mPaused = savedInstanceState.getBoolean("mAutomatonRunner_" + mInstanceID + "_mPaused");
			}
		}

		/**
		 * Saves current Runner state to specified Bundle
		 * 
		 * <p>
		 * May be called by Activities or Fragments in the equivalent methods to react to lifecycle events.
		 * </p>
		 * 
		 * @param outState
		 */
		public void saveInstanceState(Bundle outState) {
			outState.putInt("mAutomatonRunner_" + mInstanceID + "_mCurrentState", mCurrentState);
			outState.putBoolean("mAutomatonRunner_" + mInstanceID + "_mStarted", mStarted);
			outState.putBoolean("mAutomatonRunner_" + mInstanceID + "_mPaused", mPaused);
		}

		/**
		 * If not started, starts the current Runner and enters initial state of the automaton
		 * 
		 * <p>
		 * A new thread is spawned, under which Runner's Handler gets its Looper from.
		 * </p>
		 */
		public void start() {
			synchronized (this) {
				if (!mStarted) {
					_thread = new HandlerThread("Automaton runner instance");
					_thread.start();
					mAutomatonHandler = new Handler(_thread.getLooper(), _callback);
				}
				restart();
				if (!mStarted) {
					mAutomatonInstance.mStates.get(mCurrentState).enter(mAutomatonHandler, runnerContext);
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
		 * @return true if not started, or if {@link Runner#pause()} was previously called
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
		 * @return true if not started and paused (Runner{@link #stop()} was called)
		 */
		public boolean isStopped() {
			return !mStarted && mPaused;
		}

		/**
		 * Forces enter action to be reran on current state
		 */
		public void reenter() {
			mAutomatonInstance.mStates.get(mCurrentState).enter(mAutomatonHandler, runnerContext);
		}

		/**
		 * Restarts current Runner
		 * 
		 * <p>
		 * can Resume a Runner after a previous call to {@link Runner#pause()}, or during {@link Runner#start()}.
		 * </p>
		 */
		public void restart() {
			synchronized (this) {
				if (mPaused) {
					mPaused = false;
				}
			}
		}

		/**
		 * Stops current Runner
		 * 
		 * <p>
		 * Does nothing is Runner was not started.<br>
		 * Stopping a Runner will kill its internal thread, and resets all internal states.<br>
		 * <strong>A stopped Runner cannot be resumed!!</strong><br>
		 * A stopped Runner can be started again; in this case, current state will be reset to the initial state.<br>
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
		 * Calling pause on a non running, non started Runner has no effect.
		 * </p>
		 */
		public void pause() {
			synchronized (this) {
				if (!mPaused) {
					mPaused = true;
				}
			}
		}

		/**
		 * Sends a message that will trigger transitions reacting to it.
		 * 
		 * @param message
		 *            the message to send to running instances
		 */
		public void send(long message) {
			mAutomatonHandler.obtainMessage(MSG_INTENT_RECEIVED, Long.valueOf(message)).sendToTarget();

		}

		/**
		 * Returns the instance ID assigned to this runner
		 * 
		 * @return long the instance ID
		 */
		public long getInstanceID() {
			return mInstanceID;
		}

		// This is the core of the Runner process :
		// It compares every sent message to messages registered with outgoing Transitions of
		// current state
		// and tries to traverse the first one it finds whose TransitionGuard
		// returns false.
		private class AutomatonRunnerHandler implements Handler.Callback {

			@Override
			public boolean handleMessage(Message msg) {

				switch (msg.what) {
					case MSG_INTENT_RECEIVED:
						long message = ((Long) msg.obj).longValue();
						Log.d(TAG, "Message ID received : " + message);
						Map<Integer, List<Transition>> trList = mAutomatonInstance.mTransitions.get(mCurrentState);

						if (null != trList && 0 < trList.size()) {
							for (Entry<Integer, List<Transition>> trs : trList.entrySet()) {
								for (Transition tr : trs.getValue()) {
									// Received message ID matches expected ID for
									// this transition
									// Plus, the guard returned false : we perform
									// the transition
									if (message == tr.getMessage()) {
										State from = mAutomatonInstance.mStates.get(mCurrentState);
										State to = mAutomatonInstance.mStates.get(trs.getKey());

										if (null == from || null == to) { return false; }

										Log.d(TAG, "> Checking if transition " + from.getName() + " -> " + to.getName()
												+ " is guarded");

										if (tr.guarded(message, runnerContext)) {
											Log.d(TAG, ">> Transition guarded. Checking next");
											continue;
										}

										// Transition while not in final state
										if (FINAL_STATE != from.getType()) {

											Log.d(TAG, ">> Transition not guarded, applying " + from.getName() + " -> "
													+ to.getName());

											from.exit(mAutomatonHandler, runnerContext);
											tr.action(message, mAutomatonHandler, runnerContext);
											to.enter(mAutomatonHandler, runnerContext);
											mCurrentState = to.getStateId();

											Intent i = new Intent(AUTOMATON_STATE_CHANGE);
											i.putExtra(KEY_FROM_STATE, from.getName());
											i.putExtra(KEY_TO_STATE, to.getName());
											i.putExtra(KEY_MESSAGE_ID, tr.getMessage());
											i.putExtra(KEY_RUNNER_INSTANCE_ID, getInstanceID());
											mContext.sendBroadcast(i);
										}

										if (FINAL_STATE == to.getType()) {
											mAutomatonHandler
													.postDelayed(new Runnable() {

														@Override
														public void run() {
															stop();
														}
													},
															(null != to.getEnterAction()) ? (to.getEnterAction()
																	.getDelay() + 1) : 0);
										}

										return true;
									}
								}
							}
						}

						Log.d(TAG,
								"Automaton state after intent handling : "
										+ mAutomatonInstance.mStates.get(mCurrentState));
				}

				return false;
			}
		};

		private final AutomatonRunnerHandler _callback = new AutomatonRunnerHandler();
	};

	/**
	 * Creates a new Runner to walk through this Automaton
	 * <p>
	 * Instance ID will automatically be assigned as the hashCode() of the runner object.
	 * </p>
	 * 
	 * @see Runner#Runner(Context, Automaton)
	 * @param context
	 * @param g
	 * @return The new Runner instance
	 * @throws AutomatonStateException
	 */
	public static Runner instantiate(Context context, Automaton g) throws AutomatonStateException {
		return new Runner(context, g);
	}

	/**
	 * Creates a new Runner to walk through this Automaton, by supplying the RunnerContext it will use instead of
	 * letting it create its own.
	 * <p>
	 * Instance ID will automatically be assigned as the hashCode() of the runner object.
	 * </p>
	 * 
	 * @see Runner#Runner(Context, Automaton, org.ymkm.lib.automaton.Automaton.Runner.RunnerContext)
	 * @param context
	 * @param g
	 * @param rContext
	 * @return The new Runner instance
	 * @throws AutomatonStateException
	 */
	public static Runner instantiate(Context context, Automaton g, Runner.RunnerContext rContext)
			throws AutomatonStateException {
		return new Runner(context, g, rContext);
	}

	/**
	 * Creates a new Runner to walk through this Automaton, setting its instance ID.
	 * 
	 * @see Runner#Runner(Context, Automaton)
	 * @param context
	 * @param g
	 * @param id
	 *            the runner instance ID
	 * @return The new Runner instance
	 * @throws AutomatonStateException
	 */
	public static Runner instantiate(Context context, Automaton g, long instanceID) throws AutomatonStateException {
		return new Runner(context, g, instanceID);
	}

	/**
	 * Creates a new Runner to walk through this Automaton, by supplying the RunnerContext it will use instead of
	 * letting it create its own, and setting its instance ID.
	 * 
	 * @see Runner#Runner(Context, Automaton, org.ymkm.lib.automaton.Automaton.Runner.RunnerContext)
	 * @param context
	 * @param g
	 * @param id
	 *            the runner instance ID
	 * @param rContext
	 * @return The new Runner instance
	 * @throws AutomatonStateException
	 */
	public static Runner instantiate(Context context, Automaton g, long instanceID, Runner.RunnerContext rContext)
			throws AutomatonStateException {
		return new Runner(context, g, instanceID, rContext);
	}

	/**
	 * Creates a new Automaton
	 */
	public Automaton() {
		mStates = new HashMap<Integer, State>();
		mTransitions = new SparseArray<Map<Integer, List<Transition>>>();
		mMessages = new TreeSet<Long>();
	}

	/*
	 * Public API to define states/transitions on the automaton
	 */

	/**
	 * Adds a state with specified ID and name. No enter/exit state action
	 * 
	 * @see Automaton#addState(int, String, StateAction, StateAction)
	 * @param stateId
	 *            the ID uniquely defining this state in current automaton
	 * @param name
	 *            the automaton name
	 * @throws AutomatonStateException
	 *             if a state with the same ID was previously added
	 */
	public void addState(int stateId, String name) throws AutomatonStateException {
		addState(stateId, name, null, null);
	}

	/**
	 * Adds a state with specified ID and name. No exit state action
	 * 
	 * @see Automaton#addState(int, String, StateAction, StateAction)
	 * @param stateId
	 *            the ID uniquely defining this state in current automaton
	 * @param name
	 *            the automaton name
	 * @throws AutomatonStateException
	 *             if a state with the same ID was previously added
	 */
	public void addState(int stateId, String name, StateAction onEnterAction) throws AutomatonStateException {
		addState(stateId, name, onEnterAction, null);
	}

	/**
	 * Adds a state with specified ID and name. Defines enter/exit action
	 * 
	 * @param stateId
	 *            the ID uniquely defining this state in current automaton
	 * @param name
	 *            the automaton name
	 * @throws AutomatonStateException
	 *             if a state with the same ID was previously added
	 */
	public void addState(int stateId, String name, StateAction onEnterAction, StateAction onExitAction)
			throws AutomatonStateException {
		State s;

		if (mStates.containsKey(stateId)) { throw new AutomatonStateException("Error while adding state " + name
				+ " : ID " + stateId + " already defined as name + " + mStates.get(stateId).getName()); }

		if (null != onEnterAction && null != onExitAction) {
			s = new State(this, stateId, name, onEnterAction, onExitAction);
		}
		else if (null != onEnterAction) {
			s = new State(this, stateId, name, onEnterAction);
		}
		else {
			s = new State(this, stateId, name);
		}
		mStates.put(stateId, s);
	}

	/**
	 * Defines a new transition linking "from" state to "to" state with given TransitionAction, reacting to specified
	 * message ID
	 * 
	 * <p>
	 * No {@link TransitionGuard} is defined for this Transition.<br>
	 * Internally, it translates to a {@link NullTransitionGuard}.
	 * </p>
	 * 
	 * @see Automaton#addTransition(int, int, long, TransitionAction, TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param message
	 *            the message the Transition reacts to
	 * @param onTransitionAction
	 *            the TransitionAction to perform during transition
	 */
	public void addTransition(int from, int to, long message, TransitionAction onTransitionAction) {
		addTransition(from, to, message, onTransitionAction, new NullTransitionGuard());
	}

	/**
	 * Defines a new transition linking "from" state to "to" state, reacting to specified message ID. Defines a
	 * TransitionGuard. No action.
	 * 
	 * <p>
	 * The transition will not be taken by the Runner if specified TransitionGuard returns true at the time of calling.
	 * </p>
	 * 
	 * @see Automaton#addTransition(int, int, long, TransitionAction, TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param message
	 *            the message the Transition reacts to
	 * @param g
	 *            the TransitionGuard to check before traversing
	 */
	public void addTransition(int from, int to, long message, TransitionGuard g) {
		Map<Integer, List<Transition>> m;
		List<Transition> l;
		if (mTransitions.indexOfKey(from) < 0) {
			l = new ArrayList<Transition>();
			m = new HashMap<Integer, List<Transition>>();
			m.put(to, l);
			mTransitions.put(from, m);
		}
		else {
			m = mTransitions.get(from);
			if (!m.containsKey(to)) {
				l = new ArrayList<Transition>();
				m.put(to, l);
			}
			else {
				l = m.get(to);
			}
		}
		l.add(new Transition(message, null, g));

		// Add this intent to the list of intents to listen to
		mMessages.add(message);
	}

	/**
	 * Defines a new transition linking "from" state to "to" state with given TransitionAction, reacting to specified
	 * message ID. Defines a TransitionGuard.
	 * 
	 * <p>
	 * The transition will not be taken by the Runner if specified TransitionGuard returns true at the time of calling.
	 * </p>
	 * 
	 * @see Automaton#addTransition(int, int, String, TransitionAction, TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param message
	 *            the message the Transition reacts to
	 * @param onTransitionAction
	 *            the TransitionAction to perform during transition
	 * @param g
	 *            the TransitionGuard to check before traversing
	 */
	public void addTransition(int from, int to, long message, TransitionAction onTransitionAction, TransitionGuard g) {
		Map<Integer, List<Transition>> m;
		List<Transition> l;
		if (mTransitions.indexOfKey(from) < 0) {
			l = new ArrayList<Transition>();
			m = new HashMap<Integer, List<Transition>>();
			m.put(to, l);
			mTransitions.put(from, m);
		}
		else {
			m = mTransitions.get(from);
			if (!m.containsKey(to)) {
				l = new ArrayList<Transition>();
				m.put(to, l);
			}
			else {
				l = m.get(to);
			}
		}
		l.add(new Transition(message, onTransitionAction, g));

		// Add this intent to the list of intents to listen to
		mMessages.add(message);
	}

	/**
	 * Defines a new transition linking "from" state to "to" state, reacting to specified message ID. No
	 * TransitionGuard. No action.
	 * 
	 * @see Automaton#addTransition(int, int, String, TransitionAction, TransitionGuard)
	 * @param from
	 *            the outgoing source state
	 * @param to
	 *            the target state
	 * @param message
	 *            the message the Transition reacts to
	 */
	public void addTransition(int from, int to, long message) {
		Map<Integer, List<Transition>> m;
		List<Transition> l;
		if (mTransitions.indexOfKey(from) < 0) {
			l = new ArrayList<Transition>();
			m = new HashMap<Integer, List<Transition>>();
			m.put(to, l);
			mTransitions.put(from, m);
		}
		else {
			m = mTransitions.get(from);
			if (!m.containsKey(to)) {
				l = new ArrayList<Transition>();
				m.put(to, l);
			}
			else {
				l = m.get(to);
			}
		}
		l.add(new Transition(message, null, null));

		// Add this intent to the list of intents to listen to
		mMessages.add(message);
	}

	/**
	 * Sets the initial state of current automaton. Must have one and only one!
	 * 
	 * <p>
	 * The specified ID must be previously added to the automaton before this method is called.
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
	 * The specified ID must be previously added to the automaton before this method is called.
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

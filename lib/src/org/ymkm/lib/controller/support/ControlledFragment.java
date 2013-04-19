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

package org.ymkm.lib.controller.support;

import org.ymkm.lib.controller.core.ControllableFragmentException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;


/**
 * Defines a {@link Fragment} that is controllable by a {@link FragmentControllerApplication}.
 * 
 */
public abstract class ControlledFragment extends Fragment implements ControllableFragment {

	/**
	 * Instantiates a new {@linkplain ControlledFragment} of specified
	 * {@code Class}, whose handler will run in the specified Looper
	 * 
	 * @param f
	 *            the subclass of {@linkplain ControlledFragment} to instantiate
	 * @param runsInOwnThread
	 *            {@code true} if a new thread should be created for this
	 *            fragment, {@code false} to let it run in the current thread
	 * @return A new instance of {@linkplain ControllableFragment}
	 * @throws ControllableFragmentException
	 *             if instantiation failed
	 */
	public static ControllableFragment createFragment(Class<? extends ControllableFragment> f, boolean runsInOwnThread)
			throws ControllableFragmentException {
		return createFragment(f, runsInOwnThread, new Bundle());
	}

	/**
	 * Instantiates a new {@linkplain ControllableFragment} of specified
	 * {@code Class}.
	 * <p>
	 * The {@link Handler} for this fragment will run in the thread of the
	 * context that created it.
	 * </p>
	 * 
	 * @param f
	 *            the subclass of {@linkplain ControlledFragment} to instantiate
	 * @return A new instance of {@linkplain ControllableFragment}
	 * @throws ControllableFragmentException
	 *             if instantiation failed
	 */
	public static ControllableFragment createFragment(Class<? extends ControllableFragment> f)
			throws ControllableFragmentException {
		return createFragment(f, false, new Bundle());
	}

	/**
	 * Instantiates a new {@linkplain ControllableFragment} of specified
	 * {@code Class}.
	 * <p>
	 * The {@link Handler} for this fragment will run in the thread of the
	 * context that created it.
	 * </p>
	 * 
	 * @param f
	 *            the subclass of {@linkplain ControlledFragment} to instantiate
	 * @param args
	 *            optional arguments to pass to the fragment as a bundle
	 * @return A new instance of {@linkplain ControllableFragment}
	 * @throws ControllableFragmentException
	 *             if instantiation failed
	 */
	public static ControllableFragment createFragment(Class<? extends ControllableFragment> f, Bundle args)
			throws ControllableFragmentException {
		assert(null != args);
		return createFragment(f, false, args);
	}

	/**
	 * Instantiates a new {@linkplain ControllableFragment} of specified
	 * {@code Class}, whose handler will run in the specified Looper
	 * 
	 * @param f
	 *            the subclass of {@linkplain ControlledFragment} to instantiate
	 * @param runsInOwnThread
	 *            {@code true} if a new thread should be created for this
	 *            fragment, {@code false} to let it run in the current thread
	 * @return A new instance of {@linkplain ControllableFragment}
	 * @throws ControllableFragmentException
	 *             if instantiation failed
	 */
	public static ControllableFragment createFragment(Class<? extends ControllableFragment> f, boolean runsInOwnThread,
			Bundle args) throws ControllableFragmentException {
		assert(null != args);
		try {
			ControlledFragment fragment = (ControlledFragment) f.newInstance();
			args.putBoolean("__new_thread__", runsInOwnThread);
			fragment.setArguments(args);
			return fragment;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());			
		} catch (java.lang.InstantiationException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new ControllableFragmentException(e.getMessage());
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getArguments();
		if (null == b) {
			b = new Bundle();
		}
		// Creates a new thread supplying a Looper for the Handler if creation
		// requested to run in a new thread.
		boolean newThread = b.getBoolean("__new_thread__",
				(null != savedInstanceState && savedInstanceState.getBoolean("__new_thread__", false)));
		mControllableName = b.getString("__controllable_name__");
		mControllableName = (null == mControllableName && null != savedInstanceState) ? savedInstanceState
				.getString("__controllable_name__") : mControllableName;
		mControlId = b.getInt("__control_id__", 0);
		mControlId = (0 == mControlId && null != savedInstanceState)
				? savedInstanceState.getInt("__control_id__", 0)
				: mControlId;
		if (newThread) {
			synchronized (this) {
				if (null == _handlerThread) {
					this._handlerThread = new HandlerThread(getFragmentName());
					_handlerThread.start();
				}
			}
		} else {
			_handlerThread = null;
		}
		if (null != _handlerThread) {
			this.mHandler = new Handler(_handlerThread.getLooper(), getCallback());
		} else {
			this.mHandler = new Handler(getCallback());
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("__new_thread__", (null != _handlerThread));
		outState.putString("__controllable_name__", mControllableName);
		outState.putInt("__control_id__", mControlId);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (null != mControllerMessenger) {
			Message m = Message.obtain();
			m.what = FragmentController.MSG_DETACH_MESSAGE;
			m.arg1 = getControlId();
			m.obj = this;
			Bundle b = new Bundle();
			b.putString("controllableName", mControllableName);
			m.setData(b);
			try {
				mControllerMessenger.send(m);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		mControllerMessenger = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		Activity activity = getActivity();
		if (activity instanceof ControllableActivity) {
			FragmentController controller = ((ControllableActivity)activity).getController();
			mControllerMessenger = controller.getMessenger();
		}

		if (null != mControllerMessenger) {
			Message m = Message.obtain();
			m.what = FragmentController.MSG_ATTACH_MESSAGE;
			m.arg1 = getControlId();
			m.obj = this;
			Bundle b = new Bundle();
			b.putString("controllableName", mControllableName);
			m.setData(b);
			try {
				mControllerMessenger.send(m);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		synchronized (this) {
			mHandler.removeCallbacksAndMessages(null);
			if (null != _handlerThread) {
				HandlerThread ht = _handlerThread;
				_handlerThread = null;
				ht.quit();
				ht.interrupt();
			}
			mHandler = null;
			mControllerMessenger = null;
		}
	}

	@Override
	public final Messenger getMessenger() {
		return new Messenger(mHandler);
	}

	/**
	 * Returns true if the handler runs in a separate thread
	 * 
	 * @return true if it has its own thread for the inner Handler, false
	 *         otherwise
	 */
	@Override
	public final boolean hasOwnThread() {
		return (null != _handlerThread);
	}

	/**
	 * Returns an instance of the {@link ControlledFragmentCallback} that can
	 * process messages recognized by this fragment
	 * 
	 * @return the callback, or {@code null} in case of failure
	 */
	private ControlledFragmentCallback getCallback() {
		if (null != doGetCallbackClass()) {
			try {
				return ControlledFragmentCallback.createCallback(doGetCallbackClass(), this);
			} catch (ControllableFragmentException e) {
				e.printStackTrace();
			}
		}
		return new ControlledFragmentCallback() {
			@Override
			public boolean handleMessage(Message msg) {
				return true;
			}
		};
	}

	/**
	 * Returns a name for this fragment that can be used to identify the thread
	 * the handler is running within
	 * 
	 * @return the fragment name
	 */
	@Override
	public final String getFragmentName() {
		return doGetFragmentName();
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControlledFragment} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControlledFragment that owns this
	 * callback</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be
	 * added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what passed as
	 * parameter of this method.<br>
	 * 
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToController(int what) {
		return sendToController(what, 0, 0, null);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControlledFragment} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControlledFragment that owns this
	 * callback</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be
	 * added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1 passed
	 * as parameters of this method.<br>
	 * 
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToController(int what, int arg1) {
		return sendToController(what, arg1, 0, null);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControlledFragment} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControlledFragment that owns this
	 * callback</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be
	 * added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, obj passed
	 * as parameters of this method.<br>
	 * 
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToController(int what, Object obj) {
		return sendToController(what, 0, 0, obj);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControlledFragment} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControlledFragment that owns this
	 * callback</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be
	 * added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2
	 * passed as parameters of this method.<br>
	 * 
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @param arg2
	 *            second message int argment
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToController(int what, int arg1, int arg2) {
		return sendToController(what, arg1, arg2, null);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControlledFragment} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControlledFragment that owns this
	 * callback</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be
	 * added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, obj
	 * passed as parameters of this method.<br>
	 * 
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToController(int what, int arg1, Object obj) {
		return sendToController(what, arg1, 0, obj);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControlledFragment} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControlledFragment that owns this
	 * callback</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be
	 * added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2,
	 * obj passed as parameters of this method.<br>
	 * 
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @param arg2
	 *            second message int argment
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToController(int what, int arg1, int arg2, Object obj) {
		if (null != mControllerMessenger) {
			Message m = Message.obtain();
			m.what = FragmentController.MSG_DISPATCH_MESSAGE;
			m.arg1 = getControlId();
			m.arg2 = what;
			m.obj = FragmentController.CallbackMessage.obtain(arg1, arg2, obj);
			Bundle b = new Bundle();
			b.putString("controllableName", mControllableName);
			m.setData(b);
			try {
				mControllerMessenger.send(m);
			} catch (RemoteException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToSelf(int what) {
		return sendToSelf(what, 0, 0, null);
	}

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param arg1
	 *            first message int argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToSelf(int what, int arg1) {
		return sendToSelf(what, arg1, 0, null);
	}

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToSelf(int what, Object obj) {
		return sendToSelf(what, 0, 0, obj);
	}

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param arg1
	 *            first message int argument
	 * @param arg2
	 *            second message int argment
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToSelf(int what, int arg1, int arg2) {
		return sendToSelf(what, arg1, arg2, null);
	}

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param arg1
	 *            first message int argument
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToSelf(int what, int arg1, Object obj) {
		return sendToSelf(what, arg1, 0, obj);
	}

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param arg1
	 *            first message int argument
	 * @param arg2
	 *            second message int argment
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public final boolean sendToSelf(int what, int arg1, int arg2, Object obj) {
		Message m = Message.obtain();
		m.what = what;
		m.arg1 = arg1;
		m.arg2 = arg2;
		m.obj = obj;
		return getHandler().sendMessage(m);
	}

	/**
	 * Sends the specified Runnable to run on the UI thread
	 * 
	 * @param runnable
	 *            the task to run on the UI thread
	 * @return itself for chaining
	 */
	@Override
	public final ControlledFragment sendToUi(Runnable runnable) {
		if (null != getActivity()) {
			getActivity().runOnUiThread(runnable);
		}
		return this;
	}

	/**
	 * Returns the control Id associated with this fragment
	 * 
	 * @return the control ID
	 */
	@Override
	public final int getControlId() {
		return mControlId;
	}

	/**
	 * Returns the {@link Handler} associated with current fragment
	 * 
	 * @return the handler
	 */
	@Override
	public final Handler getHandler() {
		return mHandler;
	}

	/**
	 * Returns a name for this fragment that can be used ot identify the thread
	 * the handler is running within
	 * 
	 * @return the fragment name
	 */
	protected String doGetFragmentName() {
		return getClass().getSimpleName();
	}

	/**
	 * Returns the {@code Class} to be used to handle messages targeting this
	 * fragment
	 * 
	 * @return The {@code Class}, subclass of {@link ControlledFragmentCallback}
	 */
	protected Class<? extends ControlledFragmentCallback> doGetCallbackClass() {
		return null;
	}

	private Messenger mControllerMessenger = null;
	private int mControlId = -1;
	private String mControllableName = null;

	private Handler mHandler;
	/** null if runsInOwnThread is false during creation */
	private HandlerThread _handlerThread;
}

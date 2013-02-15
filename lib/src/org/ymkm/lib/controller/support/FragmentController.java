package org.ymkm.lib.controller.support;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.ymkm.lib.controller.ControlledDialogFragment;
import org.ymkm.lib.controller.ControlledFragment;
import org.ymkm.lib.controller.core.ControllableActivity;
import org.ymkm.lib.controller.core.ControllableFragment;
import org.ymkm.lib.controller.core.ControlledFragmentException;
import org.ymkm.lib.controller.core.FragmentControllerCallbackAbstract;
import org.ymkm.lib.controller.core.FragmentControllerException;
import org.ymkm.lib.controller.core.FragmentControllerInterface;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;



/**
 * TODO
 * 
 */
public final class FragmentController implements
		FragmentControllerInterface<FragmentManager, FragmentTransaction> {

	public final static class CallbackMessage {

		public static Message obtain(int arg1) {
			return CallbackMessage.obtain(arg1, 0, null);
		}

		public static Message obtain(int arg1, int arg2) {
			return CallbackMessage.obtain(arg1, arg2, null);
		}

		public static Message obtain(Object obj) {
			return CallbackMessage.obtain(0, 0, obj);
		}

		public static Message obtain(int arg1, Object obj) {
			return CallbackMessage.obtain(arg1, 0, obj);
		}

		public static Message obtain(int arg1, int arg2, Object obj) {
			Message m = Message.obtain();
			m.arg1 = arg1;
			m.arg2 = arg2;
			m.obj = obj;
			return m;
		}
	}

	/**
	 * Registers a new {@linkplain ControllableActivity<FragmentManager>} to this controller
	 * 
	 * @param controllable
	 *            the {@linkplain ControllableActivity<FragmentManager>} to register
	 * @return the FragmentControllerApplication for chaining
	 */
	@Override
	public FragmentController register(ControllableActivity<FragmentManager, FragmentTransaction> controllable) {
		Log.d("FragmentControllerApplication", "◆ register : " + controllable.getControllableName());

		if (mRefCount++ <= 0) {
			doInit();
		}

		if (!mCallbacks.containsKey(controllable.getControllableName())) {
			mCallbacks.put(controllable.getControllableName(),
					new ArrayList<FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction>>());
		}
		else {
			mCallbacks.get(controllable.getControllableName()).clear();
		}
		if (!mFragments.containsKey(controllable.getControllableName())) {
			mFragments.put(controllable.getControllableName(),
					new SparseArray<WeakReference<ControllableFragment>>());
		}
		else {
			mFragments.get(controllable.getControllableName()).clear();
		}
		if (!mMessengers.containsKey(controllable.getControllableName())) {
			mMessengers.put(controllable.getControllableName(), new SparseArray<Messenger>());
		}
		else {
			mMessengers.get(controllable.getControllableName()).clear();
		}

		if (null != controllable.getCallbacks(true)) {
			for (Class<? extends FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction>> cls : controllable
					.getCallbacks(true)) {
				FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction> callback = newFragmentControllerCallback(
						controllable, cls);
				if (null != callback) {
					addCallback(controllable, callback);
				}
			}
		}

		return this;
	}

	/**
	 * Unregisters the {@link FragmentControllerApplication} from the current activity.
	 * 
	 * @param controllable
	 *            the {@linkplain ControllableActivity<FragmentManager>} to unregister
	 * @return the FragmentControllerApplication for chaining
	 */
	@Override
	public FragmentController unregister(ControllableActivity<FragmentManager, FragmentTransaction> controllable) {
		Log.d("FragmentControllerApplication", "◆ unregister : " + controllable.getControllableName());

		if (mCallbacks.containsKey(controllable.getControllableName())) {
			mCallbacks.remove(controllable.getControllableName());
		}
		if (mMessengers.containsKey(controllable.getControllableName())) {
			mMessengers.remove(controllable.getControllableName());
		}
		if (mFragments.containsKey(controllable.getControllableName())) {
			mFragments.remove(controllable.getControllableName());
		}

		if (--mRefCount <= 0) {
			doFinish();
		}

		return this;
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, Class<? extends ControllableFragment> fragmentClass)
			throws ControlledFragmentException, FragmentControllerException {
		return add(controllable, ft, controlId, 0, fragmentClass, false);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, Class<? extends ControllableFragment> fragmentClass,
			boolean runsInNewThread) throws ControlledFragmentException, FragmentControllerException {
		return add(controllable, ft, controlId, 0, fragmentClass, runsInNewThread);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, int containerViewId,
			Class<? extends ControllableFragment> fragmentClass) throws ControlledFragmentException,
			FragmentControllerException {
		Bundle args = new Bundle();
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		args.putParcelable("__controller_messenger__", getMessenger());
		ControllableFragment frag = newControllableFragment(fragmentClass, false, args);
		assert (null != frag);
		return add(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, int containerViewId,
			Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread)
			throws ControlledFragmentException, FragmentControllerException {
		Bundle args = new Bundle();
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		ControllableFragment frag = newControllableFragment(fragmentClass, runsInNewThread, args);
		assert (null != frag);
		return add(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, Class<? extends ControllableFragment> fragmentClass, Bundle args)
			throws ControlledFragmentException, FragmentControllerException {
		return add(controllable, ft, controlId, 0, fragmentClass, false, args);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, Class<? extends ControllableFragment> fragmentClass,
			boolean runsInNewThread, Bundle args) throws ControlledFragmentException, FragmentControllerException {
		return add(controllable, ft, controlId, 0, fragmentClass, runsInNewThread, args);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, int containerViewId,
			Class<? extends ControllableFragment> fragmentClass, Bundle args) throws ControlledFragmentException,
			FragmentControllerException {
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		args.putParcelable("__controller_messenger__", getMessenger());
		ControllableFragment frag = newControllableFragment(fragmentClass, false, args);
		assert (null != frag);
		return add(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, int containerViewId,
			Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread, Bundle args)
			throws ControlledFragmentException, FragmentControllerException {
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		args.putParcelable("__controller_messenger__", getMessenger());
		ControllableFragment frag = newControllableFragment(fragmentClass, runsInNewThread, args);
		assert (null != frag);
		return add(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, ControllableFragment fragment)
			throws ControlledFragmentException, FragmentControllerException {
		return add(controllable, ft, controlId, 0, fragment);
	}

	@Override
	public FragmentController add(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, int containerViewId, ControllableFragment fragment)
			throws ControlledFragmentException, FragmentControllerException {

		String tag = generateTag(controlId, fragment.getClass(), controllable.getControllableName());
		Fragment frag = controllable.getSupportFragmentManager().findFragmentByTag(tag);
		if (null == frag) {
			if (fragment instanceof DialogFragment) {
				((DialogFragment) fragment).show(ft, tag);
			}
			else {
				ft.add(containerViewId, (Fragment) fragment, tag);
			}
		}
		else {
			if (frag instanceof DialogFragment && !frag.isAdded()) {
				((DialogFragment) frag).show(ft, tag);
			}
			else if (!frag.isAdded()) {
				ft.add(containerViewId, frag, tag);
			}
		}
		return this;
	}

	@Override
	public FragmentController addToBackStack(FragmentTransaction ft, String name)
			throws FragmentControllerException {
		ft.addToBackStack(name);
		return this;
	}

	@Override
	public FragmentController show(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId) throws FragmentControllerException {

		ControllableFragment fragment = mFragments.get(controllable.getControllableName()).get(controlId, null)
				.get();
		if (null == fragment) { throw new FragmentControllerException(String.format(
				"No fragment found registered with control ID %d", controlId)); }

		ft.show((Fragment) fragment);
		return this;
	}

	@Override
	public FragmentController hide(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId) throws FragmentControllerException {

		ControllableFragment fragment = mFragments.get(controllable.getControllableName()).get(controlId, null)
				.get();
		if (null == fragment) { throw new FragmentControllerException(String.format(
				"No fragment found registered with control ID %d", controlId)); }

		ft.hide((Fragment) fragment);
		return this;
	}

	@Override
	public FragmentController setTransition(FragmentTransaction ft, int transit) throws FragmentControllerException {
		ft.setTransition(transit);
		return this;
	}

	@Override
	public FragmentController setTransitionStyle(FragmentTransaction ft, int styleRes)
			throws FragmentControllerException {
		ft.setTransitionStyle(styleRes);
		return this;
	}

	@Override
	public FragmentController setCustomAnimations(FragmentTransaction ft, int enter, int exit)
			throws FragmentControllerException {
		ft.setCustomAnimations(enter, exit);
		return this;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	@Override
	public FragmentController setCustomAnimations(FragmentTransaction ft, int enter, int exit, int popEnter,
			int popExit) throws FragmentControllerException {

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
			ft.setCustomAnimations(enter, exit, popEnter, popExit);
		}
		return this;
	}

	@Override
	public FragmentController replace(
			final ControllableActivity<FragmentManager, FragmentTransaction> controllable, FragmentTransaction ft,
			int controlId, int containerViewId, Class<? extends ControllableFragment> fragmentClass)
			throws ControlledFragmentException, FragmentControllerException {
		Bundle args = new Bundle();
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		args.putParcelable("__controller_messenger__", getMessenger());
		ControllableFragment frag = newControllableFragment(fragmentClass, false, args);
		assert (null != frag);
		return replace(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController replace(
			final ControllableActivity<FragmentManager, FragmentTransaction> controllable, FragmentTransaction ft,
			int controlId, int containerViewId, Class<? extends ControllableFragment> fragmentClass,
			boolean runsInNewThread) throws ControlledFragmentException, FragmentControllerException {
		Bundle args = new Bundle();
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		ControllableFragment frag = newControllableFragment(fragmentClass, runsInNewThread, args);
		assert (null != frag);
		return replace(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController replace(
			final ControllableActivity<FragmentManager, FragmentTransaction> controllable, FragmentTransaction ft,
			int controlId, int containerViewId, Class<? extends ControllableFragment> fragmentClass, Bundle args)
			throws ControlledFragmentException, FragmentControllerException {
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		args.putParcelable("__controller_messenger__", getMessenger());
		ControllableFragment frag = newControllableFragment(fragmentClass, false, args);
		assert (null != frag);
		return replace(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController replace(
			final ControllableActivity<FragmentManager, FragmentTransaction> controllable, FragmentTransaction ft,
			int controlId, int containerViewId, Class<? extends ControllableFragment> fragmentClass,
			boolean runsInNewThread, Bundle args) throws ControlledFragmentException, FragmentControllerException {
		args.putInt("__control_id__", controlId);
		args.putString("__controllable_name__", controllable.getControllableName());
		args.putParcelable("__controller_messenger__", getMessenger());
		ControllableFragment frag = newControllableFragment(fragmentClass, runsInNewThread, args);
		assert (null != frag);
		return replace(controllable, ft, controlId, containerViewId, frag);
	}

	@Override
	public FragmentController replace(ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId, int containerViewId, ControllableFragment fragment) {

		String tag = generateTag(controlId, fragment.getClass(), controllable.getControllableName());
		Fragment frag = controllable.getSupportFragmentManager().findFragmentByTag(tag);
		if (null == frag) {
			ft.replace(containerViewId, (Fragment) fragment, tag);
		}
		else {
			ft.replace(containerViewId, frag, tag);
		}
		return this;
	}

	@Override
	public FragmentController remove(final ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentTransaction ft, int controlId) throws FragmentControllerException {

		ControllableFragment fragment = mFragments.get(controllable.getControllableName()).get(controlId, null)
				.get();
		if (null == fragment) { throw new FragmentControllerException(String.format(
				"No fragment found registered with control ID %d", controlId)); }

		ft.remove((Fragment) fragment);
		return this;
	}

	@Override
	public Messenger getMessenger() {
		if (null != mHandler) { return new Messenger(mHandler); }
		return null;
	}

	@Override
	public void post(Runnable runnable) {
		if (null != mHandler) {
			mHandler.post(runnable);
		}
	}

	@Override
	public void postDelayed(Runnable runnable, long delayMillis) {
		if (null != mHandler) {
			mHandler.postDelayed(runnable, delayMillis);
		}
	}

	@Override
	public void postAtTime(Runnable runnable, long uptimeMillis) {
		if (null != mHandler) {
			mHandler.postAtTime(runnable, uptimeMillis);
		}
	}

	@Override
	public void postAtTime(Runnable runnable, Object token, long uptimeMillis) {
		if (null != mHandler) {
			mHandler.postAtTime(runnable, token, uptimeMillis);
		}
	}

	@Override
	public void removeCallbacks(Runnable runnable) {
		if (null != mHandler) {
			mHandler.removeCallbacks(runnable);
		}
	}

	@Override
	public void removeCallbacks(Runnable runnable, Object token) {
		if (null != mHandler) {
			mHandler.removeCallbacks(runnable, token);
		}
	}

	@Override
	public boolean hasControlId(final String controllableName, int controlId) {
		return (mFragments.containsKey(controllableName) && null != mFragments.get(controllableName).get(controlId));
	}

	@Override
	public FragmentController addCallback(ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction> callback) {
		if (mCallbacks.containsKey(controllable.getControllableName())
				&& !mCallbacks.get(controllable.getControllableName()).contains(callback)) {
			mCallbacks.get(controllable.getControllableName()).add(callback);
		}
		return this;
	}

	@Override
	public FragmentController removeCallback(
			ControllableActivity<FragmentManager, FragmentTransaction> controllable,
			FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction> callback) {
		if (mCallbacks.containsKey(controllable.getControllableName())
				&& mCallbacks.get(controllable.getControllableName()).contains(callback)) {
			mCallbacks.remove(callback);
		}
		return this;
	}

	@Override
	public boolean sendTo(final String controllableName, int targetControlId, int what) {
		return sendTo(controllableName, targetControlId, what, 0, 0, null);
	}

	@Override
	public boolean sendTo(final String controllableName, int targetControlId, int what, int arg1) {
		return sendTo(controllableName, targetControlId, what, arg1, 0, null);
	}

	@Override
	public boolean sendTo(final String controllableName, int targetControlId, int what, Object obj) {
		return sendTo(controllableName, targetControlId, what, 0, 0, obj);
	}

	@Override
	public boolean sendTo(final String controllableName, int targetControlId, int what, int arg1, int arg2) {
		return sendTo(controllableName, targetControlId, what, arg1, arg2, null);
	}

	@Override
	public boolean sendTo(final String controllableName, int targetControlId, int what, int arg1, Object obj) {
		return sendTo(controllableName, targetControlId, what, arg1, 0, obj);
	}

	@Override
	public boolean sendTo(final String controllableName, int targetControlId, int what, int arg1, int arg2,
			Object obj) {
		boolean sendSuccess = false;
		Messenger mess = getMessengerFor(controllableName, targetControlId);
		if (null != mess) {
			Message m = Message.obtain();
			m.what = what;
			m.arg1 = arg1;
			m.arg2 = arg2;
			m.obj = obj;
			try {
				mess.send(m);
				sendSuccess = true;
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return sendSuccess;
	}

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following
	 * values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID : set to 0</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what passed as parameter of this method.<br>
	 * 
	 * @param controllableName
	 *            the controllable name to use
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public boolean send(String controllableName, int what) {
		return send(controllableName, what, 0, 0, null);
	}

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following
	 * values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID : set to 0</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1 passed as parameters of this method.<br>
	 * 
	 * @param controllableName
	 *            the controllable name to use
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public boolean send(String controllableName, int what, int arg1) {
		return send(controllableName, what, arg1, 0, null);
	}

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following
	 * values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID : set to 0</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, obj passed as parameters of this method.<br>
	 * 
	 * @param controllableName
	 *            the controllable name to use
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public boolean send(String controllableName, int what, Object obj) {
		return send(controllableName, what, 0, 0, obj);
	}

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following
	 * values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID : set to 0</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2 passed as parameters of this method.<br>
	 * 
	 * @param controllableName
	 *            the controllable name to use
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @param arg2
	 *            second message int argment
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public boolean send(String controllableName, int what, int arg1, int arg2) {
		return send(controllableName, what, arg1, arg2, null);
	}

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following
	 * values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID : set to 0</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, obj passed as parameters of this method.<br>
	 * 
	 * @param controllableName
	 *            the controllable name to use
	 * @param what
	 *            message ID to get dispatched by the controller
	 * @param arg1
	 *            first message int argument
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	@Override
	public boolean send(String controllableName, int what, int arg1, Object obj) {
		return send(controllableName, what, arg1, 0, obj);
	}

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following
	 * values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID : set to 0</dd>
	 * <dt>arg2</dt>
	 * <dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt>
	 * <dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2, obj passed as parameters of this
	 * method.<br>
	 * 
	 * @param controllableName
	 *            the controllable name to use
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
	public boolean send(String controllableName, int what, int arg1, int arg2, Object obj) {
		Message m = Message.obtain();
		m.what = MSG_DISPATCH_MESSAGE;
		m.arg1 = 0;
		m.arg2 = what;
		m.obj = CallbackMessage.obtain(arg1, arg2, obj);
		Bundle b = new Bundle();
		b.putString("controllableName", controllableName);
		m.setData(b);
		mHandler.sendMessage(m);
		return true;
	}

	@Override
	public Messenger getMessengerFor(final String controllableName, int controlId) {
		if (!mMessengers.containsKey(controllableName)) { return null; }
		return mMessengers.get(controllableName).get(controlId);
	}

	@Override
	public ControllableFragment getFragmentFor(final String controllableName, int controlId) {
		if (!mFragments.containsKey(controllableName)) { return null; }
		return mFragments.get(controllableName).get(controlId).get();
	}

	public FragmentController() {
	}

	private void doInit() {
		if (null == mHandler) {
			if (null == _handlerThread) {
				this._handlerThread = new HandlerThread("FragmentControllerApplication");
				_handlerThread.start();
			}
			this.mHandler = new Handler(_handlerThread.getLooper(), mHandlerCallback);
		}
	}

	/**
	 * Ends the handler thread, closes what needs to be closed
	 */
	private void doFinish() {
		mCallbacks.clear();
		mMessengers.clear();
		mFragments.clear();
		synchronized (this) {
			mHandler.removeCallbacksAndMessages(null);
			if (null != _handlerThread) {
				HandlerThread ht = _handlerThread;
				_handlerThread = null;
				ht.quit();
				ht.interrupt();
			}
		}
		mHandler = null;
	}

	private static String generateTag(int controlId, Class<? extends ControllableFragment> fclass,
			String controllableName) {
		return String.format(Locale.getDefault(), "%1$s__%2$d__%3$s", fclass.getCanonicalName(), controlId,
				controllableName);
	}

	private synchronized void attachControllableFragment(final String controllableName, int controlId,
			ControllableFragment fragment) throws ControlledFragmentException {
		Log.d("FragmentControllerApplication",
				"◆ attachControllableFragment :" + controlId + "[" + fragment.getFragmentName() + "]");
		if (!mFragments.containsKey(controllableName)) { return; }
		// controlId already defined, raise an error
		if (mFragments.get(controllableName).indexOfKey(controlId) > 0) { throw new ControlledFragmentException(
				String.format("Specified ControlID [%d] already defined: cannot add fragment", controlId)); }

		if (controlId == fragment.getControlId()) {
			mFragments.get(controllableName).put(controlId, new WeakReference<ControllableFragment>(fragment));
			mMessengers.get(controllableName).put(controlId, fragment.getMessenger());
		}
	}

	private synchronized void detachControllableFragment(final String controllableName, int controlId,
			ControllableFragment fragment) throws ControlledFragmentException {
		Log.d("FragmentControllerApplication",
				"◆ detachControllableFragment :" + controlId + "[" + fragment.getFragmentName() + "]");
		if (controlId == fragment.getControlId()) {
			if (mFragments.containsKey(controllableName)) {
				mFragments.get(controllableName).remove(controlId);
			}
			if (mMessengers.containsKey(controllableName)) {
				mMessengers.get(controllableName).remove(controlId);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction> newFragmentControllerCallback(
			ControllableActivity<?, ?> controllable, Class<? extends FragmentControllerCallbackAbstract<?, ?>> cls) {
		Constructor<?>[] allCs = cls.getDeclaredConstructors();
		FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction> callback = null;
		for (Constructor<?> c : allCs) {
			Class<?>[] types = c.getParameterTypes();
			if (types.length == 2) {
				try {
					callback = (FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction>) c
							.newInstance(controllable, this);
					return callback;
				}
				catch (ClassCastException e) {
					e.printStackTrace();
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				catch (InstantiationException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Returns a new {@link ControllableFragment} based on the input Class type.
	 * 
	 * The instantiation is delegated to the class implementing {@link ControllableFragment} passed as a parameter.
	 * <p>
	 * It looks up for a static {@code createFragment(Class<? extends ControllableFragment>, boolean, Bundle)}
	 * method should be defined in the class implementing {@link ControllableFragment} that is passed as the first
	 * parameter of this method.<br>
	 * E.g. {@linkplain ControlledFragment#createFragment} , {@linkplain ControlledDialogFragment#createFragment}.
	 * </p>
	 * <p>
	 * Reflection is used, so ControllableFragment should have their {@code createFragment} method obfuscated by
	 * ProGuard.
	 * </p>
	 * 
	 * @param fragmentClass
	 *            The subclass of ControllableFragment to instantiate
	 * @param runsInNewThread
	 *            passed as is to the constructor
	 * @param args
	 *            passed as is to the constructor
	 * @return ControllableFragment instance, or null in case of failure
	 */
	private ControllableFragment newControllableFragment(Class<? extends ControllableFragment> fragmentClass,
			boolean runsInNewThread, Bundle args) {
		Method[] allMs = fragmentClass.getMethods();
		ControllableFragment fragment = null;
		for (Method m : allMs) {
			if (!"createFragment".equals(m.getName())) {
				continue;
			}
			Class<?>[] types = m.getParameterTypes();
			if (types.length == 3 && types[0].equals(Class.class) && types[1].equals(boolean.class)
					&& types[2].equals(Bundle.class)) {
				try {
					fragment = (ControllableFragment) m.invoke(fragmentClass, fragmentClass, runsInNewThread, args);
					return fragment;
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private Handler.Callback mHandlerCallback = new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			Log.d("FragmentControllerApplication", "■ handleMessage : from : " + msg.arg1 + " what = " + msg.arg2);
			boolean handled = false;
			// arg1 == controlId (source of the message), arg2 == what (target),
			// obj == Message (target)
			// obj.arg1 == target arg1, obj.arg2 == target arg2, obj.obj == target obj
			// bundle data : controllableName => the target controllable name
			String controllableName = msg.getData().getString("controllableName");
			if (MSG_DISPATCH_MESSAGE == msg.what) {
				Message targetMesg = (Message) msg.obj;
				targetMesg.what = msg.arg2;
				if (null == controllableName || null == mCallbacks.get(controllableName)) {
					Log.d("FragmentControllerApplication", "Received dead message");
					return false;
				}
				for (FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction> c : mCallbacks
						.get(controllableName)) {
					handled = c.handleMessage(msg.arg1, targetMesg) || handled;
				}
			}
			else if (MSG_ATTACH_MESSAGE == msg.what) {
				try {
					attachControllableFragment(controllableName, msg.arg1, (ControllableFragment) msg.obj);
				}
				catch (ControlledFragmentException e) {
					e.printStackTrace();
				}
			}
			else if (MSG_DETACH_MESSAGE == msg.what) {
				try {
					detachControllableFragment(controllableName, msg.arg1, (ControllableFragment) msg.obj);
				}
				catch (ControlledFragmentException e) {
					e.printStackTrace();
				}
			}
			return handled;
		}
	};

	private Map<String, SparseArray<WeakReference<ControllableFragment>>> mFragments = new HashMap<String, SparseArray<WeakReference<ControllableFragment>>>();
	private Map<String, SparseArray<Messenger>> mMessengers = new HashMap<String, SparseArray<Messenger>>();
	private Map<String, ArrayList<FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction>>> mCallbacks = new HashMap<String, ArrayList<FragmentControllerCallbackAbstract<FragmentManager, FragmentTransaction>>>();

	private int mRefCount = 0;
	private Handler mHandler;
	private HandlerThread _handlerThread;
}

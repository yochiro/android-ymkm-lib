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

package org.ymkm.lib.controller.core;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.ymkm.lib.controller.ControlledFragment;
import org.ymkm.lib.controller.FragmentControllerApplication;

/**
 * TODO
 *
 * <p>
 * The message handling is performed inside the {@link FragmentControllerCallbackAbstract#handleMessage(int, Message)}
 * method, which mimicks the {@link Handler.Callback#handleMessage(Message)}, with the difference
 * that an additional {@code sourceControlId} parameter is passed alongwith the {@link Message}.<br>
 * It defines the control ID that sent the message, which can be used by the implementor if needed.
 * </p>
 * <p>
 * To allow interaction between {@link ControlledFragment}, this class supplies a
 * {@link FragmentControllerCallbackAbstract#sendTo(int, int, int, int, Object)} methods, which takes the
 * target control ID as its first parameter, defining which {@link ControlledFragment}'s {@link Handler} should process the message.<br>
 * A query is made to the controller using {@link FragmentControllerApplication#getMessengerFor(int)} with the target control ID
 * as the parameter, which returns (if available) the {@link Messenger} for the given fragment.
 * </p>
 * <p>
 * {@link FragmentControllerCallbackAbstract#doHandleMessage(int, Message)} should be overridden by subclasses and contain the
 * message handling that the related fragment can process.<br>
 * variations of the {@code sendTo} method can be called to dispatch messages to other parts of the system in response to its own.
 * </p>
 */
public abstract class FragmentControllerCallbackAbstract<FM, FT> {

	/**
	 * Creates a new Callback usable by {@link FragmentControllerApplication}.
	 * 
	 * @param the
	 *            {@linkparam Activity} containing this callback. May be used by subclasses if needed
	 * @param controller
	 *            the controller that owns it
	 */
	protected FragmentControllerCallbackAbstract(final ControllableActivity<FM,FT> controllable, FragmentControllerInterface<FM,FT> controller) {
		 mController = controller;
		 mControllable = controllable;
	}

	public boolean handleMessage(int sourceControlId, Message msg) {
		return doHandleMessage(sourceControlId, msg);
	}

	protected boolean sendTo(int targetControlId, int what) {
		return sendTo(targetControlId, what, 0, 0, null);
	}

	protected boolean sendTo(int targetControlId, int what, int arg1) {
		return sendTo(targetControlId, what, arg1, 0, null);
	}

	protected boolean sendTo(int targetControlId, int what, Object obj) {
		return sendTo(targetControlId, what, 0, 0, obj);
	}

	protected boolean sendTo(int targetControlId, int what, int arg1, int arg2) {
		return sendTo(targetControlId, what, arg1, arg2, null);		
	}

	protected boolean sendTo(int targetControlId, int what, int arg1, Object obj) {
		return sendTo(targetControlId, what, arg1, 0, obj);
	}

	protected boolean sendTo(int targetControlId, int what, int arg1, int arg2, Object obj) {
		return mController.sendTo(mControllable.getControllableName(), targetControlId, what, arg1, arg2, obj);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <br>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The source control ID passed as a parameter</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what passed as parameter of this method.<br>
	 * 
	 * @param sourceControlId source control ID to set
	 * @param what message ID to get dispatched by the controller
	 * @return {@code true} if message could be sent, {@code false} otherwise	 */
	protected boolean sendToController(int sourceControlId, int what) {
		return sendToController(sourceControlId, what, 0, 0, null);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <br>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The source control ID passed as a parameter</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1 passed as parameters
	 * of this method.<br>
	 * 
	 * @param sourceControlId source control ID to set
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @return {@code true} if message could be sent, {@code false} otherwise	 */
	protected boolean sendToController(int sourceControlId, int what, int arg1) {
		return sendToController(sourceControlId, what, arg1, 0, null);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <br>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The source control ID passed as a parameter</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, obj passed as parameters
	 * of this method.<br>
	 * 
	 * @param sourceControlId source control ID to set
	 * @param what message ID to get dispatched by the controller
	 * @param obj Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	protected boolean sendToController(int sourceControlId, int what, Object obj) {
		return sendToController(sourceControlId, what, 0, 0, obj);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <br>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The source control ID passed as a parameter</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2 passed as parameters
	 * of this method.<br>
	 * 
	 * @param sourceControlId source control ID to set
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @param arg2 second message int argment
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	protected boolean sendToController(int sourceControlId, int what, int arg1, int arg2) {
		return sendToController(sourceControlId, what, arg1, arg2, null);
	}

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <br>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The source control ID passed as a parameter</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, obj passed as parameters
	 * of this method.<br>
	 * 
	 * @param sourceControlId source control ID to set
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @param obj Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	protected boolean sendToController(int sourceControlId, int what, int arg1, Object obj) {
		return sendToController(sourceControlId, what, arg1, 0, obj);
	}
	
	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <br>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The source control ID passed as a parameter</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2, obj passed as parameters
	 * of this method.<br>
	 * 
	 * @param sourceControlId source control ID to set
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @param arg2 second message int argment
	 * @param obj Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	protected boolean sendToController(int sourceControlId, int what, int arg1, int arg2, Object obj) {
		Messenger messenger = getController().getMessenger();
		if (null != messenger) {
			Message m = Message.obtain();
			m.what = FragmentControllerInterface.MSG_DISPATCH_MESSAGE;
			m.arg1 = sourceControlId;
			m.arg2 = what;
			m.obj = FragmentControllerInterface.CallbackMessage.obtain(arg1, arg2, obj);
			Bundle b = new Bundle();
			b.putString("controllableName", getControllableName());
			m.setData(b);
			try {
				messenger.send(m);
			} catch (RemoteException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	protected void sendToUi(Runnable runnable) {
		getActivity().runOnUiThread(runnable);
	}
	

	protected String getControllableName() {
		return mControllable.getControllableName();
	}
	
	protected FragmentControllerInterface<FM,FT> getController() {
		return mController;
	}

	protected ControllableActivity<FM,FT> getControllableActivity() {
		return mControllable;
	}
	
	protected Context getContext() {
		return (Context) mControllable;
	}
	
	protected Activity getActivity() {
		return (Activity) mControllable;
	}

	protected abstract boolean doHandleMessage(int sourceControlId, Message msg);


	/** Reference to the controller */
	private FragmentControllerInterface<FM,FT> mController;
	
	/** Controllable name */
	private ControllableActivity<FM,FT> mControllable;
}

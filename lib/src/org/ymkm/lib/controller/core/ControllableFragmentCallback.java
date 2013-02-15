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

import android.os.Handler.Callback;
import android.os.Message;
import android.os.Messenger;

/**
 * TODO 
 * 
 * <p>
 * - Note : A constructor that takes the {@link ControllableFragment} as a parameter MUST be defined in the subclass<br>
 * - When defined as an inner class of another class, it will added automatically by the compiler,
 * unless the {@code static} keyword is used, which will cause the empty constructor to be created instead.<br>
 * </p>
 * <p>
 * All messages sent using any variation of {@link ControllableFragmentCallback#sendToController()} are
 * sent to the controller using {@linkplain FragmentControllerApplication#MSG_DISPATCH_MESSAGE} as the message.
 * </p>
 */
public interface ControllableFragmentCallback extends Callback {

	/**
	 * Initializes this {@linkplain ControllableFragmentCallback} by setting the {@link ControllerFragment}
	 * <p>
	 * Subclass contract : superclass doInit MUST be called, otherwise an assertion failure will occur.
	 * </p>
	 * 
	 * @param controllerFragment
	 */
	void init(ControllableFragment controllerFragment);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragment} can only communicate with the {@link FragmentControllerApplication}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The control ID assigned to the ControllableFragment that owns this callback</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what passed as parameter of this method.<br>
	 * 
	 * @param what message ID to get dispatched by the controller
	 * @return {@code true} if message could be sent, {@code false} otherwise	 */
	boolean sendToController(int what);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragment} can only communicate with the {@link FragmentControllerApplication}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The control ID assigned to the ControllableFragment that owns this callback</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1 passed as parameters
	 * of this method.<br>
	 * 
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @return {@code true} if message could be sent, {@code false} otherwise	 */
	boolean sendToController(int what, int arg1);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragment} can only communicate with the {@link FragmentControllerApplication}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The control ID assigned to the ControllableFragment that owns this callback</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, obj passed as parameters
	 * of this method.<br>
	 * 
	 * @param what message ID to get dispatched by the controller
	 * @param obj Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToController(int what, Object obj);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragment} can only communicate with the {@link FragmentControllerApplication}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The control ID assigned to the ControllableFragment that owns this callback</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2 passed as parameters
	 * of this method.<br>
	 * 
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @param arg2 second message int argment
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToController(int what, int arg1, int arg2);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragment} can only communicate with the {@link FragmentControllerApplication}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The control ID assigned to the ControllableFragment that owns this callback</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, obj passed as parameters
	 * of this method.<br>
	 * 
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @param obj Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToController(int what, int arg1, Object obj);
	
	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragment} can only communicate with the {@link FragmentControllerApplication}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the controller,
	 * and has the following values :
	 * <dl>
	 * <dt>what</dt><dd>{@link FragmentControllerApplication#MSG_DISPATCH_MESSAGE} => Dispatches the message in the controller</dd>
	 * <dt>arg1</dt><dd>The control ID assigned to the ControllableFragment that owns this callback</dd>
	 * <dt>arg2</dt><dd>The {@code what} passed as a parameter that needs to be dispatched</dd>
	 * <dt>obj</dt><dd>{@link Message} that will get dispatched (the {@code what} will be added to it)</dd>
	 * </dl>
	 * obj is a message instance that will eventually contain what, arg1, arg2, obj passed as parameters
	 * of this method.<br>
	 * 
	 * @param what message ID to get dispatched by the controller
	 * @param arg1 first message int argument
	 * @param arg2 second message int argment
	 * @param obj Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToController(int what, int arg1, int arg2, Object obj);

	/**
	 * Returns true if the current callback runs in the UI thread
	 * <p>
	 * This may be used within the {@linkplain Callback#handleMessage(Message)} to force
	 * certain operations to run in the UI thread.
	 * </p>
	 * 
	 * @return true if callback runs in the UI thread, false otherwise
	 */
	boolean runsOnUiThread();

	/**
	 * Sends the specified Runnable to run on the UI thread
	 * 
	 * @param runnable the task to run on the UI thread
	 * @return itself for chaining
	 */	
	ControllableFragmentCallback sendToUi(Runnable runnable);
}

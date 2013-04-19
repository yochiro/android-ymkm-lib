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

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;


/**
 * Defines a {@link Fragment} that is controllable by a {@link FragmentControllerApplication}.
 * 
 */
public interface ControllableFragmentInterface extends MessageCallback {

	/**
	 * Returns true if the handler runs in a separate thread
	 * 
	 * @return true if it has its own thread for the inner Handler, false
	 *         otherwise
	 */
	boolean hasOwnThread();

	/**
	 * Returns a name for this fragment that can be used to identify the thread
	 * the handler is running within
	 * 
	 * @return the fragment name
	 */
	String getFragmentName();

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragmentInterface} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControllableFragment that owns this
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
	boolean sendToController(int what);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragmentInterface} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControllableFragment that owns this
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
	boolean sendToController(int what, int arg1);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragmentInterface} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControllableFragment that owns this
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
	boolean sendToController(int what, Object obj);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragmentInterface} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControllableFragment that owns this
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
	boolean sendToController(int what, int arg1, int arg2);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragmentInterface} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControllableFragment that owns this
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
	boolean sendToController(int what, int arg1, Object obj);

	/**
	 * Sends a message to the controller via its supplied {@link Messenger}
	 * <p>
	 * A {@link ControllableFragmentInterface} can only communicate with the
	 * {@link FragmentController}.
	 * </p>
	 * The sent message wraps the actual message that will be dispatched by the
	 * controller, and has the following values :
	 * <dl>
	 * <dt>what</dt>
	 * <dd>{@link FragmentController#MSG_DISPATCH_MESSAGE} => Dispatches the
	 * message in the controller</dd>
	 * <dt>arg1</dt>
	 * <dd>The control ID assigned to the ControllableFragment that owns this
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
	boolean sendToController(int what, int arg1, int arg2, Object obj);

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToSelf(int what);

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param arg1
	 *            first message int argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToSelf(int what, int arg1);

	/**
	 * Sends a message to itself
	 * 
	 * @param what
	 *            message ID to send to itself
	 * @param obj
	 *            Object argument
	 * @return {@code true} if message could be sent, {@code false} otherwise
	 */
	boolean sendToSelf(int what, Object obj);

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
	boolean sendToSelf(int what, int arg1, int arg2);

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
	boolean sendToSelf(int what, int arg1, Object obj);

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
	boolean sendToSelf(int what, int arg1, int arg2, Object obj);

	/**
	 * Sends the specified Runnable to run on the UI thread
	 * 
	 * @param runnable
	 *            the task to run on the UI thread
	 * @return itself for chaining
	 */
	ControllableFragmentInterface sendToUi(Runnable runnable);
	
	/**
	 * Returns the control Id associated with this fragment
	 * 
	 * @return the control ID
	 */
	int getControlId();

	/**
	 * Returns the handler associated with this {@link ControllableFragmentInterface}
	 * 
	 * @return the Handler
	 */
	Handler getHandler();
}

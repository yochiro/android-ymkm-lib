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

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.ymkm.lib.controller.FragmentControllerApplication;

/**
 * TODO
 * 
 */
public interface FragmentControllerInterface<FM, FT> extends MessageCallback, RunnableQueue {

	/**
	 * Message for dispatching
	 */
	public final static int MSG_DISPATCH_MESSAGE = 0x10000;
	/**
	 * Message sent by ControllableFragment on attach arg1 = controlId obj = ControllableFragment data :
	 * Bundle['controllableName']
	 */
	public final static int MSG_ATTACH_MESSAGE = 0x20000;
	/**
	 * Message sent by ControllableFragment on detach arg1 = controlId obj = ControllableFragment data :
	 * Bundle['controllableName']
	 */
	public final static int MSG_DETACH_MESSAGE = 0x40000;

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
	 * Registers a new {@linkplain ControllableActivity} to this controller
	 * 
	 * @param controllable
	 *            the {@linkplain ControllableActivity} to register
	 * @return the FragmentControllerApplication for chaining
	 */
	FragmentControllerInterface<FM, FT> register(ControllableActivity<FM, FT> controllable);

	/**
	 * Unregisters the {@link FragmentControllerApplication} from the current activity.
	 * 
	 * @param controllable
	 *            the {@linkplain ControllableActivity} to unregister
	 * @return the FragmentControllerApplication for chaining
	 */
	FragmentControllerInterface<FM, FT> unregister(ControllableActivity<FM, FT> controllable);

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			Class<? extends ControllableFragment> fragmentClass) throws ControlledFragmentException,
			FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			Class<? extends ControllableFragment> fragmentClass, Bundle args) throws ControlledFragmentException,
			FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread, Bundle args)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass, Bundle args)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread,
			Bundle args) throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			ControllableFragment fragment) throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> add(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, ControllableFragment fragment) throws ControlledFragmentException,
			FragmentControllerException;

	FragmentControllerInterface<FM, FT> addToBackStack(FT ft, String name) throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> show(final ControllableActivity<FM, FT> controllable, FT ft, int controlId)
			throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> hide(final ControllableActivity<FM, FT> controllable, FT ft, int controlId)
			throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> setTransition(FT ft, int transit) throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> setTransitionStyle(FT ft, int styleRes) throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> setCustomAnimations(FT ft, int enter, int exit)
			throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> setCustomAnimations(FT ft, int enter, int exit, int popEnter, int popExit)
			throws FragmentControllerException;

	FragmentControllerInterface<FM, FT> replace(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> replace(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> replace(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass, Bundle args)
			throws ControlledFragmentException, FragmentControllerException;

	FragmentControllerInterface<FM, FT> replace(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, Class<? extends ControllableFragment> fragmentClass, boolean runsInNewThread,
			Bundle args) throws ControlledFragmentException, FragmentControllerException;
	
	FragmentControllerInterface<FM, FT> replace(final ControllableActivity<FM, FT> controllable, FT ft, int controlId,
			int containerViewId, ControllableFragment fragment);

	FragmentControllerInterface<FM, FT> remove(final ControllableActivity<FM, FT> controllable, FT ft, int controlId)
			throws FragmentControllerException;

	boolean hasControlId(final String controllableName, int controlId);

	FragmentControllerInterface<FM, FT> addCallback(ControllableActivity<FM, FT> controllable,
			FragmentControllerCallbackAbstract<FM, FT> callback);

	FragmentControllerInterface<FM, FT> removeCallback(ControllableActivity<FM, FT> controllable,
			FragmentControllerCallbackAbstract<FM, FT> callback);

	boolean sendTo(final String controllableName, int targetControlId, int what);

	boolean sendTo(final String controllableName, int targetControlId, int what, int arg1);

	boolean sendTo(final String controllableName, int targetControlId, int what, Object obj);

	boolean sendTo(final String controllableName, int targetControlId, int what, int arg1, int arg2);

	boolean sendTo(final String controllableName, int targetControlId, int what, int arg1, Object obj);

	boolean sendTo(final String controllableName, int targetControlId, int what, int arg1, int arg2, Object obj);

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following values
	 * :
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
	boolean send(String controllableName, int what);

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following values
	 * :
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
	boolean send(String controllableName, int what, int arg1);

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following values
	 * :
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
	boolean send(String controllableName, int what, Object obj);

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following values
	 * :
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
	boolean send(String controllableName, int what, int arg1, int arg2);

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following values
	 * :
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
	boolean send(String controllableName, int what, int arg1, Object obj);

	/**
	 * Sends a message to this controller via its supplied {@link Messenger} <br>
	 * The sent message wraps the actual message that will be dispatched by the controller, and has the following values
	 * :
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
	 * obj is a message instance that will eventually contain what, arg1, arg2, obj passed as parameters of this method.<br>
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
	boolean send(String controllableName, int what, int arg1, int arg2, Object obj);

	Messenger getMessengerFor(final String controllableName, int controlId);

	ControllableFragment getFragmentFor(final String controllableName, int controlId);
}

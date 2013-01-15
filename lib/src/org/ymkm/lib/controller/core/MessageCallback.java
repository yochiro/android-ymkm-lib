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

import android.os.Messenger;


/**
 * Provides an interface to an entity that returns a {@link Messenger} through the {@link MessageCallback#getMessenger()} method.
 *
 */
public interface MessageCallback {

	/**
	 * Returns a {@link Messenger} tied to the implementing class
	 * 
	 * @return the {@link Messenger}
	 */
	Messenger getMessenger();
}

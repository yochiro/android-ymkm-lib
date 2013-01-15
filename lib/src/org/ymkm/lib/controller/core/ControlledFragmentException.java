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

import org.ymkm.lib.controller.ControlledFragment;

/**
 * Exception related to {@link ControlledFragment}
 */
public class ControlledFragmentException extends Exception {

	/**
	 * Creates a new Exception
	 * 
	 * @param message the exception message
	 */
	public ControlledFragmentException(String message) {
		super(message);
	}

	/**
	 * Serial ID
	 */
	private static final long serialVersionUID = 3056429364721934083L;
}

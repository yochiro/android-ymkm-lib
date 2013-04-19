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

package org.ymkm.lib.controller;

import android.annotation.TargetApi;
import android.app.Application;

@TargetApi(11)
/**
 * TODO
 * 
 */
public class FragmentControllerApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public final void register(ControllableActivity controllable) {
		_controller.register(controllable);
	}

	public final void unregister(ControllableActivity controllable) {
		_controller.unregister(controllable);
	}

	public final FragmentController getController() {
		return _controller;
	}

	private FragmentController _controller = new FragmentController();
}
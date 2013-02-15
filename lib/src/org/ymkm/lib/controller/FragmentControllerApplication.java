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

import org.ymkm.lib.controller.core.ControllableActivity;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

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

	public void register(ControllableActivity<FragmentManager, FragmentTransaction> controllable) {
		_controller.register(controllable);
	}

	public void unregister(ControllableActivity<FragmentManager, FragmentTransaction> controllable) {
		_controller.unregister(controllable);
	}

	public FragmentController getController() {
		return _controller;
	}

	private FragmentController _controller = new FragmentController();
}
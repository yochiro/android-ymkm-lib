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

import java.util.List;

import org.ymkm.lib.controller.core.ControllableActivityInterface;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public interface ControllableActivity extends ControllableActivityInterface<FragmentManager, FragmentTransaction> {

	FragmentController getController();

	List<Class<? extends FragmentControllerCallback>> getCallbacks();
}
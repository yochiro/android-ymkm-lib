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

/**
 * Provides an interface that allows Runnable instances to be
 * queued then executed sequentially in FIFO order.
 * 
 * Interface follows the {@linkplain Handler} method signatures, and
 * thus can be implemented using a {@linkplain Handler}.
 */
public interface RunnableQueue {
	void post(Runnable runnable);
	
	void postAtTime(Runnable runnable, long uptimeMillis);
	
	void postAtTime(Runnable runnable, Object token, long uptimeMillis);
	
	void postDelayed(Runnable runnable, long delayMillis);
	
	void removeCallbacks(Runnable runnable);
	
	void removeCallbacks(Runnable runnable, Object token);
}

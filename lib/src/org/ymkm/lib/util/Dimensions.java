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

package org.ymkm.lib.util;

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

public class Dimensions {

	/**
	 * Gets the display width
	 *
	 * @param c Context
	 * @return int display width
	 */
	@SuppressWarnings("deprecation")
	public static int getDisplayWidth(Context c) {
		WindowManager wm = (WindowManager) c
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return display.getWidth();
	}

	/**
	 * Gets the display height
	 *
	 * @param c Context
	 * @return int display height
	 */
	@SuppressWarnings("deprecation")
	public static int getDisplayHeight(Context c) {
		WindowManager wm = (WindowManager) c
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		return display.getHeight();
	}

	/**
	 * Gets the display density
	 *
	 * @param c Context
	 * @return float display density
	 */
	public static float getDisplayDensity(Context c) {
		return c.getResources().getDisplayMetrics().density;
	}

	/**
	 * Converts pixels into density-independent pixels (dip)
	 *
	 * @param c Context
	 * @param px pixels to convert into dip
	 * @return float pixels in dip
	 */
	public static float pixelsToDip(Context c, int px) {
		return px / c.getResources().getDisplayMetrics().density;
	}

	/**
	 * Converts pixels into density-independent pixels (dip)
	 *
	 * @param c Context
	 * @param px pixels to convert into dip
	 * @return float pixels in dip
	 */
	public static float pixelsToDip(Context c, float px) {
		return px / c.getResources().getDisplayMetrics().density;
	}

	/**
	 * Converts density-independent pixels (dip) into pixels
	 *
	 * @param c Context
	 * @param px dip pixels to convert into pixels
	 * @return float pixels
	 */
	public static float dipToPixels(Context c, int px) {
		return c.getResources().getDisplayMetrics().density * px;
	}

	/**
	 * Converts density-independent pixels (dip) into pixels
	 *
	 * @param c Context
	 * @param px dip pixels to convert into pixels
	 * @return float pixels
	 */
	public static float dipToPixels(Context c, float px) {
		return c.getResources().getDisplayMetrics().density * px;
	}
}

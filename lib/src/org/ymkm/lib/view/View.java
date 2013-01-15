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

package org.ymkm.lib.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import org.ymkm.lib.R;

/**
 * Extends default View with additional XML properties
 * to create Views which are size constrained based on
 * another dimension (e.g. width VS height), or to specify
 * a ratio to apply on each dimension.
 * <p>
 * This is useful when one does not want to specify hard-coded dimensions for the
 * width or the height, even if these are defined in dp as they should be (avoid px!).<br>
 * One could just define width/height to be either {@code wrap_content} or {@code match_parent}
 * and use width/height ratios to dynamically adjust dimensions to the layout.<br>
 * This is similar to the way we specify % as CSS values for width/height in HTML.
 * </p>
 * <p>
 * Assuming ADT version is 17 or newer, the following XMl namespace declaration must be added :
 * <code>http://schemas.android.com/apk/res-auto</code>
 * E.g. to alias the namespace with ymkm :
 * {@code xmlns:ymkm="http://schemas.android.com/apk/lib/org.ymkm.lib"}
 * <dl>
 * <dt>{@code ymkm:heightRatio} : <em>{@code float} (default: 1.0)</em></dt>
 * <dd>Defines the height ratio. E.g. 0.75 means height should be 75% of its declared/calculated size</dd>
 * <dt>{@code ymkm:widthRatio} : <em>{@code float} (default: 1.0)</em></dt>
 * <dd>Defines the width ratio. E.g. 0.5 means width should be 50% of its declared/calculated size</dd>
 * <dt>{@code ymkm:baseDimension} : <em>{@code string}</em> (default: none)</dt>
 * <dd>If defined, sets both width and height to the same base value, based on the following accepted values :<br>
 * <ul>
 * <li>{@code none} : {@link android.view.View} default behavior. width and height are unchanged</li>
 * <li>{@code width} : height will be the same as width</li>
 * <li>{@code height} : width will be the same as height</li>
 * <li>{@code min} : width = min(width, height), height = min(width, height)</li>
 * <li>{@code max} : width = max(width, height), height = max(width, height)</li>
 * </ul>
 * </dd>
 * </dl>
 * </p>
 * <p>
 * Note: {@code heightRatio} and {@code widthRatio} can be combined with {@code baseDimension}.<br>
 * The ratio will be applied after the base dimension has been applied.<br>
 * This allows to create size-constrained views such as view height = 75% of view width, or view height = 2 * view width ...
 * </p>
 * <p>
 * {@link View.onSizeChangedListener} is a callback that can be registered
 * on an View instance which will be called whenever the View size has changed.<br>
 * This will occur after onMeasure is called, in which the above calculations on width/height are performed.
 * </p>
 * 
 * @author yoann@ymkm.com
 */
public class View extends android.view.View {

	/**
	 * No base dimension defined. Keep original width and height value. Default
	 */
	public final static int ATTR_BASE_DIMENSION_NONE = -1;
	/**
	 * Sets height to the value of width
	 */
	public final static int ATTR_BASE_DIMENSION_WIDTH = 0;
	/**
	 * Sets width to the value of height
	 */
	public final static int ATTR_BASE_DIMENSION_HEIGHT = 1;
	/**
	 * Sets width and height to be the lesser value of original width/height
	 */
	public final static int ATTR_BASE_DIMENSION_MIN = 2;
	/**
	 * Sets width and height to be the bigger value of original width/height
	 */
	public final static int ATTR_BASE_DIMENSION_MAX = 3;

	/**
	 * Callback that can be set on {@linkplain org.ymkm.lib.view.View} which gets called
	 * everytime the width/height of the view is changed during the layout.<br>
	 * Called from {@linkplain android.view.View#onSizeChanged(int,int,int,int)}.
	 * <p>
	 * Note : If old and new values of width/height are the same when onSizeChanged is called,
	 * this callback will not be triggered. At least one of the dimension has to differ.
	 * </p>
	 */
	public interface OnSizeChangedListener {
		public void onSizeChanged(android.view.View iv, int w, int h, int oldW, int oldH);
	};
	
	public View(Context context) {
		this(context, null, 0);
	}

	public View(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public View(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}
	
	public void setBaseDimension(int baseDimension) {
		mBaseDimension = baseDimension;
	}

	public int getBaseDimension() {
		return mBaseDimension;
	}
	
	public void setHeightRatio(float ratio) {
		mHeightRatio = ratio;
	}

	public float getHeightRatio() {
		return mHeightRatio;
	}

	public void setWidthRatio(float ratio) {
		mWidthRatio = ratio;
	}

	public float getWidthRatio() {
		return mWidthRatio;
	}

	/**
	 * Sets a {@linkplain View#OnSizeChangedListener} callback on this view.
	 * 
	 * @param l the listener to attach
	 */
	public void setOnSizeChangedListener(OnSizeChangedListener l) {
		mSizeListener = l;
	}
	
	/**
	 * Removes a previously registered {@linkplain View#OnSizeChangedListener} callback from this view.
	 */
	public void removeOnSizeChangedListener() {
		mSizeListener = null;
	}

	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);

		switch (mBaseDimension) {
			case ATTR_BASE_DIMENSION_WIDTH: {
				chosenHeight = chosenWidth;
				break;
			}
			case ATTR_BASE_DIMENSION_HEIGHT: {
				chosenWidth = chosenHeight;
				break;
			}
			case ATTR_BASE_DIMENSION_MIN: {
				chosenWidth = Math.min(chosenWidth, chosenHeight);
				chosenHeight = chosenWidth;
				break;
			}
			case ATTR_BASE_DIMENSION_MAX: {
				chosenWidth = Math.max(chosenWidth, chosenHeight);
				chosenHeight = chosenWidth;
				break;
			}
		}

		setMeasuredDimension((int)Math.floor(chosenWidth*mWidthRatio), (int)Math.floor(chosenHeight*mHeightRatio));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (null != mSizeListener && (w != oldw || h != oldh)) {
			mSizeListener.onSizeChanged(this, w, h, oldw, oldh);
		}
	}


	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else {
			return 100;
		} 
	}

	private void init(Context context, AttributeSet attrs, int defStyle) {
		if (null == attrs) { return; }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.View, defStyle, 0);
        mHeightRatio = a.getFloat(R.styleable.View_heightRatio, 1.0f);
        mWidthRatio = a.getFloat(R.styleable.View_widthRatio, 1.0f);
        String baseDimension = a.getString(R.styleable.View_baseDimension);
        a.recycle();

		if (null != baseDimension)  {
			if ("none".equals(baseDimension)) {
				mBaseDimension = ATTR_BASE_DIMENSION_NONE;
			}
			if ("width".equals(baseDimension)) {
				mBaseDimension = ATTR_BASE_DIMENSION_WIDTH;
			}
			else if ("height".equals(baseDimension)) {				
				mBaseDimension = ATTR_BASE_DIMENSION_HEIGHT;
			}
			else if ("min".equals(baseDimension)) {
				mBaseDimension = ATTR_BASE_DIMENSION_MIN;
			}
			else if ("max".equals(baseDimension)) {
				mBaseDimension = ATTR_BASE_DIMENSION_MAX;
			}
		}
	}

	private float mHeightRatio = 1.0f;
	private float mWidthRatio = 1.0f;
	private int mBaseDimension = ATTR_BASE_DIMENSION_NONE;
	private OnSizeChangedListener mSizeListener = null;
}

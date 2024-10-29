package com.pocket.ui.util;

import android.view.View;

/**
 * Create an instance in your view class and then implement onMeasure like so:
 *
 * <pre>
 		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec);
			heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	</pre>
 *
 */
public class IntrinsicSizeHelper {
	
	private final int width;
	private final int height;
	
	public IntrinsicSizeHelper(int diameter) {
		this(diameter, diameter);
	}
	
	/**
	 * @param width Intrinsic width in px, or -1 to not declare one
	 * @param height Intrinsic height in px, or -1 to not declare one
	 */
	public IntrinsicSizeHelper(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public int applyWidth(int measureSpec) {
		return applyDimension(measureSpec, width);
	}
	
	public int applyHeight(int measureSpec) {
		return applyDimension(measureSpec, height);
	}
	
	private int applyDimension(int measureSpec, int defaultSize) {
		if (defaultSize < 0)  {
			return measureSpec;
		}
		int specMode = View.MeasureSpec.getMode(measureSpec);
		int specSize = View.MeasureSpec.getSize(measureSpec);
		if (specMode == View.MeasureSpec.EXACTLY) {
			return measureSpec;
		} else {
			if (specMode == View.MeasureSpec.AT_MOST) {
				specSize = Math.min(defaultSize, specSize);
			} else {
				specSize = defaultSize;
			}
			return View.MeasureSpec.makeMeasureSpec(specSize, View.MeasureSpec.EXACTLY);
		}
	}
	
}

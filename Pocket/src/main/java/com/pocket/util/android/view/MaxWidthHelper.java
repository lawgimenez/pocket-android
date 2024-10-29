package com.pocket.util.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.ideashower.readitlater.R;

import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;

/**
 * Encapsulate the necessary logic to add maxWidth functionality to a view.
 * 
 * @author marcin
 */

public final class MaxWidthHelper {

	private int maxWidth;

	public MaxWidthHelper() {}

	public MaxWidthHelper(Context context, AttributeSet attrs) {
		this(context, attrs, 0, 0);
	}

	public MaxWidthHelper(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MaxWidthHelper(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.MaxWidthView,
				defStyleAttr,
				defStyleRes);
		maxWidth = a.getDimensionPixelSize(R.styleable.MaxWidthView_maxWidth, 0);
		a.recycle();
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	public int onMeasure(int widthMeasureSpec) {
		// Adjust width if necessary
		int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec);
		if (maxWidth > 0 && maxWidth < measuredWidth) {
			int measureMode = View.MeasureSpec.getMode(widthMeasureSpec);
			widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, measureMode);
		}
		return widthMeasureSpec;
	}
	
	interface MaxWidthView {
		int getMaxWidth();
		void setMaxWidth(int maxWidth);
	}
}

package com.pocket.ui.view.scroll;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.pocket.ui.view.themed.ThemedNestedScrollView;

/**
 * A {@link ThemedNestedScrollView} that will not intercept touch events if its content is not scrollable.
 */
public class YieldingNestedScrollView extends ThemedNestedScrollView {
	
	private boolean isScrollable;
	
	public YieldingNestedScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public YieldingNestedScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public YieldingNestedScrollView(Context context) {
		super(context);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		isScrollable = getChildCount() > 0 && (
				getHeight() < getChildAt(0).getHeight() + getPaddingTop() + getPaddingBottom() ||
				getWidth() < getChildAt(0).getWidth() + getPaddingLeft() + getPaddingRight()
				);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (isScrollable) {
			return super.onInterceptTouchEvent(ev);
		} else {
			return false;
		}
	}
	
}

package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public abstract class ThemedViewGroup extends ViewGroup {
	
	public ThemedViewGroup(Context context) {
		super(context);
	}
	
	public ThemedViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ThemedViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public ThemedViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, AppThemeUtil.getState(this));
		return state;
	}
	
}

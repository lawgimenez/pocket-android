package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ThemedView extends View {
	
	public ThemedView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public ThemedView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ThemedView(Context context) {
		super(context);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, AppThemeUtil.getState(this));
		return state;
	}
	
}

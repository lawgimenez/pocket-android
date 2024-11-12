package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;

public class ThemedSeekBar extends AppCompatSeekBar {
	
	public ThemedSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public ThemedSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ThemedSeekBar(Context context) {
		super(context);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, AppThemeUtil.getState(this));
		return state;
	}
}

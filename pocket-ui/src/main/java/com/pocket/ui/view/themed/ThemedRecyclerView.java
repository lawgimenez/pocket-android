package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;

public class ThemedRecyclerView extends RecyclerView {

	public ThemedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ThemedRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ThemedRecyclerView(Context context) {
		super(context);
	}
	
	@Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] state = super.onCreateDrawableState(extraSpace + 1);
        mergeDrawableStates(state, AppThemeUtil.getState(this));
        return state;
    }

}

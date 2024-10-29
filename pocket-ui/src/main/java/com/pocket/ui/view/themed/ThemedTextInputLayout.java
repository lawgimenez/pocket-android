package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.textfield.TextInputLayout;

public class ThemedTextInputLayout extends TextInputLayout {

    public ThemedTextInputLayout(Context context) {
        super(context);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] state = super.onCreateDrawableState(extraSpace + 1);
        mergeDrawableStates(state, AppThemeUtil.getState(this));
        return state;
    }

}

package com.pocket.ui.view.themed;

import android.content.Context;
import android.util.AttributeSet;

import com.facebook.shimmer.ShimmerFrameLayout;

/**
 * A Pocket themed version of Facebook's {@link ShimmerFrameLayout}
 */
public class ThemedShimmerFrameLayout extends ShimmerFrameLayout {

    public ThemedShimmerFrameLayout(Context context) {
        super(context);
    }

    public ThemedShimmerFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemedShimmerFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] state = super.onCreateDrawableState(extraSpace + 1);
        mergeDrawableStates(state, AppThemeUtil.getState(this));
        return state;
    }

}

package com.pocket.sdk.util.view.tooltip;

import android.view.View;
import android.view.ViewGroup;

/**
 * Display tooltip views within a view group.
 */
public class ViewGroupDisplayer implements ViewDisplayer {

    private final ViewGroup mParent;
    private View mView;

    public ViewGroupDisplayer(ViewGroup parent) {
        mParent = parent;
    }

    @Override
    public void setView(View view) {
        mView = view;
        mParent.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void dismiss() {
        mView.setVisibility(View.GONE);
        mParent.removeView(mView);
    }

}

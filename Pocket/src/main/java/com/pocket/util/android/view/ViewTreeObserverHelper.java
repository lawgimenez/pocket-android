package com.pocket.util.android.view;

import android.view.View;
import android.view.ViewTreeObserver;

/**
 * TODO Documentation
 */
public class ViewTreeObserverHelper {

    private final ViewTreeObserver mObserver;
    private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;
    private final ViewTreeObserver.OnScrollChangedListener mScrollListener;
    private boolean mIsEnabled = true;

    public ViewTreeObserverHelper(View view, final Listener listener) {
        mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mIsEnabled) {
                    listener.onGlobalLayout();
                }
            }
        };
        mScrollListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (mIsEnabled) {
                    listener.onScrollChanged();
                }
            }
        };
        mObserver = view.getViewTreeObserver();
        mObserver.addOnGlobalLayoutListener(mLayoutListener);
        mObserver.addOnScrollChangedListener(mScrollListener);
    }

    public void stop() {
        mIsEnabled = false;
        if (mObserver.isAlive()) {
            mObserver.removeOnGlobalLayoutListener(mLayoutListener);
            mObserver.removeOnScrollChangedListener(mScrollListener);
        }
    }

    public interface Listener {
        public void onGlobalLayout();
        public void onScrollChanged();
    }

}

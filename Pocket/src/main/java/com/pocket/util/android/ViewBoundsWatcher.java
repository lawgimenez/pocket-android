package com.pocket.util.android;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;

/**
 * Helper class for tracking when a view when a view's absolute bounds (position and/or size) on screen has changed.
 * <b>Note</b> this does not currently track animation/transformation changes, only hard layout changes. Perhaps support could be added.
 */
public class ViewBoundsWatcher {

    private final View mView;
    private final OnViewAbsoluteBoundsChangedListener mListener;
    private final boolean mIsScrollable;
    private final Rect mBounds = new Rect();
    private final Callbacks mCallbacks = new Callbacks();
    private final int[] mRecycleXY = new int[2];

    private boolean mIsEnabled = true;
    private @Nullable ViewTreeObserver mViewTreeObserver;
    private boolean mIsVisible;

    /**
     * @param view The view to listen to
     * @param listener The callback when when the view bounds change
     * @param isScrollable If you know the view is within a scrollable area, pass true. Otherwise false will allow it to optimize for a non-scrolling case.
     * @return The new tracker or null if the view's {@link View#getViewTreeObserver()} returned a null or non-alive observer and we aren't able to track.
     */
    public static @Nullable ViewBoundsWatcher create(View view, OnViewAbsoluteBoundsChangedListener listener, boolean isScrollable) {
        ViewTreeObserver observer = view.getViewTreeObserver();
        if (observer == null || !observer.isAlive()) {
            return null;
        } else {
            return new ViewBoundsWatcher(view, observer, listener, isScrollable);
        }
    }

    /**
     * @see #create(View, OnViewAbsoluteBoundsChangedListener, boolean)
     */
    private ViewBoundsWatcher(View view, ViewTreeObserver observer, OnViewAbsoluteBoundsChangedListener listener, boolean isScrollable) {
        mView = view;
        mListener = listener;
        mIsScrollable = isScrollable;
        mViewTreeObserver = observer;

        view.addOnAttachStateChangeListener(mCallbacks);
        view.addOnLayoutChangeListener(mCallbacks);

        invalidateGlobalListeners(view.isAttachedToWindow());
        invalidateBounds();
    }

    /**
     * Stop listening. This cannot be undone. If you need it again, create a new instance.
     */
    public void stop() {
        mIsEnabled = false;
        releaseGlobalListeners();
    }

    /**
     * Check if the bounds or visibility changed.
     */
    private void invalidateVisibilityAndBounds() {
        setVisibility(ViewUtil.isVisibleToUserCompat(mView, 0));
        invalidateBounds();
    }

    /**
     * Check if the bounds changed.
     */
    private void invalidateBounds() {
        int left;
        int top;
        int right;
        int bottom;

        if (mIsVisible) {
            mView.getLocationOnScreen(mRecycleXY);
            left = mRecycleXY[0];
            top = mRecycleXY[1];
            right = mRecycleXY[0] + mView.getMeasuredWidth();
            bottom = mRecycleXY[1] + mView.getMeasuredHeight();

        } else {
            left = 0;
            top = 0;
            right = 0;
            bottom = 0;
        }

        if (mBounds.left != left
         || mBounds.top != top
         || mBounds.right != right
         || mBounds.bottom != bottom) {
            mBounds.set(left, top, right, bottom);
            if (mIsEnabled) {
                mListener.onViewAbsoluteBoundsChanged(left, top, right, bottom);
            }

        }
    }

    /**
     * Set the visibility state to a specific value. This does not trigger any listeners.
     *
     * @param isVisible The new state
     */
    private void setVisibility(boolean isVisible) {
        if (isVisible == mIsVisible) {
            return; // No change
        }
        mIsVisible = isVisible;
    }

    /**
     * Global listeners are only registered while the view is attached to a window as an optimization. This
     * method will check if the global listeners should be registered or not and updates as needed.
     *
     * @param isAttachedToWindow Whether or not this view is attached to a window.
     */
    private void invalidateGlobalListeners(boolean isAttachedToWindow) {
        if (!mIsEnabled) {
            releaseGlobalListeners();
            return;
        }

        if (isAttachedToWindow) {
            if (mViewTreeObserver == null) {
                mViewTreeObserver = mView.getViewTreeObserver();
            }
            if (!mViewTreeObserver.isAlive()) {
                return; // Can't register listeners. REVIEW retry later?
            }
            if (mIsScrollable) {
                mViewTreeObserver.addOnScrollChangedListener(mCallbacks);
            }
            mViewTreeObserver.addOnGlobalLayoutListener(mCallbacks);

        } else if (mViewTreeObserver != null) {
            releaseGlobalListeners();

        } else {
            // Already in the correct state.
        }
    }

    /**
     * Release the global listeners.
     */
    @SuppressLint("NewApi")
    private void releaseGlobalListeners() {
        if (mViewTreeObserver != null && mViewTreeObserver.isAlive()) {
            if (mIsScrollable) {
                mViewTreeObserver.removeOnScrollChangedListener(mCallbacks);
            }
            mViewTreeObserver.removeOnGlobalLayoutListener(mCallbacks);
        }
        mViewTreeObserver = null;
    }

    public interface OnViewAbsoluteBoundsChangedListener {
        /**
         * The view's bounds or position have changed. Values may be 0 if the view is invisible.
         *
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        public void onViewAbsoluteBoundsChanged(int left, int top, int right, int bottom);
    }

    private class Callbacks implements ViewTreeObserver.OnScrollChangedListener, View.OnAttachStateChangeListener, View.OnLayoutChangeListener, ViewTreeObserver.OnGlobalLayoutListener {

        @Override
        public void onScrollChanged() {
            invalidateVisibilityAndBounds();
        }

        @Override
        public void onGlobalLayout() {
            invalidateVisibilityAndBounds();
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            invalidateGlobalListeners(true);
            invalidateVisibilityAndBounds();
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            invalidateGlobalListeners(false);
            setVisibility(false); // View's internal implementation invokes this listener before clearing its mAttachInfo field, so calls to View.isAttachedToWindow() will actually return true right now. So just force the visibility to false otherwise, it will get the wrong value if it checks.
            invalidateBounds();
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            invalidateVisibilityAndBounds();
        }

    }

}

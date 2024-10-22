package com.pocket.ui.view.notification;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import com.google.android.material.behavior.SwipeDismissBehavior;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *  A modified version of {@link SwipeDismissBehavior} which fixes not showing the slide animation while the user is swiping.
 */
public class PktSwipeDismissBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {

    @IntDef({SWIPE_DIRECTION_START_TO_END, SWIPE_DIRECTION_END_TO_START, SWIPE_DIRECTION_ANY})
    @Retention(RetentionPolicy.SOURCE)
    private @interface SwipeDirection {}
    /**
     * Swipe direction that only allows swiping in the direction of start-to-end. That is
     * left-to-right in LTR, or right-to-left in RTL.
     */
    public static final int SWIPE_DIRECTION_START_TO_END = 0;
    /**
     * Swipe direction that only allows swiping in the direction of end-to-start. That is
     * right-to-left in LTR or left-to-right in RTL.
     */
    public static final int SWIPE_DIRECTION_END_TO_START = 1;
    /**
     * Swipe direction which allows swiping in either direction.
     */
    public static final int SWIPE_DIRECTION_ANY = 2;
    private static final float DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f;
    private ViewDragHelper mViewDragHelper;
    private OnDismissListener mListener;
    private boolean mIgnoreEvents;
    private float mSensitivity = 0f;
    private boolean mSensitivitySet;
    private int mSwipeDirection = SWIPE_DIRECTION_ANY;
    private float mDragDismissThreshold = DEFAULT_DRAG_DISMISS_THRESHOLD;
    /**
     * Callback interface used to notify the application that the view has been dismissed.
     */
    public interface OnDismissListener {
        /**
         * Called when {@code view} has been dismissed via swiping.
         */
        void onDismiss(View view);
        /**
         * Called when the drag state has changed.
         *
         * @param state the new state. One of
         * {@link ViewDragHelper#STATE_IDLE}, {@link ViewDragHelper#STATE_DRAGGING} or {@link ViewDragHelper#STATE_SETTLING}.
         */
        void onDragStateChanged(int state);
    }
    /**
     * Set the listener to be used when a dismiss event occurs.
     *
     * @param listener the listener to use.
     */
    public void setListener(OnDismissListener listener) {
        mListener = listener;
    }
    /**
     * Sets the swipe direction for this behavior.
     *
     * @param direction one of the {@link #SWIPE_DIRECTION_START_TO_END},
     *                  {@link #SWIPE_DIRECTION_END_TO_START} or {@link #SWIPE_DIRECTION_ANY}
     */
    public void setSwipeDirection(@SwipeDirection int direction) {
        mSwipeDirection = direction;
    }
    /**
     * Set the threshold for telling if a view has been dragged enough to be dismissed.
     *
     * @param distance a ratio of a view's width, values are clamped to 0 >= x <= 1f;
     */
    public void setDragDismissDistance(float distance) {
        mDragDismissThreshold = clamp(0f, distance, 1f);
    }
    /**
     * Set the sensitivity used for detecting the start of a swipe. This only takes effect if
     * no touch handling has occured yet.
     *
     * @param sensitivity Multiplier for how sensitive we should be about detecting
     *                    the start of a drag. Larger values are more sensitive. 1.0f is normal.
     */
    public void setSensitivity(float sensitivity) {
        mSensitivity = sensitivity;
        mSensitivitySet = true;
    }
    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false;
                    return false;
                }
                break;
            default:
                mIgnoreEvents = !parent.isPointInChildBounds(child,
                        (int) event.getX(), (int) event.getY());
                break;
        }
        if (mIgnoreEvents) {
            return false;
        }
        ensureViewDragHelper(parent);
        return mViewDragHelper.shouldInterceptTouchEvent(event);
    }
    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (mViewDragHelper != null) {
            mViewDragHelper.processTouchEvent(event);
            return true;
        }
        return false;
    }
    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {
        private int mOriginalCapturedViewLeft;
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            mOriginalCapturedViewLeft = child.getLeft();
            return true;
        }
        @Override
        public void onViewDragStateChanged(int state) {
            if (mListener != null) {
                mListener.onDragStateChanged(state);
            }
        }
        @Override
        public void onViewReleased(View child, float xvel, float yvel) {
            final int childWidth = child.getWidth();
            int targetLeft;
            boolean dismiss = false;
            if (shouldDismiss(child, xvel)) {
                targetLeft = child.getLeft() < mOriginalCapturedViewLeft
                        ? mOriginalCapturedViewLeft - childWidth
                        : mOriginalCapturedViewLeft + childWidth;
                dismiss = true;
            } else {
                // Else, reset back to the original left
                targetLeft = mOriginalCapturedViewLeft;
            }
            if (mViewDragHelper.settleCapturedViewAt(targetLeft, child.getTop())) {
                ViewCompat.postOnAnimation(child, new SettleRunnable(child, dismiss));
            } else if (dismiss) {
                if (mListener != null) {
                    mListener.onDismiss(child);
                }
            }
        }
        private boolean shouldDismiss(View child, float xvel) {
            if (xvel != 0f) {
                final boolean isRtl = ViewCompat.getLayoutDirection(child)
                        == ViewCompat.LAYOUT_DIRECTION_RTL;
                if (mSwipeDirection == SWIPE_DIRECTION_ANY) {
                    // We don't care about the direction so return true
                    return true;
                } else if (mSwipeDirection == SWIPE_DIRECTION_START_TO_END) {
                    // We only allow start-to-end swiping, so the fling needs to be in the
                    // correct direction
                    return isRtl ? xvel < 0f : xvel > 0f;
                } else if (mSwipeDirection == SWIPE_DIRECTION_END_TO_START) {
                    // We only allow end-to-start swiping, so the fling needs to be in the
                    // correct direction
                    return isRtl ? xvel > 0f : xvel < 0f;
                }
            } else {
                final int distance = child.getLeft() - mOriginalCapturedViewLeft;
                final int thresholdDistance = Math.round(child.getWidth() * mDragDismissThreshold);
                return Math.abs(distance) >= thresholdDistance;
            }
            return false;
        }
        @Override
        public int getViewHorizontalDragRange(View child) {
            return child.getWidth();
        }
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final boolean isRtl = ViewCompat.getLayoutDirection(child)
                    == ViewCompat.LAYOUT_DIRECTION_RTL;
            int min, max;
            if (mSwipeDirection == SWIPE_DIRECTION_START_TO_END) {
                if (isRtl) {
                    min = mOriginalCapturedViewLeft - child.getWidth();
                    max = mOriginalCapturedViewLeft;
                } else {
                    min = mOriginalCapturedViewLeft;
                    max = mOriginalCapturedViewLeft + child.getWidth();
                }
            } else if (mSwipeDirection == SWIPE_DIRECTION_END_TO_START) {
                if (isRtl) {
                    min = mOriginalCapturedViewLeft;
                    max = mOriginalCapturedViewLeft + child.getWidth();
                } else {
                    min = mOriginalCapturedViewLeft - child.getWidth();
                    max = mOriginalCapturedViewLeft;
                }
            } else {
                min = mOriginalCapturedViewLeft - child.getWidth();
                max = mOriginalCapturedViewLeft + child.getWidth();
            }
            return clamp(min, left, max);
        }
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }
    };

    private void ensureViewDragHelper(ViewGroup parent) {
        if (mViewDragHelper == null) {
            mViewDragHelper = mSensitivitySet
                    ? ViewDragHelper.create(parent, mSensitivity, mDragCallback)
                    : ViewDragHelper.create(parent, mDragCallback);
        }
    }
    private class SettleRunnable implements Runnable {
        private final View mView;
        private final boolean mDismiss;
        SettleRunnable(View view, boolean dismiss) {
            mView = view;
            mDismiss = dismiss;
        }
        @Override
        public void run() {
            if (mViewDragHelper != null && mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this);
            } else {
                if (mDismiss) {
                    if (mListener != null) {
                        mListener.onDismiss(mView);
                    }
                }
            }
        }
    }
    private static float clamp(float min, float value, float max) {
        return Math.min(Math.max(min, value), max);
    }
    private static int clamp(int min, int value, int max) {
        return Math.min(Math.max(min, value), max);
    }
    /**
     * Retrieve the current drag state of this behavior. One of {@link ViewDragHelper#STATE_IDLE},
     * {@link ViewDragHelper#STATE_DRAGGING} or {@link ViewDragHelper#STATE_SETTLING}.
     *
     * @return The current drag state
     */
    public int getDragState() {
        return mViewDragHelper != null ? mViewDragHelper.getViewDragState() : ViewDragHelper.STATE_IDLE;
    }
}

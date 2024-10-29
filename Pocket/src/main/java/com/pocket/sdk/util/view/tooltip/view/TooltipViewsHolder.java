package com.pocket.sdk.util.view.tooltip.view;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.pocket.sdk.util.view.tooltip.OutsideTouchAction;
import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.sdk.util.view.tooltip.ViewDisplayer;
import com.pocket.util.android.animation.AnimatorEndListener;

import java.util.ArrayList;

/**
 * Holds and manages all views of a Tooltip.
 */
public class TooltipViewsHolder {

    private final FrameLayout mFrame;
    private final int[] mRecycleXY = new int[2];
    private final Rect mRecycleRect = new Rect();
    private final ViewDisplayer mWindow;

    private boolean mIsShowing;
    private ArrayList<TooltipView> mViews;
    private Tooltip.TooltipController mController;

    private OutsideTouchAction mOutsideTouchAction;
    private boolean mDownOnAnchor;
    private Rect mAnchorBounds = new Rect(0, 0, 0, 0);
    
    public TooltipViewsHolder(Context context, ArrayList<TooltipView> views, ViewDisplayer window) {
        mWindow = window;

        mFrame = new FrameLayout(context);
        mFrame.setOnTouchListener((v, event) -> {
            if (mOutsideTouchAction.dismiss) {
                final Tooltip.DismissReason reason = isOnAnchor(event)
                        ? Tooltip.DismissReason.ANCHOR_CLICKED
                        : Tooltip.DismissReason.DISMISS_REQUESTED;
                mController.dismiss(reason);
            }
            
            switch (mOutsideTouchAction.block) {
                case EVERYWHERE:
                    return true;
                case IF_NOT_ON_ANCHOR:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mDownOnAnchor = isOnAnchor(event);
                    }
                    return false;
                case NOWHERE:
                default:
                    return false;
            }
        });

        mViews = views;
        for (TooltipView tooltipView : views) {
            View view = tooltipView.getView();
            mFrame.addView(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setVisibility(View.INVISIBLE); // Hide until we are ready to show, but use invisible so they are ready to measure.
        }

        mWindow.setView(mFrame);
    }
    
    private boolean isOnAnchor(MotionEvent event) {
        return mAnchorBounds.contains((int) event.getX(), (int) event.getY());
    }
    
    public void setOutsideTouchAction(OutsideTouchAction value, Tooltip.TooltipController tooltipController) {
        mOutsideTouchAction = value;
        mController = tooltipController;
        
        if (value.block == OutsideTouchAction.Block.IF_NOT_ON_ANCHOR) {
            mFrame.setOnClickListener(v -> {
                if (mDownOnAnchor) {
                    mController.clickAnchor(Tooltip.DismissReason.ANCHOR_CLICKED);
                }
            });
            
        } else {
            mFrame.setOnClickListener(null);
        }
    }

    public boolean showViews(Rect anchorBounds) {
        mAnchorBounds.set(anchorBounds);
        
        // Get our frames absolute screen coordinates
        Rect frameBounds = mRecycleRect;
        int[] xy = mRecycleXY;

        mFrame.getLocationOnScreen(xy);
        frameBounds.set(xy[0], xy[1], xy[0] + mFrame.getWidth(), xy[1] + mFrame.getHeight());

        boolean failed = false;
        for (TooltipView view : mViews) {
            boolean success = view.applyAnchor(xy, anchorBounds, frameBounds);
            if (success) {
                view.getView().setX(xy[0] - frameBounds.left);
                view.getView().setY(xy[1] - frameBounds.top);
            } else {
                failed = true;
            }
        }

        if (!mIsShowing && !failed) {
            mIsShowing = true;
            for (TooltipView view : mViews) {
                view.getView().setVisibility(View.VISIBLE);
                view.animateIn();
            }
        }

        return !failed;
    }

    public void dismiss() {
        if (!mIsShowing) {
            mWindow.dismiss();
            return;
        }
        mIsShowing = false;

        if (mViews.isEmpty()) {
            mWindow.dismiss();
            return;
        }

        AnimatorEndListener listener = new AnimatorEndListener() {

            int count = mViews.size();

            @Override
            public void onAnimationEnd(Animator animation) {
                count--;
                if (count <= 0) {
                    mWindow.dismiss();
                }
            }
        };

        for (TooltipView view : mViews) {
            view.animateOut(listener);
        }
    }

}

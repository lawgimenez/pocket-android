package com.pocket.ui.view.bottom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class PktBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

    public interface TouchCondition {
        boolean canTouch();
    }

    private TouchCondition touchCondition;

    public PktBottomSheetBehavior() {
        super();
    }

    public PktBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (touchCondition == null || touchCondition.canTouch()) {
            return super.onInterceptTouchEvent(parent, child, event);
        } else {
            super.onInterceptTouchEvent(parent, child, event);
            return false;
        }
    }

    public void setTouchCondition(TouchCondition condition) {
        this.touchCondition = condition;
    }

}

package com.pocket.util.android.view;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.pocket.util.android.drawable.DrawableUtil;

/**
 * Helper for adding a Foreground drawable to a view.
 * <p>
 * To use in your custom view:
 * <ul>
 *  <li>Create a final instance of this during construction.</li>
 *  <li>Set your foreground drawable with {@link #setForegroundDrawable(android.graphics.drawable.Drawable)}. The drawable is ok to change or become null at anytime.</li>
 *  <li>Invoke all of the onParent... methods of this class from the matching View methods in your custom class. For example, override jumpDrawablesToCurrentState() in your View, and invoke the onParentJumpDrawablesToCurrentState() method in this class at the end of the method override.</li>
 *  <li>It is safe to invoke all onParent methods even while a foreground drawable is null and not set.</li>
 * </ul>
 */
public class ForegroundDrawableHelper {

    private final View mParent;

    private @Nullable Drawable mForegroundDrawable;

    public ForegroundDrawableHelper(View parent) {
        mParent = parent;
    }

    public void setForegroundDrawable(@Nullable Drawable drawable) {
        if (mForegroundDrawable != null) {
            mForegroundDrawable.setCallback(null);
        }
        mForegroundDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(mParent);
        }
        mParent.invalidate();
    }

    public void onParentSizeChanged(int w, int h, @SuppressWarnings("unused") int oldw, @SuppressWarnings("unused") int oldh) {
        if (mForegroundDrawable != null) {
            mForegroundDrawable.setBounds(0, 0, w, h);
        }
    }

    public void onParentDrawableStateChanged() {
        if (mForegroundDrawable != null && mForegroundDrawable.isStateful()) {
            mForegroundDrawable.setState(mParent.getDrawableState());
        }
    }

    public void onParentTouchEvent(MotionEvent event) {
        DrawableUtil.setHotspot(mForegroundDrawable, event);
    }

    public void onParentJumpDrawablesToCurrentState() {
        if (mForegroundDrawable != null) {
            mForegroundDrawable.jumpToCurrentState();
        }
    }

    public boolean onParentVerifyDrawable(Drawable who) {
        return who == mForegroundDrawable;
    }


    public void onParentDispatchDraw(Canvas canvas) {
        if (mForegroundDrawable != null) {
            mForegroundDrawable.draw(canvas);
        }
    }

    public interface Setter {
        void setForegroundDrawable(Drawable drawable);
    }
}

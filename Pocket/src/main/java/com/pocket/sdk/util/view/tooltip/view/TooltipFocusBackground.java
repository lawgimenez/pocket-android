package com.pocket.sdk.util.view.tooltip.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.animation.AnimatorEndListener;
import com.pocket.util.android.animation.Interpolators;
import com.pocket.util.android.drawable.ColorUtil;
import com.pocket.util.android.drawable.SimpleStatefulDrawable;
import com.pocket.util.android.drawable.SoftwareCanvas;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * TODO Documentation
 */
public class TooltipFocusBackground implements TooltipView {

    public enum Shape {
        CIRCLE,
        RECT
    }

    private final Shape mShape;
    private final View mView;
    private final FocusDrawable mDrawable;

    public TooltipFocusBackground(Context context, Shape shape) {
        mShape = shape;
        mView = new View(context);
        final int blackoutColor = ColorUtil.setAlpha(.4f, ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_themed_grey_1));
        mDrawable = new FocusDrawable(blackoutColor);
        mView.setBackground(mDrawable);
    }

    @Override
    public void bind(Tooltip.TooltipController controller) {}

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public boolean applyAnchor(int[] xy, Rect anchorBounds, Rect windowBounds) {
        mDrawable.setTargetBounds(
                anchorBounds.left - windowBounds.left,
                anchorBounds.top - windowBounds.top,
                anchorBounds.right - windowBounds.left,
                anchorBounds.bottom - windowBounds.top);

        xy[0] = windowBounds.left;
        xy[1] = windowBounds.top;
        return true;
    }

    @Override
    public void animateIn() {
        mView.setAlpha(0);
        mView.animate()
                .alpha(1)
                .setDuration(333)
                .setInterpolator(Interpolators.CB_LINEAR_OUT_SLOW_IN)
                .setListener(null);
    }

    @Override
    public void animateOut(AnimatorEndListener callback) {
        mView.animate()
                .alpha(0)
                .setDuration(333)
                .setInterpolator(Interpolators.CB_LINEAR_OUT_SLOW_IN)
                .setListener(callback);
    }

    private class FocusDrawable extends SimpleStatefulDrawable implements SoftwareCanvas.Drawer {

        private final SoftwareCanvas mSoftwareCanvas = new SoftwareCanvas(this);

        private final Paint mBlackOutPaint;
        private final Paint mErasePaint;

        private final RectF mTarget = new RectF();

        private FocusDrawable(int color) {
            mBlackOutPaint = new Paint();
            mBlackOutPaint.setColor(color);
            mBlackOutPaint.setStyle(Paint.Style.FILL);
            registerPaint(mBlackOutPaint);

            mErasePaint = new Paint();
            mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            mErasePaint.setStyle(Paint.Style.FILL);
            registerPaint(mErasePaint);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            mSoftwareCanvas.onBoundsChanged(bounds);
            super.onBoundsChange(bounds);
        }

        public void setTargetBounds(int left, int top, int right, int bottom) {
            mTarget.set(left, top, right, bottom);
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            mSoftwareCanvas.draw(canvas);
        }

        @Override
        public void drawSoftware(Canvas canvas) {
            canvas.drawRect(getBounds(), mBlackOutPaint);

            float borderStroke = FormFactor.dpToPxF(15);
            switch (mShape) {
                case CIRCLE:
                    mErasePaint.setAlpha(ColorUtil.to255(.2f));
                    canvas.drawCircle(mTarget.centerX(), mTarget.centerY(), Math.min(mTarget.width(), mTarget.height()) / 2f + borderStroke, mErasePaint);
                    mErasePaint.setAlpha(ColorUtil.to255( .5f));
                    canvas.drawCircle(mTarget.centerX(), mTarget.centerY(), Math.min(mTarget.width(), mTarget.height()) / 2f + borderStroke / 2f, mErasePaint);
                    mErasePaint.setAlpha(255);
                    canvas.drawCircle(mTarget.centerX(), mTarget.centerY(), Math.min(mTarget.width(), mTarget.height()) / 2f, mErasePaint);
                    break;

                case RECT:
                    mErasePaint.setAlpha(ColorUtil.to255( .5f));
                    canvas.drawRect(mTarget, mErasePaint);
                    mTarget.inset(borderStroke, borderStroke);
                    mErasePaint.setAlpha(0);
                    canvas.drawRect(mTarget, mErasePaint);
                    mTarget.inset(-borderStroke, -borderStroke);
                    break;
            }
        }

    }
}

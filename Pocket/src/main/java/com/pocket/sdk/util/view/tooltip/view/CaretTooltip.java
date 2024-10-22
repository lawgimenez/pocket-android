package com.pocket.sdk.util.view.tooltip.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.animation.AnimatorEndListener;
import com.pocket.util.android.animation.Interpolators;
import com.pocket.util.android.drawable.ColorUtil;
import com.pocket.util.android.drawable.SimpleStatefulDrawable;
import com.pocket.util.android.drawable.StatefulPaint;
import com.pocket.util.android.view.ResizeDetectRelativeLayout;

/**
 * A simple tooltip style that has a simple single color rounded box with content and a caret arrow extending out of it
 * towards the anchor/target. It automatically positions itself where it fits best. It can be shown above, below, to
 * the right or left of the anchor.
 */
public class CaretTooltip extends ResizeDetectRelativeLayout implements TooltipView {

    public static class Builder {

        private final Context mContext;
        private final CaretTooltip mTooltip;

        public Builder(Context context) {
            mContext = context;
            mTooltip = new CaretTooltip(context);
        }

        /**
         * @param message Required. The string resource id of the tooltip's message.
         * @param button Optional. A button to display or 0 to not have a button.
         * @see #setText(CharSequence, CharSequence) for important interaction details.
         */
        public Builder setText(int message, int button) {
            return setText(mContext.getText(message),
                    button != 0 ? mContext.getText(button) : null);
        }

        /**
         * Set the text for the tooltip and also its interaction mode. When there is a button,
         * taps on the tooltip or the button dismiss. When there is no
         * button, taps on the tooltip proxy a click onto the anchor and dismiss.
         * <p>
         * TODO: Remove the button param. We seem to always pass null. Which is a good thing, because we ignore the 
         * text anyway.
         *
         * @param message Required. The tooltip's message.
         * @param button Optional. A button to display or null to not have a button.
         */
        public Builder setText(CharSequence message, CharSequence button) {
            boolean showButton = button != null && button.length() > 0;

            View view = LayoutInflater.from(mContext)
                    .inflate(
                            showButton ? R.layout.view_tooltip_v3 : R.layout.view_tooltip_v3_simple,
                            mTooltip, false);

            ((TextView) view.findViewById(R.id.text)).setText(message);

            if (showButton) {
                View buttonView = view.findViewById(R.id.button);
                buttonView.setVisibility(View.VISIBLE);
                buttonView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTooltip.mController.dismiss();
                    }
                });
            }
            view.setOnClickListener(v -> mTooltip.mController.dismiss());

            return setView(view);
        }

        /**
         * Set a custom view instead of a text view.
         *
         * @param value The layout resource id of the view to inflate.
         * @return
         */
        public Builder setView(int value) {
            View view = LayoutInflater.from(mContext)
                    .inflate(value, mTooltip, false);
            return setView(view);
        }

        /**
         * Set a custom view instead of a text view.
         *
         * @param value
         * @return
         */
        public Builder setView(View value) {
            mTooltip.removeAllViews();
            mTooltip.addView(value);
            return this;
        }

        /**
         * Set the distance between the caret's point and the edge of the target/anchor.
         * There is a default value already set, this is provided for custom cases.
         *
         * @param value
         * @return
         */
        public Builder setDistance(int value) {
            mTooltip.mDistanceBetweenCaretAndAnchor = value;
            return this;
        }

        /**
         * Set a listener for when the tooltip is clicked. The tooltip will be dismissed
         * when clicked.
         *
         * @param onClickListener
         * @return
         */
        public Builder setOnClickListener(final OnClickListener onClickListener) {
            mTooltip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTooltip.mController != null) {
                        mTooltip.mController.dismiss();
                    }
                    onClickListener.onClick(v);
                }
            });
            return this;
        }

        public CaretTooltip build() {
            return mTooltip;
        }

    }

    private final CaretTooltipDrawable mDrawable;
    private final Rect mRecycleRect = new Rect();

    private int mDistanceBetweenCaretAndAnchor = FormFactor.dpToPx(4f);
    private Tooltip.TooltipController mController;

    private CaretTooltip(Context context) {
        super(context);
        mDrawable = new CaretTooltipDrawable(context, R.color.caret_tooltip_bg,
                FormFactor.dpToPxF(3), FormFactor.dpToPxF(4), FormFactor.dpToPx(14f), FormFactor.dpToPx(33.25f));
        setBackgroundDrawable(mDrawable);

        // Need to change the padding ourselves and resize the view as the caret position changes.
        mDrawable.setPaddingListener(new PaddingListener() {
            @Override
            public void onPaddingChanged(int left, int top, int right, int bottom) {
                if (left != getPaddingLeft()
                        || top != getPaddingTop()
                        || right != getPaddingRight()
                        || bottom != getPaddingBottom()) {
                    setPadding(left, top, right, bottom);
                }
            }
        });
        mDrawable.setState(getDrawableState());
        setClickable(true); // Prevent touches from passing through.
    }

    @Override
    public void bind(Tooltip.TooltipController controller) {
        mController = controller;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void animateIn() {
        setScaleX(0.92f);
        setScaleY(0.92f);
        setAlpha(0);

        long delay = 555;
        long duration = 555;

        animate()
            .alpha(1)
            .setStartDelay(delay)
            .setDuration((long) (duration * 0.66f))
            .setInterpolator(Interpolators.DECEL)
            .setListener(null);

        animate()
            .setDuration(duration)
            .setStartDelay(delay)
            .scaleX(1)
            .scaleY(1)
            .setInterpolator(Interpolators.OVERSHOOT);
    }

    @Override
    public void animateOut(AnimatorEndListener callback) {
        animate()
            .setDuration(333)
                .alpha(0)
            .setStartDelay(0)
                .scaleX(0.95f)
            .scaleY(0.95f)
            .setInterpolator(Interpolators.CB_FAST_OUT_LINEAR_IN)
            .setListener(callback);
    }

    @Override
    public boolean applyAnchor(int[] xy, Rect anchorBounds, Rect windowBounds) {
        /*
         * Our tooltip can be shown above, below, right or left of the anchor, so
         * first thing is to find where it fits and then from there, figure out
         * where the caret needs to be placed within the tooltip to point at the
         * anchor target's center.
         */

        // Measure our content view
        View child = getChildAt(0);
        int wspec = ViewUtil.makeMeasureSpec(child.getLayoutParams().width, windowBounds.width());
        int hspec = ViewUtil.makeMeasureSpec(child.getLayoutParams().height, windowBounds.height());
        child.measure(wspec, hspec);

        Rect basePadding = mRecycleRect;
        mDrawable.getBasePadding(basePadding);

        // Calculate a few key values
        int baseWidth = child.getMeasuredWidth() + basePadding.left + basePadding.right; // Width of view without a caret
        int baseHeight = child.getMeasuredHeight() + basePadding.top + basePadding.bottom; // Height of view without a caret
        int additionalCaretLength = mDrawable.getAdditionalCaretLength(); // When a caret is on a side, how much does it add to the baseWidth or baseHeight?
        int distance = mDistanceBetweenCaretAndAnchor - mDrawable.getCaretDistanceFromEdge(); // Requested distance between the caret point to the edge of the anchor, may be negative to be within the anchor bounds.
		int minCaretOffset = mDrawable.getMinCaretOffset(); // The minimum allowed caret offset, if this can't be met, then the caret would extend outside of the box and be broken. This would only happen if the anchor was small and close to an edge.
		int acceptableShift = (int) (mDrawable.getShadowLength() / 2f); // Amount of the tooltip content view that we can shift offscreen to hit preferred positions. In this view, we allow some of the shadow not to be on screen. This helps target elements that are close to edges of screens.
		
        // How much space is available around the anchor's edges?
        int availableBelowAnchor = windowBounds.bottom - anchorBounds.bottom + acceptableShift;
        int availableAboveAnchor = anchorBounds.top - windowBounds.top + acceptableShift;
        int availableRightOfAnchor = windowBounds.right - anchorBounds.right + acceptableShift;
        int availableLeftOfAnchor = anchorBounds.left - windowBounds.left + acceptableShift;

        // How much space is available around the anchor's center? (Used for the caretOffset)
        int availableBelowAnchorCenter = windowBounds.bottom - anchorBounds.centerY() + acceptableShift;
        int availableAboveAnchorCenter = anchorBounds.centerY() - windowBounds.top + acceptableShift;
        int availableRightOfAnchorCenter = windowBounds.right - anchorBounds.centerX() + acceptableShift;
        int availableLeftOfAnchorCenter = anchorBounds.centerX() - windowBounds.left + acceptableShift;
        // Is there anywhere that the caret will break?
        boolean caretFitsHorizontal = availableLeftOfAnchorCenter >= minCaretOffset && availableRightOfAnchorCenter >= minCaretOffset;
        boolean caretFitsVertical = availableAboveAnchorCenter >= minCaretOffset && availableBelowAnchorCenter >= minCaretOffset;

        // How much space is needed if the caret is vertical, or if it is horizontal?
        int minVerticalSpace = baseHeight + additionalCaretLength + distance;
        int minHorizontalSpace = baseWidth + additionalCaretLength + distance;

        // Determine where the tooltip can fit, going through priority of various placements
        // Priority: Below, Above, Right then Left.
        int x;
        int y;
        CaretPosition caret;
        int caretOffset; // The position of the caret along the side it is placed. This is the distance from the corner, which is to the left of the caret, if you were standing on the edge the caret is on.
        if (availableBelowAnchor >= minVerticalSpace && caretFitsHorizontal) {
            // Fits below anchor
            caret = CaretPosition.TOP;
            y = anchorBounds.bottom + distance;
            x = fit(baseWidth, windowBounds.left, windowBounds.right, anchorBounds.centerX(), acceptableShift);
            caretOffset = anchorBounds.centerX() - x;

        } else if (availableAboveAnchor >= minVerticalSpace && caretFitsHorizontal) {
            // Above anchor
            caret = CaretPosition.BOTTOM;
            y = anchorBounds.top - minVerticalSpace;
            x = fit(baseWidth, windowBounds.left, windowBounds.right, anchorBounds.centerX(), acceptableShift);
            caretOffset = anchorBounds.centerX() - x;

        } else if (availableRightOfAnchor >= minHorizontalSpace && caretFitsVertical) {
            // Right of anchor
            caret = CaretPosition.LEFT;
            x = anchorBounds.right + distance;
            y = fit(baseHeight, windowBounds.top, windowBounds.bottom, anchorBounds.centerY(), acceptableShift);
            caretOffset = anchorBounds.centerY() - y;

        } else if (availableLeftOfAnchor >= minHorizontalSpace && caretFitsVertical) {
            // Left of anchor
            caret = CaretPosition.RIGHT;
            x = anchorBounds.left - minHorizontalSpace;
            y = fit(baseHeight, windowBounds.top, windowBounds.bottom, anchorBounds.centerY(), acceptableShift);
            caretOffset = anchorBounds.centerY() - y;

        } else {
            return false; // Cannot fit
        }

        if (x < 0-acceptableShift || y < 0-acceptableShift) {
            return false; // fit() failed
        }

        mDrawable.setCaretPosition(caret, caretOffset);
        invalidate();
        requestLayout();

        xy[0] = x;
        xy[1] = y;
        return true;
    }

    /**
     * Find where the tooltip must be placed in an axis in order to fit in the window. This
     * can be used for width or height. For width, min is left and max is right. For height min
     * is top and max is bottom.
     *
     * @param tooltipLength The width or height of the tooltip.
     * @param windowMin The left/top of the window
     * @param windowMax The right/bottom of the window
     * @param anchorCenter The center of the anchor on the axis (if measuring width, then it is centerX())
     * @return The left or top position to be used or -1 if it doesn't fit.
     */
    private int fit(int tooltipLength, int windowMin, int windowMax, int anchorCenter, int acceptableShift) {
    	windowMin -= acceptableShift;
    	windowMax += acceptableShift;
        int windowLength = windowMax - windowMin;
        int windowCenter = windowMin + (int) (windowLength / 2f);
        int halfTooltipLength = (int) (tooltipLength / 2f);

        int minIfCentered = anchorCenter - halfTooltipLength; // The min (left/top) position of the tooltip if we were able to center it with the anchor
        int maxIfCentered = minIfCentered + tooltipLength; // (right/bottom) if it was centered

        if (tooltipLength > windowLength) {
            return -1;

        } else if (minIfCentered > windowMin && maxIfCentered < windowMax) {
            // We can center, yippeeeeee
            return minIfCentered;

        } else if (anchorCenter < windowCenter) {
            // Move caret closer to min
            int out = windowMin - minIfCentered; // This is the minimum we must move to get back in bounds
            int min = minIfCentered + out;
            if (min + tooltipLength > windowMax) {
                return -1;
            } else {
                return min;
            }

        } else {
            // Move caret closer to max
            int out = maxIfCentered - windowMax; // This is the minimum we must move to get back in bounds
            int min = minIfCentered - out;
            if (min < windowMin) {
                return -1;
            } else {
                return min;
            }
        }
    }

    private enum CaretPosition {
        LEFT, TOP, RIGHT, BOTTOM
    }

    /**
     * The visual background of the tooltip. Shows a rounded rect, caret and drop shadow.
     */
    private static class CaretTooltipDrawable extends SimpleStatefulDrawable {

        private final Path mCaret = new Path();
        private final Rect mPadding = new Rect();
        private final RectF mRecycleRect = new RectF();
        private final StatefulPaint mShapePaint;
        private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float mShadowDrop = FormFactor.dpToPxF(2);

        private final float mCornerRadius;
        private final float mShadowLength;
        private final int mCaretLength;
        private final int mCaretWidth;

        private CaretPosition mCaretPosition;
        private int mCaretOffset;

        private Bitmap mShapeBitmap;
        private Canvas mShapeBitmapCanvas;
        private Bitmap mShadowBitmap;
        private Canvas mShadowBitmapCanvas;

        private PaddingListener mPaddingListener;

        private CaretTooltipDrawable(Context context, int shapeColorResId, float cornerRadius, float shadowLength, int caretLength, int caretWidth) {
            mShapePaint = new StatefulPaint(context, shapeColorResId);
            mShapePaint.setAntiAlias(true);
            mShapePaint.setStyle(Paint.Style.FILL);
            registerPaint(mShapePaint);

            mShadowPaint.setMaskFilter(new BlurMaskFilter(shadowLength, BlurMaskFilter.Blur.NORMAL));
            mShadowPaint.setColor(ColorUtil.gray(50, 0));
            registerPaint(mShadowPaint);

            mCornerRadius = cornerRadius;
            mShadowLength = shadowLength;

            mCaretLength = caretLength;
            mCaretWidth = caretWidth;

            invalidateCaret();
        }

        private void setPaddingListener(PaddingListener listener) {
            mPaddingListener = listener;
        }

        @Override
        public boolean getPadding(Rect padding) {
            padding.set(mPadding);
            return true;
        }

        /**
         * Rebuild the shape (box, caret and shadow) bitmaps.
         */
        private void invalidateShape() {
            if (mShapeBitmap != null) {
                mShapeBitmap.recycle();
                mShapeBitmap = null;
            }
            if (mShadowBitmap != null) {
                mShadowBitmap.recycle();
                mShadowBitmap = null;
            }

            getBasePadding(mPadding);

            Rect bounds = getBounds();

            if (!isValid()) {
                return;
            }

            // Draw the shape into a bitmap so we can turn it into a shadow easily.
            mShapeBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
            mShapeBitmapCanvas = new Canvas(mShapeBitmap);
            // Draw the shadow into a bitmap so we can use a blur filter, which only works on a software non-hardware accel canvas.
            mShadowBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
            mShadowBitmapCanvas = new Canvas(mShadowBitmap);

            switch (mCaretPosition) {
                case LEFT:
                    mPadding.left += mCaretLength;
                    break;
                case TOP:
                    mPadding.top += mCaretLength;
                    break;
                case RIGHT:
                    mPadding.right += mCaretLength;
                    break;
                case BOTTOM:
                    mPadding.bottom += mCaretLength;
                    break;
            }

            if (mPaddingListener != null) {
                mPaddingListener.onPaddingChanged(mPadding.left, mPadding.top, mPadding.right, mPadding.bottom);
            }
            drawShape(mShapeBitmapCanvas, bounds, mPadding);
            drawShadow(mShapeBitmap.extractAlpha(), mShadowBitmapCanvas);
            invalidateSelf();
        }

        private void drawShadow(Bitmap shape, Canvas canvas) {
            canvas.drawBitmap(shape, 0, 0, mShadowPaint);
        }

        private void drawShape(Canvas canvas, Rect bounds, Rect padding) {
            // Inside padding
            int left = bounds.left + padding.left;
            int top = bounds.top + padding.top;
            int right = bounds.right - padding.right;
            int bottom = bounds.bottom - padding.bottom;
            mRecycleRect.set(left, top, right, bottom);

            // Rounded box
            canvas.drawRoundRect(mRecycleRect,
                    mCornerRadius, mCornerRadius,
                    mShapePaint);

            // Caret transform and drawing
            float cornerX;
            float cornerY;
            float degrees;
            float cornerOffset;
            int mod;

            switch (mCaretPosition) {
                case TOP:
                    cornerX = left;
                    cornerY = top;
                    cornerOffset = left;
                    degrees = 0;
                    mod = 1;
                    break;

                case RIGHT:
                    cornerX = top;
                    cornerY = -right;
                    cornerOffset = top;
                    degrees = 90;
                    mod = 1;
                    break;

                case BOTTOM:
                    cornerX = -right;
                    cornerY = -bottom;
                    cornerOffset = right;
                    degrees = 180;
                    mod = -1;
                    break;

                case LEFT:
                    cornerX = -bottom;
                    cornerY = left;
                    cornerOffset = bottom;
                    degrees = 270;
                    mod = -1;
                    break;

                default:
                    throw new RuntimeException("unexpected " + mCaretPosition);
            }

            canvas.save();

            canvas.rotate(degrees, 0, 0); // Rotate so that corner is "top left" in the new coordinate system
            canvas.translate(cornerX, cornerY); // Move to corner
            canvas.translate(0, -mCaretLength); // Move caret int proper vertical position
            canvas.translate(mCaretWidth / 2f - mCaretWidth, 0); // Center caret point horizontally on the corner
            canvas.translate(mod * (mCaretOffset - cornerOffset), 0); // Move to centered on target

            canvas.drawPath(mCaret, mShapePaint);

            canvas.restore();
        }

        public void setCaretPosition(CaretPosition position, int offset) {
            mCaretPosition = position;
            mCaretOffset = offset;
            invalidateShape();
        }

        private void invalidateCaret() {
            mCaret.rewind();

            float coverageLength = mCaretLength + mCornerRadius * 2;

            // Start at the tip of the point
            mCaret.moveTo(mCaretWidth/2f, 0);
            mCaret.lineTo(mCaretWidth, mCaretLength);
            mCaret.lineTo(mCaretWidth, coverageLength);
            mCaret.lineTo(0, coverageLength);
            mCaret.lineTo(0, mCaretLength);
            mCaret.close();

            invalidateShape();
        }

        @Override
        public void draw(Canvas canvas) {
            if (!isValid()) {
                invalidateShape();
                if (!isValid()) {
                    // Give up
                    return;
                }
            }

            // Shadow of Shapes
            canvas.save();
            canvas.translate(0, mShadowDrop);
            canvas.drawBitmap(mShadowBitmap, 0, 0, null);
            canvas.restore();

            // Shapes
            canvas.drawBitmap(mShapeBitmap, 0, 0, null);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            invalidateShape();
        }

        /**
         * Sets the paddings to the supplied Rect. These are the paddings of each side as if there was no caret on that side.
         * @param out
         */
        public void getBasePadding(Rect out) {
            int padding = calcBasePadding();
            out.set(padding, padding, padding, padding);
        }

        private int calcBasePadding() {
            return (int) (Math.ceil(mShadowLength * 2) + mShadowDrop);
        }

        /**
         * @return true if everything needed to measure/draw is set.
         */
        private boolean isValid() {
            return !getBounds().isEmpty() && mCaretPosition != null;
        }

        /**
         * @return The distance between the edge of the bounds and the caret point.
         */
        public int getCaretDistanceFromEdge() {
            return calcBasePadding();
        }

        /**
         * @return The amount that having a caret adds to the width or height of a dimension.
         */
        public int getAdditionalCaretLength() {
            return mCaretLength;
        }

        /**
         * @return The caret must be at least this far away from an edge in order to display properly.
         */
        public int getMinCaretOffset() {
            return (int) (calcBasePadding() + mCaretWidth/2f);
        }
        
        public float getShadowLength() {
        	return mShadowLength;
		}

    }

    private static abstract class PaddingListener {
        public abstract void onPaddingChanged(int left, int top, int right, int bottom);
    }

}



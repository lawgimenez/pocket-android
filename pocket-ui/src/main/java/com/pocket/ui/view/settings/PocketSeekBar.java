package com.pocket.ui.view.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.Gravity;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.themed.ThemedSeekBar;

import java.util.ArrayList;

public class PocketSeekBar extends ThemedSeekBar {

    private int thumbRadius;
    private int thumbShadowLength;
    private int thumbStroke;
    private int trackHeight;

    public PocketSeekBar(Context context) {
        super(context);
        init(context);
    }

    public PocketSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PocketSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        thumbRadius = DimenUtil.dpToPxInt(context,11);
        thumbShadowLength = DimenUtil.dpToPxInt(context,6.5f);
        thumbStroke = DimenUtil.dpToPxInt(context,1.1f);
        trackHeight = DimenUtil.dpToPxInt(context,5);

        setThumb(new Handle(getResources()));
        setBackgroundDrawable(null); // Removes the ripple on L

        int progressColorRes = R.color.pkt_themed_grey_4;

        Drawable progress = new ClipDrawable(new Progress(getResources(), progressColorRes), Gravity.LEFT, ClipDrawable.HORIZONTAL);
        Drawable track = new Progress(getResources(), R.color.pkt_themed_grey_5);

        setProgressDrawable(new LayerDrawable(new Drawable[]{track, progress}));

        // Adjust the thumb offset so that it ends at edge of the track rather than going beyond it.
        int offset = DimenUtil.dpToPxInt(context,9);
        setThumbOffset(offset);
        setPadding(offset, 0, offset, 0);
    }

    /**
     * A Progress Drawable is normally a layer drawable with the track and the progress bar. getProgressDrawable returns the LayerDrawable.
     * This method pulls out the progress drawable inside of the layer drawable.  If getProgressDrawable is null or not a layer drawable it will return null
     * <p>
     * Also if this is called before the constructors are completed, it will also return null.
     */
    private Drawable getRealProgressDrawable() {
        Drawable drawable = getProgressDrawable();
        if (drawable != null && drawable instanceof LayerDrawable) {
            return ((LayerDrawable) drawable).findDrawableByLayerId(android.R.id.progress);
        } else {
            return null;
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        Drawable progressDrawable = getRealProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable.setBounds(getProgressDrawable().getBounds());
            progressDrawable.setState(getDrawableState());
        }
        super.onDraw(canvas);
    }

    private class Progress extends ThemeDrawable {

        private final Paint paint = new Paint();
        private final ColorStateList colors;
        private final PillPath path = new PillPath();

        private Progress(Resources res, int colorResource) {
            super(res);
            registerPaint(paint);
            paint.setStyle(Paint.Style.FILL);
            colors = res.getColorStateList(colorResource);
        }

        @Override
        protected void updatePaints(int[] newState) {
            paint.setColor(colors.getColorForState(newState, Color.TRANSPARENT));
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            float r = trackHeight / 2f;
            path.setBounds(bounds.left,
                    bounds.exactCenterY()-r,
                    bounds.right,
                    bounds.exactCenterY()+r);
        }

        @Override
        public void draw(Canvas canvas) {
            path.draw(canvas, paint);
        }
    }

    private class Handle extends ThemeDrawable {

        private final Paint strokePaint = new Paint();
        private final Paint bgPaint = new Paint();
        private final ColorStateList strokeColors;
        private final ColorStateList bgColors;
        private final int radius = thumbRadius + thumbShadowLength;

        private Handle(Resources res) {
            super(res);

            registerPaint(bgPaint);
            registerPaint(strokePaint);

            bgPaint.setStyle(Paint.Style.FILL);
            strokePaint.setStyle(Paint.Style.FILL);

            bgColors = res.getColorStateList(R.color.pkt_seekbar_handle);
            strokeColors = res.getColorStateList(R.color.pkt_themed_grey_1);
        }

        @Override
        protected void updatePaints(int[] newState) {
            strokePaint.setColor(strokeColors.getColorForState(newState, Color.TRANSPARENT));
            bgPaint.setColor(bgColors.getColorForState(newState, Color.TRANSPARENT));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), thumbRadius, strokePaint);
            canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), thumbRadius - thumbStroke, bgPaint);
        }

        @Override
        public int getIntrinsicWidth() {
            return radius * 2;
        }

        @Override
        public int getIntrinsicHeight() {
            return radius * 2;
        }
    }

    /** Handles the theme state changing of the Drawable. Make sure you {@link #registerPaint(Paint)} for each Paint you create */
    private abstract class ThemeDrawable extends Drawable {

        private final ArrayList<Paint> paints = new ArrayList<Paint>();

        private ThemeDrawable(Resources res) {}

        protected void registerPaint(Paint paint) {
            paints.add(paint);
            paint.setAntiAlias(true);
        }

        @Override
        protected boolean onStateChange(int[] state) {
            super.onStateChange(state);
            updatePaints(state);
            return true;
        }

        /** The theme state has changed, you should update your paints to match the current state */
        protected abstract void updatePaints(int[] newState);

        @Override
        public void setAlpha(int alpha) {
            for (Paint paint : paints) {
                paint.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            for (Paint paint : paints) {
                paint.setColorFilter(cf);
            }
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public boolean isStateful() {
            return true;
        }

    }

    /**
     * Creates a {@link Path} shaped like a pill where the left and right sides are half circles and the center
     * is a rectangle.
     * <p>
     * Invoke setBounds to define the area and {@link #draw(Canvas, Paint)} to draw.
     * <p>
     * If the bounds width and height is equal, a circle will be drawn.
     */
    private class PillPath {

        private final Path mPath = new Path();
        private final RectF mReuse = new RectF();

        private float mWidth = 0;

        public PillPath() {

        }

        /**
         * @see #setBounds(float, float, float, float)
         */
        public void setBounds(RectF bounds) {
            setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
        }

        /**
         * Set the area of the pill.
         * <p>
         * Imagine a circle drawn in the center of the bounds with the diameter equal to the height so it fills
         * the height. Then if there is remaining width on the left and right sides, the circle is cut down the center and each half circle
         * moved horizontally to the far left and right. Then a rectangle fills the space between them.
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        public void setBounds(float left, float top, float right, float bottom) {
            final float width = right - left;
            final float height = bottom - top;
            final float r = height / 2f;
            final float centerX = left + (width/2f);
            final float centerY = top + (height/2f);
            mPath.rewind();

            if (width == height) {
                mPath.addCircle(centerX, centerY, r, Path.Direction.CW);

            } else {
                float tlX = left + r; // top left
                float tlY = top;
                float trX = right - r; // top right
                float trY = top;
                float blX = left + r; // bottom left
                float blY = bottom;
                float brX = right - r; // bottom right
                float brY = bottom;

                // Top straight line
                mPath.moveTo(tlX, tlY);
                mPath.lineTo(trX, trY);

                // Right side half circle cap
                mReuse.set(trX - r, trY, brX + r, brY);
                mPath.arcTo(mReuse, 270, 180);

                // Bottom straight line
                mPath.lineTo(blX, blY);

                // Left side half circle cap
                mReuse.set(tlX - r, tlY, blX + r, blY);
                mPath.arcTo(mReuse, 90, 180);
            }

            mPath.close();
            mWidth = right - left;
        }

        public void draw(Canvas canvas, Paint paint) {
            canvas.drawPath(mPath, paint);
        }

        public float getWidth() {
            return mWidth;
        }

    }

}

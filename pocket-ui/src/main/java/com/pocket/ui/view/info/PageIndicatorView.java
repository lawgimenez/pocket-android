package com.pocket.ui.view.info;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.themed.ThemedLinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A set of horizontal dots for use as page indicators.
 */
public class PageIndicatorView extends ThemedLinearLayout {

    private final Binder binder = new Binder();

    private int margin;
    private int currentIndex = 0;

    public PageIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PageIndicatorView(Context context) {
        super(context);
        init();
    }

    private void init() {
        margin = DimenUtil.dpToPxInt(getContext(), 6);
    }

    public Binder bind() {
        return binder;
    }

    public class Binder {

        public Binder clear() {
            pageCount(0);
            currentIndex(0);
            return this;
        }

        public Binder pageCount(int value) {
            removeAllViews();

            for (int i = 0; i < value; i++) {
                ImageView indicator = new ImageView(getContext());
                LinearLayout.LayoutParams indicatorParams = new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                );
                indicatorParams.setMargins(margin, 0, margin, 0);
                indicator.setLayoutParams(indicatorParams);
                indicator.setImageDrawable(new PageIndicatorDrawable(getContext()));
                addView(indicator);
            }

            currentIndex(0);

            return this;
        }

        public Binder currentIndex(int value) {
            safeSetChildSelected(currentIndex, false);
            safeSetChildSelected(value, true);
            currentIndex = value;
            return this;
        }
    
        private void safeSetChildSelected(int index, boolean selected) {
            if (getChildCount() > 0 && getChildCount() > index) {
                getChildAt(index).setSelected(selected);
            }
        }
    
    }

    private class PageIndicatorDrawable extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ColorStateList color;
        private final float radius;

        PageIndicatorDrawable(Context context) {
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            radius = DimenUtil.dpToPx(context, 3.5f);
            color = NestedColorStateList.get(context, R.color.pkt_page_indicator);
        }

        @Override
        public int getIntrinsicWidth() {
            return (int) Math.ceil(radius * 2);
        }

        @Override
        public int getIntrinsicHeight() {
            return getIntrinsicWidth();
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            super.onStateChange(state);
            return true;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            paint.setColor(color.getColorForState(getState(), Color.TRANSPARENT));
            Rect bounds = getBounds();
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

}

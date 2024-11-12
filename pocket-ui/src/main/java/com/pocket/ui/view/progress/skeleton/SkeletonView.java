package com.pocket.ui.view.progress.skeleton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.pocket.ui.R;
import com.pocket.ui.util.ColorStateListDrawable;
import com.pocket.ui.view.themed.ThemedView;
import com.pocket.util.java.RandomSingleton;

import androidx.annotation.ColorRes;

public class SkeletonView extends ThemedView {

    private final Binder binder = new Binder();

    private float randomWidthPercentFloor = 1f;
    private float randomWidthPercentCeil = 1f;

    public SkeletonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SkeletonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SkeletonView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SkeletonView);

            randomWidthPercentFloor = ta.getFloat(R.styleable.SkeletonView_randomWidthPercentFloor, 1f);
            randomWidthPercentCeil = ta.getFloat(R.styleable.SkeletonView_randomWidthPercentCeil, 1f);

            if (randomWidthPercentFloor > randomWidthPercentCeil) {
                throw new IllegalArgumentException("randomWidthPercentFloor must be less than randomWidthPercentCeil");
            }

            int colors = ta.getResourceId(R.styleable.SkeletonView_compatBackgroundColor, R.color.pkt_themed_grey_6);
            float radius = ta.getDimensionPixelSize(R.styleable.SkeletonView_cornerRadius, 0);

            bind().background(colors, radius);

            ta.recycle();
        } else {
            bind().clear();
        }
    }

    private int getRandomWidth(int originalWidth) {
        if (originalWidth == 0) {
            return 0;
        }
        int minWidth = (int)(originalWidth * randomWidthPercentFloor);
        int maxWidth = (int)(originalWidth * randomWidthPercentCeil);

        return RandomSingleton.get().nextInt(maxWidth - minWidth) + minWidth;
    }

    public Binder bind() {
        return binder;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (randomWidthPercentFloor < 1f) {
            int measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec);
            measuredWidth = getRandomWidth(measuredWidth);
            int measureMode = View.MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, measureMode);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public class Binder {

        public Binder clear() {
            background(R.color.pkt_themed_grey_6, 0);
            originalWidth();
            return this;
        }

        public Binder background(@ColorRes int color, float cornerRadius) {
            setBackground(new ColorStateListDrawable(getContext(), color, cornerRadius));
            return this;
        }

        public Binder originalWidth() {
            randomWidthPercentFloor = 1f;
            randomWidthPercentCeil = 1f;
            requestLayout();
            return this;
        }

        public Binder randomWidth(float floorPercent, float ceilPercent) {
            randomWidthPercentFloor = floorPercent;
            randomWidthPercentCeil = ceilPercent;
            requestLayout();
            return this;
        }
    }

}

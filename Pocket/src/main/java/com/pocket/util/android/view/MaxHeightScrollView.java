package com.pocket.util.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.ideashower.readitlater.R;
import com.pocket.ui.view.themed.ThemedNestedScrollView;

public class MaxHeightScrollView extends ThemedNestedScrollView {

    private int maxHeight;

    public MaxHeightScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public MaxHeightScrollView(Context context) {
        super(context);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PocketTheme);

        setMaxHeight(a.getDimensionPixelSize(R.styleable.PocketTheme_maxHeight, 0));

        a.recycle();
    }

    public void setMaxHeight(int px) {
        maxHeight = px;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = View.MeasureSpec.getMode(heightMeasureSpec);
        int measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int adjustedHeight = Math.min(measuredHeight, maxHeight);
        int adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedHeight, mode);
        super.onMeasure(widthMeasureSpec, adjustedHeightMeasureSpec);
    }

}

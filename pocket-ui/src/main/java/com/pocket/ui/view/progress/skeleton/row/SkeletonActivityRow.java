package com.pocket.ui.view.progress.skeleton.row;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class SkeletonActivityRow extends AbsSkeletonRow {

    public SkeletonActivityRow(Context context) {
        super(context);
    }

    public SkeletonActivityRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SkeletonActivityRow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected int getLayout() {
        return R.layout.view_skeleton_activity_row;
    }

}

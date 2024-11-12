package com.pocket.ui.view.progress.skeleton.row;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class SkeletonItemTileRow extends AbsSkeletonRow {

    public SkeletonItemTileRow(Context context) {
        super(context);
    }

    public SkeletonItemTileRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SkeletonItemTileRow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int defaultPadding() {
        return (int) getResources().getDimension(R.dimen.pkt_space_md);
    }

    @Override protected int getLayout() {
        return R.layout.view_skeleton_item_tile_row;
    }

}
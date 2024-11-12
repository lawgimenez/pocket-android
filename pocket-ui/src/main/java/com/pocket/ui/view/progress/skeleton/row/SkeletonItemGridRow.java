package com.pocket.ui.view.progress.skeleton.row;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class SkeletonItemGridRow extends AbsSkeletonRow {

    public SkeletonItemGridRow(Context context) {
        super(context);
        init();
    }

    public SkeletonItemGridRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SkeletonItemGridRow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        content.setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.pkt_item_tile_width_min));
        content.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.pkt_item_tile_height_min));

        final int padding = (int) getResources().getDimension(R.dimen.pkt_space_md);
        content.setPadding(padding, 0, padding, 0);
    }

    @Override protected int getLayout() {
        return R.layout.view_skeleton_item_grid_row;
    }

}

package com.pocket.ui.view.progress.skeleton.row

import android.content.Context
import android.util.AttributeSet
import com.pocket.ui.R

class SkeletonDiscoverRow : AbsSkeletonRow {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getLayout(): Int {
        return R.layout.view_skeleton_discover_row
    }

}
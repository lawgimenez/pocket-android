package com.pocket.app.reader.internal.collection

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pocket.ui.util.DimenUtil

class CollectionSpacingDecorator(
    private val margin: Float = 18f
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.bottom = DimenUtil.dpToPxInt(view.context, margin)
    }
}

class CollectionGridSpacingDecorator(
    private val margin: Float = 18f
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val isOnLeftSide = parent.getChildAdapterPosition(view) % 2 == 0

        if (isOnLeftSide) {
            outRect.right = DimenUtil.dpToPxInt(view.context, margin / 2)
        } else {
            outRect.left = DimenUtil.dpToPxInt(view.context, margin / 2)
        }

        outRect.bottom = DimenUtil.dpToPxInt(view.context, margin)
    }
}
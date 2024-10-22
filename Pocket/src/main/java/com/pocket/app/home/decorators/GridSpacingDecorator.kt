package com.pocket.app.home.decorators

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pocket.ui.util.DimenUtil

/**
 * Decorator that adds spacing to left right and bottom for items in a grid with 2 columns
 * @param margin the margin to use in dp
 */
class GridSpacingDecorator(
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
            outRect.left = DimenUtil.dpToPxInt(view.context, margin)
            outRect.right = DimenUtil.dpToPxInt(view.context, margin / 2)
        } else {
            outRect.right = DimenUtil.dpToPxInt(view.context, margin)
            outRect.left = DimenUtil.dpToPxInt(view.context, margin / 2)
        }

        outRect.bottom = DimenUtil.dpToPxInt(view.context, margin)
    }
}
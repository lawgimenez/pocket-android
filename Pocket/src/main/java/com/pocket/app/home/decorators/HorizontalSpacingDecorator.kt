package com.pocket.app.home.decorators

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pocket.ui.util.DimenUtil

/**
 * Item decorator for adding horizontal padding between object and at the ends of the list
 * @param margin the margin to use in dp
 */
class HorizontalSpacingDecorator(
    private val margin: Float = 18f,
    private val extraMarginOnFirstAndLast: Float = 0f,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        // each item gets a margin on the left
        outRect.left = DimenUtil.dpToPxInt(view.context, margin)

        // the last item gets a margin on the right as well
        if (parent.getChildAdapterPosition(view) == (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.right = DimenUtil.dpToPxInt(view.context, margin + extraMarginOnFirstAndLast)
        }

        // add any extra margin to the first item
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = DimenUtil.dpToPxInt(view.context, margin + extraMarginOnFirstAndLast)
        }
    }
}
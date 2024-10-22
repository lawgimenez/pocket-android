package com.pocket.app.reader.internal.article.highlights

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pocket.ui.util.DimenUtil

class HighlightSpacingDecorator(
    private val margin: Float = 22f
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.bottom = DimenUtil.dpToPxInt(view.context, margin)

        // big spacing on the bottom of the last item
        if (parent.getChildAdapterPosition(view) == (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.bottom = DimenUtil.dpToPxInt(view.context, 100f)
        }

        //top item spacing
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = DimenUtil.dpToPxInt(view.context, margin)
        }
    }
}
package com.pocket.app.reader.internal.article.recommendations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pocket.ui.util.DimenUtil

class RecommendationSpacingDecorator(
    private val margin: Float = 18f
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        outRect.left = DimenUtil.dpToPxInt(view.context, margin)
        outRect.right = DimenUtil.dpToPxInt(view.context, margin)
        outRect.bottom = DimenUtil.dpToPxInt(view.context, margin)
    }
}
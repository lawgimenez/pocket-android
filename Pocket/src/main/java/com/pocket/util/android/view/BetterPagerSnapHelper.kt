package com.pocket.util.android.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

/**
 * Implementation of the [SnapHelper] supporting pager style snapping for horizontal linear layout manager.
 *
 * Yes, I know there is one provided in the recycler view support lib. It doesn't work well on tablets.
 * This one is going to be way better!
 */
class BetterPagerSnapHelper(recyclerView: RecyclerView) : BetterSnapHelper(recyclerView) {

    override fun snap() {
        val layoutManager: RecyclerView.LayoutManager? = recyclerView.layoutManager
        if (recyclerView.adapter == null || layoutManager == null || layoutManager.childCount < 2) {
            return
        }

        val distances = mutableListOf<Int>()
        distances.add(getStartSnapDistance(layoutManager.getChildAt(0)!!))
        distances.add(getStartSnapDistance(layoutManager.getChildAt(1)!!))

        val endChild = layoutManager.getChildAt(layoutManager.childCount - 1)!!
        if (isLastChild(endChild)) {
            distances.add(getEndSnapDistance(endChild))
        }

        val distance = distances.minByOrNull(Math::abs) ?: 0
        recyclerView.smoothScrollBy(distance, 0)
    }

    override fun snap(velocityX: Int, velocityY: Int) {
        if (velocityX < 0) {
            snapToLeft()
        } else {
            snapToRight()
        }
    }

    private fun snapToLeft() {
        val layoutManager: RecyclerView.LayoutManager? = recyclerView.layoutManager
        if (recyclerView.adapter == null || layoutManager == null || layoutManager.childCount < 1) {
            return
        }

        val firstChild = layoutManager.getChildAt(0)!!
        val distance = getStartSnapDistance(firstChild)
        recyclerView.smoothScrollBy(distance, 0)
    }

    private fun snapToRight() {
        val layoutManager: RecyclerView.LayoutManager? = recyclerView.layoutManager
        if (recyclerView.adapter == null || layoutManager == null || layoutManager.childCount < 2) {
            return
        }

        val secondChild = layoutManager.getChildAt(1)!!
        val startDistance = getStartSnapDistance(secondChild)

        val endChild = layoutManager.getChildAt(layoutManager.childCount - 1)!!
        if (isLastChild(endChild)) {
            val endDistance = getEndSnapDistance(endChild)
            val distance = minOf(startDistance, endDistance, compareBy(Math::abs))
            recyclerView.smoothScrollBy(distance, 0)

        } else {
            recyclerView.smoothScrollBy(startDistance, 0)
        }
    }

    private fun getStartSnapDistance(view: View) = view.left - getHorizontalOrientationHelper().startAfterPadding

    private fun getEndSnapDistance(view: View) = view.right - getHorizontalOrientationHelper().endAfterPadding

    private fun isLastChild(endChild: View): Boolean {
        val adapter: RecyclerView.Adapter<*>? = recyclerView.adapter
        if (adapter != null) {
            return recyclerView.getChildAdapterPosition(endChild) == adapter.itemCount - 1
        } else {
            return false
        }
    }
}

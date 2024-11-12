package com.pocket.analytics

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pocket.app.App
import com.pocket.ui.view.info.InfoPage
import com.pocket.ui.view.info.InfoPageAdapter

/**
 * A version of [InfoPageAdapter] which supports impression tracking on its pages.
 */
class ImpressionableInfoPageAdapter(
    context: Context,
    screenWidth: Int,
    pages: List<InfoPage>
) : InfoPageAdapter(context, screenWidth, pages) {

    private val tracker: Tracker = App.from(context).tracker()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        tracker.enableImpressionTracking(holder.itemView, ImpressionComponent.CONTENT)
        return holder
    }
}
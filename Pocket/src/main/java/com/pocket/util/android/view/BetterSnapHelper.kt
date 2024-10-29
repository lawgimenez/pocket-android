package com.pocket.util.android.view

import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

/**
 * Support library provides a [SnapHelper], but the API is.. sigh... It assumes you'll only going to work with
 * a layout manager. While for a pretty simple requirement, like checking if a given child is the last one you also
 * need an adapter. Internally it keeps a reference to the whole [RecyclerView], it just doesn't expose it to you.
 * Also for flinging it assumes you always want a linear smooth scroller, which is the worst and why would you ever
 * want it.
 * 
 * So in this alternative base class, we make less assumptions and let you do the work (which often is simpler than
 * what the support base class tries to do for you).
 * 
 * Update: looks like in 26.1.0 they fixed some things like the linear smooth scroller. So it might be worth checking
 * out again. But I think they still don't let you check the adapter.
 */
abstract class BetterSnapHelper(internal val recyclerView: RecyclerView) {

    private val orientationHelperCache = OrientationHelperCache()

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        private var hasScrolled = false

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE && hasScrolled) {
                hasScrolled = false
                snap()
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dx != 0 || dy != 0) {
                hasScrolled = true
            }
        }
    }

    private val onFlingListener = object : RecyclerView.OnFlingListener() {
        override fun onFling(velocityX: Int, velocityY: Int): Boolean {
            snap(velocityX, velocityY)
            return true
        }
    }

    fun attach() {
        // add scroll and fling listeners
        recyclerView.addOnScrollListener(onScrollListener)
        recyclerView.onFlingListener = onFlingListener
        snap()
    }

    fun detach() {
        // remove scroll and fling listeners
        recyclerView.removeOnScrollListener(onScrollListener)
        recyclerView.onFlingListener = null
    }

    /** 
     * Called when it's time to snap, which usually just means calling [RecyclerView.smoothScrollBy]
     * to reach a certain snap point.
     */
    internal abstract fun snap()
    
    /** Optionally override this if you can make some optimizations given the additional input of fling velocities. */
    internal open fun snap(velocityX: Int, velocityY: Int): Unit = snap()

    /** Get a horizontal orientation helper that this base class creates and caches efficiently for you. */
    internal fun getHorizontalOrientationHelper() = orientationHelperCache.get()

    private inner class OrientationHelperCache {
        private var helper: OrientationHelper? = null
        private var cachedFor: RecyclerView.LayoutManager? = null

        fun get(): OrientationHelper {
            val cachedHelper = helper

            if (cachedHelper != null && cachedFor == recyclerView.layoutManager) {
                return cachedHelper

            } else {
                val newHelper = OrientationHelper.createHorizontalHelper(recyclerView.layoutManager)
                helper = newHelper
                cachedFor = recyclerView.layoutManager
                return newHelper
            }
        }
    }
}

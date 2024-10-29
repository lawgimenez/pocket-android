package com.pocket.analytics

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pocket.util.android.ViewUtil
import com.pocket.util.android.repeatOnResumed
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scroll listener for tracking "Viewable" impressions.  "Viewable" impressions must be
 * at least 50% visible on screen for 1 second.
 */
class ViewableImpressionScrollListener constructor(
    lifecycleOwner: LifecycleOwner,
) : RecyclerView.OnScrollListener(), NestedScrollView.OnScrollChangeListener {
    private val alreadyTrackedIdentifiers = mutableListOf<Any>()
    private val currentlyTracking = mutableMapOf<Any, ImpressionTracker>()

    init {
        // when we resume, check if any items are visible
        lifecycleOwner.repeatOnResumed {
            // post so the UI can finish loading before we check
            Handler(Looper.getMainLooper()).post {
                checkAll()
            }
        }
    }

    /**
     * @param view the view we are watching
     * @param identifier any value that can be used to uniquely identify the view
     * @param onImpression the callback where you should send the impression
     */
    fun track(view: View, identifier: Any, onImpression: () -> Unit) {
        if (alreadyTrackedIdentifiers.contains(identifier)) {
            return
        }

        // in recycler views, views are reused.  Check if the view is currently being tracked.
        // if it is and the identifier is different, that means the view was reassigned.
        val pairWithView = try {
            currentlyTracking.entries.first { it.value == it.value }
        } catch (e: NoSuchElementException) {
            null
        }
        // not null if we are tracking the view already
        if (pairWithView != null) {
            if (pairWithView.key != identifier) {
                // view was reassigned in a recycler view, stop tracking the old pair
                pairWithView.value.cancelJob()
                currentlyTracking.remove(pairWithView)
            } else {
                // view was not reassigned, but we are already tracking it
                return
            }
        }

        currentlyTracking[identifier] = ImpressionTracker(view, identifier, onImpression)
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        checkAll()
    }

    override fun onScrollChange(
        v: NestedScrollView,
        scrollX: Int,
        scrollY: Int,
        oldScrollX: Int,
        oldScrollY: Int,
    ) {
        checkAll()
    }

    private fun checkAll() {
        currentlyTracking.forEach {
            it.value.checkIfVisible()
        }
    }

    inner class ImpressionTracker(
        val view: View,
        private val identifier: Any,
        private val onImpression: () -> Unit,
    ) {

        private var impressionJob: Job? = null

        init {
            // post to make sure the view finishes getting drawn before checking if it's visible
            Handler(Looper.getMainLooper()).post {
                checkIfVisible()
            }
        }

        fun checkIfVisible() {
            when {
                ViewUtil.getPercentVisible(view) < MIN_VISIBLE_PERCENT -> {
                    // if the view is no longer visible, we cancel the coroutine job so the event
                    // doesn't get sent
                    cancelJob()
                }
                ViewUtil.getPercentVisible(view) >= MIN_VISIBLE_PERCENT
                        && impressionJob == null-> {
                    // if the view is visible, start a coroutine job that will wait 1 second
                    // then call the callback
                    impressionJob = MainScope().launch {
                        delay(MIN_VISIBLE_DURATION_SECONDS)
                        onImpression()
                        alreadyTrackedIdentifiers.add(identifier)
                        currentlyTracking.remove(identifier)
                    }
                }
            }
        }

        fun cancelJob() {
            impressionJob?.cancel()
            impressionJob = null
        }
    }

    companion object {
        const val MIN_VISIBLE_PERCENT = 0.5
        const val MIN_VISIBLE_DURATION_SECONDS = 1_000L
    }
}


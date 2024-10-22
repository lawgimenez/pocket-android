package com.pocket.analytics.api

import android.view.View

/**
 * Tracker sets this listener on [Engageable] UI components to get notified when
 * engagements happen.
 */
fun interface EngagementListener {
    /**
     * @param view that was engaged
     * @param value An optional value to use as TrackEngagement_1_0_1.value
     */
    fun onEngaged(view: View, value: String?)

    companion object {
        val NOT_LISTENING = EngagementListener { _, _ -> }
    }
}
/** A UI component that can trigger general engagement events. */
interface Engageable : UiEntityable {
    /** Tracker will set a [listener] to get notified when engagements happen. */
    fun setEngagementListener(listener: EngagementListener?)

    /** An optional value to use as TrackEngagement_1_0_1.value */
    val engagementValue: String?
        get() = null
}

/** A helper for implementing [Engageable] in UI components. */
class EngageableHelper : UiEntityableHelper(), Engageable {
    private var engagementListener: EngagementListener = EngagementListener.NOT_LISTENING
    
    /** Delegate UI component's [Engageable.setEngagementListener] to this method. */
    override fun setEngagementListener(listener: EngagementListener?) {
        engagementListener = listener ?: EngagementListener.NOT_LISTENING
    }
    
    /** Call when engagement happens to call the [EngagementListener] */
    @JvmOverloads fun onEngaged(view: View, value: String? = null) {
        engagementListener.onEngaged(view, value)
    }

    /** Wrap a [View.OnClickListener] to automatically call [EngagementListener] on clicks. */
    fun getWrappedClickListener(listener: View.OnClickListener?): View.OnClickListener? {
        if (listener == null) return null

        return View.OnClickListener {
            onEngaged(it, (it as? Engageable)?.engagementValue)
            listener.onClick(it)
        }
    }
}

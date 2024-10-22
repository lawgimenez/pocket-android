package com.pocket.app.home.slates.overflow

import androidx.lifecycle.ViewModel
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.RecommendationBottomSheetEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class RecommendationOverflowBottomSheetViewModel @Inject constructor(
    private val tracker: Tracker,
): ViewModel(), RecommendationOverflowInteractions {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    private lateinit var url: String
    private lateinit var title: String
    private var corpusRecommendationId: String? = null

    override fun onInitialized(url: String, title: String, corpusRecommendationId: String?) {
        this.url = url
        this.title = title
        this.corpusRecommendationId = corpusRecommendationId
    }

    override fun onReportThisItemClicked() {
        tracker.track(RecommendationBottomSheetEvents.reportClicked(
            url = url,
            itemTitle = title,
            corpusRecommendationId = corpusRecommendationId
        ))
        _events.tryEmit(Event.ShowReport)
    }

    override fun onShareClicked() {
        tracker.track(RecommendationBottomSheetEvents.shareClicked(
            url = url,
            itemTitle = title,
            corpusRecommendationId = corpusRecommendationId
        ))
        _events.tryEmit(Event.ShowShare)
    }

    sealed class Event {
        object ShowShare: Event()
        object ShowReport: Event()
    }
}

interface RecommendationOverflowInteractions {
    fun onInitialized(url: String, title: String, corpusRecommendationId: String?)
    fun onReportThisItemClicked()
    fun onShareClicked()
}
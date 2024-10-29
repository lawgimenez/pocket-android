package com.pocket.app.home.saves.overflow

import androidx.lifecycle.ViewModel
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.app.home.slates.overflow.RecommendationOverflowBottomSheetViewModel
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.thing.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class RecentSaveOverflowViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val tracker: Tracker,
) : ViewModel(), RecentSaveOverflowInteractions {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    private lateinit var item: Item
    // used for analytics
    private var index: Int = 0

    override fun onInitialized(item: Item, index: Int) {
        this.item = item
        this.index = index
    }

    override fun onMarkAsViewedClicked() {
        tracker.track(HomeEvents.recentSavesOverflowMarkAsViewed(index, item.id_url!!.url))
        itemRepository.toggleViewed(item)
        _events.tryEmit(Event.Dismiss)
    }

    override fun onShareClicked() {
        tracker.track(HomeEvents.recentSavesOverflowShare(index, item.id_url!!.url))
        _events.tryEmit(Event.ShowShare)
    }

    override fun onArchiveClicked() {
        tracker.track(HomeEvents.recentSavesOverflowArchive(index, item.id_url!!.url))
        itemRepository.archive(item)
        _events.tryEmit(Event.Dismiss)
    }

    override fun onDeleteClicked() {
        tracker.track(HomeEvents.recentSavesOverflowDelete(index, item.id_url!!.url))
        itemRepository.delete(item)
        _events.tryEmit(Event.Dismiss)
    }

    sealed class Event {
        object ShowShare: Event()
        object Dismiss: Event()
    }
}

interface RecentSaveOverflowInteractions {
    fun onInitialized(item: Item, index: Int)
    fun onMarkAsViewedClicked()
    fun onShareClicked()
    fun onArchiveClicked()
    fun onDeleteClicked()
}
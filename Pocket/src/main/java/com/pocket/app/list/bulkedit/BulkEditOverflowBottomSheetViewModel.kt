package com.pocket.app.list.bulkedit

import androidx.lifecycle.ViewModel
import com.ideashower.readitlater.R
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.analytics.appevents.SavesTab
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.util.StringLoader
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BulkEditOverflowBottomSheetViewModel @Inject constructor(
    private val stringLoader: StringLoader,
    private val itemRepository: ItemRepository,
    private val tracker: Tracker,
): ViewModel(), BulkEditOverflowBottomSheetInteractions {

    private val _uiState = MutableStateFlow(BulkEditOverflowBottomSheetUiState())
    val uiState: StateFlow<BulkEditOverflowBottomSheetUiState> = _uiState

    private val _navigationEvents = MutableSharedFlow<BulkEditOverflowNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<BulkEditOverflowNavigationEvent> = _navigationEvents

    private var items: List<Item> = listOf()
    private lateinit var savesTab: SavesTab

    private fun List<Item>.containsUnFavorited(): Boolean =
        firstOrNull { it.favorite != true  } != null

    override fun onInitialized(items: List<Item>, savesTab: SavesTab) {
        this.savesTab = savesTab
        this.items = items
        _uiState.edit { copy(
            title = stringLoader.getQuantityString(R.plurals.lb_selected, items.size, items.size),
            favoriteText = if (items.containsUnFavorited()) {
                stringLoader.getString(com.pocket.ui.R.string.ic_favorite)
            } else {
                stringLoader.getString(com.pocket.ui.R.string.ic_unfavorite)
            }
        ) }
    }

    override fun onFavoriteClicked() {
        tracker.track(SavesEvents.bulkEditOverflowFavoriteClicked(savesTab))
        if (items.containsUnFavorited()) {
            itemRepository.favorite(*items.toTypedArray())
        } else {
            itemRepository.unFavorite(*items.toTypedArray())
        }
        _navigationEvents.tryEmit(BulkEditOverflowNavigationEvent.Close)
    }

    override fun onEditTagsClicked() {
        tracker.track(SavesEvents.bulkEditOverflowAddTagsClicked(savesTab))
        _navigationEvents.tryEmit(BulkEditOverflowNavigationEvent.OpenTagScreen)
    }

    override fun onMarkAsViewedClicked() {
        tracker.track(SavesEvents.bulkEditOverflowMarkAsViewedClicked(savesTab))
        itemRepository.markAsViewed(*items.toTypedArray())
        _navigationEvents.tryEmit(BulkEditOverflowNavigationEvent.Close)
    }

    override fun onMarkAsNotViewedClicked() {
        tracker.track(SavesEvents.bulkEditOverflowMarkAsNotViewedClicked(savesTab))
        itemRepository.markAsNotViewed(*items.toTypedArray())
        _navigationEvents.tryEmit(BulkEditOverflowNavigationEvent.Close)
    }
}

data class BulkEditOverflowBottomSheetUiState(
    val title: String = "",
    val favoriteText: String = "",
)

sealed class BulkEditOverflowNavigationEvent {
    object Close : BulkEditOverflowNavigationEvent()
    object OpenTagScreen: BulkEditOverflowNavigationEvent()
}

interface BulkEditOverflowBottomSheetInteractions {
    fun onInitialized(items: List<Item>, savesTab: SavesTab)
    fun onFavoriteClicked()
    fun onEditTagsClicked()
    fun onMarkAsViewedClicked()
    fun onMarkAsNotViewedClicked()
}
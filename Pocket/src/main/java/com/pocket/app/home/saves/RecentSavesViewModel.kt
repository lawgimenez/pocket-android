package com.pocket.app.home.saves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.app.home.Home
import com.pocket.repository.ItemRepository
import com.pocket.repository.SavesRepository
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk2.view.ModelBindingHelper
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentSavesViewModel @Inject constructor(
    private val savesRepository: SavesRepository,
    private val modelBindingHelper: ModelBindingHelper,
    private val itemRepository: ItemRepository,
    private val tracker: Tracker,
    private val contentOpenTracker: ContentOpenTracker,
) : ViewModel(), Home.SavesInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _recentSavesUiState = MutableStateFlow(listOf<SaveUiState>())
    val recentSavesUiState: StateFlow<List<SaveUiState>> = _recentSavesUiState

    private val _events = MutableSharedFlow<Home.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Home.Event> = _events

    override fun onInitialized() {
        setupSavesFlow()
    }

    private fun setupSavesFlow() {
        viewModelScope.launch {
            savesRepository.getRecentSavesAsFlow(count = 5)
                .mapNotNull { it.list }
                .collect { savesItemList ->
                    updateSaves(savesItemList)
                }
        }
    }

    private fun updateSaves(
        saves: List<Item>,
    ) {
        _recentSavesUiState.edit {
            saves.map { item ->
                SaveUiState(
                    item = item,
                    title = item.display_title ?: "",
                    domain = item.display_domain ?: "",
                    timeToRead = modelBindingHelper.timeToReadEstimate(item) ?: "",
                    imageUrl = item.display_thumbnail?.url,
                    isCollection = item.collection?.slug != null,
                    isFavorited = item.favorite ?: false,
                    titleIsBold = item.viewed != true,
                    index = saves.indexOf(item)
                )
            }
        }
        _uiState.edit {
            copy(
                screenState = if (saves.isEmpty()) {
                    ScreenState.Empty
                } else {
                    ScreenState.Saves
                }
            )
        }
    }

    override fun onFavoriteClicked(item: Item, positionInList: Int) {
        tracker.track(HomeEvents.recentSavesFavorite(positionInList))
        itemRepository.toggleFavorite(item)
    }

    override fun onSaveOverflowClicked(item: Item, itemPosition: Int) {
        tracker.track(HomeEvents.recentSavesOverflow(itemPosition))
        _events.tryEmit(Home.Event.ShowSaveOverflow(item, itemPosition))
    }

    override fun onSeeAllSavesClicked() {
        tracker.track(HomeEvents.recentSavesSeeAllClicked())
        _events.tryEmit(Home.Event.GoToMyList)
    }

    override fun onItemClicked(item: Item, positionInList: Int) {
        contentOpenTracker.track(
            HomeEvents.recentSavesCardContentOpen(
                itemUrl = item.id_url?.url!!,
                positionInList = positionInList,
            )
        )
        _events.tryEmit(Home.Event.GoToReader(item.id_url?.url!!))
    }

    override fun onSaveViewed(positionInList: Int, url: String) {
        tracker.track(HomeEvents.recentSavesImpression(positionInList, url))
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
    )

    sealed class ScreenState(
        val titleVisible: Boolean = false,
        val recentSavesVisible: Boolean = false,
        val recentSavesLoadingVisible: Boolean = false,
    ) {
        object Loading : ScreenState(
            recentSavesLoadingVisible = true,
            titleVisible = true,
        )

        object Saves : ScreenState(
            recentSavesVisible = true,
            titleVisible = true,
        )

        object Empty : ScreenState()
    }

    data class SaveUiState(
        val item: Item,
        val title: String,
        val domain: String,
        val timeToRead: String,
        val imageUrl: String?,
        val isCollection: Boolean,
        val isFavorited: Boolean,
        val titleIsBold: Boolean,
        // used in analytics
        val index: Int,
    )
}
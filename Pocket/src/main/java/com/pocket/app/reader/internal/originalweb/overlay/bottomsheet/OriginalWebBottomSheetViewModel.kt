package com.pocket.app.reader.internal.originalweb.overlay.bottomsheet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.OriginalWebViewEvents
import com.pocket.data.models.DomainItem
import com.pocket.data.models.ItemType
import com.pocket.repository.ItemRepository
import com.pocket.usecase.GetTrack
import com.pocket.usecase.Save
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OriginalWebBottomSheetViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val save: Save,
    private val getTrack: GetTrack,
    private val tracker: Tracker,
) : ViewModel(),
    OriginalWebBottomSheet.Initializer,
    OriginalWebBottomSheet.ButtonInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<OriginalWebBottomSheet.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<OriginalWebBottomSheet.Event> = _events

    lateinit var url: String

    override fun onInitialized(url: String) {
        this.url = url
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            try {
                val item = itemRepository.getDomainItem(url)
                updateItemState(item)
            } catch (e: Exception) {
                Log.e("OriginalWebBottomSheetViewModel", e.message ?: "")
                _uiState.edit { copy(
                    mainActionState = MainActionState.Save,
                    listenState = ListenState.Disabled,
                ) }
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun updateItemState(item: DomainItem) {
        _uiState.edit { copy(
            mainActionState = when {
                item.isArchived -> MainActionState.ReAdd
                item.isSaved -> MainActionState.Archive
                else -> MainActionState.Save
            },
            favoriteState = when {
                item.isFavorited -> FavoriteState.Favorited
                else -> FavoriteState.NotFavorited
            },
            viewedState = when {
                item.isViewed -> ViewedState.Viewed
                else -> ViewedState.NotViewed
            },
            switchToArticleViewVisible = item.type == ItemType.ARTICLE,
            listenState = if (item.type == ItemType.ARTICLE) {
                ListenState.Enabled
            } else {
                ListenState.Disabled
            },
        ) }
    }

    override fun onMainActionClicked() {
        when (uiState.value.mainActionState) {
            MainActionState.ReAdd -> {
                tracker.track(OriginalWebViewEvents.reAddClicked())
                itemRepository.unArchive(url)
                _uiState.edit { copy(
                    mainActionState = MainActionState.Archive
                ) }
                _events.tryEmit(OriginalWebBottomSheet.Event.ShowReAddedToast)
            }
            MainActionState.Archive -> {
                tracker.track(OriginalWebViewEvents.archiveClicked())
                itemRepository.archive(url)
                _uiState.edit { copy(
                    mainActionState = MainActionState.ReAdd
                ) }
                _events.tryEmit(OriginalWebBottomSheet.Event.ShowArchivedToast)
                _events.tryEmit(OriginalWebBottomSheet.Event.GoBack)
            }
            MainActionState.Save -> {
                tracker.track(OriginalWebViewEvents.saveClicked(url))
                viewModelScope.launch {
                    when (save(url)) {
                        Save.Result.Success -> {
                            loadItem()
                            _events.emit(OriginalWebBottomSheet.Event.ShowSavedToast)
                        }
                        Save.Result.NotLoggedIn -> {
                            _events.emit(OriginalWebBottomSheet.Event.GoToSignIn)
                        }
                    }
                }
            }
        }
    }

    override fun onListenClicked() {
        tracker.track(OriginalWebViewEvents.listenClicked())
        viewModelScope.launch {
            val item = try {
                getTrack(url)
            } catch (e: Exception) {
                null
            }
            item?.let { _events.tryEmit(OriginalWebBottomSheet.Event.OpenListen(it)) }
        }
    }

    override fun onShareClicked() {
        tracker.track(OriginalWebViewEvents.shareClicked())
        viewModelScope.launch {
            val title = try {
                itemRepository.getDomainItem(url).displayTitle
            } catch (e: Exception) {
                null
            }
            _events.tryEmit(OriginalWebBottomSheet.Event.ShowShare(title ?: ""))
        }
    }

    override fun onSwitchToArticleViewClicked() {
        tracker.track(OriginalWebViewEvents.switchToArticleClicked())
        _events.tryEmit(OriginalWebBottomSheet.Event.SwitchToArticleView)
    }

    override fun onFavoriteClicked() {
        tracker.track(OriginalWebViewEvents.favoriteClicked())
        when (uiState.value.favoriteState) {
            FavoriteState.Favorited -> {
                itemRepository.unfavorite(url)
                _uiState.edit { copy(
                    favoriteState = FavoriteState.NotFavorited
                ) }
            }
            FavoriteState.NotFavorited -> {
                itemRepository.favorite(url)
                _uiState.edit { copy(
                    favoriteState = FavoriteState.Favorited
                ) }
            }
        }
    }

    override fun onAddTagsClicked() {
        tracker.track(OriginalWebViewEvents.addTagsClicked())
        viewModelScope.launch {
            val item = try {
                itemRepository.getItemOrThrow(url)
            } catch (e: Exception) {
                null
            }
            item?.let { _events.tryEmit(OriginalWebBottomSheet.Event.OpenTagScreen(it)) }
        }

    }

    override fun onMarkAsViewedClicked() {
        tracker.track(OriginalWebViewEvents.markAsViewedClicked())
        when (uiState.value.viewedState) {
            ViewedState.Viewed -> {
                itemRepository.markAsNotViewed(url)
                _uiState.edit { copy(
                    viewedState = ViewedState.NotViewed
                ) }
            }
            ViewedState.NotViewed -> {
                itemRepository.markAsViewed(url)
                _uiState.edit { copy(
                    viewedState = ViewedState.Viewed
                ) }
            }
        }
    }

    override fun onDeleteClicked() {
        tracker.track(OriginalWebViewEvents.deleteClicked())
        itemRepository.delete(url)
        _uiState.edit { copy(
            mainActionState = MainActionState.Save,
            listenState = ListenState.Disabled
        ) }
        _events.tryEmit(OriginalWebBottomSheet.Event.GoBack)
    }

    data class UiState(
        val mainActionState: MainActionState = MainActionState.Save,
        val favoriteState: FavoriteState = FavoriteState.NotFavorited,
        val viewedState: ViewedState = ViewedState.Viewed,
        val switchToArticleViewVisible: Boolean = false,
        val listenState: ListenState = ListenState.Disabled,
    )

    sealed class MainActionState(
        val textId: Int,
        val drawableId: Int,
        val savedContentVisible: Boolean = false,
    ) {
        object Save : MainActionState(
            textId = com.pocket.ui.R.string.ic_save,
            drawableId = com.pocket.ui.R.drawable.ic_pkt_save_line,
        )
        object Archive : MainActionState(
            textId = com.pocket.ui.R.string.ic_archive,
            drawableId = com.pocket.ui.R.drawable.ic_pkt_archive_line,
            savedContentVisible = true,
        )
        object ReAdd : MainActionState(
            textId = com.pocket.ui.R.string.ic_readd,
            drawableId = com.pocket.ui.R.drawable.ic_pkt_re_add_line,
            savedContentVisible = true,
        )
    }

    sealed class FavoriteState(
        val textId: Int,
        val drawableId: Int,
    ) {
        object Favorited : FavoriteState(
            textId = com.pocket.ui.R.string.ic_unfavorite,
            drawableId = com.pocket.ui.R.drawable.ic_pkt_favorite_solid,
        )
        object NotFavorited : FavoriteState(
            textId = com.pocket.ui.R.string.ic_favorite,
            drawableId = com.pocket.ui.R.drawable.ic_pkt_favorite_line,
        )
    }

    sealed class ViewedState(
        val textId: Int,
        val drawableId: Int,
    ) {
        object Viewed : ViewedState(
            textId = com.pocket.ui.R.string.ic_mark_as_not_viewed,
            drawableId = com.pocket.ui.R.drawable.ic_viewed_not,
        )
        object NotViewed : ViewedState(
            textId = com.pocket.ui.R.string.ic_mark_as_viewed,
            drawableId = com.pocket.ui.R.drawable.ic_viewed,
        )
    }

    sealed class ListenState(
        val enabled: Boolean,
        val colorId: Int,
    ) {
        object Enabled : ListenState(
            enabled = true,
            colorId = com.pocket.ui.R.color.pkt_themed_grey_1
        )
        object Disabled : ListenState(
            enabled = false,
            colorId = com.pocket.ui.R.color.pkt_themed_grey_5
        )
    }
}
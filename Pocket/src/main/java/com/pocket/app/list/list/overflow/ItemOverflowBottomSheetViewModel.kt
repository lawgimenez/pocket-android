package com.pocket.app.list.list.overflow

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import com.ideashower.readitlater.R
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.undobar.UndoBar
import com.pocket.app.undobar.UndoableItemAction.Companion.fromDomainItem
import com.pocket.data.models.toDomainItem
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.enums.ItemStatus
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.util.DrawableLoader
import com.pocket.util.StringLoader
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ItemOverflowBottomSheetViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val undoable: UndoBar,
    private val stringLoader: StringLoader,
    private val drawableLoader: DrawableLoader,
    private val tracker: Tracker,
) : ViewModel(), ItemOverflowBottomSheetInteractions {

    private val _uiState = MutableStateFlow(ItemOverflowBottomSheetUiState())
    val uiState: StateFlow<ItemOverflowBottomSheetUiState> = _uiState

    private lateinit var item: Item
    private lateinit var savesTab: SavesTab

    override fun onInitialized(
        item: Item,
        savesTab: SavesTab,
    ) {
        this.item = item
        this.savesTab = savesTab
        _uiState.edit { copy(
            title = item.display_title ?: "",
            imageUrl = item.display_thumbnail?.url,
            viewedText = if (item.viewed == true) {
                stringLoader.getString(com.pocket.ui.R.string.ic_mark_as_not_viewed)
            } else {
                stringLoader.getString(com.pocket.ui.R.string.ic_mark_as_viewed)
            },
            viewedIcon = if (item.viewed == true) {
                drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_viewed_not)
            } else {
                drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_viewed)
            },
            archiveText = if (item.status == ItemStatus.ARCHIVED) {
                stringLoader.getString(R.string.move_to_my_list)
            } else {
                stringLoader.getString(com.pocket.ui.R.string.ic_archive)
            },
            archiveIcon = if (item.status == ItemStatus.ARCHIVED) {
                drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_pkt_re_add_line)
            } else {
                drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_pkt_archive_line)
            },
        ) }
    }

    override fun onViewedClicked() {
        tracker.track(SavesEvents.itemOverflowMarkAsViewedClicked(savesTab))
        itemRepository.toggleViewed(item)
        _uiState.edit { copy(
            screenState = ItemOverflowBottomSheetScreenState.CLOSING
        ) }
    }

    override fun onTagClicked() {
        tracker.track(SavesEvents.itemOverflowEditTagsClicked(savesTab))
        _uiState.edit { copy(
            screenState = ItemOverflowBottomSheetScreenState.OPEN_TAG_SCREEN
        ) }
    }

    override fun onArchiveClicked() {
        tracker.track(SavesEvents.itemOverflowArchiveClicked(savesTab))
        val isReAdding = item.status == ItemStatus.ARCHIVED
        if (isReAdding) {
            itemRepository.unArchive(item)
        } else {
            undoable.archive(fromDomainItem(item.toDomainItem()))
        }
        _uiState.edit { copy(
            screenState = ItemOverflowBottomSheetScreenState.CLOSING
        ) }
    }

    override fun onDeleteClicked() {
        tracker.track(SavesEvents.itemOverflowDeleteClicked(savesTab))
        undoable.delete(item)
        _uiState.edit { copy(
            screenState = ItemOverflowBottomSheetScreenState.CLOSING
        ) }
    }
}

data class ItemOverflowBottomSheetUiState(
    val title: String = "",
    val imageUrl: String? = null,
    val viewedText: String = "",
    val viewedIcon: Drawable? = null,
    val archiveText: String = "",
    val archiveIcon: Drawable? = null,
    val screenState: ItemOverflowBottomSheetScreenState = ItemOverflowBottomSheetScreenState.SHOWING,
)

enum class ItemOverflowBottomSheetScreenState {
    SHOWING,
    CLOSING,
    OPEN_TAG_SCREEN,
}

interface ItemOverflowBottomSheetInteractions {
    fun onInitialized(item: Item, savesTab: SavesTab)
    fun onViewedClicked()
    fun onTagClicked()
    fun onArchiveClicked()
    fun onDeleteClicked()
}
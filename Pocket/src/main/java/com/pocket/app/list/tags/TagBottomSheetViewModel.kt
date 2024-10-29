package com.pocket.app.list.tags

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideashower.readitlater.R
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.list.list.ListManager
import com.pocket.repository.TagRepository
import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.util.StringLoader
import com.pocket.util.collect
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TagBottomSheetViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val stringLoader: StringLoader,
    private val listManager: ListManager,
    private val tracker: Tracker,

) : ViewModel(), TagBottomSheetInteractions {

    private val _uiState = MutableStateFlow(TagBottomSheetUiState(
        title = stringLoader.getString(R.string.lb_tags_autocomplete)
    ))
    val uiState: StateFlow<TagBottomSheetUiState> = _uiState

    private val _tagsListUiState = MutableStateFlow(listOf<BottomSheetItemUiState>())
    val tagsListUiState: StateFlow<List<BottomSheetItemUiState>> = _tagsListUiState

    private val _navigationEvents = MutableSharedFlow<TagBottomSheetNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<TagBottomSheetNavigationEvent> = _navigationEvents

    private lateinit var savesTab: SavesTab
    private var tagList: List<String> = listOf()
    private val tagEditMap: MutableMap<String, String> = mutableMapOf()
    var tagToDelete: String? = null
        private set

    override fun onInitialized(savesTab: SavesTab) {
        this.savesTab = savesTab
        tagRepository.getTagsAsFlow().collect(viewModelScope) { tags ->
            mutableListOf<String>().apply {
                val recentlyUsed = tags.recentlyUsed?.take(3)?.map { it.tag }
                recentlyUsed?.let { addAll(it.filterNotNull()) }
                tags.tags?.sortedBy { it }
                    ?.map {
                        if (recentlyUsed?.contains(it) != true) {
                            add(it)
                        }
                    }
                tagList = this
                if (tagList.isEmpty()) {
                    _navigationEvents.tryEmit(TagBottomSheetNavigationEvent.Close)
                }
                invalidateTagListItemUiState()
            }
        }
    }

    override fun onTagClicked(tag: String) {
        listManager.setTag(tag)
        _navigationEvents.tryEmit(TagBottomSheetNavigationEvent.Close)
    }

    override fun onNotTaggedClicked() {
        listManager.addFilter(ItemFilterKey.NOT_TAGGED)
        _navigationEvents.tryEmit(TagBottomSheetNavigationEvent.Close)
    }

    override fun onEditClicked() {
        tracker.track(SavesEvents.tagsOverflowClicked(savesTab))
        enableEditMode()
    }

    override fun onTagEdited(oldValue: String, newValue: String) {
        tagEditMap[oldValue] = newValue
        _uiState.edit { copy(
            saveVisibility = View.VISIBLE
        ) }
    }

    override fun onSaveClicked() {
        tracker.track(SavesEvents.tagsSaveChangesClicked(savesTab))
        val map = mutableMapOf<String, String>()
        tagEditMap.map {
            if (it.key != it.value) {
                map[it.key] = it.value
            }
        }
        tagRepository.editTags(map)
        disableEditMode()
    }

    override fun onCancelClicked() {
        tracker.track(SavesEvents.tagsCancelEditClicked(savesTab))
        disableEditMode()
    }

    private fun enableEditMode() {
        _uiState.edit { copy(
            title = stringLoader.getString(R.string.edit_tags),
            cancelVisibility = View.VISIBLE,
            overflowVisibility = View.GONE,
        ) }
        invalidateTagListItemUiState()
    }

    private fun disableEditMode() {
        tagEditMap.clear()
        _uiState.edit { TagBottomSheetUiState(
            title = stringLoader.getString(R.string.lb_tags_autocomplete),
        ) }
        invalidateTagListItemUiState()
    }

    private fun invalidateTagListItemUiState() {
        val inEditMode = uiState.value.cancelVisibility == View.VISIBLE
        _tagsListUiState.update {
            mutableListOf<BottomSheetItemUiState>().apply {
                add(
                    index = 0,
                    element = NotTaggedBottomSheetItemUiState(
                        clickable = !inEditMode
                    )
                )
                addAll(
                    tagList.map { tag ->
                        TagBottomSheetItemUiState(
                            tag = tag,
                            editable = inEditMode,
                            trashVisibility = uiState.value.cancelVisibility
                        )
                    }
                )
            }
        }
    }

    override fun onDeleteTagClicked(tag: String) {
        tagToDelete = tag
        _navigationEvents.tryEmit(TagBottomSheetNavigationEvent.ShowConfirmDelete)
    }

    override fun onDeleteTagConfirmed() {
        tracker.track(SavesEvents.tagsDeleteClicked(savesTab))
        tagToDelete?.let { tagRepository.deleteTag(it) }
        tagToDelete = null
    }

    override fun onDeleteCanceled() {
        tagToDelete = null
    }

    override fun onDismissed() {
        if (listManager.sortFilterState.value.tag.isNullOrBlank()
            && !listManager.sortFilterState.value.filters.contains(ItemFilterKey.NOT_TAGGED)
        ) {
            listManager.clearFilters()
        }
    }
}

data class TagBottomSheetUiState(
    val title: String,
    val cancelVisibility: Int = View.GONE,
    val saveVisibility: Int = View.GONE,
    val overflowVisibility: Int = View.VISIBLE,
)

sealed class BottomSheetItemUiState

data class TagBottomSheetItemUiState(
    val tag: String,
    val editable: Boolean = false,
    val trashVisibility: Int = View.GONE,
) : BottomSheetItemUiState()

data class NotTaggedBottomSheetItemUiState(
    val clickable: Boolean = true
) : BottomSheetItemUiState()

sealed class TagBottomSheetNavigationEvent {
    object Close: TagBottomSheetNavigationEvent()
    object ShowConfirmDelete: TagBottomSheetNavigationEvent()
}

interface TagBottomSheetInteractions {
    fun onInitialized(savesTab: SavesTab)
    fun onEditClicked()
    fun onSaveClicked()
    fun onCancelClicked()
    fun onTagEdited(oldValue: String, newValue: String)
    fun onTagClicked(tag: String)
    fun onNotTaggedClicked()
    fun onDeleteTagClicked(tag: String)
    fun onDeleteTagConfirmed()
    fun onDeleteCanceled()
    fun onDismissed()
}
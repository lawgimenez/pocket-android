package com.pocket.app.list

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideashower.readitlater.R
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.list.MyListViewModel.OnAddClick.*
import com.pocket.app.list.list.ListManager
import com.pocket.app.list.list.ListStatus
import com.pocket.app.notes.Notes
import com.pocket.app.undobar.UndoBar
import com.pocket.app.undobar.UndoableItemAction.Companion.fromDomainItem
import com.pocket.data.models.DomainItem
import com.pocket.data.models.toDomainItem
import com.pocket.repository.ItemRepository
import com.pocket.repository.SearchRepository
import com.pocket.repository.TagRepository
import com.pocket.sdk.api.AppSync
import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.SearchItem
import com.pocket.sdk.api.value.HtmlString
import com.pocket.sdk.offline.OfflineDownloading
import com.pocket.sdk.util.data.DataSourceCache
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.sdk2.view.ModelBindingHelper
import com.pocket.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    val listManager: ListManager,
    private val itemRepository: ItemRepository,
    private val undoable: UndoBar,
    private val modelBindingHelper: ModelBindingHelper,
    private val stringLoader: StringLoader,
    private val tagRepository: TagRepository,
    private val pocketCache: PocketCache,
    private val offlineDownloading: OfflineDownloading,
    private val appSync: AppSync,
    private val searchRepository: SearchRepository,
    private val notes: Notes,
    private val tracker: Tracker,
    private val contentOpenTracker: ContentOpenTracker,
) : ViewModel(), MyListInteractions {

    private val _uiState = MutableStateFlow(MyListUiState())
    val uiState: StateFlow<MyListUiState> = _uiState

    private val _listState = MutableStateFlow(listOf<ListItemUiState>())
    val listState: StateFlow<List<ListItemUiState>> = _listState

    private val _recentSearchState = MutableStateFlow(listOf<RecentSearchItemUiState>())
    val recentSearchState: StateFlow<List<RecentSearchItemUiState>> = _recentSearchState

    private val _navigationEvents = MutableSharedFlow<MyListNavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<MyListNavigationEvent> = _navigationEvents

    private val itemsSelectedForBulkEdit: MutableList<Item> = mutableListOf()

    val savesTab: SavesTab
        get() = if (listManager.sortFilterState.value.listStatus == ListStatus.ARCHIVE) {
            SavesTab.ARCHIVE
        } else {
            SavesTab.SAVES
        }

    var hasTags = true
        private set

    private var searchDelayJob: Job? = null
    // this is the search text currently shown in the search bar
    // it may not be the search text the list is currently using because we added a slight delay.
    // to get the current search text the list is using, try listManager.sortFilterState.value.search
    private var delayedSearchText = ""

    private enum class OnAddClick { JustOpenAddUrl, ShowChoiceBetweenAddUrlAndAddNote }
    private var onAddClick = JustOpenAddUrl

    init {
        setupListSortObserver()
        setupListManagerStateObserver()
        setupListObserver()
        setupTagListener()
        setupRecentSearchesListener()
        viewModelScope.launch {
            val notesEnabled = notes.areEnabled()
            _uiState.update {
                it.copy(
                    filterCarouselState = it.filterCarouselState.copy(
                        notesFilterVisible = notesEnabled,
                    ),
                )
            }
            onAddClick = when (notesEnabled) {
                true -> ShowChoiceBetweenAddUrlAndAddNote
                false -> JustOpenAddUrl
            }
        }
    }

    private fun setupTagListener() {
        tagRepository.getTagsAsFlow().collect(viewModelScope) {
            hasTags = !it.tags.isNullOrEmpty()
        }
    }

    @Suppress("MagicNumber")
    private fun setupRecentSearchesListener() {
        searchRepository.getRecentSearches().collect(viewModelScope) { recentSearches ->
            recentSearches.searches?.let { searches ->
                _recentSearchState.value = searches
                    .filter { !it.search.isNullOrBlank() }
                    .take(5) // only show 5 most recent searches
                    .map { searchQuery ->
                        RecentSearchItemUiState(searchQuery?.search ?: "")
                    }
                if (recentSearchState.value.isNotEmpty()) {
                    _uiState.edit { copy(
                        recentSearchVisibility = if (pocketCache.hasPremium()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    ) }
                }
            }
        }
    }

    private fun setupListSortObserver() {
        listManager.sortFilterState.collect(viewModelScope) { state ->
            _uiState.update { it.copy(
                myListChipState = ChipState(selected = state.listStatus == ListStatus.SAVES),
                archiveChipState = ChipState(selected = state.listStatus == ListStatus.ARCHIVE),
                filterCarouselState = FilterCarouselState(
                    selected = FilterCarouselState.Type.Saves,
                    savesFilter = when {
                        state.filters.isEmpty() -> SavesFilter.All
                        state.filters.contains(ItemFilterKey.FAVORITE) -> SavesFilter.Favorites
                        state.filters.contains(ItemFilterKey.HIGHLIGHTED) -> SavesFilter.Highlighted
                        state.filters.containsAny(ItemFilterKey.TAG, ItemFilterKey.NOT_TAGGED) -> {
                            SavesFilter.Tagged
                        }
                        state.filters.containsAny(
                            ItemFilterKey.VIEWED,
                            ItemFilterKey.NOT_VIEWED,
                            ItemFilterKey.SHORT_READS,
                            ItemFilterKey.LONG_READS
                        ) -> {
                            SavesFilter.FilterMenu
                        }
                        else -> SavesFilter.All
                    }
                ),
                selectedTagChipState = ChipState(
                    selected = true,
                    text = if (state.filters.contains(ItemFilterKey.NOT_TAGGED)) {
                        stringLoader.getString(R.string.mu_untagged)
                    } else {
                        state.tag ?: ""
                    },
                    visibility = if (state.tag != null || state.filters.contains(ItemFilterKey.NOT_TAGGED)) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    },
                ),
                selectedFilterChipState = ChipState(
                    selected = true,
                    text = when {
                        state.filters.contains(ItemFilterKey.LONG_READS) -> {
                            stringLoader.getString(R.string.long_reads)
                        }
                        state.filters.contains(ItemFilterKey.SHORT_READS) -> {
                            stringLoader.getString(R.string.short_reads)
                        }
                        else -> ""
                    },
                    visibility = if (state.filters.containsAny(
                            ItemFilterKey.LONG_READS,
                            ItemFilterKey.SHORT_READS
                        )
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                ),
                emptyViewState = when {
                    !pocketCache.isLoggedIn -> EmptyViewState.SignedOut
                    state.tag != null -> EmptyViewState.SpecificTag
                    state.listStatus == ListStatus.ARCHIVE -> EmptyViewState.Archive
                    state.filters.contains(ItemFilterKey.TAG) -> EmptyViewState.Tagged
                    state.filters.contains(ItemFilterKey.FAVORITE) -> EmptyViewState.Favorite
                    state.filters.contains(ItemFilterKey.HIGHLIGHTED) -> EmptyViewState.Highlights
                    listManager.isSearching -> EmptyViewState.Search
                    else -> EmptyViewState.All
                },
                searchHint = stringLoader.getString(R.string.lb_list_search_hint)
            ) }
        }
    }

    private fun setupListManagerStateObserver() {
        listManager.loadState.collect(viewModelScope) { loadState ->
            when (loadState) {
                DataSourceCache.LoadState.INITIAL_LOADING,
                DataSourceCache.LoadState.INITIAL ->
                    _uiState.edit { copy(
                        screenState = if (listManager.isSearching) {
                            MyListScreenState.SearchLoading
                        } else {
                            MyListScreenState.Loading
                        }
                    ) }
                DataSourceCache.LoadState.INITIAL_ERROR ->
                    _uiState.edit { copy(screenState = MyListScreenState.Error) }
                DataSourceCache.LoadState.LOADED,
                DataSourceCache.LoadState.LOADED_APPENDING,
                DataSourceCache.LoadState.LOADED_APPEND_ERROR,
                DataSourceCache.LoadState.LOADED_REFRESHING,
                DataSourceCache.LoadState.LOADED_REFRESH_ERROR -> setDefaultScreenState()
            }
        }
    }

    private fun setupListObserver() {
        listManager.list.collect(viewModelScope) { list ->
            updateList(list)
        }
    }

    private fun invalidateBulkEdit() {
        updateList(listManager.list.value)

        _uiState.edit { copy(
            bulkEditSnackBarText = if (itemsSelectedForBulkEdit.isEmpty()) {
                stringLoader.getString(R.string.ac_select_all)
            } else {
                stringLoader.getQuantityString(
                    R.plurals.lb_selected,
                    itemsSelectedForBulkEdit.size,
                    itemsSelectedForBulkEdit.size
                )
            },
            bulkEditActionsEnabled = itemsSelectedForBulkEdit.isNotEmpty(),
            bulkEditTextClickable = itemsSelectedForBulkEdit.isEmpty()
        ) }
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun updateList(list: List<Any>) {
        setDefaultScreenState()
        _listState.update {
            list.filter {
                it is Item ||
                        it is SearchItem
            }.mapIndexed { index, value ->
                val item = when (value) {
                    is Item -> value
                    is SearchItem -> value.item!!
                    else -> throw Exception() // not possible with the above filter
                }
                val searchMatch = when (value) {
                    is SearchItem -> value.matches
                    else -> null
                }
                val excerpt = searchMatch?.fullText ?: HtmlString("")
                val imageUrl = item.display_thumbnail?.url

                ListItemUiState(
                    title = modelBindingHelper.title(
                        item,
                        searchMatch,
                        listManager.isSearching,
                        !listManager.isRemoteData,
                        listManager.sortFilterState.value.search,
                    ),
                    domain = modelBindingHelper.domain(
                        item,
                        searchMatch,
                        listManager.isSearching,
                        !listManager.isRemoteData,
                        listManager.sortFilterState.value.search,
                    ),
                    timeEstimate = modelBindingHelper.timeEstimate(item)?.let { " · $it" } ?: "",
                    excerpt = excerpt,
                    excerptVisible = excerpt.toString().isNotBlank()
                            && listManager.isSearching
                            && listManager.isRemoteData,
                    imageUrl = imageUrl,
                    thumbnailVisible = imageUrl != null,
                    favorite = item.favorite == true,
                    badges = mutableListOf<BadgeState>().apply {
                        if (!item.annotations.isNullOrEmpty()) {
                            add(BadgeState(
                                type = BadgeType.HIGHLIGHT,
                                text = item.annotations?.size.toString()
                            ))
                        }
                        item.tags?.forEach { tag ->
                            add(
                                BadgeState(
                                    type = if (listManager.isSearching
                                        && listManager.isRemoteData
                                        && tag.tag?.contains(listManager.sortFilterState.value.search) == true
                                    ) {
                                        BadgeType.SEARCH_MATCHING_TAG
                                    } else {
                                        BadgeType.TAG
                                    },
                                    text = tag.tag ?: "",
                                )
                            )
                        }
                    },
                    titleBold = item.viewed != true,
                    item = item,
                    isInEditMode = _uiState.value.editChipState.selected,
                    isSelectedForBulkEdit = itemsSelectedForBulkEdit.contains(item),
                    showSearchHighlights = listManager.isSearching,
                    isInArchive = listManager.sortFilterState.value.listStatus == ListStatus.ARCHIVE,
                    index = index
                )
            }
        }
    }

    private fun setDefaultScreenState() {
        val hasSearchTerms = listManager.sortFilterState.value.search.isNotBlank()
        val isEmpty = listManager.list.value.isEmpty()
        _uiState.update { it.copy(
            screenState = when {
                listManager.isSearching && hasSearchTerms && !isEmpty -> MyListScreenState.SearchList
                listManager.isSearching && isEmpty -> MyListScreenState.SearchEmpty
                listManager.isSearching -> MyListScreenState.SearchLanding
                isEmpty -> MyListScreenState.Empty
                else -> MyListScreenState.List
            },
            filterCarouselState = it.filterCarouselState.copy(
                selected = FilterCarouselState.Type.Saves,
            ),
            editChipState = uiState.value.editChipState.copy(enabled = !isEmpty),
        ) }
    }

    override fun onScrolledNearBottom() {
        listManager.loadNextPage()
    }

    override fun onAddClicked() {
        tracker.track(SavesEvents.addButtonClicked())
        requireSignedIn {
            when (onAddClick) {
                JustOpenAddUrl -> {
                    _navigationEvents.tryEmit(MyListNavigationEvent.ShowAddUrlBottomSheet)
                }
                ShowChoiceBetweenAddUrlAndAddNote -> {
                    _navigationEvents.tryEmit(MyListNavigationEvent.ShowAddMenu)
                }
            }
        }
    }

    override fun onAddUrlClicked() {
        // TODO(notes): tracker.track(…)
        _navigationEvents.tryEmit(MyListNavigationEvent.ShowAddUrlBottomSheet)
    }

    override fun onAddNoteClicked() {
        // TODO(notes): tracker.track(…)
        // TODO(notes) POCKET-10881
    }

    override fun onMyListChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.savesChipClicked())
        if (!uiState.value.myListChipState.selected) {
            listManager.setStatusFilter(ListStatus.SAVES)
            _uiState.edit {
                copy(
                    searchHint = stringLoader.getString(R.string.lb_list_search_hint)
                )
            }
        }
    }

    override fun onArchiveChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.archiveChipClicked())
        requireSignedIn {
            if (!uiState.value.archiveChipState.selected) {
                listManager.setStatusFilter(ListStatus.ARCHIVE)
                _uiState.edit {
                    copy(
                        searchHint = stringLoader.getString(R.string.lb_list_archive_search_hint)
                    )
                }
            }
        }
    }

    override fun onAllChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.allChipClicked(savesTab))
        showAll()
    }

    private fun showAll() {
        if (!uiState.value.allChipState.selected) {
            listManager.clearFilters()
        }
    }

    override fun onTaggedChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.taggedChipClicked(savesTab))

        requireSignedIn {
            if (!uiState.value.taggedChipState.selected) {
                listManager.addFilter(ItemFilterKey.TAG)
            } else if (!hasTags) {
                showAll()
            }
            if (hasTags) {
                _navigationEvents.tryEmit(MyListNavigationEvent.ShowTagBottomSheet)
            }
        }
    }

    override fun onFavoritesChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.favoritesChipClicked(savesTab))
        requireSignedIn {
            if (!uiState.value.favoritesChipState.selected) {
                listManager.addFilter(ItemFilterKey.FAVORITE)
            } else {
                showAll()
            }
        }
    }

    override fun onHighlightsChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.highlightsChipClicked(savesTab))
        requireSignedIn {
            if (!uiState.value.highlightsChipState.selected) {
                listManager.addFilter(ItemFilterKey.HIGHLIGHTED)
            } else {
                showAll()
            }
        }
    }

    override fun onNotesChipClicked() {
        if (exitEditMode()) return
        // TODO(notes): tracker.track(…)
        requireSignedIn {
            if (!uiState.value.notesChipState.selected) {
                _uiState.update {
                    it.copy(
                        screenState = MyListScreenState.Notes,
                        filterCarouselState = it.filterCarouselState.copy(
                            selected = FilterCarouselState.Type.Notes,
                        ),
                    )
                }
            } else {
                showAll()
            }
        }
    }

    override fun onEditChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.editChipClicked(savesTab))
        requireSignedIn {
            _uiState.edit {
                copy(
                    editChipState = ChipState(selected = true),
                    bulkEditSnackBarText = stringLoader.getString(R.string.ac_select_all),
                    bulkEditTextClickable = true
                )
            }
            invalidateBulkEdit()
        }
    }

    override fun onSelectedTagChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.selectedTagChipClicked(savesTab))
        showAll()
    }

    override fun onFavoriteClicked(item: Item) {
        tracker.track(SavesEvents.itemFavoriteButtonClicked(savesTab))
        itemRepository.toggleFavorite(item)
    }

    override fun onItemClicked(
        item: Item,
        positionInList: Int,
    ) {
        contentOpenTracker.track(
            SavesEvents.savedCardContentOpen(
                itemUrl = item.id_url?.url!!,
                positionInList = positionInList,
                savesTab = savesTab,
            )
        )
        _navigationEvents.tryEmit(MyListNavigationEvent.GoToReader(
            item,
            _listState.value.indexOf(_listState.value.find { it.item == item })
        ))
    }

    override fun onSearchClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.searchChipClicked(savesTab))
        requireSignedIn {
            listManager.isSearching = true
            listManager.clearFilters()
            _uiState.edit {
                copy(
                    screenState = MyListScreenState.SearchLanding
                )
            }
            _navigationEvents.tryEmit(MyListNavigationEvent.SetSearchFocus)
        }
    }

    override fun onCloseSearchClicked() {
        tracker.track(SavesEvents.searchCloseClicked(savesTab))
        clearAndCloseSearch()
    }

    private fun clearAndCloseSearch() {
        if (delayedSearchText.isNotBlank()) {
            _navigationEvents.tryEmit(MyListNavigationEvent.UpdateSearch(""))
            onSearchTextChanged("", 0, true)
            setDefaultScreenState()
        } else {
            closeSearch()
        }
    }

    private fun closeSearch(): Boolean {
        if (listManager.isSearching) {
            listManager.isSearching = false
            _navigationEvents.tryEmit(MyListNavigationEvent.UpdateSearch(""))
            _navigationEvents.tryEmit(MyListNavigationEvent.CloseKeyboard)
            onSearchTextChanged("", 0, true)
            return true
        }
        return false
    }

    override fun onSearchTextChanged(text: String, delayTime: Long, forceListManagerUpdate: Boolean) {
        delayedSearchText = text
        // slight delay so we don't launch tons of network requests as the user types
        searchDelayJob?.cancel()
        searchDelayJob = viewModelScope.launch {
            delay(delayTime)
            _navigationEvents.tryEmit(MyListNavigationEvent.TrackSearchAnalytics(text))
            if (listManager.sortFilterState.value.search != text || forceListManagerUpdate) {
                listManager.setSearchText(text)
            }
        }
    }

    override fun onSearchDoneClicked() {
        tracker.track(SavesEvents.searchDoneClicked(savesTab))
        if (delayedSearchText.isNotBlank()) {
            searchRepository.addRecentSearch(delayedSearchText)
        }
        onSearchTextChanged(delayedSearchText, 0)
    }

    override fun onRecentSearchClicked(searchText: String) {
        _navigationEvents.tryEmit(MyListNavigationEvent.UpdateSearch(searchText))
        _navigationEvents.tryEmit(MyListNavigationEvent.CloseKeyboard)
        onSearchTextChanged(searchText, 0)
    }

    override fun onListenClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.listenChipClicked(savesTab))
        requireSignedIn {
            _navigationEvents.tryEmit(MyListNavigationEvent.GoToListen)
        }
    }

    override fun onFilterChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.filterChipClicked(savesTab))
        requireSignedIn {
            _navigationEvents.tryEmit(MyListNavigationEvent.ShowFilterBottomSheet)
        }
    }

    /**
     * returns true if consumed
     */
    override fun onBackButtonClicked(): Boolean {
        if (exitEditMode()) {
            return true
        }
        return closeSearch()
    }

    override fun onItemSelectedForBulkEdit(item: Item) {
        if (itemsSelectedForBulkEdit.contains(item)) {
            itemsSelectedForBulkEdit.remove(item)
        } else {
            itemsSelectedForBulkEdit.add(item)
        }
        invalidateBulkEdit()
    }

    override fun onBulkEditSelectAllClicked() {
        itemsSelectedForBulkEdit.clear()
        itemsSelectedForBulkEdit.addAll(listState.value.map { it.item })
        invalidateBulkEdit()
    }

    override fun onBulkReAddClicked() {
        tracker.track(SavesEvents.bulkEditReAddClicked(savesTab))
        itemRepository.unArchive(itemsSelectedForBulkEdit)
        exitEditMode()
    }

    override fun onBulkArchiveClicked() {
        tracker.track(SavesEvents.bulkEditArchiveClicked(savesTab))
        undoable.archive(itemsSelectedForBulkEdit.map { fromDomainItem(it.toDomainItem()) })
        exitEditMode()
    }

    override fun onBulkDeleteClicked() {
        tracker.track(SavesEvents.bulkEditDeleteClicked(savesTab))
        undoable.delete(itemsSelectedForBulkEdit)
        exitEditMode()
    }

    override fun onBulkEditOverflowClicked() {
        tracker.track(SavesEvents.bulkEditOverflowClicked(savesTab))
        _navigationEvents.tryEmit(
            MyListNavigationEvent.ShowBulkEditOverflowBottomSheet(
                itemsSelectedForBulkEdit
            )
        )
    }

    override fun onBulkEditFinished() {
        exitEditMode()
    }

    /**
     * returns true if edit mode was exited
     */
    private fun exitEditMode(): Boolean {
        if (_uiState.value.editChipState.selected) {
            itemsSelectedForBulkEdit.clear()
            _uiState.edit {
                copy(
                    editChipState = ChipState(selected = false)
                )
            }
            invalidateBulkEdit()
            return true
        }
        return false
    }

    override fun onTagBadgeClicked(tag: String) {
        tracker.track(SavesEvents.itemTagButtonClicked(savesTab))
        listManager.setTag(tag)
    }

    override fun onPulledToRefresh() {
        _uiState.edit {
            copy(
                isRefreshing = true
            )
        }
        offlineDownloading.releaseAutoDownload()
        offlineDownloading.allowRetries()
        appSync.sync(
            {
                _uiState.edit {
                    copy(
                        isRefreshing = false
                    )
                }
            },
            { error ->
                _navigationEvents.tryEmit(MyListNavigationEvent.ShowSyncError(error))
                _uiState.edit {
                    copy(
                        isRefreshing = false
                    )
                }
            },
            null
        )
    }

    override fun onShareItemClicked(item: Item) {
        val domainItem = item.toDomainItem()
        tracker.track(SavesEvents.itemShareButtonClicked(savesTab, domainItem.idUrl))
        val showShare = MyListNavigationEvent.ShowShare(domainItem)
        _navigationEvents.tryEmit(showShare)
    }

    override fun onItemOverflowClicked(item: Item) {
        tracker.track(SavesEvents.itemOverflowButtonClicked(savesTab))
        _navigationEvents.tryEmit(MyListNavigationEvent.ShowItemOverflow(item))
    }

    override fun onShortReadFilterClicked() {
        tracker.track(SavesEvents.shortReadsFilterClicked(savesTab))
        onCloseSearchClicked()
        listManager.addFilter(ItemFilterKey.SHORT_READS)
    }

    override fun onLongReadFilterClicked() {
        tracker.track(SavesEvents.longReadsFilterClicked(savesTab))
        clearAndCloseSearch()
        listManager.addFilter(ItemFilterKey.LONG_READS)
    }

    override fun onSelectedFilterChipClicked() {
        if (exitEditMode()) return
        tracker.track(SavesEvents.selectedFilterChipClicked(savesTab))
        showAll()
    }

    override fun onItemSwipedRight(item: Item) {
        if (listManager.sortFilterState.value.listStatus == ListStatus.SAVES) {
            undoable.archive(fromDomainItem(item.toDomainItem()))
        } else {
            itemRepository.unArchive(item)
        }
    }

    override fun onItemSwipedLeft(item: Item) {
        if (listManager.sortFilterState.value.listStatus == ListStatus.SAVES) {
            undoable.archive(fromDomainItem(item.toDomainItem()))
        } else {
            itemRepository.unArchive(item)
        }
    }

    override fun onSaveViewed(itemUrl: String, position: Int) {
        tracker.track(SavesEvents.saveImpression(
            positionInList = position,
            itemUrl = itemUrl,
            savesTab = savesTab,
        ))
    }

    fun onSignedOutEmptyButtonClicked() {
        tracker.track(SavesEvents.emptySignedOutButtonClicked())
        _navigationEvents.tryEmit(MyListNavigationEvent.GoToSignIn)
    }

    private inline fun requireSignedIn(crossinline block: () -> Unit) {
        if (pocketCache.isLoggedIn) {
            block()
        } else {
            _navigationEvents.tryEmit(MyListNavigationEvent.GoToSignIn)
        }
    }
}

data class MyListUiState(
    val screenState: MyListScreenState = MyListScreenState.Loading,
    val myListChipState: ChipState = ChipState(),
    val archiveChipState: ChipState = ChipState(),
    val filterCarouselState: FilterCarouselState = FilterCarouselState(),
    val editChipState: ChipState = ChipState(),
    val selectedTagChipState: ChipState = ChipState(
        selected = true,
        visibility = View.GONE
    ),
    val selectedFilterChipState: ChipState = ChipState(
        selected = true,
        visibility = View.GONE
    ),
    val bulkEditSnackBarText: String = "",
    val bulkEditActionsEnabled: Boolean = false,
    val bulkEditTextClickable: Boolean = true,
    val emptyViewState: EmptyViewState = EmptyViewState.All,
    val isRefreshing: Boolean = false,
    val recentSearchVisibility: Int = View.GONE,
    val searchHint: String = "",
) {
    val allChipState: ChipState get() = ChipState(
        selected = filterCarouselState.selected == FilterCarouselState.Type.Saves &&
                filterCarouselState.savesFilter == SavesFilter.All,
    )
    val taggedChipState: ChipState get() = ChipState(
        selected = filterCarouselState.selected == FilterCarouselState.Type.Saves &&
                filterCarouselState.savesFilter == SavesFilter.Tagged,
    )
    val favoritesChipState: ChipState get() = ChipState(
        selected = filterCarouselState.selected == FilterCarouselState.Type.Saves &&
                filterCarouselState.savesFilter == SavesFilter.Favorites,
    )
    val highlightsChipState: ChipState get() = ChipState(
        selected = filterCarouselState.selected == FilterCarouselState.Type.Saves &&
                filterCarouselState.savesFilter == SavesFilter.Highlighted,
    )
    val notesChipState: ChipState get() = ChipState(
        selected = filterCarouselState.selected == FilterCarouselState.Type.Notes,
        visibility = if (filterCarouselState.notesFilterVisible) View.VISIBLE else View.GONE,
    )
    val filterChipState: ChipState get() = ChipState(
        badgeVisible = filterCarouselState.selected == FilterCarouselState.Type.Saves &&
                filterCarouselState.savesFilter == SavesFilter.FilterMenu,
    )
}

data class MyListScreenState(
    val listVisible: Int = View.GONE,
    val loadingVisible: Int = View.GONE,
    val errorVisible: Int = View.GONE,
    val emptyVisible: Int = View.GONE,
    val searchBarVisible: Int = View.GONE,
    val searchLandingVisible: Int = View.GONE,
    val filterCarouselVisible: Int = View.GONE,
    val notesVisible: Int = View.GONE,
) {

    companion object {
        val Loading = MyListScreenState(
            loadingVisible = View.VISIBLE,
            filterCarouselVisible = View.VISIBLE
        )

        val Error = MyListScreenState(
            errorVisible = View.VISIBLE,
            filterCarouselVisible = View.VISIBLE
        )

        val List = MyListScreenState(
            listVisible = View.VISIBLE,
            filterCarouselVisible = View.VISIBLE
        )

        val Empty = MyListScreenState(
            emptyVisible = View.VISIBLE,
            filterCarouselVisible = View.VISIBLE
        )

        val SearchLanding = MyListScreenState(
            searchBarVisible = View.VISIBLE,
            searchLandingVisible = View.VISIBLE
        )

        val SearchList = MyListScreenState(
            searchBarVisible = View.VISIBLE,
            listVisible = View.VISIBLE
        )

        val SearchLoading = MyListScreenState(
            searchBarVisible = View.VISIBLE,
            loadingVisible = View.VISIBLE,
        )

        val SearchEmpty = MyListScreenState(
            searchBarVisible = View.VISIBLE,
            emptyVisible = View.VISIBLE,
        )

        val Notes = MyListScreenState(
            notesVisible = View.VISIBLE,
            filterCarouselVisible = View.VISIBLE,
        )
    }
}

data class FilterCarouselState(
    val selected: Type = Type.Saves,
    val savesFilter: SavesFilter = SavesFilter.All,
    val notesFilterVisible: Boolean = false,
) {
    enum class Type {
        Saves, Notes,
    }

}
enum class SavesFilter {
    All, Tagged, Favorites, Highlighted, FilterMenu,
}

data class ChipState(
    val selected: Boolean = false,
    val badgeVisible: Boolean = false,
    val enabled: Boolean = true,
    val text: String = "",
    val visibility: Int = View.VISIBLE
)

sealed class EmptyViewState(
    val signedOutVisible: Boolean = false,
    val allVisible: Boolean = false,
    val taggedVisible: Boolean = false,
    val favoriteVisible: Boolean = false,
    val highlightsVisible: Boolean = false,
    val archiveVisible: Boolean = false,
    val specificTagVisible: Boolean = false,
    val searchVisible: Boolean = false,
) {
    data object SignedOut: EmptyViewState(
        signedOutVisible = true
    )
    data object All: EmptyViewState(
        allVisible = true
    )
    data object Tagged: EmptyViewState(
        taggedVisible = true
    )
    data object SpecificTag: EmptyViewState(
        specificTagVisible = true
    )
    data object Favorite: EmptyViewState(
        favoriteVisible = true
    )
    data object Highlights: EmptyViewState(
        highlightsVisible = true
    )
    data object Archive: EmptyViewState(
        archiveVisible = true
    )
    data object Search: EmptyViewState(
        searchVisible = true
    )
}

data class ListItemUiState(
    val item: Item,
    val title: HtmlString = HtmlString(""),
    val domain: HtmlString = HtmlString(""),
    val timeEstimate: String = "",
    val excerpt: HtmlString = HtmlString(""),
    val excerptVisible: Boolean = false,
    val imageUrl: String? = null,
    val thumbnailVisible: Boolean = true,
    val favorite: Boolean = false,
    val badges: List<BadgeState> = emptyList(),
    val titleBold: Boolean = true,
    val isInEditMode: Boolean = false,
    val isSelectedForBulkEdit: Boolean = false,
    val showSearchHighlights: Boolean = false,
    val isInArchive: Boolean = false,
    val index: Int,
)

data class RecentSearchItemUiState(
    val text: String
)

data class BadgeState(
    val type: BadgeType,
    val text: String = "",
)

enum class BadgeType {
    TAG,
    HIGHLIGHT,
    SEARCH_MATCHING_TAG
}

sealed class MyListNavigationEvent {
    data object ShowAddMenu : MyListNavigationEvent()
    data object ShowAddUrlBottomSheet : MyListNavigationEvent()
    data object ShowTagBottomSheet : MyListNavigationEvent()
    data object ShowFilterBottomSheet : MyListNavigationEvent()

    data class ShowBulkEditOverflowBottomSheet(
        val items: List<Item>
    ): MyListNavigationEvent()

    data class ShowSyncError(
        val error: Throwable?
    ): MyListNavigationEvent()

    data object GoToListen: MyListNavigationEvent()
    data object GoToSignIn: MyListNavigationEvent()
    data class GoToReader(
        val item: Item,
        val startingIndex: Int
    ): MyListNavigationEvent()

    data class ShowShare(
        val item: DomainItem,
    ): MyListNavigationEvent()

    data class ShowItemOverflow(
        val item: Item
    ): MyListNavigationEvent()
     object SetSearchFocus: MyListNavigationEvent()
    object CloseKeyboard: MyListNavigationEvent()
    data class UpdateSearch(
        val searchText: String
    ): MyListNavigationEvent()
    data class TrackSearchAnalytics(
        val searchText: String
    ): MyListNavigationEvent()
}

interface MyListInteractions {
    fun onScrolledNearBottom()
    fun onSearchClicked()
    fun onCloseSearchClicked()
    fun onSearchTextChanged(text: String, delayTime: Long = 600, forceListManagerUpdate: Boolean = false)
    fun onSearchDoneClicked()
    fun onRecentSearchClicked(searchText: String)
    fun onListenClicked()
    fun onFilterChipClicked()
    fun onBackButtonClicked(): Boolean
    fun onAddClicked()
    fun onAddUrlClicked()
    fun onAddNoteClicked()
    fun onMyListChipClicked()
    fun onArchiveChipClicked()
    fun onAllChipClicked()
    fun onTaggedChipClicked()
    fun onFavoritesChipClicked()
    fun onHighlightsChipClicked()
    fun onNotesChipClicked()
    fun onEditChipClicked()
    fun onSelectedTagChipClicked()
    fun onSelectedFilterChipClicked()
    fun onFavoriteClicked(item: Item)
    fun onItemClicked(
        item: Item,
        positionInList: Int,
    )
    fun onItemSelectedForBulkEdit(item: Item)
    fun onBulkEditSelectAllClicked()
    fun onBulkReAddClicked()
    fun onBulkArchiveClicked()
    fun onBulkDeleteClicked()
    fun onBulkEditOverflowClicked()
    fun onBulkEditFinished()
    fun onTagBadgeClicked(tag: String)
    fun onPulledToRefresh()
    fun onShareItemClicked(item: Item)
    fun onItemOverflowClicked(item: Item)
    fun onShortReadFilterClicked()
    fun onLongReadFilterClicked()
    fun onItemSwipedRight(item: Item)
    fun onItemSwipedLeft(item: Item)
    fun onSaveViewed(itemUrl: String, position: Int)
}
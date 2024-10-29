package com.pocket.app.reader.internal.collection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.CollectionEvents
import com.pocket.app.reader.queue.UrlListQueueManager
import com.pocket.app.reader.toolbar.ReaderToolbar
import com.pocket.app.reader.toolbar.ReaderToolbarDelegate
import com.pocket.repository.CollectionRepository
import com.pocket.data.models.Collection
import com.pocket.data.models.DomainItem
import com.pocket.repository.ArticleRepository
import com.pocket.repository.ItemRepository
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
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val itemRepository: ItemRepository,
    private val articleRepository: ArticleRepository,
    private val save: Save,
    private val tracker: Tracker,
    private val contentOpenTracker: ContentOpenTracker,
) : ViewModel(),
    CollectionScreen.Initializer,
    CollectionScreen.StoryInteractions,
    CollectionScreen.ErrorInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _storyListUiState = MutableStateFlow(listOf<StoryUiState>())
    val storyListUiState: StateFlow<List<StoryUiState>> = _storyListUiState

    private val _events = MutableSharedFlow<CollectionScreen.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<CollectionScreen.Event> = _events

    private lateinit var collectionUrl: String

    val toolbar = Toolbar()

    override fun onInitialized(url: String) {
        collectionUrl = url
        toolbar.setupToolbar(url)
        fetchCollection()
    }

    private fun fetchCollection() {
        _uiState.edit { copy(
            screenState = ScreenState.Loading
        ) }
        viewModelScope.launch {
            try {
                updateCollection(collectionRepository.getCollection(collectionUrl))
            } catch (e: Exception) {
                _uiState.edit { copy(
                    screenState = ScreenState.Error
                ) }
                Log.e("CollectionViewModel", e.message ?: "")
            }
        }
    }

    private fun updateCollection(collection: Collection) {
        _storyListUiState.edit {
            collection.stories.map { story ->
                StoryUiState(
                    title = story.title,
                    publisher = story.publisher,
                    excerpt = story.excerpt,
                    isSaved = story.isSaved,
                    imageUrl = story.imageUrl,
                    url = story.url,
                    collectionLabelVisible = story.isCollection
                )
            }
        }

        _uiState.edit { copy(
            screenState = ScreenState.Default,
            title = collection.title,
            author = collection.authors.joinToString(separator = ", ") { it.name },
            intro = collection.intro
        ) }
    }

    override fun onSaveClicked(url: String) {
        val story = storyListUiState.value.find { it.url == url }
        if (story != null) {
            if (story.isSaved) {
                itemRepository.delete(url)
            } else {
                tracker.track(CollectionEvents.recommendationSaveClicked(url))
                viewModelScope.launch {
                    when (save(url)) {
                        Save.Result.Success -> {
                            // Nothing to do here.
                        }
                        Save.Result.NotLoggedIn -> _events.emit(CollectionScreen.Event.GoToSignIn)
                    }
                }
            }
            val newList = storyListUiState.value.toMutableList()
            val newStory = story.copy(
                isSaved = !story.isSaved
            )
            val storyIndex = newList.indexOf(story)
            newList.remove(story)
            newList.add(storyIndex, newStory)
            _storyListUiState.edit { newList }
        }
    }

    override fun onCardClicked(url: String) {
        contentOpenTracker.track(CollectionEvents.contentOpen(url))
        val urls = storyListUiState.value.map { it.url }
        _events.tryEmit(CollectionScreen.Event.OpenUrl(
            url,
            UrlListQueueManager(
                urls,
                urls.indexOf(url)
            )
        ))
    }

    override fun onOverflowClicked(url: String, title: String, corpusRecommendationId: String?) {
        tracker.track(CollectionEvents.recommendationOverflowClicked())
        _events.tryEmit(CollectionScreen.Event.ShowOverflowBottomSheet(
            url = url,
            title = title,
            corpusRecommendationId = corpusRecommendationId
        ))
    }

    override fun onRetryClicked() {
        fetchCollection()
    }

    inner class Toolbar : ReaderToolbarDelegate(
        itemRepository,
        articleRepository,
        save,
        viewModelScope,
        tracker,
    ) {

        fun setupToolbar(url: String) {
            this.url = url
            viewModelScope.launch {
                val item: DomainItem? = getDomainItem()

                _toolbarUiState.edit {
                    when {
                        item?.isSaved ?: false -> {
                            CollectionToolbarState(
                                when {
                                    item?.isArchived
                                        ?: false -> ReaderToolbar.ActionButtonState.ReAdd()
                                    else -> ReaderToolbar.ActionButtonState.Archive()
                                }
                            )
                        }
                        else -> UnsavedCollectionToolbarState()
                    }
                }
            }
        }

        override fun onSaveClicked() {
            _toolbarUiState.edit {
                CollectionToolbarState(actionButtonState = ReaderToolbar.ActionButtonState.Archive())
            }
            _events.tryEmit(CollectionScreen.Event.ShowSavedToast)
            super.onSaveClicked()
        }

        override fun onArchiveClicked() {
            _events.tryEmit(CollectionScreen.Event.ShowArchivedToast)
            super.onArchiveClicked()
        }

        override fun onReAddClicked() {
            _events.tryEmit(CollectionScreen.Event.ShowReAddedToast)
            super.onReAddClicked()
        }

        override suspend fun getToolbarOverflow(): ReaderToolbar.ToolbarOverflowUiState {
            return CollectionToolbarOverflow(
                getDomainItem()?.isFavorited ?: false
            )
        }
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val title: String? = null,
        val author: String? = null,
        val intro: String? = null,
    )

    data class StoryUiState(
        val title: String,
        val publisher: String,
        val excerpt: String,
        val isSaved: Boolean,
        val imageUrl: String,
        val url: String,
        val collectionLabelVisible: Boolean,
    )

    sealed class ScreenState(
        val loadingVisible: Boolean = false,
        val mainLayoutVisible: Boolean = false,
        val errorVisible: Boolean = false,
    ) {
        object Loading : ScreenState(
            loadingVisible = true
        )
        object Error : ScreenState(
            errorVisible = true
        )
        object Default : ScreenState(
            mainLayoutVisible = true
        )
    }

    private fun UnsavedCollectionToolbarState() = ReaderToolbar.ToolbarUiState(
        upVisible = true,
        actionButtonState = ReaderToolbar.ActionButtonState.Save(),
        shareVisible = true,
    )

    private fun CollectionToolbarState(
        actionButtonState: ReaderToolbar.ActionButtonState
    ) = ReaderToolbar.ToolbarUiState(
        upVisible = true,
        actionButtonState = actionButtonState,
        shareVisible = true,
        overflowVisible = true,
    )

    private fun CollectionToolbarOverflow(
        isFavorited: Boolean,
    ) = ReaderToolbar.ToolbarOverflowUiState(
        favoriteVisible = !isFavorited,
        unfavoriteVisible = isFavorited,
        addTagsVisible = true,
        deleteVisible = true,
        markAsNotViewedVisible = true,
    )
}
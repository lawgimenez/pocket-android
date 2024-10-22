package com.pocket.app.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ReaderEvents
import com.pocket.app.list.list.ListManager
import com.pocket.app.reader.queue.*
import com.pocket.repository.ItemRepository
import com.pocket.util.Stack
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val reader: Reader,
    private val listManager: ListManager,
    private val tracker: Tracker,
    private val destinationHelper: DestinationHelper,
) : ViewModel(),
    Reader.PreviousNextInteractions,
    Reader.Initializer,
    Reader.NavigationInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _navigationEvents = MutableSharedFlow<Reader.NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<Reader.NavigationEvent> = _navigationEvents

    /**
     * A stack of queue managers for the previous and next feature within the reader.
     * A stack is required because the user can enter a collection queue from within another queue.
     */
    private val queueManagerStack = Stack<QueueManager>()

    val hasNext: Boolean
        get() = queueManagerStack.peek()?.hasNext() ?: false
    val hasPrevious: Boolean
        get() = queueManagerStack.peek()?.hasPrevious() ?: false

    override fun onInitialized(
        url: String,
        initialQueueType: InitialQueueType,
        queueStartingIndex: Int,
    ) {
        openUrl(
            url = url,
            queueManager = when (initialQueueType) {
                InitialQueueType.SavesList -> {
                    SavesListQueueManager(
                        listManager,
                        queueStartingIndex,
                    )
                }
                InitialQueueType.Empty -> {
                    EmptyQueueManager()
                }
            }
        )
    }

    override fun onPreviousClicked() {
        queueManagerStack.peek()?.getPreviousUrl()?.let { url ->
            tracker.track(ReaderEvents.previousClicked(url))
            openUrl(url)
        }
    }

    override fun onNextClicked() {
        queueManagerStack.peek()?.getNextUrl()?.let { url ->
            tracker.track(ReaderEvents.nextClicked(url))
            openUrl(url)
        }
    }

    override fun onBackstackPopped() {
        queueManagerStack.pop()
        updatePreviousNextState()
    }

    private fun updatePreviousNextState() {
        _uiState.edit { copy(
            previousVisible = queueManagerStack.peek()?.hasPrevious() ?: false,
            nextVisible = queueManagerStack.peek()?.hasNext() ?: false,
            previousAndNextBarVisible = queueManagerStack.peek() != null
                    && (queueManagerStack.peek()?.hasPrevious() ?: false
                        || queueManagerStack.peek()?.hasNext() ?: false)
                    && reader.isPreviousAndNextOn
        ) }
    }

    /**
     * @param url the url to open
     * @param queueManager the queue manager to add to the queue manager stack.  If this is not null,
     * the current page will be kept on the backstack
     * @param forceOpenInWebView if we want to open the article in web view no matter what
     */
    fun openUrl(
        url: String,
        queueManager: QueueManager? = null,
        forceOpenInWebView: Boolean = false,
    ) {
        viewModelScope.launch {
            val destination = destinationHelper.getDestination(
                url = url,
                forceOpenInWebView = forceOpenInWebView,
            )

            when (destination) {
                Destination.ARTICLE ->
                    _navigationEvents.tryEmit(Reader.NavigationEvent.GoToArticle(url, queueManager != null))
                Destination.ORIGINAL_WEB ->
                    _navigationEvents.tryEmit(Reader.NavigationEvent.GoToOriginalWeb(url, queueManager != null))
                Destination.COLLECTION ->
                    _navigationEvents.tryEmit(Reader.NavigationEvent.GoToCollection(url, queueManager != null))
                else -> {}
            }
            itemRepository.markAsViewed(url)
            queueManager?.let { queueManagerStack.push(it) }
            updatePreviousNextState()
        }
    }

    data class UiState(
        val previousAndNextBarVisible: Boolean = false,
        val previousVisible: Boolean = true,
        val nextVisible: Boolean = true,
    )
}
package com.pocket.app.reader.internal.article.highlights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ArticleViewEvents
import com.pocket.repository.HighlightRepository
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HighlightsBottomSheetViewModel @Inject constructor(
    private val highlightRepository: HighlightRepository,
    private val tracker: Tracker,
) : ViewModel(),
    HighlightsBottomSheet.Initializer,
    HighlightsBottomSheet.HighlightInteractions {

    private val _highlights = MutableStateFlow<List<HighlightUiState>>(emptyList())
    val highlights: StateFlow<List<HighlightUiState>> = _highlights

    private val _events = MutableSharedFlow<HighlightsBottomSheet.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<HighlightsBottomSheet.Event> = _events

    lateinit var url: String

    override fun onInitialized(url: String) {
        this.url = url
        setupHighlightsObserver()
    }

    @Suppress("MagicNumber")
    private fun setupHighlightsObserver() {
        viewModelScope.launch {
            highlightRepository.getHighlightsFlow(url).collect { highlights ->
                _highlights.edit {
                    highlights.sortedBy {
                        // Sort the highlights by the location in the article.
                        // We can tell it's location by looking at the patch
                        // patches look like this "@@ -857,16 +857,36 @@"
                        it.patch.substring(4, it.patch.indexOf(',')).toIntOrNull()
                    }.map {
                        HighlightUiState(
                            id = it.id,
                            text = it.quote,
                        )
                    }
                }
            }
        }
    }

    override fun onHighlightClicked(id: String) {
        _events.tryEmit(HighlightsBottomSheet.Event.Dismiss(id))
    }

    override fun onShareClicked(text: String) {
        tracker.track(ArticleViewEvents.highlightShareClicked(url))
        _events.tryEmit(HighlightsBottomSheet.Event.ShowShare(text))
    }

    override fun onDeleteClicked(id: String) {
        viewModelScope.launch {
            val numberOfHighlights = highlights.value.size
            highlightRepository.deleteHighlight(id, url)
            _events.tryEmit(HighlightsBottomSheet.Event.RemoveHighlightFromWebView)
            // if this was the last highlight, dismiss the bottom sheet
            if (numberOfHighlights <= 1) {
                _events.tryEmit(HighlightsBottomSheet.Event.Dismiss())
            }
        }
    }

    data class HighlightUiState(
        val id: String,
        val text: String,
    )
}
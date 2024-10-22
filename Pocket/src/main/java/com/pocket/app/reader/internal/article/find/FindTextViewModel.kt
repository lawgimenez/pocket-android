package com.pocket.app.reader.internal.article.find

import androidx.lifecycle.ViewModel
import com.pocket.app.reader.internal.article.ArticleScreen
import com.pocket.app.reader.internal.article.javascript.JavascriptFunctions
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FindTextViewModel @Inject constructor(

) : ViewModel(),
    ArticleScreen.FindTextToolbarInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<ArticleScreen.Event>(extraBufferCapacity = 20)
    val events: SharedFlow<ArticleScreen.Event> = _events

    private var searchText = ""
    private var currentInstance: Int = 0
    private var count: Int = 0

    override fun onShow() {
        _uiState.edit { copy(
            visible = true
        ) }
    }

    override fun onCloseClicked() {
        _uiState.edit { copy(
            visible = false
        ) }
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.clearSearchText()
        ))
    }

    override fun onNextClicked() {
        currentInstance = when {
            count == 0 -> 0
            currentInstance == count -> 1
            else -> currentInstance + 1
        }
        updateCountString()
        scrollToText()
    }

    override fun onPreviousClicked() {
        currentInstance = when {
            count == 0 -> 0
            currentInstance == 1 -> count
            else -> currentInstance - 1
        }
        updateCountString()
        scrollToText()
    }

    override fun onTextChanged(text: String) {
        searchText = text
        if (text.isBlank()) {
            _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                JavascriptFunctions.clearSearchText()
            ))
            currentInstance = 0
            count = 0
            updateCountString()
        } else {
            _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                JavascriptFunctions.searchForText(searchText)
            ))
        }
    }

    override fun onTextHighlighted(count: Int) {
        this.count = count
        currentInstance = if (count > 0) {
            1
        } else {
            0
        }
        updateCountString()
        scrollToText()
    }

    private fun updateCountString() {
        _uiState.edit { copy(
            countText = "$currentInstance/$count"
        ) }
    }

    private fun scrollToText() {
        if (currentInstance != 0) {
            _events.tryEmit(
                ArticleScreen.Event.ExecuteJavascript(
                    JavascriptFunctions.scrollToSearchText(searchText, currentInstance)
                )
            )
        }
    }

    data class UiState(
        val countText: String = "0/0",
        val visible: Boolean = false,
    )
}
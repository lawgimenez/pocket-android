package com.pocket.app.reader.internal.article.highlights

object HighlightsBottomSheet {

    interface Initializer {
        fun onInitialized(url: String)
    }

    interface HighlightInteractions {
        fun onHighlightClicked(id: String)
        fun onShareClicked(text: String)
        fun onDeleteClicked(id: String)
    }

    sealed class Event {
        data class ShowShare(
            val text: String
        ) : Event()
        data class Dismiss(
            val scrollToId: String? = null
        ) : Event()
        object RemoveHighlightFromWebView : Event()
    }
}
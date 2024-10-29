package com.pocket.app.reader.internal.collection

import com.pocket.app.reader.queue.QueueManager

class CollectionScreen {

    interface Initializer {
        fun onInitialized(url: String)
    }

    interface StoryInteractions {
        fun onSaveClicked(url: String)
        fun onCardClicked(url: String)
        fun onOverflowClicked(url: String, title: String, corpusRecommendationId: String?)
    }

    interface ErrorInteractions {
        fun onRetryClicked()
    }

    sealed class Event {
        data class ShowOverflowBottomSheet(
            val url: String,
            val title: String,
            val corpusRecommendationId: String?,
        ) : Event()
        data class OpenUrl(
            val url: String,
            val queueManager: QueueManager,
        ) : Event()
        data object ShowSavedToast : Event()
        data object ShowArchivedToast : Event()
        data object ShowReAddedToast : Event()
        data object GoToSignIn : Event()
    }
}
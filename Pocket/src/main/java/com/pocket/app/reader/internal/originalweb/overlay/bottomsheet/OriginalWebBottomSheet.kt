package com.pocket.app.reader.internal.originalweb.overlay.bottomsheet

import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.tts.Track

object OriginalWebBottomSheet {

    interface Initializer {
        fun onInitialized(url: String)
    }

    interface ButtonInteractions {
        fun onMainActionClicked()
        fun onListenClicked()
        fun onShareClicked()
        fun onSwitchToArticleViewClicked()
        fun onFavoriteClicked()
        fun onAddTagsClicked()
        fun onMarkAsViewedClicked()
        fun onDeleteClicked()
    }

    sealed class Event {
        data object GoBack : Event()
        data object GoToSignIn : Event()
        data class OpenListen(
            val track: Track,
        ) : Event()
        data object SwitchToArticleView : Event()
        data class OpenTagScreen(
            val item: Item,
        ) : Event()
        data class ShowShare(
            val title: String
        ) : Event()
        data object ShowSavedToast : Event()
        data object ShowArchivedToast : Event()
        data object ShowReAddedToast : Event()
    }
}
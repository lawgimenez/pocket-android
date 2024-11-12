package com.pocket.app.reader.internal.article.textsettings.fontSettings

object FontSettings {

    interface Initializer {
        fun onInitialized()
    }

    interface ListInteractions {
        fun onFontSelected(fontId: Int)
    }

    interface ClickListener {
        fun onUpClicked()
    }

    sealed class Event {
        object ReturnToTextSettings : Event()
        object GoToPremium : Event()
    }
}
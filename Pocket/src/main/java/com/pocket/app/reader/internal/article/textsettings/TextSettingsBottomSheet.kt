package com.pocket.app.reader.internal.article.textsettings

object TextSettingsBottomSheet {

    interface Initializer {
        fun onInitialized()
    }

    interface Interactions {
        fun onFontSizeUpClicked()
        fun onFontSizeDownClicked()
        fun onLineHeightUpClicked()
        fun onLineHeightDownClicked()
        fun onMarginUpClicked()
        fun onMarginDownClicked()
        fun onPremiumUpgradeClicked()
        fun onFontChangeClicked()
        fun onLightThemeClicked()
        fun onDarkThemeClicked()
        fun onSystemThemeClicked()
        fun onBrightnessChanged(value: Int)
    }

    sealed class Event {
        object ShowPremiumScreen : Event()
        object ShowFontChangeBottomSheet : Event()
    }
}
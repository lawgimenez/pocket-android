package com.pocket.app.reader.internal.article.textsettings

import androidx.databinding.BindingAdapter
import com.pocket.ui.view.menu.DisplaySettingsView

@BindingAdapter("premiumUpsellVisible")
fun setPremiumUpsellVisible(view: DisplaySettingsView, premiumUpsellVisible: Boolean) {
    view.bind().premiumUpsellVisible(premiumUpsellVisible)
}

@BindingAdapter("premiumSettingsVisible")
fun setPremiumSettingsVisible(view: DisplaySettingsView, visible: Boolean) {
    view.bind().premiumSettingsVisible(visible)
}

@BindingAdapter("fontSizeUpEnabled")
fun setFontSizeUpEnabled(view: DisplaySettingsView, enabled: Boolean) {
    view.bind().fontSizeUpEnabled(enabled)
}

@BindingAdapter("fontSizeDownEnabled")
fun setFontSizeDownEnabled(view: DisplaySettingsView, enabled: Boolean) {
    view.bind().fontSizeDownEnabled(enabled)
}

@BindingAdapter("lineHeightUpEnabled")
fun setLineHeightUpEnabled(view: DisplaySettingsView, enabled: Boolean) {
    view.bind().lineHeightUpEnabled(enabled)
}

@BindingAdapter("lineHeightDownEnabled")
fun setLineHeightDownEnabled(view: DisplaySettingsView, enabled: Boolean) {
    view.bind().lineHeightDownEnabled(enabled)
}

@BindingAdapter("marginUpEnabled")
fun setMarginUpEnabled(view: DisplaySettingsView, enabled: Boolean) {
    view.bind().marginUpEnabled(enabled)
}

@BindingAdapter("marginDownEnabled")
fun setMarginDownEnabled(view: DisplaySettingsView, enabled: Boolean) {
    view.bind().marginDownEnabled(enabled)
}

@BindingAdapter("fontChangeText")
fun setFontChangeText(view: DisplaySettingsView, value: Int) {
    view.bind().fontChangeText(value)
}

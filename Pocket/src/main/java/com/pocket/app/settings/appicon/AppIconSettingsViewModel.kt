package com.pocket.app.settings.appicon

import androidx.lifecycle.ViewModel
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SettingsEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppIconSettingsViewModel
@Inject constructor(
    private val appIcons: AppIcons,
    private val tracker: Tracker,
) : ViewModel() {
    fun onViewShown() {
        tracker.track(SettingsEvents.appIconSwitcherImpression())
    }

    private val currentIcon = appIcons.current
    val automaticIcon = appIcons.automatic.toAppIconUiState()
    val allIcons = appIcons.all.map { it.toAppIconUiState() }

    fun onAutomaticIconClick() {
        tracker.track(SettingsEvents.appIconChanged(appIcons.automatic.description))
        appIcons.current = appIcons.automatic
    }

    fun onIconClick(index: Int) {
        val appIcon = appIcons.all[index]
        tracker.track(SettingsEvents.appIconChanged(appIcon.description))
        appIcons.current = appIcon
    }

    private fun AppIconOption.toAppIconUiState() = AppIconUiState(
        appIcons.loadIcon(this),
        label,
        currentIcon == this,
    )
}

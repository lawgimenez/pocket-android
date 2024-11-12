package com.pocket.app.reader.internal.article.textsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.app.reader.internal.article.DisplaySettingsManager
import com.pocket.app.settings.Theme
import com.pocket.repository.UserRepository
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TextSettingsBottomSheetViewModel @Inject constructor(
    private val displaySettingsManager: DisplaySettingsManager,
    private val userRepository: UserRepository,
) : ViewModel(),
    TextSettingsBottomSheet.Initializer,
    TextSettingsBottomSheet.Interactions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<TextSettingsBottomSheet.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<TextSettingsBottomSheet.Event> = _events

    override fun onInitialized() {
        _uiState.edit { copy(
            fontChangeText = displaySettingsManager.currentFont.displayName
        ) }
        viewModelScope.launch {
            launch {
                userRepository.hasPremiumDisplaySettings().collect { hasPremiumDisplaySettings ->
                    _uiState.update { it.copy(premiumSettingsVisible = hasPremiumDisplaySettings) }
                }
            }
            launch {
                userRepository.isPremiumUpgradeAvailable().collect { premiumUpgradeAvailable ->
                    _uiState.update { it.copy(premiumUpsellVisible = premiumUpgradeAvailable) }
                }
            }
        }
        recheckLimits()
    }

    override fun onFontSizeUpClicked() {
        displaySettingsManager.incrementFontSize()
        recheckLimits()
    }

    override fun onFontSizeDownClicked() {
        displaySettingsManager.decrementFontSize()
        recheckLimits()
    }

    override fun onLineHeightUpClicked() {
        displaySettingsManager.incrementLineHeight()
        recheckLimits()
    }

    override fun onLineHeightDownClicked() {
        displaySettingsManager.decrementLineHeight()
        recheckLimits()
    }

    override fun onMarginUpClicked() {
        displaySettingsManager.incrementMargin()
        recheckLimits()
    }

    override fun onMarginDownClicked() {
        displaySettingsManager.decrementMargin()
        recheckLimits()
    }

    override fun onPremiumUpgradeClicked() {
        _events.tryEmit(TextSettingsBottomSheet.Event.ShowPremiumScreen)
    }

    override fun onFontChangeClicked() {
        _events.tryEmit(TextSettingsBottomSheet.Event.ShowFontChangeBottomSheet)
    }

    override fun onLightThemeClicked() {
        displaySettingsManager.setTheme(
            null,
            Theme.LIGHT
        )
    }

    override fun onDarkThemeClicked() {
        displaySettingsManager.setTheme(
            null,
            Theme.DARK
        )
    }

    override fun onSystemThemeClicked() {
        displaySettingsManager.setSystemDarkThemeOn(null)
    }

    @Suppress("MagicNumber")
    override fun onBrightnessChanged(value: Int) {
        displaySettingsManager.setBrightness(value / 100.0f)
    }

    private fun recheckLimits() {
        _uiState.edit { copy(
            fontSizeUpEnabled = !displaySettingsManager.isFontSizeAtMax,
            fontSizeDownEnabled = !displaySettingsManager.isFontSizeAtMin,
            lineHeightUpEnabled = !displaySettingsManager.isLineHeightAtMax,
            lineHeightDownEnabled = !displaySettingsManager.isLineHeightAtMin,
            marginUpEnabled = !displaySettingsManager.isMarginAtMax,
            marginDownEnabled = !displaySettingsManager.isMarginAtMin,
        ) }
    }

    data class UiState(
        val fontSizeUpEnabled: Boolean = true,
        val fontSizeDownEnabled: Boolean = true,
        val lineHeightUpEnabled: Boolean = true,
        val lineHeightDownEnabled: Boolean = true,
        val marginUpEnabled: Boolean = true,
        val marginDownEnabled: Boolean = true,
        val premiumSettingsVisible: Boolean = false,
        val premiumUpsellVisible: Boolean = false,
        val fontChangeText: Int = 0
    )
}

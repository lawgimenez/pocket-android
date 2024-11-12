package com.pocket.app.reader.internal.article.textsettings.fontSettings

import androidx.lifecycle.ViewModel
import com.pocket.app.premium.PremiumReader
import com.pocket.app.reader.internal.article.DisplaySettingsManager
import com.pocket.util.StringLoader
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FontSettingsBottomSheetViewModel @Inject constructor(
    private val premiumReader: PremiumReader,
    private val displaySettingsManager: DisplaySettingsManager,
    private val stringLoader: StringLoader,
) : ViewModel(),
    FontSettings.Initializer,
    FontSettings.ListInteractions,
    FontSettings.ClickListener {

    private val _fontChoiceUiState = MutableStateFlow<List<FontChoiceUiState>>(emptyList())
    val fontChoiceUiState: StateFlow<List<FontChoiceUiState>> = _fontChoiceUiState

    private val _events = MutableSharedFlow<FontSettings.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<FontSettings.Event> = _events

    override fun onInitialized() {
        refreshList()
    }

    private fun refreshList() {
        _fontChoiceUiState.edit {
            DisplaySettingsManager.FontOption.values().map { fontOption ->
                FontChoiceUiState(
                    fontName = stringLoader.getString(fontOption.displayName),
                    premiumIconVisible = fontOption.isPremium,
                    upgradeVisible = fontOption.isPremium && !premiumReader.isEnabled,
                    isSelected = displaySettingsManager.currentFont == fontOption,
                    fontId = fontOption.id
                )
            }
        }
    }

    override fun onFontSelected(fontId: Int) {
        val fontOption = DisplaySettingsManager.FontOption.values().find { it.id == fontId }!!
        if (fontOption.isPremium && !premiumReader.isEnabled) {
            _events.tryEmit(FontSettings.Event.GoToPremium)
        } else {
            displaySettingsManager.setFont(fontId)
            refreshList()
        }
    }

    override fun onUpClicked() {
        _events.tryEmit(FontSettings.Event.ReturnToTextSettings)
    }

    data class FontChoiceUiState(
        val fontName: String,
        val premiumIconVisible: Boolean,
        val upgradeVisible: Boolean,
        val isSelected: Boolean,
        val fontId: Int,
    )
}
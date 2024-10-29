package com.pocket.app.home.slates.overflow.report

import androidx.lifecycle.ViewModel
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ReportBottomSheetEvents
import com.pocket.analytics.entities.ReportEntity
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ReportItemBottomSheetViewModel @Inject constructor(
    private val tracker: Tracker,
): ViewModel(), ReportItemInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    private lateinit var url: String
    private var corpusRecommendationId: String? = null

    private var otherText: String = ""

    override fun onInitialized(url: String, corpusRecommendationId: String?) {
        this.url = url
        this.corpusRecommendationId = corpusRecommendationId
    }

    override fun onBrokenClicked() {
        _uiState.edit { copy(
            submitButtonEnabled = true,
            reportReason = ReportReason.Broken()
        ) }
        _events.tryEmit(Event.HideKeyboard)
    }

    override fun onWrongCategoryClicked() {
        _uiState.edit { copy(
            submitButtonEnabled = true,
            reportReason = ReportReason.WrongCategory()
        ) }
        _events.tryEmit(Event.HideKeyboard)
    }

    override fun onSexuallyExplicitClicked() {
        _uiState.edit { copy(
            submitButtonEnabled = true,
            reportReason = ReportReason.SexuallyExplicit()
        ) }
        _events.tryEmit(Event.HideKeyboard)
    }

    override fun onOffensiveClicked() {
        _uiState.edit { copy(
            submitButtonEnabled = true,
            reportReason = ReportReason.Offensive()
        ) }
        _events.tryEmit(Event.HideKeyboard)
    }

    override fun onMisinformationClicked() {
        _uiState.edit { copy(
            submitButtonEnabled = true,
            reportReason = ReportReason.Misinformation()
        ) }
        _events.tryEmit(Event.HideKeyboard)
    }

    override fun onOtherClicked() {
        _uiState.edit { copy(
            submitButtonEnabled = true,
            reportReason = ReportReason.Other()
        ) }
    }

    override fun onOtherTextChanged(text: String) {
        otherText = text
    }

    override fun onSubmitClicked() {
        tracker.track(ReportBottomSheetEvents.reportClicked(
            url,
            uiState.value.reportReason.value,
            if (uiState.value.reportReason is ReportReason.Other) {
                otherText
            } else {
                null
            },
            corpusRecommendationId,
        ))
        _events.tryEmit(Event.ShowToastAndClose)
    }

    data class UiState(
        val submitButtonEnabled: Boolean = false,
        val reportReason: ReportReason = ReportReason.None,
    )

    sealed class ReportReason(
        val brokenSelected: Boolean = false,
        val wrongCategorySelected: Boolean = false,
        val sexuallyExplicitSelected: Boolean = false,
        val offensiveSelected: Boolean = false,
        val misinformationSelected: Boolean = false,
        val otherSelected: Boolean = false,
        val otherTextBoxVisible: Boolean = false,
        val value: ReportEntity.Reason = ReportEntity.Reason.BROKEN_META,
    ) {
        object None: ReportReason()

        class Broken: ReportReason(
            brokenSelected = true,
            value = ReportEntity.Reason.BROKEN_META,
        )

        class WrongCategory: ReportReason(
            wrongCategorySelected = true,
            value = ReportEntity.Reason.WRONG_CATEGORY,
        )

        class SexuallyExplicit: ReportReason(
            sexuallyExplicitSelected = true,
            value = ReportEntity.Reason.SEXUALLY_EXPLICIT,
        )

        class Offensive: ReportReason(
            offensiveSelected = true,
            value = ReportEntity.Reason.OFFENSIVE,
        )

        class Misinformation: ReportReason(
            misinformationSelected = true,
            value = ReportEntity.Reason.MISINFORMATION,
        )

        class Other: ReportReason(
            otherSelected = true,
            otherTextBoxVisible = true,
            value = ReportEntity.Reason.OTHER
        )
    }

    sealed class Event {
        object ShowToastAndClose : Event()
        object HideKeyboard: Event()
    }
}

interface ReportItemInteractions {
    fun onInitialized(url: String, corpusRecommendationId: String?)
    fun onBrokenClicked()
    fun onWrongCategoryClicked()
    fun onSexuallyExplicitClicked()
    fun onOffensiveClicked()
    fun onMisinformationClicked()
    fun onOtherClicked()
    fun onOtherTextChanged(text: String)
    fun onSubmitClicked()
}
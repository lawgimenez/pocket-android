package com.pocket.app.home.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.repository.ItemRepository
import com.pocket.usecase.Save
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Abstract view model with common code shared between
 * [com.pocket.app.home.details.slates.SlateDetailsViewModel] and
 * [com.pocket.app.home.details.topics.TopicDetailsViewModel]
 */
abstract class DetailsViewModel(
    private val itemRepository: ItemRepository,
    private val save: Save,
    private val tracker: Tracker,
) : ViewModel(), DetailsInteractions {

    protected val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    protected val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    override fun onSaveClicked(url: String, isSaved: Boolean, corpusRecommendationId: String?) {
        if (isSaved) {
            itemRepository.delete(url)
        } else {
            tracker.track(HomeEvents.recommendationSaveClicked(
                url = url,
                corpusRecommendationId = corpusRecommendationId
            ))
            viewModelScope.launch {
                when (save(url)) {
                    Save.Result.Success -> {
                        // Nothing to do here.
                    }
                    Save.Result.NotLoggedIn -> _events.emit(Event.GoToSignIn)
                }
            }
        }
    }

    override fun onOverflowClicked(url: String, title: String, corpusRecommendationId: String?) {
        tracker.track(HomeEvents.recommendationOverflowClicked(
            corpusRecommendationId = corpusRecommendationId,
            url = url
        ))
        _events.tryEmit(Event.ShowRecommendationOverflow(
            url = url,
            title = title,
            corpusRecommendationId = corpusRecommendationId,
        ))
    }

    abstract override fun onErrorRetryClicked()

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val title: String = "",
        val recommendations: List<RecommendationUiState> = emptyList(),
        val errorSnackBarVisible: Boolean = false,
        val errorSnackBarRefreshing: Boolean = false,
        val errorMessage: String = "",
    )

    sealed class ScreenState(
        val loadingVisible: Boolean = false,
        val recommendationsVisible: Boolean = false,
    ) {
        object Loading : ScreenState(
            loadingVisible = true,
            recommendationsVisible = false,
        )

        object Recommendations : ScreenState(
            loadingVisible = false,
            recommendationsVisible = true,
        )
    }

    sealed class Event {
        data class GoToReader(
            val url: String,
        ) : Event()
        data object GoToSignIn: Event()
        data class ShowRecommendationOverflow(
            val url: String,
            val title: String,
            val corpusRecommendationId: String?,
        ) : Event()
    }
}

interface DetailsInteractions {
    fun onSaveClicked(url: String, isSaved: Boolean, corpusRecommendationId: String?)
    fun onItemClicked(
        url: String,
        positionInList: Int,
        corpusRecommendationId: String?,
    )

    fun onOverflowClicked(url: String, title: String, corpusRecommendationId: String?)
    fun onErrorRetryClicked()
    fun onItemViewed(positionInList: Int, url: String, corpusRecommendationId: String?)
}
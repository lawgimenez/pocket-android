package com.pocket.app.home.details.slates

import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.app.home.details.DetailsViewModel
import com.pocket.app.home.details.toRecommendationUiState
import com.pocket.repository.HomeRepository
import com.pocket.repository.ItemRepository
import com.pocket.usecase.Save
import com.pocket.util.StringLoader
import com.pocket.util.edit
import com.pocket.util.java.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlateDetailsViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    locale: Locale,
    private val tracker: Tracker,
    private val stringLoader: StringLoader,
    itemRepository: ItemRepository,
    save: Save,
    private val contentOpenTracker: ContentOpenTracker,
) : DetailsViewModel(
    itemRepository = itemRepository,
    save = save,
    tracker = tracker,
), SlateDetailsInteractions {

    private val localeString = locale.toString()

    override fun onInitialized(slateId: String) {
        setupSlateCollector(slateId)
        _uiState.edit {
            copy(
                screenState = ScreenState.Recommendations
            )
        }
    }

    private fun setupSlateCollector(slateId: String) {
        viewModelScope.launch {
            homeRepository.getLineup(localeString).collect { lineup ->
                val slate = lineup.find { it.id == slateId }
                _uiState.edit {
                    copy(
                        title = slate?.title ?: "",
                        recommendations = slate?.recommendations?.map { recommendation ->
                            recommendation.toRecommendationUiState(stringLoader = stringLoader)
                        } ?: emptyList()
                    )
                }
            }
        }
    }

    override fun onErrorRetryClicked() {
        // we don't load data from network for slates, so error state is not possible
    }

    override fun onItemClicked(url: String, positionInList: Int, corpusRecommendationId: String?) {
        contentOpenTracker.track(
            HomeEvents.slateDetailsArticleContentOpen(
                slateTitle = uiState.value.title,
                positionInSlate = positionInList,
                itemUrl = url,
                corpusRecommendationId = corpusRecommendationId,
            )
        )
        _events.tryEmit(Event.GoToReader(url))
    }

    override fun onItemViewed(positionInList: Int, url: String, corpusRecommendationId: String?) {
        tracker.track(
            HomeEvents.slateDetailsArticleImpression(
                slateTitle = uiState.value.title,
                positionInSlate = positionInList,
                itemUrl = url,
                corpusRecommendationId = corpusRecommendationId,
            )
        )
    }
}

interface SlateDetailsInteractions {
    fun onInitialized(slateId: String)
}
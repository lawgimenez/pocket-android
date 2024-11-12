package com.pocket.app.home.details.topics

import androidx.lifecycle.viewModelScope
import com.ideashower.readitlater.R
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.app.home.details.DetailsViewModel
import com.pocket.app.home.details.toRecommendationUiState
import com.pocket.repository.ItemRepository
import com.pocket.repository.TopicsRepository
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.usecase.Save
import com.pocket.util.StringLoader
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicDetailsViewModel @Inject constructor(
    private val topicsRepository: TopicsRepository,
    private val stringLoader: StringLoader,
    private val tracker: Tracker,
    itemRepository: ItemRepository,
    save: Save,
    private val contentOpenTracker: ContentOpenTracker,
) : DetailsViewModel(
    itemRepository = itemRepository,
    save = save,
    tracker = tracker,
), TopicDetailsInteractions {

    private lateinit var topicId: String

    override fun onInitialized(topicId: String) {
        this.topicId = topicId
        _uiState.edit { copy(
            errorMessage = stringLoader.getString(R.string.home_topic_error_message)
        ) }
        setupTopicCollector()
        refreshTopic()
    }

    private fun refreshTopic() {
        viewModelScope.launch {
            try {
                topicsRepository.refreshTopic(topicId)
                _uiState.edit { copy(
                    screenState = ScreenState.Recommendations,
                    errorSnackBarVisible = false,
                    errorSnackBarRefreshing = false,
                ) }
            } catch (e: Exception) {
                _uiState.edit { copy(
                    errorSnackBarVisible = true,
                    errorSnackBarRefreshing = false,
                ) }
                println("error ${e.message}")
            }
        }
    }

    private fun setupTopicCollector() {
        viewModelScope.launch {
            // find the display title based on the topic ID
            val title = topicsRepository.getTopicsLocal().topics?.find { it.topic == topicId }?.display_name ?: ""
            _uiState.edit { copy(
                title = title
            ) }
            // collect the items
            topicsRepository.getTopicAsFlow(topicId).collect { topicFeed ->
                val recommendationItems = buildList<Item> {
                    topicFeed.curated?.mapNotNull { it.item }?.let { curatedList ->
                        addAll(curatedList)
                    }
                    topicFeed.algorithmic?.mapNotNull { it.item }?.let { algorithmicList ->
                        addAll(algorithmicList)
                    }
                }
                _uiState.edit { copy(
                    recommendations = recommendationItems.mapIndexed { index, item ->
                        item.toRecommendationUiState(stringLoader = stringLoader, index = index)
                    }
                ) }
            }
        }
    }

    override fun onErrorRetryClicked() {
        _uiState.edit { copy(
            errorSnackBarRefreshing = true,
        ) }
        refreshTopic()
    }

    override fun onItemClicked(
        url: String,
        positionInList: Int,
        corpusRecommendationId: String?,
    ) {
        contentOpenTracker.track(
            HomeEvents.topicArticleContentOpen(
                topicTitle = uiState.value.title,
                positionInTopic = positionInList,
                itemUrl = url,
            )
        )
        _events.tryEmit(Event.GoToReader(url = url))
    }

    override fun onItemViewed(positionInList: Int, url: String, corpusRecommendationId: String?) {
        tracker.track(HomeEvents.topicArticleImpression(
            topicTitle = uiState.value.title,
            positionInTopic = positionInList,
            itemUrl = url,
        ))
    }
}

interface TopicDetailsInteractions {
    fun onInitialized(topicId: String)
}
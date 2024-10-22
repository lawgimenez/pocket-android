package com.pocket.app.reader.internal.article.recommendations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ArticleViewEvents
import com.pocket.app.reader.internal.article.ArticleScreen
import com.pocket.app.reader.queue.UrlListQueueManager
import com.pocket.repository.ItemRepository
import com.pocket.repository.RecommendationsRepository
import com.pocket.usecase.Save
import com.pocket.util.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EndOfArticleRecommendationsViewModel @Inject constructor(
    private val recommendationsRepository: RecommendationsRepository,
    private val itemRepository: ItemRepository,
    private val save: Save,
    private val tracker: Tracker,
    private val contentOpenTracker: ContentOpenTracker,
) : ViewModel(),
    ArticleScreen.Initializer,
    ArticleScreen.EndOfArticleRecommendationInteractions {

    private val _recommendations = MutableStateFlow<List<CorpusItemUiState>>(emptyList())
    val recommendations: StateFlow<List<CorpusItemUiState>> = _recommendations

    private val _events = MutableSharedFlow<ArticleScreen.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<ArticleScreen.Event> = _events

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private lateinit var url: String

    override fun onInitialized(url: String) {
        this.url = url
        setupFlow()
        refreshData()
    }

    private fun setupFlow() {
        viewModelScope.launch {
            recommendationsRepository.getEndOfArticleRecommendationsFlow(url).collect { corpusRecommendations ->
                _recommendations.edit {
                    corpusRecommendations.map { corpusRecommendation ->
                        CorpusItemUiState(
                            title = corpusRecommendation.corpusItem.title,
                            publisher = corpusRecommendation.corpusItem.publisher,
                            excerpt = corpusRecommendation.corpusItem.excerpt,
                            imageUrl = corpusRecommendation.corpusItem.imageUrl,
                            url = corpusRecommendation.corpusItem.url,
                            isSaved = corpusRecommendation.corpusItem.isSaved,
                            corpusRecommendationId = corpusRecommendation.id,
                        )
                    }
                }
                if (corpusRecommendations.isNotEmpty()) {
                    _uiState.edit { copy(
                        visible = true,
                    ) }
                }
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            try {
                recommendationsRepository.refreshEndOfArticleRecommendations(url)
            } catch (e: Exception) {
                // do nothing
                Log.e("EndOfArticleRecs", e.message ?: "")
            }
        }
    }

    override fun onCardClicked(url: String, corpusRecommendationId: String) {
        contentOpenTracker.track(ArticleViewEvents.endOfArticleContentOpen(
            url,
            corpusRecommendationId,
        ))
        _events.tryEmit(ArticleScreen.Event.OpenNewUrl(
            url,
            UrlListQueueManager(
                recommendations.value.map { it.url },
                recommendations.value.indexOfFirst { it.url == url }
            )
        ))
    }

    override fun onSaveClicked(
        url: String,
        isSaved: Boolean,
        corpusRecommendationId: String,
    ) {
        if (isSaved) {
            itemRepository.delete(url)
        } else {
            tracker.track(ArticleViewEvents.recommendationSaveClicked(url, corpusRecommendationId))
            viewModelScope.launch {
                when (save(url)) {
                    Save.Result.Success -> {
                        // Nothing to do here.
                    }
                    Save.Result.NotLoggedIn -> _events.emit(ArticleScreen.Event.GoToSignIn)
                }
            }
        }
    }

    override fun onOverflowClicked(url: String, title: String, corpusRecommendationId: String?) {
        tracker.track(ArticleViewEvents.recommendationOverflowClicked())
        _events.tryEmit(value = ArticleScreen.Event.OpenOverflowBottomSheet(
            url = url,
            title = title,
            corpusRecommendationId = corpusRecommendationId
        ))
    }

    override fun onArticleViewed(
        position: Int,
        url: String,
        corpusRecommendationId: String,
    ) {
        tracker.track(ArticleViewEvents.endOfArticleImpression(
            positionInList = position,
            itemUrl = url,
            corpusRecommendationId = corpusRecommendationId,
        ))
    }

    data class CorpusItemUiState(
        val title: String,
        val publisher: String,
        val excerpt: String,
        val imageUrl: String,
        val url: String,
        val isSaved: Boolean,
        val corpusRecommendationId: String,
    )

    data class UiState(
        val visible: Boolean = false
    )
}
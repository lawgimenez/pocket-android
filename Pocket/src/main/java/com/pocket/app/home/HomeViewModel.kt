package com.pocket.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.app.home.details.RecommendationUiState
import com.pocket.app.home.details.toRecommendationUiState
import com.pocket.data.models.DomainSlate
import com.pocket.repository.HomeRepository
import com.pocket.repository.ItemRepository
import com.pocket.repository.TopicsRepository
import com.pocket.repository.UserRepository
import com.pocket.sdk.api.generated.thing.DiscoverTopic
import com.pocket.sdk.dev.ErrorHandler
import com.pocket.usecase.Save
import com.pocket.util.StringLoader
import com.pocket.util.edit
import com.pocket.util.java.Locale
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.getValue
import com.pocket.util.prefs.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import org.threeten.bp.Clock
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferences: Preferences,
    private val homeRepository: HomeRepository,
    private val topicsRepository: TopicsRepository,
    locale: Locale,
    private val itemRepository: ItemRepository,
    private val userRepository: UserRepository,
    private val save: Save,
    private val tracker: Tracker,
    private val stringLoader: StringLoader,
    private val contentOpenTracker: ContentOpenTracker,
    private val errorHandler: ErrorHandler,
    private val clock: Clock,
) : ViewModel(),
    Home.Interactions,
    Home.RecommendationInteractions,
    Home.ErrorSnackBarInteractions {

    private val localeString = locale.toString()

    private var lastRefresh by preferences.forUser("home_slates_refresh_time", 0L)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _slatesUiState = MutableStateFlow(listOf<RecommendationSlateUiState>())
    val slatesUiState: StateFlow<List<RecommendationSlateUiState>> = _slatesUiState

    private val _topicsUiState = MutableStateFlow(listOf<TopicUiState>())
    val topicsUiState: StateFlow<List<TopicUiState>> = _topicsUiState

    private val _events = MutableSharedFlow<Home.Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Home.Event> = _events

    private val refreshDataMutex = Mutex()

    private fun dataIsStale(): Boolean {
        return when {
            homeRepository.currentLocale != localeString -> true
            Instant.ofEpochMilli(lastRefresh) < clock.instant() - Duration.ofHours(12) -> true
            else -> false
        }
    }

    override fun onInitialized() {
        setupSlateFlows()
        setupTopicsFlows()
        setupLoginInfoFlow()
        refreshData()
    }

    override fun onUserReturned() {
        refreshData()
    }

    private fun setupLoginInfoFlow() {
        viewModelScope.launch {
            userRepository.isPremiumUpgradeAvailable().collect { available ->
                _uiState.update {
                    it.copy(upgradeButtonVisible = available)
                }
            }
        }
        viewModelScope.launch {
            userRepository.isLoggedIn().collect { loggedIn ->
                _uiState.update {
                    it.copy(signInBannerVisible = !loggedIn)
                }
            }
        }
    }

    private fun setupSlateFlows() {
        viewModelScope.launch {
            homeRepository.getLineup(localeString)
                .collect { slates ->
                    updateSlates(slates)
                }
        }
    }

    private fun setupTopicsFlows() {
        viewModelScope.launch {
            topicsRepository.getTopicsAsFlow().collect { discoverTopicList ->
                discoverTopicList.topics?.let { updateTopics(it) }
            }
        }
    }

    /** Refreshes all data for home. */
    private fun refreshData() {
        viewModelScope.launch { supervisorScope {
            // use a mutex to make sure network calls don't happen multiple times simultaneously
            // required because both onInitialized and onUserReturned attempt to refresh data
            // and both will be called at about the same time at the beginning of an app session.
            // Also, a user could pull to refresh while the onInitialized call is happening.
            refreshDataMutex.lock()

            _uiState.edit {
                copy(
                    screenState = if (dataIsStale()) {
                        ScreenState.Loading
                    } else {
                        ScreenState.Slates
                    }
                )
            }

            try {
                val slates = async {
                    homeRepository.refreshLineup(localeString)
                }
                val topics = async {
                    topicsRepository.refreshTopics()
                }
                awaitAll(slates, topics)

                _uiState.edit {
                    copy(
                        screenState = ScreenState.Slates,
                        isRefreshing = false,
                        errorSnackBarRefreshing = false,
                        errorSnackBarVisible = false,
                    )
                }
                lastRefresh = clock.millis()
            } catch (exception: Exception) {
                // if any async {} jobs fail, they are all cancelled and the catch block in invoked
                val hasCachedData = try {
                    val hasCachedTopics = async { topicsRepository.hasCachedTopics() }
                    val hasCachedSlates = async { homeRepository.hasCachedLineup(localeString) }
                    hasCachedTopics.await() && hasCachedSlates.await()
                } catch (cacheError: Exception) {
                    false
                }

                _uiState.edit {
                    copy(
                        isRefreshing = false,
                        errorSnackBarRefreshing = false,
                        errorSnackBarVisible = true,
                        screenState = if (hasCachedData) {
                            ScreenState.Slates
                        } else {
                            ScreenState.Loading
                        }
                    )
                }
            }
        }}.invokeOnCompletion {
            refreshDataMutex.unlock()
        }
    }

    private fun updateSlates(
        slates: List<DomainSlate>,
    ) {

        _slatesUiState.edit {
            slates.mapNotNull { slate ->
                if (slate.recommendations.isEmpty()) {
                    // Report and filter out.
                    errorHandler.reportOnProductionOrThrow(RuntimeException(
                        "Slate is empty: " + listOf(
                            "id = ${slate.id}",
                            "title = ${slate.title}",
                            "locale = $localeString",
                        )
                    ))
                    null
                } else {
                    slate.toRecommendationSlateUiState(
                        stringLoader = stringLoader,
                        recommendationsPerSlate = RECOMMENDATIONS_PER_SLATE,
                    )
                }
            }
        }
    }

    private fun updateTopics(topics: List<DiscoverTopic>) {
        _topicsUiState.update {
            topics.map { topic ->
                TopicUiState(
                    title = topic.display_name.orEmpty(),
                    topicId = topic.topic!!,
                )
            }
        }
    }

    override fun onSaveClicked(url: String, isSaved: Boolean, corpusRecommendationId: String?) {
        if (isSaved) {
            itemRepository.delete(url)
        } else {
            tracker.track(
                HomeEvents.recommendationSaveClicked(
                    url = url,
                    corpusRecommendationId = corpusRecommendationId
                )
            )
            viewModelScope.launch {
                when (save(url)) {
                    Save.Result.Success -> {
                        // Nothing to do here.
                    }
                    Save.Result.NotLoggedIn -> _events.emit(Home.Event.GoToSignIn)
                }
            }
        }
    }

    override fun onItemClicked(
        url: String,
        slateTitle: String,
        positionInSlate: Int,
        corpusRecommendationId: String?,
    ) {
        contentOpenTracker.track(
            HomeEvents.slateArticleContentOpen(
                slateTitle = slateTitle,
                positionInSlate = positionInSlate,
                itemUrl = url,
                corpusRecommendationId = corpusRecommendationId,
            )
        )
        _events.tryEmit(Home.Event.GoToReader(url = url))
    }

    override fun onRecommendationOverflowClicked(
        url: String,
        title: String,
        corpusRecommendationId: String?,
    ) {
        tracker.track(
            HomeEvents.recommendationOverflowClicked(
                corpusRecommendationId = corpusRecommendationId,
                url = url
            )
        )
        _events.tryEmit(
            Home.Event.ShowRecommendationOverflow(
                url = url,
                title = title,
                corpusRecommendationId = corpusRecommendationId,
            )
        )
    }

    override fun onSeeAllRecommendationsClicked(slateId: String, slateTitle: String) {
        tracker.track(HomeEvents.slateSeeAllClicked(slateTitle = slateTitle))
        _events.tryEmit(Home.Event.GoToSlateDetails(slateId))
    }

    override fun onTopicClicked(topicId: String, topicTitle: String) {
        tracker.track(HomeEvents.topicClicked(topicTitle))
        _events.tryEmit(Home.Event.GoToTopicDetails(topicId))
    }

    override fun onRecommendationViewed(
        slateTitle: String,
        positionInSlate: Int,
        itemUrl: String,
        corpusRecommendationId: String?,
    ) {
        tracker.track(
            HomeEvents.slateArticleImpression(
                slateTitle = slateTitle,
                positionInSlate = positionInSlate,
                itemUrl = itemUrl,
                corpusRecommendationId = corpusRecommendationId,
            )
        )
    }

    override fun onErrorRetryClicked() {
        _uiState.edit {
            copy(
                errorSnackBarRefreshing = true,
            )
        }
        refreshData()
    }

    override fun onSwipedToRefresh() {
        _uiState.edit {
            copy(
                isRefreshing = true,
            )
        }
        refreshData()
    }

    override fun onPremiumUpgradeClicked() {
        _events.tryEmit(Home.Event.GoToPremium)
    }

    override fun onErrorSnackBarDismissed() {
        _uiState.edit {
            copy(
                errorSnackBarVisible = false,
                errorSnackBarRefreshing = false,
            )
        }
    }

    fun onSignInBannerViewed() {
        tracker.track(HomeEvents.signInBannerImpression())
    }

    fun onSignInClicked() {
        tracker.track(HomeEvents.signInBannerButtonClicked())
        _events.tryEmit(Home.Event.GoToSignIn)
    }

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
        val isRefreshing: Boolean = false,
        val errorSnackBarRefreshing: Boolean = false,
        val errorSnackBarVisible: Boolean = false,
        val upgradeButtonVisible: Boolean = false,
        val signInBannerVisible: Boolean = false,
    )

    sealed class ScreenState(
        val slatesSkeletonVisible: Boolean = false,
        val slatesVisible: Boolean = false,
        val topicsVisible: Boolean = true,
    ) {
        object Loading : ScreenState(
            slatesSkeletonVisible = true,
        )

        object Slates : ScreenState(
            slatesVisible = true,
        )
    }

    data class RecommendationSlateUiState(
        val title: String?,
        val subheadline: String?,
        val recommendations: List<RecommendationUiState>,
    ) {
        // Exclude slate id from `.equals()` etc., because it's not stable.
        lateinit var slateId: String
    }

    private fun DomainSlate.toRecommendationSlateUiState(
        stringLoader: StringLoader,
        recommendationsPerSlate: Int,
    ) = RecommendationSlateUiState(
        title = title,
        subheadline = subheadline,
        recommendations = recommendations
            .map { it.toRecommendationUiState(stringLoader) }
            .take(recommendationsPerSlate)
    ).apply {
        slateId = id
    }


    data class TopicUiState(
        val title: String,
        val topicId: String,
    )

    companion object {
        private const val RECOMMENDATIONS_PER_SLATE = 5
    }
}
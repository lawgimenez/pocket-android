package com.pocket.app.home

import com.pocket.BaseCoroutineTest
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.data.models.DomainRecommendation
import com.pocket.data.models.DomainSlate
import com.pocket.repository.HomeRepository
import com.pocket.repository.ItemRepository
import com.pocket.repository.TopicsRepository
import com.pocket.repository.SyncEngineUserRepository
import com.pocket.sdk.dev.ErrorHandler
import com.pocket.usecase.Save
import com.pocket.util.StringLoader
import com.pocket.util.java.Locale
import com.pocket.util.prefs.Preferences
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test

class HomeViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val preferences = mockk<Preferences>(relaxed = true)
    @SpyK
    private val homeRepository = mockk<HomeRepository>(relaxed = true)
    @SpyK
    private val topicsRepository = mockk<TopicsRepository>(relaxed = true)
    @SpyK
    private val locale = mockk<Locale>(relaxed = true)
    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val userRepository = mockk<SyncEngineUserRepository>(relaxed = true)
    @SpyK
    private val save = mockk<Save>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val stringLoader = mockk<StringLoader>(relaxed = true)
    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)
    @SpyK
    private val errorHandler = mockk<ErrorHandler>(relaxed = true)

    private lateinit var subject: HomeViewModel

    @BeforeTest
    fun setup() {
        val time = Instant.from(ZonedDateTime.of(2024, 7, 4, 0, 0, 0, 0, ZoneOffset.UTC))
        subject = HomeViewModel(
            preferences,
            homeRepository,
            topicsRepository,
            locale,
            itemRepository,
            userRepository,
            save,
            tracker,
            stringLoader,
            contentOpenTracker,
            errorHandler,
            Clock.fixed(time, ZoneOffset.UTC),
        )
    }

    @Test
    fun `WHEN a rec is viewed THEN an analytics event is sent`() {
        subject.onRecommendationViewed(
            "title",
            1,
            "url",
            "id"
        )

        verify(exactly = 1) {
            tracker.track(
                HomeEvents.slateArticleImpression(
                    slateTitle = "title",
                    positionInSlate = 1,
                    itemUrl = "url",
                    corpusRecommendationId = "id"
                )
            )
        }
    }

    @Test
    fun `WHEN lineup contains empty slates THEN empty slates are filtered out`() = runTest {
        val testRecommendation = DomainRecommendation(
            corpusId = null,
            itemId = "item 1",
            url = "https://getpocket.com",
            title = "Pocket",
            domain = "https://getpocket.com",
            imageUrl = null,
            isCollection = false,
            isSaved = false,
            excerpt = "",
            index = 0,
            viewingTime = null,
        )

        val homeSlateLineup = MutableSharedFlow<List<DomainSlate>>()
        every { homeRepository.getLineup(any()) } returns homeSlateLineup

        subject.onInitialized()
        homeSlateLineup.emit(
            listOf(
                DomainSlate(null, null, "1", listOf(testRecommendation)),
                DomainSlate("empty", null, "2", emptyList()),
                DomainSlate(null, null, "3", listOf(testRecommendation)),
            )
        )

        with(subject.slatesUiState.value) {
            assert(isNotEmpty())
            assert(none { it.recommendations.isEmpty() })
        }
    }
}

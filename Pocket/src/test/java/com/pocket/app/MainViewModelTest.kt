package com.pocket.app

import app.cash.turbine.turbineScope
import com.pocket.BaseCoroutineTest
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.FakeTracker
import com.pocket.analytics.appevents.ReaderEvents
import com.pocket.repository.FakeItemRepository
import com.pocket.util.prefs.MemoryPrefStore
import com.pocket.util.prefs.Prefs
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MainViewModelTest : BaseCoroutineTest() {

    private val preferences = Prefs(MemoryPrefStore(), MemoryPrefStore())
    private val itemRepository = FakeItemRepository()

    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)
    @SpyK
    private val userManager = mockk<UserManager>(relaxed = true)

    private lateinit var subject: MainViewModel

    @BeforeTest
    fun setup() {
        subject = MainViewModel(
            preferences,
            itemRepository,
            contentOpenTracker,
            userManager,
            FakeTracker(),
        )
    }

    @Test
    fun `WHEN receives a reader deeplink AND the deeplink is not a short link THEN the proper analytics event is sent`() {
        subject.onReaderDeepLinkReceived("https://getpocket.com/something", false)
        verify(exactly = 1) {
            contentOpenTracker.track(ReaderEvents.deeplinkContentOpen("https://getpocket.com/something"))
        }
    }

    @Test
    fun `WHEN receives a short link THEN the proper analytics event is sent`() {
        val shortLink = "https://pocket.co/sXYZ123"
        subject.onReaderDeepLinkReceived(shortLink, openListen = false)
        verify(exactly = 1) {
            contentOpenTracker.track(ReaderEvents.pocketCoContentOpen(shortLink))
        }
    }

    @Test
    fun `WHEN receives a syndicated article or a collection THEN opens it directly in reader`() = runTest {
        turbineScope {
            val events = subject.events
                .filterNot { it is MainViewModel.Event.ShowProgress }
                .filterNot { it is MainViewModel.Event.HideProgress }
                .testIn(backgroundScope)
            val originalUrl = "https://getpocket.com/article/1"

            subject.onReaderDeepLinkReceived(originalUrl, false)

            val event = events.awaitItem()
            assertIs<MainViewModel.Event.OpenReader>(event)
            assertNull(event.item)
            assertEquals(originalUrl, event.url)
        }
    }

    @Test
    fun `WHEN receives a share link THEN resolves it before opening in reader`() = runTest {
        turbineScope {
            val events = subject.events
                .filterNot { it is MainViewModel.Event.ShowProgress }
                .filterNot { it is MainViewModel.Event.HideProgress }
                .testIn(backgroundScope)
            val slug = "some-uuid"
            val originalUrl = "https://item.example/1"
            itemRepository.mapItemToShareSlug(originalUrl, slug)

            subject.onReaderDeepLinkReceived("https://pocket.co/share/$slug", false)

            val event = events.awaitItem()
            assertIs<MainViewModel.Event.OpenReader>(event)
            assertNotNull(event.item)
            assertEquals(originalUrl, event.url)
        }
    }

    @Test
    fun `WHEN receives a short link THEN un-shortens it before opening in reader`() = runTest {
        turbineScope {
            val events = subject.events
                .filterNot { it is MainViewModel.Event.ShowProgress }
                .filterNot { it is MainViewModel.Event.HideProgress }
                .testIn(backgroundScope)
            val shortLink = "https://pocket.co/xQwe742"
            val originalUrl = "https://item.example/1"
            itemRepository.mapItemToUrl(originalUrl, shortLink)
            subject.onReaderDeepLinkReceived(shortLink, false)

            val event = events.awaitItem()
            assertIs<MainViewModel.Event.OpenReader>(event)
            assertNotNull(event.item)
            assertEquals(originalUrl, event.url)
        }
    }
}

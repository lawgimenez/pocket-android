package com.pocket.app.reader

import com.pocket.BaseCoroutineTest
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ReaderEvents
import com.pocket.app.list.list.ListManager
import com.pocket.app.reader.queue.UrlListQueueManager
import com.pocket.repository.ItemRepository
import com.pocket.sdk.preferences.AppPrefs
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReaderViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val reader = mockk<Reader>(relaxed = true)
    @SpyK
    private val listManager = mockk<ListManager>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val destinationHelper = mockk<DestinationHelper>(relaxed = true)

    private lateinit var subject: ReaderViewModel

    @BeforeTest
    fun setup() {
        subject = ReaderViewModel(
            itemRepository,
            reader,
            listManager,
            tracker,
            destinationHelper,
        )
    }

    @Test
    fun `WHEN next is clicked AND there is an article in the queue THEN an analytics event is sent`() {
        subject.openUrl(
            "url1",
            UrlListQueueManager(
                listOf(
                    "url1",
                    "url2"
                ),
                0
            )
        )

        subject.onNextClicked()

        verify(exactly = 1) {
            tracker.track(ReaderEvents.nextClicked("url2"))
        }
    }

    @Test
    fun `WHEN previous is clicked AND there is an article in the queue THEN an analytics event is sent`() {
        subject.openUrl(
            "url2",
            UrlListQueueManager(
                listOf(
                    "url1",
                    "url2"
                ),
                1
            )
        )

        subject.onPreviousClicked()

        verify(exactly = 1) {
            tracker.track(ReaderEvents.previousClicked("url1"))
        }
    }
}
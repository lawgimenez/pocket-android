package com.pocket.app.home.saves

import com.pocket.BaseCoroutineTest
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.repository.ItemRepository
import com.pocket.repository.SavesRepository
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk2.view.ModelBindingHelper
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class RecentSavesViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val savesRepository = mockk<SavesRepository>(relaxed = true)
    @SpyK
    private val modelBindingHelper = mockk<ModelBindingHelper>(relaxed = true)
    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)

    private lateinit var subject: RecentSavesViewModel

    @BeforeTest
    fun setup() {
        subject = RecentSavesViewModel(
            savesRepository,
            modelBindingHelper,
            itemRepository,
            tracker,
            contentOpenTracker,
        )
    }

    @Test
    fun `WHEN an item is viewed THEN an analytics event is sent`() {
        subject.onSaveViewed(1, "url")

        verify(exactly = 1) {
            tracker.track(HomeEvents.recentSavesImpression(1, "url"))
        }
    }

    @Test
    fun `WHEN an item is clicked THEN an analytics event is sent`() {
        val item = Item.Builder().given_url(UrlString("url")).build()
        subject.onItemClicked(item, 1)

        verify(exactly = 1) {
            contentOpenTracker.track(
                HomeEvents.recentSavesCardContentOpen(
                    itemUrl = "url",
                    positionInList = 1,
                )
            )
        }
    }
}
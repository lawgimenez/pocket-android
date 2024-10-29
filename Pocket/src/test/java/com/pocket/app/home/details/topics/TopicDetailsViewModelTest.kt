package com.pocket.app.home.details.topics

import com.pocket.BaseCoroutineTest
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.HomeEvents
import com.pocket.repository.ItemRepository
import com.pocket.repository.TopicsRepository
import com.pocket.usecase.Save
import com.pocket.util.StringLoader
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class TopicDetailsViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val topicsRepository = mockk<TopicsRepository>(relaxed = true)
    @SpyK
    private val stringLoader = mockk<StringLoader>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val save = mockk<Save>(relaxed = true)
    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)

    private lateinit var subject: TopicDetailsViewModel

    @BeforeTest
    fun setup() {
        subject = TopicDetailsViewModel(
            topicsRepository,
            stringLoader,
            tracker,
            itemRepository,
            save,
            contentOpenTracker,
        )
    }

    @Test
    fun `WHEN an item is clicked THEN an analytics event is sent`() {
        subject.onItemClicked("url", 1, "recId")

        verify(exactly = 1) {
            contentOpenTracker.track(
                HomeEvents.topicArticleContentOpen(
                    topicTitle = "",
                    positionInTopic = 1,
                    itemUrl = "url",
                )
            )
        }
    }
}
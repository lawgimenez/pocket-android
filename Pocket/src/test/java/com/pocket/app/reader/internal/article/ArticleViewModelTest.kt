package com.pocket.app.reader.internal.article

import com.pocket.BaseCoroutineTest
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ArticleViewEvents
import com.pocket.app.premium.PremiumFonts
import com.pocket.repository.ArticleRepository
import com.pocket.repository.HighlightRepository
import com.pocket.repository.ItemRepository
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.usecase.Save
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class ArticleViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val articleRepository = mockk<ArticleRepository>(relaxed = true)
    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val save = mockk<Save>(relaxed = true)
    @SpyK
    private val pocketCache = mockk<PocketCache>(relaxed = true)
    @SpyK
    private val highlightRepository = mockk<HighlightRepository>(relaxed = true)
    @SpyK
    private val displaySettingsManager = mockk<DisplaySettingsManager>(relaxed = true)
    @SpyK
    private val premiumFonts = mockk<PremiumFonts>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)

    private lateinit var subject: ArticleViewModel

    @BeforeTest
    fun setup() {
        subject = ArticleViewModel(
            articleRepository,
            itemRepository,
            save,
            pocketCache,
            highlightRepository,
            displaySettingsManager,
            premiumFonts,
            tracker,
            contentOpenTracker,
        )
    }

    @Test
    fun `WHEN an article link is opened THEN an analytics event is sent`() {
        subject.onArticleLinkOpened("url")

        verify(exactly = 1) {
            contentOpenTracker.track(ArticleViewEvents.articleLinkContentOpen("url"))
        }
    }
}
package com.pocket.app.list.bulkedit

import com.ideashower.readitlater.R
import com.pocket.BaseCoroutineTest
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesTab
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.UrlString
import com.pocket.testutils.SharedFlowTracker
import com.pocket.util.StringLoader
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BulkEditOverflowBottomSheetViewModelTest : BaseCoroutineTest() {

    @SpyK private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK private val stringLoader = mockk<StringLoader>(relaxed = true)
    @SpyK private val tracker = mockk<Tracker>(relaxed = true)

    private lateinit var subject: BulkEditOverflowBottomSheetViewModel

    private lateinit var navigationEventTracker: SharedFlowTracker<BulkEditOverflowNavigationEvent>

    private val state: BulkEditOverflowBottomSheetUiState
        get() = subject.uiState.value

    private val testItem1 = Item.Builder()
        .title("title")
        .domain("domain")
        .top_image_url(UrlString("test.com"))
        .favorite(true)
        .viewed(true)
        .build()

    private val testItem2 = Item.Builder()
        .title("title2")
        .domain("domain2")
        .build()

    @BeforeTest
    fun setup() {
        subject = BulkEditOverflowBottomSheetViewModel(
            stringLoader = stringLoader,
            itemRepository = itemRepository,
            tracker = tracker
        )
        navigationEventTracker = SharedFlowTracker(subject.navigationEvents)
        val quantity = slot<Int>()
        every { stringLoader.getQuantityString(R.plurals.lb_selected, capture(quantity), any()) } answers { "${quantity.captured} selected" }
        every { stringLoader.getString(com.pocket.ui.R.string.ic_favorite) } returns "favorite"
        every { stringLoader.getString(com.pocket.ui.R.string.ic_unfavorite) } returns "unfavorite"
        subject.onInitialized(
            listOf(
                testItem1,
                testItem2,
            ),
            SavesTab.SAVES,
        )
    }

    @Test
    fun `onInitialized behavior`() {
        assertEquals(
            expected = "2 selected",
            actual = state.title
        )

        assertEquals(
            expected = "favorite",
            actual = state.favoriteText
        )

        subject.onInitialized(
            listOf(
                testItem1
            ),
            SavesTab.SAVES,
        )

        assertEquals(
            expected = "1 selected",
            actual = state.title
        )

        assertEquals(
            expected = "unfavorite",
            actual = state.favoriteText
        )
    }

    @Test
    fun `on favorite clicked behavior`() {
        subject.onFavoriteClicked()
        verify { itemRepository.favorite(testItem1, testItem2) }
        assertEquals(
            expected = BulkEditOverflowNavigationEvent.Close,
            actual = navigationEventTracker.lastValue
        )
    }

    @Test
    fun `on edit tags clicked test`() {
        subject.onEditTagsClicked()
        assertEquals(
            expected = BulkEditOverflowNavigationEvent.OpenTagScreen,
            actual = navigationEventTracker.lastValue
        )
    }

    @Test
    fun `on mark as viewed clicked test`() {
        subject.onMarkAsViewedClicked()
        verify { itemRepository.markAsViewed(testItem1, testItem2) }
        assertEquals(
            expected = BulkEditOverflowNavigationEvent.Close,
            actual = navigationEventTracker.lastValue
        )
    }

    @Test
    fun `on mark as not viewed clicked test`() {
        subject.onMarkAsNotViewedClicked()
        verify { itemRepository.markAsNotViewed(testItem1, testItem2) }
        assertEquals(
            expected = BulkEditOverflowNavigationEvent.Close,
            actual = navigationEventTracker.lastValue
        )
    }
}
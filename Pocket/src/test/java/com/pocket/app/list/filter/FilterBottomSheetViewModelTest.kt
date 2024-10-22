package com.pocket.app.list.filter

import com.ideashower.readitlater.R
import com.pocket.BaseCoroutineTest
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.list.list.ListManager
import com.pocket.app.list.list.ListStatus
import com.pocket.app.list.list.SortFilterState
import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.sdk.api.generated.enums.ItemSortKey
import com.pocket.util.StringLoader
import com.pocket.util.edit
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.test.*

class FilterBottomSheetViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val listManager = mockk<ListManager>(relaxed = true)
    @SpyK
    private val stringLoader = mockk<StringLoader>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)

    private lateinit var subject: FilterBottomSheetViewModel

    private val state: SortFilterUiState
        get() = subject.uiState.value

    private val listManagerSortFilterState = MutableStateFlow(
        SortFilterState(
            sort = ItemSortKey.NEWEST,
            filters = listOf(),
            listStatus = ListStatus.SAVES,
            tag = null
        )
    )

    @BeforeTest
    fun setup() {
        every { stringLoader.getString(R.string.lb_sort_by_newest) } returns "Newest saved"
        every { stringLoader.getString(R.string.lb_sort_by_oldest) } returns "Oldest saved"
        every { stringLoader.getString(R.string.lb_sort_by_newest_archive) } returns "Newest archived"
        every { stringLoader.getString(R.string.lb_sort_by_oldest_archive) } returns "Oldest archived"
        every { listManager.sortFilterState } returns listManagerSortFilterState

        subject = FilterBottomSheetViewModel(listManager, stringLoader, tracker)
        subject.onInitialized(SavesTab.SAVES)
    }

    @Test
    fun `WHEN a sort button is clicked THEN the appropriate list sort function is called AND the checked state is correct`() {
        subject.onNewestClicked()
        verify {
            listManager.updateCurrentSort(ItemSortKey.NEWEST)
        }
        listManagerSortFilterState.edit { copy(
            sort = ItemSortKey.NEWEST
        ) }
        assertTrue(state.sortOrders.newest.checked)
        assertFalse(state.sortOrders.oldest.checked)
        assertFalse(state.sortOrders.longest.checked)
        assertFalse(state.sortOrders.shortest.checked)

        subject.onOldestClicked()
        verify {
            listManager.updateCurrentSort(ItemSortKey.OLDEST)
        }
        listManagerSortFilterState.edit { copy(
            sort = ItemSortKey.OLDEST
        ) }
        assertFalse(state.sortOrders.newest.checked)
        assertTrue(state.sortOrders.oldest.checked)
        assertFalse(state.sortOrders.longest.checked)
        assertFalse(state.sortOrders.shortest.checked)

        subject.onLongestClicked()
        verify {
            listManager.updateCurrentSort(ItemSortKey.LONGEST)
        }
        listManagerSortFilterState.edit { copy(
            sort = ItemSortKey.LONGEST
        ) }
        assertFalse(state.sortOrders.newest.checked)
        assertFalse(state.sortOrders.oldest.checked)
        assertTrue(state.sortOrders.longest.checked)
        assertFalse(state.sortOrders.shortest.checked)

        subject.onShortestClicked()
        verify {
            listManager.updateCurrentSort(ItemSortKey.SHORTEST)
        }
        listManagerSortFilterState.edit { copy(
            sort = ItemSortKey.SHORTEST
        ) }
        assertFalse(state.sortOrders.newest.checked)
        assertFalse(state.sortOrders.oldest.checked)
        assertFalse(state.sortOrders.longest.checked)
        assertTrue(state.sortOrders.shortest.checked)
    }

    @Test
    fun `WHEN a filter button is clicked THEN the appropriate list sort function is called AND the checked state is correct`() {
        listManagerSortFilterState.edit { copy(
            filters = listOf(ItemFilterKey.VIEWED)
        ) }
        subject.onViewedClicked()
        verify {
            listManager.onFilterToggled(ItemFilterKey.VIEWED)
        }
        assertTrue(state.filters.viewed.checked)
        assertFalse(state.filters.notViewed.checked)
        assertFalse(state.filters.shortReads.checked)
        assertFalse(state.filters.longReads.checked)

        listManagerSortFilterState.edit { copy(
            filters = listOf(ItemFilterKey.NOT_VIEWED)
        ) }
        subject.onNotViewedClicked()
        verify {
            listManager.onFilterToggled(ItemFilterKey.NOT_VIEWED)
        }
        assertFalse(state.filters.viewed.checked)
        assertTrue(state.filters.notViewed.checked)
        assertFalse(state.filters.shortReads.checked)
        assertFalse(state.filters.longReads.checked)

        listManagerSortFilterState.edit { copy(
            filters = listOf(ItemFilterKey.SHORT_READS)
        ) }
        subject.onShortReadsClicked()
        verify {
            listManager.onFilterToggled(ItemFilterKey.SHORT_READS)
        }
        assertFalse(state.filters.viewed.checked)
        assertFalse(state.filters.notViewed.checked)
        assertTrue(state.filters.shortReads.checked)
        assertFalse(state.filters.longReads.checked)

        listManagerSortFilterState.edit { copy(
            filters = listOf(ItemFilterKey.LONG_READS)
        ) }
        subject.onLongReadsClicked()
        verify {
            listManager.onFilterToggled(ItemFilterKey.LONG_READS)
        }
        assertFalse(state.filters.viewed.checked)
        assertFalse(state.filters.notViewed.checked)
        assertFalse(state.filters.shortReads.checked)
        assertTrue(state.filters.longReads.checked)
    }

    @Test
    fun `WHEN in archive THEN shortest and longest to read aren't visible`() {
        listManagerSortFilterState.update {
            it.copy(
                listStatus = ListStatus.ARCHIVE,
            )
        }
        assertTrue(state.sortOrders.newest.visible)
        assertTrue(state.sortOrders.oldest.visible)
        assertFalse(state.sortOrders.shortest.visible)
        assertFalse(state.sortOrders.longest.visible)
    }

    @Test
    fun `WHEN shortest or longest to read sort is selected in archive THEN it falls back to newest`() {
        listManagerSortFilterState.update {
            it.copy(
                sort = ItemSortKey.SHORTEST,
                listStatus = ListStatus.ARCHIVE,
            )
        }
        assertTrue(state.sortOrders.newest.checked)
        assertFalse(state.sortOrders.oldest.checked)
        assertFalse(state.sortOrders.shortest.checked)
        assertFalse(state.sortOrders.longest.checked)

        listManagerSortFilterState.update {
            it.copy(
                sort = ItemSortKey.LONGEST,
                listStatus = ListStatus.ARCHIVE,
            )
        }
        assertTrue(state.sortOrders.newest.checked)
        assertFalse(state.sortOrders.oldest.checked)
        assertFalse(state.sortOrders.shortest.checked)
        assertFalse(state.sortOrders.longest.checked)
    }
}

package com.pocket.app.list.tags

import android.view.View
import com.ideashower.readitlater.R
import com.pocket.BaseCoroutineTest
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.list.list.ListManager
import com.pocket.app.list.list.ListStatus
import com.pocket.app.list.list.SortFilterState
import com.pocket.repository.TagRepository
import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.sdk.api.generated.enums.ItemSortKey
import com.pocket.sdk.api.generated.thing.Tag
import com.pocket.sdk.api.generated.thing.Tags
import com.pocket.testutils.SharedFlowTracker
import com.pocket.util.StringLoader
import com.pocket.util.edit
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagBottomSheetViewModelTest : BaseCoroutineTest() {

    @SpyK private val listManager = mockk<ListManager>(relaxed = true)
    @SpyK private val stringLoader = mockk<StringLoader>(relaxed = true)
    @SpyK private val tagRepository = mockk<TagRepository>(relaxed = true)
    @SpyK private val tracker = mockk<Tracker>(relaxed = true)

    private lateinit var subject: TagBottomSheetViewModel

    private lateinit var navigationEventTracker: SharedFlowTracker<TagBottomSheetNavigationEvent>

    private val state: TagBottomSheetUiState
        get() = subject.uiState.value

    private val list: List<BottomSheetItemUiState>
        get() = subject.tagsListUiState.value

    private val tagFlow = MutableSharedFlow<Tags>(extraBufferCapacity = 1)

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
        every { tagRepository.getTagsAsFlow() } returns tagFlow
        every { stringLoader.getString(R.string.edit_tags) } returns "edit tags"
        every { stringLoader.getString(R.string.lb_tags_autocomplete) } returns "tags"
        every { listManager.sortFilterState } returns listManagerSortFilterState

        subject = TagBottomSheetViewModel(
            tagRepository = tagRepository,
            stringLoader = stringLoader,
            listManager = listManager,
            tracker = tracker
        )
        navigationEventTracker = SharedFlowTracker(subject.navigationEvent)
        subject.onInitialized(SavesTab.SAVES)
    }

    @Test
    fun `tag list should show not tagged first, then 3 recently used tags and then the rest in alphabetical order`() {
        tagFlow.tryEmit(
            Tags.Builder()
                .tags(listOf(
                    "1",
                    "2",
                    "3",
                    "4",
                    "5"
                ))
                .recentlyUsed(listOf(
                    Tag.Builder()
                        .tag("5")
                        .build(),
                    Tag.Builder()
                        .tag("4")
                        .build(),
                    Tag.Builder()
                        .tag("3")
                        .build(),
                    Tag.Builder()
                        .tag("2")
                        .build(),
                    Tag.Builder()
                        .tag("1")
                        .build()
                ))
                .build()
        )

        assertTrue(
            list.first() is NotTaggedBottomSheetItemUiState
        )
        assertEquals(
            expected = "5",
            actual = (list[1] as TagBottomSheetItemUiState).tag
        )
        assertEquals(
            expected = "4",
            actual = (list[2] as TagBottomSheetItemUiState).tag
        )
        assertEquals(
            expected = "3",
            actual = (list[3] as TagBottomSheetItemUiState).tag
        )
        assertEquals(
            expected = "1",
            actual = (list[4] as TagBottomSheetItemUiState).tag
        )
        assertEquals(
            expected = "2",
            actual = (list[5] as TagBottomSheetItemUiState).tag
        )

        assertEquals(
            expected = 6,
            actual = list.size
        )
    }

    @Test
    fun `should close if there are no tags`() {
        tagFlow.tryEmit(
            Tags.Builder()
                .tags(listOf())
                .build()
        )
        assertEquals(
            actual = navigationEventTracker.lastValue,
            expected = TagBottomSheetNavigationEvent.Close
        )
    }

    @Test
    fun `on tag clicked`() {
        subject.onTagClicked("test")
        verify { listManager.setTag("test") }
        assertEquals(
            actual = navigationEventTracker.lastValue,
            expected = TagBottomSheetNavigationEvent.Close
        )
    }

    @Test
    fun `enter and exit edit mode`() {
        tagFlow.tryEmit(
            Tags.Builder()
                .tags(listOf("test"))
                .build()
        )

        // enter edit mode
        subject.onEditClicked()

        assertEquals(
            expected = "edit tags",
            actual = state.title
        )
        assertEquals(
            expected = View.VISIBLE,
            actual = state.cancelVisibility
        )
        assertEquals(
            expected = View.GONE,
            actual = state.overflowVisibility
        )

        assertEquals(
            expected = TagBottomSheetItemUiState(
                tag = "test",
                editable = true,
                trashVisibility = View.VISIBLE
            ),
            actual = list[1]
        )

        // exit edit mode
        subject.onCancelClicked()

        assertEquals(
            expected = "tags",
            actual = state.title
        )

        assertEquals(
            expected = TagBottomSheetItemUiState(
                tag = "test",
                editable = false,
                trashVisibility = View.GONE
            ),
            actual = list[1]
        )
    }

    @Test
    fun `edit a tag and save`() {
        tagFlow.tryEmit(
            Tags.Builder()
                .tags(listOf("test"))
                .build()
        )

        subject.onTagEdited("test", "new")
        assertEquals(
            expected = View.VISIBLE,
            actual = state.saveVisibility
        )

        subject.onSaveClicked()
        verify {
            tagRepository.editTags(mutableMapOf(
                Pair("test", "new")
            ))
        }

        // edit mode disabled
        assertEquals(
            expected = "tags",
            actual = state.title
        )

        assertEquals(
            expected = TagBottomSheetItemUiState(
                tag = "test",
                editable = false,
                trashVisibility = View.GONE
            ),
            actual = list[1]
        )
    }

    @Test
    fun `delete tag`() {
        subject.onDeleteTagClicked("test")
        assertEquals(
            actual = navigationEventTracker.lastValue,
            expected = TagBottomSheetNavigationEvent.ShowConfirmDelete
        )

        subject.onDeleteTagConfirmed()
        verify { tagRepository.deleteTag("test") }
    }

    @Test
    fun `on dismiss`() {
        subject.onDismissed()
        verify(exactly = 1) { listManager.clearFilters() }

        listManagerSortFilterState.edit { copy(
            tag = "test"
        ) }

        subject.onDismissed()
        verify(exactly = 1) { listManager.clearFilters() }
    }

    @Test
    fun `WHEN not tagged is clicked THEN the list is filtered`() {
        subject.onNotTaggedClicked()

        verify { listManager.addFilter(ItemFilterKey.NOT_TAGGED) }

        assertEquals(
            actual = navigationEventTracker.lastValue,
            expected = TagBottomSheetNavigationEvent.Close
        )
    }
}
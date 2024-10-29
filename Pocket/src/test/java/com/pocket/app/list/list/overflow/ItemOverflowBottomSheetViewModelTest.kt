package com.pocket.app.list.list.overflow

import android.graphics.drawable.Drawable
import com.ideashower.readitlater.R
import com.pocket.BaseCoroutineTest
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.undobar.UndoBar
import com.pocket.app.undobar.UndoableItemAction
import com.pocket.data.models.toDomainItem
import com.pocket.fakes.fakeItem
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.enums.ItemStatus
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.UrlString
import com.pocket.util.DrawableLoader
import com.pocket.util.StringLoader
import io.mockk.coEvery
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemOverflowBottomSheetViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)

    @SpyK
    private val undoable = mockk<UndoBar>(relaxed = true)

    @SpyK
    private val stringLoader = mockk<StringLoader>(relaxed = true)

    @SpyK
    private val drawableLoader = mockk<DrawableLoader>(relaxed = true)

    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)

    private lateinit var subject: ItemOverflowBottomSheetViewModel

    private val state: ItemOverflowBottomSheetUiState
        get() = subject.uiState.value

    private val notViewedDrawable = mockk<Drawable>()
    private val viewedDrawable = mockk<Drawable>()
    private val reAddDrawable = mockk<Drawable>()
    private val archiveDrawable = mockk<Drawable>()

    private val emptyItem = Item.Builder().build()

    @BeforeTest
    fun setup() {
        subject =
            ItemOverflowBottomSheetViewModel(itemRepository, undoable, stringLoader, drawableLoader, tracker)

        coEvery { stringLoader.getString(com.pocket.ui.R.string.ic_mark_as_not_viewed) } returns "mark as not viewed"
        coEvery { stringLoader.getString(com.pocket.ui.R.string.ic_mark_as_viewed) } returns "mark as viewed"
        coEvery { stringLoader.getString(R.string.move_to_my_list) } returns "move to my list"
        coEvery { stringLoader.getString(com.pocket.ui.R.string.ic_archive) } returns "archive"

        coEvery { drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_viewed_not) } returns notViewedDrawable
        coEvery { drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_viewed) } returns viewedDrawable
        coEvery { drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_pkt_re_add_line) } returns reAddDrawable
        coEvery { drawableLoader.getDrawable(com.pocket.ui.R.drawable.ic_pkt_archive_line) } returns archiveDrawable
    }

    @Test
    fun `WHEN the view model is initialized THEN the ui state reflects the items values`() {
        val item = Item.Builder()
            .title("title")
            .top_image_url(UrlString("image.com"))
            .viewed(true)
            .status(ItemStatus.ARCHIVED)
            .build()
        subject.onInitialized(item, SavesTab.SAVES)

        assertEquals(state.title, "title")
        assertEquals(state.imageUrl, "image.com")
        assertEquals(state.viewedText, "mark as not viewed")
        assertEquals(state.viewedIcon, notViewedDrawable)
        assertEquals(state.archiveText, "move to my list")
        assertEquals(state.archiveIcon, reAddDrawable)

        val item2 = Item.Builder()
            .title("title")
            .top_image_url(UrlString("image.com"))
            .viewed(false)
            .status(ItemStatus.UNREAD)
            .build()
        subject.onInitialized(item2, SavesTab.SAVES)

        assertEquals(state.viewedText, "mark as viewed")
        assertEquals(state.viewedIcon, viewedDrawable)
        assertEquals(state.archiveText, "archive")
        assertEquals(state.archiveIcon, archiveDrawable)
    }

    @Test
    fun `WHEN the item has null values THEN nothing breaks`() {
        subject.onInitialized(emptyItem, SavesTab.SAVES)
    }

    @Test
    fun `WHEN mark as viewed is clicked THEN the viewed state is toggled AND the screen closes`() {
        subject.onInitialized(emptyItem, SavesTab.SAVES)
        subject.onViewedClicked()

        verify { itemRepository.toggleViewed(emptyItem) }
        assertEquals(state.screenState, ItemOverflowBottomSheetScreenState.CLOSING)
    }

    @Test
    fun `WHEN edit tags is clicked THEN the screen state is open tags`() {
        subject.onInitialized(emptyItem, SavesTab.SAVES)
        subject.onTagClicked()

        assertEquals(state.screenState, ItemOverflowBottomSheetScreenState.OPEN_TAG_SCREEN)
    }

    @Test
    fun `WHEN archive is clicked THEN the archive state is toggled AND the screen closes`() {
        val item = fakeItem()
        subject.onInitialized(item, SavesTab.SAVES)
        subject.onArchiveClicked()

        verify { undoable.archive(UndoableItemAction.fromDomainItem(item.toDomainItem())) }
        assertEquals(state.screenState, ItemOverflowBottomSheetScreenState.CLOSING)
    }

    @Test
    fun `WHEN re add is clicked THEN the archive state is toggled AND the screen closes`() {
        val item = Item.Builder()
            .status(ItemStatus.ARCHIVED)
            .build()
        subject.onInitialized(item, SavesTab.SAVES)
        subject.onArchiveClicked()

        verify { itemRepository.unArchive(item) }
        assertEquals(state.screenState, ItemOverflowBottomSheetScreenState.CLOSING)
    }

    @Test
    fun `WHEN delete is clicked THEN the screen state is open tags`() {
        subject.onInitialized(emptyItem, SavesTab.SAVES)
        subject.onDeleteClicked()

        assertEquals(state.screenState, ItemOverflowBottomSheetScreenState.CLOSING)
    }
}
package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.ContentOpen
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

object SavesEvents {

    /**
     * Fired when a user clicks the card on Saves
     */
    fun savedCardContentOpen(
        itemUrl: String,
        positionInList: Int,
        savesTab: SavesTab,
    ) = ContentOpen(
        contentEntity = ContentEntity(
            url = itemUrl
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "saves.card.open",
            index = positionInList,
            componentDetail = savesTab.value,
        )
    )

    fun savesChipClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.list.saves",
        )
    )

    fun archiveChipClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.list.archive"
        )
    )

    fun searchChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.search",
            componentDetail = savesTab.value,
        )
    )

    fun searchCloseClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.search.close",
            componentDetail = savesTab.value,
        )
    )

    fun searchDoneClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.search.done",
            componentDetail = savesTab.value,
        )
    )

    fun listenChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.listen",
            componentDetail = savesTab.value,
        )
    )

    fun allChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.all",
            componentDetail = savesTab.value,
        )
    )

    fun taggedChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.tagged",
            componentDetail = savesTab.value,
        )
    )

    fun favoritesChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.favorites",
            componentDetail = savesTab.value,
        )
    )

    fun highlightsChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.highlights",
            componentDetail = savesTab.value,
        )
    )

    fun filterChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.filter",
            componentDetail = savesTab.value,
        )
    )

    fun editChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.edit",
            componentDetail = savesTab.value,
        )
    )

    fun selectedTagChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.selectedtag",
            componentDetail = savesTab.value,
        )
    )

    fun selectedFilterChipClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.selectedfilter",
            componentDetail = savesTab.value,
        )
    )

    fun longReadsFilterClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.longreads",
            componentDetail = savesTab.value,
        )
    )

    fun shortReadsFilterClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.shortreads",
            componentDetail = savesTab.value,
        )
    )

    fun itemTagButtonClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.tag",
            componentDetail = savesTab.value,
        )
    )

    fun itemFavoriteButtonClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.favorite",
            componentDetail = savesTab.value,
        )
    )

    fun itemShareButtonClicked(
        savesTab: SavesTab,
        url: String,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier =  "saves.share",
            componentDetail = savesTab.value,
        ),
        extraEntities = listOf(
            ContentEntity(url),
        ),
    )

    fun itemOverflowButtonClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.overflow",
            componentDetail = savesTab.value,
        )
    )

    fun itemOverflowEditTagsClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.overflow.edittags",
            componentDetail = savesTab.value,
        )
    )

    fun itemOverflowMarkAsViewedClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.overflow.markasviewed",
            componentDetail = savesTab.value,
        )
    )

    fun itemOverflowArchiveClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.overflow.archive",
            componentDetail = savesTab.value,
        )
    )

    fun itemOverflowDeleteClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.overflow.delete",
            componentDetail = savesTab.value,
        )
    )

    fun sortNewestClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.sort.newest",
            componentDetail = savesTab.value,
        )
    )

    fun sortOldestClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.sort.oldest",
            componentDetail = savesTab.value,
        )
    )

    fun sortShortestClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.sort.shortest",
            componentDetail = savesTab.value,
        )
    )

    fun sortLongestClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.sort.longest",
            componentDetail = savesTab.value,
        )
    )

    fun filterViewedClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.viewed",
            componentDetail = savesTab.value,
        )
    )

    fun filterNotViewedClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.notviewed",
            componentDetail = savesTab.value,
        )
    )

    fun filterShortReadsClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.shortreads",
            componentDetail = savesTab.value,
        )
    )

    fun filterLongReadsClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.filter.longreads",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditReAddClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.readd",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditArchiveClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.archive",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditDeleteClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.delete",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditOverflowClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.overflow",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditOverflowFavoriteClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.overflow.favorite",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditOverflowAddTagsClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.overflow.addtags",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditOverflowMarkAsViewedClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.overflow.markasviewed",
            componentDetail = savesTab.value,
        )
    )

    fun bulkEditOverflowMarkAsNotViewedClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.bulk.overflow.markasnotviewed",
            componentDetail = savesTab.value,
        )
    )

    fun tagsOverflowClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.tags.overflow",
            componentDetail = savesTab.value,
        )
    )

    fun tagsCancelEditClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.tags.canceledit",
            componentDetail = savesTab.value,
        )
    )

    fun tagsSaveChangesClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.tags.savechanges",
            componentDetail = savesTab.value,
        )
    )

    fun tagsDeleteClicked(
        savesTab: SavesTab,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.tags.delete",
            componentDetail = savesTab.value,
        )
    )

    /**
     * Fired when viewing a save on Saves
     */
    fun saveImpression(
        positionInList: Int,
        itemUrl: String,
        savesTab: SavesTab,
    ) = Impression(
        component = Impression.Component.Content(
            contentEntity = ContentEntity(
                url = itemUrl
            )
        ),
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "saves.card.impression",
            index = positionInList,
            componentDetail = savesTab.value,
        )
    )

    /** Fired when a user clicks the add/plus button. */
    fun addButtonClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.add-item",
        ),
    )

    /** Fired when the add URL drawer is shown. */
    fun addUrlBottomSheetShown() = Impression(
        component = Impression.Component.Screen,
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "saves.addItem.open",
        ),
    )

    /**
     * Fired when Save to Pocket button is clicked in the add URL drawer
     * and the input contains a valid URL.
     */
    fun addUrlBottomSheetSaveSucceeded() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.addItem.success",
        ),
    )

    /**
     * Fired when Save to Pocket button is clicked in the add URL drawer
     * and the input doesn't contain a valid URL.
     */
    fun addUrlBottomSheetSaveFailed() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.addItem.fail",
        ),
    )

    fun clipboardPromptSaveClicked(url: String) = Engagement(
        type = Engagement.Type.Save(ContentEntity(url)),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "clipboard.prompt.save",
        ),
    )

    /** Fired when someone presses the "Sign up or sign in" button on the signed out empty saves view. */
    fun emptySignedOutButtonClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "saves.empty.signed-out.button",
        ),
    )
}

enum class SavesTab(val value: String) {
    SAVES("saves"),
    ARCHIVE("archive")
}

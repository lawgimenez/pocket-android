package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement

object ReaderToolbarEvents {

    /**
     * Fired when a user clicks the up button (back button) in the reader toolbar
     */
    fun upClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.up",
        )
    )

    /**
     * Fired when a user clicks the save button from within the reader toolbar
     */
    fun saveClicked(
        url: String
    ) = Engagement(
        type = Engagement.Type.Save(
            contentEntity = ContentEntity(
                url = url,
            ),
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.save",
        )
    )

    /**
     * Fired when a user clicks the archive button from within the reader toolbar
     */
    fun archiveClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.archive",
        )
    )

    /**
     * Fired when a user clicks the re-add button from within the reader toolbar
     */
    fun reAddClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.readd",
        )
    )

    /**
     * Fired when a user clicks the listen button from within the reader toolbar
     */
    fun listenClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.listen",
        )
    )

    /**
     * Fired when a user clicks the share button from within the reader toolbar
     */
    fun shareClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.share",
        )
    )

    /**
     * Fired when a user clicks the overflow button from within the reader toolbar
     */
    fun overflowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.overflow",
        )
    )

    /**
     * Fired when a user clicks the text settings button from within the reader toolbar overflow menu
     */
    fun textSettingsClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.text_settings",
        )
    )

    /**
     * Fired when a user clicks the view original button from within the reader toolbar overflow menu
     */
    fun viewOriginalClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.view_original",
        )
    )

    /**
     * Fired when a user clicks the refresh button from within the reader toolbar overflow menu
     */
    fun refreshClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.refresh",
        )
    )

    /**
     * Fired when a user clicks the find in page button from within the reader toolbar overflow menu
     */
    fun findInPageClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.find_in_page",
        )
    )

    /**
     * Fired when a user clicks the favorite button from within the reader toolbar overflow menu
     */
    fun favoriteClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.favorite",
        )
    )

    /**
     * Fired when a user clicks the unfavorite button from within the reader toolbar overflow menu
     */
    fun unfavoriteClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.unfavorite",
        )
    )

    /**
     * Fired when a user clicks the add tags button from within the reader toolbar overflow menu
     */
    fun addTagsClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.addTags",
        )
    )

    /**
     * Fired when a user clicks the highlights button from within the reader toolbar overflow menu
     */
    fun highlightsClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.highlights",
        )
    )

    /**
     * Fired when a user clicks the mark as viewed button from within the reader toolbar overflow menu
     */
    fun markAsViewedClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.mark_as_viewed",
        )
    )

    /**
     * Fired when a user clicks the delete button from within the reader toolbar overflow menu
     */
    fun deleteClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.delete",
        )
    )

    /**
     * Fired when a user clicks the report button from within the reader toolbar overflow menu
     */
    fun reportClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.toolbar.report",
        )
    )
}
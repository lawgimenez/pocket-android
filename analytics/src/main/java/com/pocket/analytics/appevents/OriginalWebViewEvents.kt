package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

/**
 * Events in custom chrome tabs in the reader
 */
object OriginalWebViewEvents {

    /**
     * Fired when a user opens an article in the original web view (on android, custom chrome tabs)
     */
    fun screenView() = Impression(
        component = Impression.Component.Screen,
        requirement = Impression.Requirement.INSTANT,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "tabs.screen"
        ),
    )

    /**
     * Fired when a user clicks the pocket menu from within the original web view (custom chrome tabs)
     */
    fun pocketMenuClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.menu.click",
        )
    )

    /**
     * Fired when a user clicks save from within the original web view (custom chrome tabs)
     */
    fun saveClicked(
        url: String
    ) = Engagement(
        type = Engagement.Type.Save(
            contentEntity = ContentEntity(
                url = url,
            )
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.save",
        )
    )

    /**
     * Fired when a user clicks archive from within the original web view (custom chrome tabs)
     */
    fun archiveClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.archive",
        )
    )

    /**
     * Fired when a user clicks re-add from within the original web view (custom chrome tabs)
     */
    fun reAddClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.readd",
        )
    )

    /**
     * Fired when a user clicks listen from within the original web view (custom chrome tabs)
     */
    fun listenClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.listen",
        )
    )

    /**
     * Fired when a user clicks share from within the original web view (custom chrome tabs)
     */
    fun shareClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.share",
        )
    )

    /**
     * Fired when a user clicks favorite from within the original web view (custom chrome tabs)
     */
    fun favoriteClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.favorite",
        )
    )

    /**
     * Fired when a user clicks add tags from within the original web view (custom chrome tabs)
     */
    fun addTagsClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.add_tags",
        )
    )

    /**
     * Fired when a user clicks mark as viewed from within the original web view (custom chrome tabs)
     */
    fun markAsViewedClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.mark_as_viewed",
        )
    )

    /**
     * Fired when a user clicks delete from within the original web view (custom chrome tabs)
     */
    fun deleteClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.delete",
        )
    )

    /**
     * Fired when a user clicks switch to article view from within the original web view (custom chrome tabs)
     */
    fun switchToArticleClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "tabs.overlay.switch_to_article_view",
        )
    )
}
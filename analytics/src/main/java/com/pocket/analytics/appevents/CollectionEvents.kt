package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.ContentOpen
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

object CollectionEvents {

    /**
     * Fired when a user clicks the overflow button on an article from within the collection screen
     */
    fun recommendationOverflowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "collection.story.overflow",
        )
    )

    /**
     * Fired when a user Saves a card on a collection
     */
    fun recommendationSaveClicked(
        url: String
    ) = Engagement(
        type = Engagement.Type.Save(
            contentEntity = ContentEntity(
                url = url
            )
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "collection.story.save",
        )
    )

    /**
     * Fired when a user views the collection screen
     */
    fun screenView() = Impression(
        component = Impression.Component.Screen,
        requirement = Impression.Requirement.INSTANT,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "collection.screen"
        )
    )

    /**
     * Fired when a user clicks a card on a collection
     */
    fun contentOpen(
        url: String,
    ) = ContentOpen(
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url,
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "collection.story.open",
        ),
    )
}
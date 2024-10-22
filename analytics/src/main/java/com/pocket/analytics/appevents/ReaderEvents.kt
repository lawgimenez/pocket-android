package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.ContentOpen
import com.pocket.analytics.events.Engagement

object ReaderEvents {

    /**
     * Fired when a user opens an article by clicking previous within the reader
     */
    fun previousClicked(
        url: String
    ) = ContentOpen(
        destination = ContentOpen.Destination.INTERNAL,
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url,
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.previous.open",
        ),
    )

    /**
     * Fired when a user opens an article by clicking next within the reader
     */
    fun nextClicked(
        url: String
    ) = ContentOpen(
        destination = ContentOpen.Destination.INTERNAL,
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url,
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "reader.next.open",
        ),
    )

    /**
     * Fired when a user opens an article via deeplink, excluding pocket.co links
     */
    fun deeplinkContentOpen(
        url: String,
    ) = ContentOpen(
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url,
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.CARD,
            identifier = "article.deeplink.open",
        ),
    )

    /**
     * Fired when a user opens an article via pocket.co deeplink
     */
    fun pocketCoContentOpen(
        url: String,
    ) = ContentOpen(
        trigger = ContentOpen.Trigger.CLICK,
        contentEntity = ContentEntity(
            url = url
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.LINK,
            identifier = "article.deeplink.shortlink.open",
        ),
    )
}
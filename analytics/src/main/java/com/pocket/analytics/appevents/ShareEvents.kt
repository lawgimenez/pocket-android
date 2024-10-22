package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement

object ShareEvents {
    /** Fired when a user clicks an app in the share sheet. */
    fun shareSheetAppClicked(
        appPackageName: String,
        url: String?,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "share.extension",
            componentDetail = appPackageName,
        ),
        extraEntities = buildList {
            if (url != null) {
                add(ContentEntity(url))
            }
        },
    )
}

package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement

object RecommendationBottomSheetEvents {

    /**
     * Fired when a user clicks the share button from within a recommendation's overflow menu (the overflow menu is in a bottom sheet)
     */
    fun shareClicked(
        url: String,
        itemTitle: String,
        corpusRecommendationId: String?,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "recommendation.share",
            componentDetail = itemTitle,
        ),
        extraEntities = buildList {
            add(ContentEntity(url = url))
            withCorpusRecommendationEntity(corpusRecommendationId)
        }
    )

    /**
     * Fired when a user clicks the report button from within a recommendation's overflow menu (the overflow menu is in a bottom sheet)
     */
    fun reportClicked(
        url: String,
        itemTitle: String,
        corpusRecommendationId: String?,
    ) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "recommendation.report",
            componentDetail = itemTitle,
        ),
        extraEntities = buildList {
            add(ContentEntity(url = url))
            withCorpusRecommendationEntity(corpusRecommendationId)
        }
    )
}
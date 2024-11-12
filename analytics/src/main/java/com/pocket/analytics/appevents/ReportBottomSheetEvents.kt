package com.pocket.analytics.appevents

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.ReportEntity
import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement

object ReportBottomSheetEvents {

    /**
     * Fired when a user clicks the submit button in the report bottom sheet
     */
    fun reportClicked(
        url: String,
        reportReason: ReportEntity.Reason,
        reportComment: String?,
        corpusRecommendationId: String?,
    ) = Engagement(
        type = Engagement.Type.Report(
            reportEntity = ReportEntity(
                reason = reportReason,
                comment = reportComment,
            ),
            contentEntity = ContentEntity(
                url = url,
            )
        ),
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "report.submit",
        ),
        extraEntities = buildList { withCorpusRecommendationEntity(corpusRecommendationId = corpusRecommendationId) }
    )
}
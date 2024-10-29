package com.pocket.analytics.appevents

import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

object MainEvents {
    /** Fired when a user sees the exit survey banner. */
    fun accountDeleteBannerImpression() = Impression(
        component = Impression.Component.Ui,
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.DIALOG,
            identifier = "global-nav.accountdelete.banner",
        ),
    )

    /** Fired when a user clicks the exit survey banner. */
    fun accountDeleteExitSurveyClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.accountdelete.banner.exitsurvey.click",
        ),
    )

    /** Fired when a user sees the exit survey. */
    fun accountDeleteExitSurveyImpression() = Impression(
        component = Impression.Component.Ui,
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "global-nav.accountdelete.exitsurvey"
        ),
    )
}

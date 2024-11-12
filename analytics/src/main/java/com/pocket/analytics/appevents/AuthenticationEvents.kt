package com.pocket.analytics.appevents

import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement

object AuthenticationEvents {
    fun continueButtonClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "login.continue.tapped",
        ),
    )

    fun loginComplete() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "login.login.complete",
        ),
    )

    fun signupComplete() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "login.signup.complete",
        ),
    )
}

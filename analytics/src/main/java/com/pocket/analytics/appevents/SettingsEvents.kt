package com.pocket.analytics.appevents

import com.pocket.analytics.entities.UiEntity
import com.pocket.analytics.events.Engagement
import com.pocket.analytics.events.Impression

object SettingsEvents {
    /** Account management screen settings row tapped. */
    fun accountManagementRowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.account-management.click",
        )
    )

    /** Account management screen viewed. */
    fun accountManagementImpression() = Impression(
        component = Impression.Component.Screen,
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "global-nav.settings.account-management",
        ),
    )

    /** Delete user settings row tapped. */
    fun deleteAccountRowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.account-management.delete.click",
        ),
    )

    /** Delete confirmation screen impression. */
    fun deleteAccountConfirmationImpression() = Impression(
        component = Impression.Component.Screen,
        requirement = Impression.Requirement.VIEWABLE,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "global-nav.settings.account-management.delete",
        ),
    )

    /** Delete confirmation tapped. */
    fun deleteConfirmationClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.account-management.delete.confirm.click",
        ),
    )

    /** Delete cancel tapped. */
    fun deleteDismissed() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.account-management.delete.dismissed",
        ),
    )

        /** App icon screen settings row tapped. */
    fun appIconRowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.appIcon.tap",
        )
    )

    fun appIconSwitcherImpression() = Impression(
        component = Impression.Component.Ui,
        requirement = Impression.Requirement.INSTANT,
        uiEntity = UiEntity(
            type = UiEntity.Type.SCREEN,
            identifier = "global-nav.settings.iconSelector",
        ),
    )

    fun appIconChanged(iconName: String) = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.iconSelector.iconChanged",
            componentDetail = iconName,
        ),
    )

    fun logoutRowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.logout"
        ),
    )

    fun logoutConfirmClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.logout-confirmed"
        ),
    )

    fun loginRowClicked() = Engagement(
        uiEntity = UiEntity(
            type = UiEntity.Type.BUTTON,
            identifier = "global-nav.settings.login"
        ),
    )
}

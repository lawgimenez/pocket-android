package com.pocket.app.settings.account

import android.app.Activity
import com.ideashower.readitlater.R
import com.pocket.app.App
import com.pocket.ui.view.notification.PktSnackbar

object DeletedAccountConfirmationSnackbar {
    private const val ExitSurveyUrl = "https://survey.alchemer.com/s3/7169338/Pocket-exit-survey"

    fun make(
        activity: Activity,
        onExitSurveyActionClick: ((Boolean) -> Unit)? = null,
    ): PktSnackbar {
        val snackbar = PktSnackbar.make(
            activity,
            PktSnackbar.Type.DEFAULT_DISMISSABLE,
            null,
            null,
        )
        snackbar.bind()
            .title(activity.getString(R.string.setting_delete_account_toast_title))
            .message(activity.getString(R.string.setting_delete_account_toast_message))
            .onAction(R.string.setting_delete_account_toast_button) {
                val shown = App.viewUrl(activity, ExitSurveyUrl)
                snackbar.bind().dismiss()
                if (onExitSurveyActionClick != null) {
                    onExitSurveyActionClick(shown)
                }
            }
        return snackbar
    }
}

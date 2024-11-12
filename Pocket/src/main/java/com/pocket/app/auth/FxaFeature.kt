package com.pocket.app.auth

import android.app.Activity
import com.ideashower.readitlater.R
import com.pocket.ui.view.notification.PktSnackbar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * If a user logs in and goes through the migration process of converting their old
 * pocket login credentials to a firefox account, we show a popup saying their account
 * has been migrated successfully
 *
 * When can we remove this popup?  FXA initially happened Q1 of 2022.
 */
@Singleton
class FxaFeature @Inject constructor() {

    var shouldShowMigrationMessage = false

    fun showMigrationMessage(activity: Activity?) {
        if (shouldShowMigrationMessage) {
            shouldShowMigrationMessage = false
            PktSnackbar.make(
                activity,
                PktSnackbar.Type.DEFAULT_DISMISSABLE,
                activity?.getString(R.string.fxa_account_migrated),
                null
            ).show()
        }
    }

}
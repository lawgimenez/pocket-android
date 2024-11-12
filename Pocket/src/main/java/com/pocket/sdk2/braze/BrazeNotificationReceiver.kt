package com.pocket.sdk2.braze

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.braze.Constants
import com.braze.push.BrazeNotificationUtils
import com.pocket.app.App
import com.pocket.app.AppOpen
import com.pocket.sdk.util.DeepLinks
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BrazeNotificationReceiver: BroadcastReceiver() {

    @Inject
    lateinit var appOpen: AppOpen

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Constants.BRAZE_PUSH_INTENT_NOTIFICATION_RECEIVED -> {
                // do nothing for now
            }
            Constants.BRAZE_PUSH_INTENT_NOTIFICATION_OPENED -> {
                val deepLink = intent.getStringExtra(Constants.BRAZE_PUSH_DEEP_LINK_KEY)
                if (!deepLink.isNullOrBlank()) {
                    setValuesForAppOpenTracking(deepLink)
                    val newIntent = DeepLinks.Parser.parseLinkOpenedInPocket(context, deepLink, null)
                    if (newIntent != null) {
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(newIntent)
                    } else {
                        App.viewUrl(context, deepLink)
                    }
                    return
                }
                // default braze behavior
                BrazeNotificationUtils.routeUserWithNotificationOpenedIntent(context, intent)
            }
            Constants.BRAZE_PUSH_INTENT_NOTIFICATION_DELETED -> {
                // do nothing for now
            }
        }
    }

    private fun setValuesForAppOpenTracking(deepLink: String) {
        appOpen.deepLink = deepLink
    }
}

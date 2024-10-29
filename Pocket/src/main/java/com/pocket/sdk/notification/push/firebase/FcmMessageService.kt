package com.pocket.sdk.notification.push.firebase

import com.braze.push.BrazeFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pocket.app.App

/**
 * When background syncing is set to "Instant" it means that the server will send a silent push notification to the
 * device any time there are changes to sync. When the app gets this silent firebase cloud message, it kicks off a sync.
 * This service is what receives the silent pushes.
 */
class FcmMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val app = App.from(this)
        val data: Map<String, String> = remoteMessage.data
        val isPinpointPushNotification = (data[PUSH_TITLE] != null || data[PUSH_BODY] != null)
        if (BrazeFirebaseMessagingService.handleBrazeRemoteMessage(this, remoteMessage)) {
            // notification was handled by Braze.  Nothing else is required
        } else if (isPinpointPushNotification) {
            // leaving empty until fully removed.  Differentiating from an instant sync notification
        } else {
            // instant sync notification
            if (app.backgroundSync().isInstantSync) {
                app.backgroundSync().scheduleSyncFromPush()
            }
        }
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        App.from(this).push().invalidate()
    }

    companion object {
        private const val PUSH_TITLE = "pinpoint.notification.title"
        private const val PUSH_BODY = "pinpoint.notification.body"
    }
}
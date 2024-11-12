package com.pocket.sdk.notification

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ideashower.readitlater.R
import com.pocket.analytics.EngagementType
import com.pocket.analytics.ExternalView
import com.pocket.analytics.Tracker
import com.pocket.analytics.UiEntityType
import com.pocket.app.Jobs
import com.pocket.app.build.Versioning
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier
import com.pocket.util.android.ApiLevel
import com.pocket.util.prefs.BooleanPreference
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.StringPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android notification APIs like [NotificationManagerCompat].
 * Provides methods for pushing Android system notifications in a standardized way.
 * Choose one of the methods to get a builder appropriate for your use case.
 */
@Singleton
class SystemNotifications @Inject constructor(
    @ApplicationContext context: Context,
    prefs: Preferences,
    jobs: Jobs,
    tracker: Tracker,
    versioning: Versioning,
) {

    val sound: StringPreference = prefs.forUser("notifySound", null as String?)
    val lights: BooleanPreference = prefs.forUser("notifyLights", true)

    private val impl: Impl =
        if (ApiLevel.isPreOreo()) PreOreo(context, sound, lights) else Oreo(context, sound, lights)

    init {
        jobs.registerCreator(DeviceLevelNotificationSettingWorker::class.java) { c, workerParams ->
            DeviceLevelNotificationSettingWorker(c, workerParams, this, tracker)
        }
        if (versioning.isFirstRun || versioning.upgraded(7, 48, 0, 0)) {
            jobs.schedulePeriodic(
                DeviceLevelNotificationSettingWorker::class.java,
                AlarmManager.INTERVAL_DAY
            )
        }
    }

    /**
     * For functional, important, app related notifications that provide the user controls
     * over the features of the app or alerts them to some kind of background process.
     */
    fun newDefaultBuilder() = impl.app()

    /** Are notifications, in general, enabled in system app settings. */
    fun areNotificationsEnabled() = impl.areNotificationsEnabled()

    enum class Channel(val id: String, val nameResId: Int, val descriptionResId: Int?) {
        /** @see .newDefaultBuilder for a use case */
        APP("app", R.string.nt_channel_functional_name, R.string.nt_channel_functional_description),

        @Deprecated("No longer needed and in replace of verbose notification channels")
        COMMUNICATION("comms", R.string.nt_channel_alerts_name, R.string.nt_channel_alerts_description),

        ARTICLE_RECOMMENDATIONS("com_pocket_article_recommendations", R.string.nt_channel_article_recommendations, R.string.nt_channel_article_recommendations_description),

        LEGAL_UPDATES("com_pocket_legal_updates", R.string.nt_channel_legal_updates, null),

        /** @see .newDefaultAlertBuilder for a use case */
        NEW_FEATURES("com_pocket_new_features", R.string.nt_channel_new_features, null);
    }

    private interface Impl {
        fun app(): NotificationCompat.Builder

        /** Check if notifications are enabled in system settings. */
        fun areNotificationsEnabled(): Boolean
    }

    private open class PreOreo(
        private val context: Context,
        private val sound: StringPreference,
        private val lights: BooleanPreference,
    ) : Impl {
        override fun app(): NotificationCompat.Builder {
            return NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setWhen(System.currentTimeMillis())
                .setColor(ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_coral_2))
        }

        override fun areNotificationsEnabled(): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private class Oreo(
        private val context: Context,
        sound: StringPreference,
        lights: BooleanPreference,
    ) : PreOreo(context, sound, lights) {
        init {
            for (channel in Channel.values()) {
                channel(channel)
            }
            removeChannel(Channel.COMMUNICATION.id)
        }

        override fun app(): NotificationCompat.Builder {
            return newBuilder(Channel.APP)
        }

        override fun areNotificationsEnabled(): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun newBuilder(channel: Channel): NotificationCompat.Builder {
            return NotificationCompat.Builder(context, channel.id)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setWhen(System.currentTimeMillis())
                .setColor(ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_coral_2))
        }

        private fun removeChannel(channelId: String) {
            val notificationManager = NotificationManagerCompat.from(context)
            val id: String = channelId
            notificationManager.deleteNotificationChannel(id)
        }

        private fun channel(type: Channel): String {
            // Apparently it is considered ok to "recreate" the channel everytime, as the settings won't override once created. https://developer.android.com/preview/features/notification-channels.html#CreatingChannels
            val id = type.id
            val name = context.getString(type.nameResId)
            val description = type.descriptionResId?.let { context.getString(it) }
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(id, name, importance)
            channel.description = description
            when (type) {
                Channel.APP -> {
                    // NOTE: I disagree with this being "LOW" but this seems like the only way to have it not make a sound by default.
                    channel.importance = NotificationManager.IMPORTANCE_LOW
                    channel.enableLights(false)
                    channel.enableVibration(false)
                }
                Channel.COMMUNICATION -> {
                    channel.importance = NotificationManager.IMPORTANCE_DEFAULT
                    channel.enableLights(true)
                    channel.lightColor = context.getColor(com.pocket.ui.R.color.pkt_coral_2)
                    channel.enableVibration(false)
                }
                Channel.ARTICLE_RECOMMENDATIONS -> {
                    channel.importance = NotificationManager.IMPORTANCE_DEFAULT
                    channel.enableLights(false)
                    channel.enableVibration(false)
                }
                Channel.LEGAL_UPDATES -> {
                    channel.importance = NotificationManager.IMPORTANCE_DEFAULT
                    channel.enableLights(false)
                    channel.enableVibration(false)
                }
                Channel.NEW_FEATURES -> {
                    channel.importance = NotificationManager.IMPORTANCE_DEFAULT
                    channel.enableLights(false)
                    channel.enableVibration(false)
                }
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
            return id
        }
    }
}

@SuppressLint("WorkerHasAPublicModifier") // We have a custom WorkerFactory in Jobs which allows private Workers.
private class DeviceLevelNotificationSettingWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notifications: SystemNotifications,
    private val tracker: Tracker,
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Check if notifications are enabled for the push notifications channel.
        val areNotificationsEnabled = notifications.areNotificationsEnabled()

        tracker.trackEngagement(
            ExternalView(
                UiEntityIdentifier.DEVICE_NOTIFICATIONS_ENABLED.value,
                UiEntityType.BUTTON,
            ),
            EngagementType.GENERAL,
            areNotificationsEnabled.toString()
        )

        return Result.success()
    }
}

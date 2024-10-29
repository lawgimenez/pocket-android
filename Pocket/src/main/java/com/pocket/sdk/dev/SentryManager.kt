package com.pocket.sdk.dev

import android.content.Context
import android.util.Log
import com.ideashower.readitlater.BuildConfig
import com.pocket.app.AppScope
import com.pocket.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.protocol.User
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryManager @Inject constructor(
    @ApplicationContext context: Context,
    private val userRepository: UserRepository,
    appScope: AppScope,
) {

    init {
        SentryAndroid.init(context) { options: SentryAndroidOptions ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = BuildConfig.FLAVOR

            // enable view hierarchy for crashes
            options.isAttachViewHierarchy = true

            // Disabling for now due to privacy reasons (screenshot for crashes)
            options.isAttachScreenshot = false

            // enable automatic breadcrumbs for user interactions (clicks, swipes, scrolls)
            options.isEnableUserInteractionTracing = true

            options.setBeforeSend { event, hint ->
                Log.d("Sentry", "Sending Sentry event with type: ${event.exceptions?.firstOrNull()?.type}")
                event
            }
        }
        Log.d("Sentry", "Sentry initialized")
        appScope.launch {
            userRepository.getLoginInfoAsFlow().collect { loginInfo ->
                Sentry.setUser(
                    User().apply {
                        id = loginInfo.account?.user_id
                    }
                )
                Log.d("Sentry", "Sentry user set")
            }
        }
    }
}
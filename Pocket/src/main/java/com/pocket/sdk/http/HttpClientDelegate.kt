package com.pocket.sdk.http

import com.pocket.app.AppLifecycle
import com.pocket.app.AppLifecycle.LogoutPolicy
import com.pocket.app.AppLifecycleEventDispatcher
import com.pocket.app.AppMode
import com.pocket.sdk.api.PocketServer
import com.pocket.sdk.http.sentry.ExcludedTargets
import com.pocket.sdk.network.eclectic.EclecticHttp
import com.pocket.sdk.network.toEclecticOkHttpClient
import com.pocket.util.prefs.Preferences
import io.sentry.okhttp.SentryOkHttpEventListener
import io.sentry.okhttp.SentryOkHttpInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a singleton http client for network access.
 * On logout, all pending requests are cancelled.
 */
@Singleton
class HttpClientDelegate
@Inject constructor(
    private val mode: AppMode,
    private val server: PocketServer,
    private val status: NetworkStatus,
    prefs: Preferences,
    dispatcher: AppLifecycleEventDispatcher,
) : AppLifecycle {
    private val loggingLevel = prefs.forApp(
        "dcfig_lg_http",
        EclecticHttp.Logging::class.java,
        if (mode.isDevBuild) EclecticHttp.Logging.API else EclecticHttp.Logging.NONE
    )
    private var client: EclecticHttp? = null

    init {
        dispatcher.registerAppLifecycleObserver(this)
    }

    fun getClient(): EclecticHttp? {
        if (client == null) {
            client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addSentry()
                .addLogging()
                .build()
                .toEclecticOkHttpClient()
        }
        return client
    }

    private fun OkHttpClient.Builder.addSentry() = apply {
        val regex = ExcludedTargets()
            .apply {
                exclude("https://api.getpocket.com/graphql", ExcludedTargets.Mode.Exact) // graph
                exclude("https://text.getpocket.com/v3beta/mobile", ExcludedTargets.Mode.Exact) // parser
                exclude("https://api.getpocket.com/v3/send", ExcludedTargets.Mode.Exact) // v3 proxy
                exclude("https://api.getpocket.com/v3/get", ExcludedTargets.Mode.Exact) // v3 proxy
                exclude("https://api.getpocket.com/v3/fetch", ExcludedTargets.Mode.Exact) // v3 proxy
                exclude("https://pocket-image-cache.com", ExcludedTargets.Mode.Prefix) // image cache
            }
            .toRegex()
        addInterceptor(SentryOkHttpInterceptor(failedRequestTargets = listOf(regex)))
        eventListener(SentryOkHttpEventListener())
    }

    private fun OkHttpClient.Builder.addLogging() = apply {
        if (mode.isForInternalCompanyOnly) {
            addInterceptor(LoggingInterceptor())
        }
    }

    fun getLoggingLevel(): EclecticHttp.Logging {
        return if (mode.isForInternalCompanyOnly) {
            loggingLevel.get()
        } else {
            EclecticHttp.Logging.NONE
        }
    }

    fun setLoggingLevel(value: EclecticHttp.Logging) {
        if (!mode.isForInternalCompanyOnly) return
        loggingLevel.set(value)
    }

    fun status(): NetworkStatus {
        return status
    }

    override fun onLogoutStarted(): LogoutPolicy {
        return object : LogoutPolicy {
            override fun stopModifyingUserData() {
                try {
                    client?.release()
                } catch (ignore: Throwable) {
                }
            }

            override fun deleteUserData() {}

            override fun restart() {
                client = null
            }

            override fun onLoggedOut() {}
        }
    }

    private inner class LoggingInterceptor : Interceptor {
        private val wrapped = HttpLoggingInterceptor()

        override fun intercept(chain: Interceptor.Chain): Response {
            when (getLoggingLevel()) {
                EclecticHttp.Logging.NONE -> wrapped.setLevel(HttpLoggingInterceptor.Level.NONE)
                EclecticHttp.Logging.API -> wrapped.setLevel(
                    if (chain.request().url.host == server.api().toHttpUrlOrNull()?.host) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                )
                EclecticHttp.Logging.EVERYTHING -> wrapped.setLevel(HttpLoggingInterceptor.Level.BODY)
            }
            return wrapped.intercept(chain)
        }
    }
}

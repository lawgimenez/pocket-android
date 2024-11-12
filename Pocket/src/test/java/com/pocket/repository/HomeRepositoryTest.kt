package com.pocket.repository

import app.cash.turbine.test
import com.pocket.data.models.DomainSlate
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.endpoint.AppInfo
import com.pocket.sdk.api.endpoint.DeviceInfo
import com.pocket.sdk.api.source.LoggingPocket
import com.pocket.sdk.api.source.PocketRemoteSource
import com.pocket.sdk.network.toEclecticOkHttpClient
import com.pocket.util.prefs.MemoryPrefStore
import com.pocket.util.prefs.Prefs
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeRepositoryTest {
    /**
     * This is not a unit test. But it is useful to manually test against production
     * without launching the app and setting up everything by interacting with the device.
     *
     * To run it, comment out the @Ignore annotation and fill in test account credentials.
     *
     * The first English slate might be "Recommended Reads" instead of "For You" if you haven't
     * selected preferred topics on your test account. And they can change as we make updates to
     * Home on production. So take these asserts with a grain of salt and feel free to change them
     * or comment out, but they were the quickest way I figured out to spot check if the response
     * is what I expect it to be.
     * 
     * Another way is to put a breakpoint after `awaitItem()` and inspect the response in
     * the debugger.
     */
    @Ignore("not a unit test")
    @Test fun hitsProd() = runTest {
        val pocket = pocket().apply {
            val usernameOrEmail = "your test username or email"
            val password = "your test password"
            user().login(usernameOrEmail, password, Pocket.AuthenticationExtras(null, null, null))
        }
        val repository = HomeRepository(
            pocket,
            Prefs(MemoryPrefStore(), MemoryPrefStore()),
        )
        val usLocale = "en-US"
        val germanLocale = "de-AT"

        repository.getLineup(usLocale).test {
            assertFalse(repository.hasCachedLineup(null))
            assertFalse(repository.hasCachedLineup(usLocale))
            assertFalse(repository.hasCachedLineup(germanLocale))

            repository.refreshLineup(usLocale)
            assertTrue(repository.hasCachedLineup(usLocale))
            assertFalse(repository.hasCachedLineup(germanLocale))

            val slates: List<DomainSlate> = awaitItem()
            assertEquals("For You", slates.first().title)

            val remainingEvents = cancelAndConsumeRemainingEvents()
            assertEquals(emptyList(), remainingEvents)
        }

        repository.getLineup(germanLocale).test {
            assertFalse(repository.hasCachedLineup(germanLocale))
            assertTrue(repository.hasCachedLineup(usLocale))

            repository.refreshLineup(germanLocale)
            assertTrue(repository.hasCachedLineup(germanLocale))
            assertFalse(repository.hasCachedLineup(usLocale))

            val slates = awaitItem()
            assertEquals("Empfohlene Artikel", slates.first().title)

            val remainingEvents = cancelAndConsumeRemainingEvents()
            assertEquals(emptyList(), remainingEvents)
        }
    }

    private fun pocket(debug: Boolean = false): Pocket {
        val appInfo = AppInfo(
            "5513-8646141fb5902c766272e74d",
            "Pocket",
            "Free",
            "6.7.0.0",
            "Google",
            "Google"
        )
        val deviceInfo = DeviceInfo("Test", "Test", "Test", "Test", "Test", "mobile", "en-us", null)
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
            .toEclecticOkHttpClient()
        val config =
            Pocket.Config.Builder(UUID.randomUUID().toString(), appInfo, deviceInfo)
                .remote(PocketRemoteSource(httpClient))
                .build()
        return LoggingPocket.debugCompact(config, { if (debug) println(it) })
    }
}

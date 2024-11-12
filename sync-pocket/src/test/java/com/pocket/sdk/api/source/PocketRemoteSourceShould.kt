package com.pocket.sdk.api.source

import com.pocket.sdk.api.endpoint.AppInfo
import com.pocket.sdk.api.endpoint.Credentials
import com.pocket.sdk.api.endpoint.DeviceInfo
import com.pocket.sdk.api.generated.action.PvWt
import com.pocket.sdk.api.generated.action.TrackAppOpen_1_0_0
import com.pocket.sdk.api.generated.enums.SnowplowAppId.POCKET_ANDROID_DEV
import com.pocket.sdk.api.generated.thing.ApiUserEntity_1_0_1
import com.pocket.sdk.api.generated.thing.Guid
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.network.toEclecticOkHttpClient
import com.pocket.sync.source.result.Status
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val APP_INFO =
    AppInfo("5513-8646141fb5902c766272e74d", "Pocket", "Free", "6.7.0.0", "Google", "Google")
val DEVICE_INFO = DeviceInfo("Test", "Test", "Test", "Test", "Test", "mobile", "en-us", null)

class PocketRemoteSourceShould {

    private val httpClient = OkHttpClient().toEclecticOkHttpClient()
    private val credentials = Credentials(null, "fake-guid", DEVICE_INFO, APP_INFO)

    private val v3Url = MockWebServer()
        .apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse()
                        .setResponseCode(200)
                        .addHeader("X-Source", "Pocket")
                        .setBody(
                            when (request.path) {
                                "//v3/guid" -> """{"guid": "fake-guid"}"""
                                "//v3/send_guid" -> """
                                    {
                                        "action_results": [ true ]
                                    }
                                """.trimIndent()
                                else -> "{}"
                            }
                        )
                }
            }
        }
        .url("")
        .toString()

    private val unreachableUrl = MockWebServer()
        .apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    // Fail the connection. Simulates connections issues or adblock.
                    return MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                }
            }
        }
        .url("")
        .toString()
    private val unreachableSnowplow = SnowplowSource.Config(unreachableUrl, SnowplowSource.DEV_POST_PATH, POCKET_ANDROID_DEV)

    private val guid = Guid.Builder().build()
    private val v3Action = PvWt.Builder().time(Timestamp.now()).build()
    private val snowplowEvent = TrackAppOpen_1_0_0.Builder()
        .time(Timestamp.now())
        .entities(listOf(ApiUserEntity_1_0_1.Builder().api_id(42).build()))
        .build()

    @Test
    fun `WHEN snowplow is the only action AND it fails THEN we still handle the rest of the sync AND snowplow events are retryable`() {
        val source = PocketRemoteSource(
            httpClient,
            v3Url,
            v3Url,
            unreachableSnowplow
        ).apply {
            setCredentials(credentials)
        }

        val result = source.syncFull(
            guid,
            snowplowEvent,
        )

        result.result_a[snowplowEvent]?.status?.let {
            assertNotEquals(Status.SUCCESS, it, "Snowplow request status")
            assertEquals(true, it.retryable, "Snowplow request retryable")
        }
        assertEquals(Status.SUCCESS, result.result_t.status, "v3 request status")
    }

    @Test
    fun `successfully handle v3 requests and discard snowplow events if collector is unreachable`() {
        val source = PocketRemoteSource(
            httpClient,
            v3Url,
            v3Url,
            unreachableSnowplow
        ).apply {
            setCredentials(credentials)
        }

        val result = source.syncFull(
            guid,
            v3Action,
            snowplowEvent,
        )

        // Expecting it failed, but we're not going to retry, i.e. we discarded it.
        result.result_a[snowplowEvent]?.status?.let {
            assertNotEquals(Status.SUCCESS, it, "Snowplow request status")
            assertEquals(false, it.retryable, "Snowplow request retryable")
        }
        assertEquals(Status.SUCCESS, result.result_a[v3Action]?.status, "v3 action status")
        assertEquals(Status.SUCCESS, result.result_t.status, "v3 request status")
    }

    @Test
    fun `do not discard snowplow events if network connection is down`() {
        val source = PocketRemoteSource(
            httpClient,
            unreachableUrl,
            unreachableUrl,
            unreachableSnowplow,
        ).apply {
            setCredentials(credentials)
        }

        val result = source.syncFull(
            guid,
            v3Action,
            snowplowEvent,
        )

        assertEquals(
            true,
            result.result_a[snowplowEvent]?.status?.retryable,
            "Snowplow request retryable",
        )
        assertEquals(
            true,
            result.result_a[v3Action]?.status?.retryable,
            "v3 action retryable",
        )
        assertEquals(
            true,
            result.result_t.status.retryable,
            "v3 request retryable",
        )
    }
}

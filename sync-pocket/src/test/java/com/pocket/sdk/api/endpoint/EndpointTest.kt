package com.pocket.sdk.api.endpoint

import com.nhaarman.mockitokotlin2.*
import com.pocket.sdk.network.eclectic.EclecticHttp
import com.pocket.sdk.network.eclectic.EclecticHttpRequest
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndpointTest {
    @Test fun hash() {
        val request = mock<EclecticHttpRequest>()
        val response = mock<EclecticHttp.Response> {
            on { statusCode } doReturn 200
        }

        Endpoint.execute(
            Endpoint.Request("https://example.com").apply {
                app(AppInfo(
                    "5513-8646141fb5902c766272e74d",
                    "Pocket",
                    "Free",
                    "6.7.0.0",
                    "play",
                    "play"
                ))
                device(DeviceInfo("Android",
                    "9.0",
                    "Test",
                    "Test",
                    "Test",
                    "handset",
                    "en-us",
                    null
                ))
                accessToken("token")
            },
            mock {
                on { buildRequest(any()) } doReturn request
                on { post(eq(request), any()) } doReturn response
            }
        )

        val keys = argumentCaptor<String>()
        val values = argumentCaptor<String>()
        verify(request, atLeastOnce()).appendQueryParameter(keys.capture(), values.capture())
        
        assertTrue { keys.allValues.contains("oauth_timestamp") }
        assertTrue { keys.allValues.contains("oauth_nonce") }
        assertTrue { keys.allValues.contains("sig_hash") }
        
        val timestamp = values.allValues[keys.allValues.indexOf("oauth_timestamp")]
        val nonce = values.allValues[keys.allValues.indexOf("oauth_nonce")]
        val hash = values.allValues[keys.allValues.indexOf("sig_hash")]
        assertEquals(legacyHash(timestamp, nonce, "token"), hash)
    }

    /** The original hashing method that relied on the problematic Commons Codec library. */
    private fun legacyHash(timestamp: String, nonce: String, value: String): String? {
        val builder = StringBuilder()
            .append(timestamp)
            .append(nonce)
            .append(value)
            .append(EndpointStrings.API_REQUEST_SALT)
            .toString()

        return String(Hex.encodeHex(DigestUtils.md5(builder.toByteArray())))
    }
}

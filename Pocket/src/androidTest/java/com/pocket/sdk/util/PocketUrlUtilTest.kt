package com.pocket.sdk.util

import org.junit.Test
import kotlin.test.assertEquals

class PocketUrlUtilTest {
    @Test fun asRedirect() {
        val redirect = PocketUrlUtil.asRedirect("https://example.com", true)

        assertEquals(
            "7db576d22b055fa4c502f0cfa80476031289f450edbe435394a80d2d5452ee03",
            redirect.getQueryParameter("h")
        )
    }
}

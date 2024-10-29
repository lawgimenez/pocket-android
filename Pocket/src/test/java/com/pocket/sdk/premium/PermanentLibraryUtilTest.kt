package com.pocket.sdk.premium

import org.junit.Test
import java.net.URLEncoder
import kotlin.test.assertEquals

class PermanentLibraryUtilTest {
    @Test fun hash() {
        val fakeTimestamp = "0"
        val fakeUserId = "userId"
        val fakeItemValue = URLEncoder.encode("https://example.com")
        val expectedHashForTheseFakeInputs =
            "98f88b7a62c2f8efd235c0d9c0763af5482fb8d43687b44cad8a69668a062200"


        val actualHash = PermanentLibraryUtil.hash(fakeTimestamp, fakeUserId, fakeItemValue)
        
        assertEquals(expectedHashForTheseFakeInputs, actualHash)
    }
}

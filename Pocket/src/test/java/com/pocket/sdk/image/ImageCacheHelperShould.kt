package com.pocket.sdk.image

import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageCacheHelperShould {
    @Test
    fun `always add some default filters`() {
        val url = ImageCacheHelper.convertToPocketImageCacheUrl(URL)!!.toHttpUrl()

        assertEquals("https", url.scheme)
        assertEquals("pocket-image-cache.com", url.host)
        assertEquals(2, url.pathSize)
        assertEquals(URL, url.pathSegments[1])
    }

    companion object {
        private const val URL = "http://example.com"
    }
}

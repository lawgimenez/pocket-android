package com.pocket.sdk.image

import okhttp3.HttpUrl

object ImageCacheHelper {

    @Suppress("SwallowedException")
    @JvmStatic
    fun convertToPocketImageCacheUrl(
        url: String,
    ): String {
        return HttpUrl.Builder().apply {
            scheme("https")
            host("pocket-image-cache.com")
            addPathSegment("filters:format(jpeg):quality(60):no_upscale():strip_exif()")
            addPathSegment(url)
        }.build().toString()
    }
}

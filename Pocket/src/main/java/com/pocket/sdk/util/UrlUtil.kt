package com.pocket.sdk.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UrlUtil {

    /**
     * Checks if two urls are the same, aside from the schema (http vs https) or other minor
     * differences like a "/" at the end of the url
     */
    @Suppress("ReturnCount", "SwallowedException")
    fun areUrlsTheSame(url1: String, url2: String): Boolean {
        val httpUrl1 = url1.cleanUrl().toHttpUrlOrNull()
        val httpUrl2 = url2.cleanUrl().toHttpUrlOrNull()

        if (httpUrl1 == null || httpUrl2 == null) {
            return false
        }

        if (httpUrl1.host != httpUrl2.host) {
            return false
        }

        if (httpUrl1.pathSegments.size != httpUrl2.pathSegments.size) {
            return false
        }

        for (i in 0 until httpUrl1.pathSegments.size) {
            if (httpUrl1.pathSegments[i] != httpUrl2.pathSegments[i]) {
                return false
            }
        }

        return true
    }

    private fun String.cleanUrl(): String =
        "https://" + removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .trim('/')
}
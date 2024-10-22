package com.pocket.sdk.api.value

/**
 * Value that conforms to the standard URL format as specified in RFC3986:
 * https://www.ietf.org/rfc/rfc3986.txt.
 */
data class ValidUrl(
    val url: String,
) {
    init {
        if (url.isBlank()) {
            throw IllegalArgumentException("Blank urls are not valid.")
        }
    }
}

fun String.toValidUrl() = ValidUrl(this)

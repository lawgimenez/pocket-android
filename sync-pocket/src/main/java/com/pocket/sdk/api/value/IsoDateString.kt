package com.pocket.sdk.api.value

import org.threeten.bp.Instant
import org.threeten.bp.format.DateTimeFormatter

data class IsoDateString(val raw: String) {
    fun toInstant(): Instant = DateTimeFormatter.ISO_INSTANT.parse(raw, Instant.FROM)
}

fun Instant.toIsoDateString(): IsoDateString {
    return IsoDateString(DateTimeFormatter.ISO_INSTANT.format(this))
}

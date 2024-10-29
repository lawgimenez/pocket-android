package com.pocket.util.android

import android.app.PendingIntent
import android.os.Build

object PendingIntentUtils {
    /**
     * API 31+ requires [PendingIntent.FLAG_IMMUTABLE] or [PendingIntent.FLAG_MUTABLE] to be set.
     * Since [PendingIntent.FLAG_MUTABLE] is only available in API 31+, this function exists
     * to easily add the flag without having to check the current sdk version.
     *
     * Note that prior to API 31, pending intents are mutable unless they contain the
     * [PendingIntent.FLAG_IMMUTABLE] flag
     */
    @JvmStatic
    fun addMutableFlag(flags: Int): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        flags or PendingIntent.FLAG_MUTABLE
    } else {
        flags
    }
}
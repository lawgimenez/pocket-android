package com.pocket.util.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object IntentUtils2 {

    fun isIntentUsable(
        context: Context,
        intent: Intent,
    ): Boolean =
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
}
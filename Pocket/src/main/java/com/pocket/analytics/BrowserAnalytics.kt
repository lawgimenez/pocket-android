package com.pocket.analytics

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Helpers for querying OS for useful data related to browsers on device.
 */
class BrowserAnalytics
@Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Get information about the default browser or null if none set.
     */
    fun getDefaultBrowserInfo(): BrowserInfo? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val packageName = context.packageManager
            .resolveActivity(browserIntent, 0)
            ?.activityInfo
            ?.packageName
            ?: return null
        return BrowserInfo(
            packageName,
            context.packageManager.getPackageInfo(packageName, 0)?.versionName
        )
    }

    /**
     * Get the package name of the default browser if it supports custom tabs.
     */
    fun getDefaultCustomTabsPackageName(): String? {
        return CustomTabsClient.getPackageName(context, null)
    }

    /**
     * Get a list of all package names that support Custom Tabs.
     */
    fun getCustomTabsPackageNames(): List<String> {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val browsers = context.packageManager
            .queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.packageName }

        val customTabsIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        return browsers.filter {
            customTabsIntent.setPackage(it)
            context.packageManager.resolveService(customTabsIntent, 0) != null
        }
    }
}

data class BrowserInfo(
    val name: String,
    val version: String?,
)

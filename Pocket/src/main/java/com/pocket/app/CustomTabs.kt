package com.pocket.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsService
import com.ideashower.readitlater.R
import com.pocket.analytics.BrowserAnalytics
import com.pocket.sdk.api.ServerFeatureFlags
import com.pocket.sync.await
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.StringPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val BrowserSettingKillSwitch = "perm.android.app.disable_default_browser_setting"

/**
 * Custom Tabs is an Android browser API, led by Chrome but supported (to an extent) by most
 * major alternative browsers. It lets apps (like us) delegate displaying a website
 * in a light-weight way, i.e. without throwing the user to the full browser UI.
 * It is also possible to customise the appearance and add custom buttons/menu options.
 *
 * This class owns any shared logic and resources like user settings
 * related to Custom Tabs or browsers in general.
 */
@Singleton
class CustomTabs
@Inject constructor(
    @ApplicationContext private val context: Context,
    private val flags: ServerFeatureFlags,
    private val browserAnalytics: BrowserAnalytics,
    prefs: Preferences,
) {
    private val browserPref = prefs.forApp("defaultBrowser", null as String?)

    /** Package name of the browser selected in Pocket settings. Null means system default. */
    val preferredBrowserPackageName: String?
        get() = browserPref.get()

    /**
     * @return true if the combination of Pocket and system settings will result in showing
     *         the disambiguation modal asking for a browser choice
     */
    val willShowChooser: Boolean
        get() {
            return preferredBrowserPackageName == null &&
                    browserAnalytics.getDefaultBrowserInfo()?.name == "android"
        }

    /**
     * Checks if the feature flag/kill switch allows the new browser selection logic
     * that includes showing an in-app setting.
     */
    suspend fun isBrowserSettingEnabled(): Boolean {
        val killSwitch = flags.get(BrowserSettingKillSwitch).await()
        return killSwitch?.assigned != true
    }

    fun getBrowserOptions(): BrowserOptions {
        val packageManager = context.packageManager
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val customTabsIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        val browsers = packageManager
            .queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .map {
                customTabsIntent.setPackage(it.packageName)
                InstalledBrowser(
                    it.packageName,
                    packageManager.getApplicationLabel(it),
                    packageManager.resolveService(customTabsIntent, 0) != null,
                )
            }
        val options = listOf(DefaultBrowser) + browsers
        return BrowserOptions(
            options,
            browserPref,
            context.resources.getText(R.string.setting_default_browser_system),
        )
    }
}

/** Represents different kinds of options available in the preferred browser setting. */
sealed interface BrowserOption
/** A singleton option that lets the OS choose the user configured default browser. */
data object DefaultBrowser : BrowserOption
/** A browser app installed on the device as queried from the [PackageManager]. */
data class InstalledBrowser(
    val packageName: String,
    val applicationLabel: CharSequence,
    val supportsCustomTabs: Boolean,
) : BrowserOption

/** Encapsulates available options and allows checking and modifying the current selection. */
class BrowserOptions(
    private val options: List<BrowserOption>,
    private val browserPref: StringPreference,
    private val defaultBrowserLabel: CharSequence,
) {
    /** Human readable labels for available options. */
    val labels: Array<CharSequence>
        get() = Array(options.size) { i ->
            when(val option = options[i]) {
                is DefaultBrowser -> defaultBrowserLabel
                is InstalledBrowser -> option.applicationLabel
            }
        }

    /** Index of the selected option. */
    val selected: Int
        get() = options.indexOfLast {
            when (it) {
                is InstalledBrowser -> it.packageName == browserPref.get()
                is DefaultBrowser -> true
            }
        }

    /** Human readable label of the selected option. */
    val selectedLabel: CharSequence
        get() = options.filterIsInstance<InstalledBrowser>()
            .find { it.packageName == browserPref.get() }
            ?.applicationLabel
            ?: defaultBrowserLabel

    /** Call to update which option was selected. */
    fun onSelected(which: Int) {
        when (val option = options[which]) {
            is DefaultBrowser -> browserPref.set(null)
            is InstalledBrowser -> browserPref.set(option.packageName)
        }
    }

    /** Check if the selected option is still valid (i.e. available/installed) and reset if not. */
    fun resetIfSelectionInvalid() {
        val packageName = browserPref.get()
            ?: return // Return early, because null means system default which is always valid.

        // If the remembered package name is not in the list of current options…
        if (options.none { it is InstalledBrowser && it.packageName == packageName }) {
            // …default back to the system default option.
            onSelected(options.indexOfFirst { it is DefaultBrowser })
        }
    }

    /** Overwrite the current choice based on our preferences/heuristics. */
    fun pickPreferredBrowser() {
        onSelected(findPreferred(options))
    }

    private fun findPreferred(browsers: List<BrowserOption>): Int {
        // First look for specific browsers we tested on, in the order of preference.
        val preferredPackageNames = listOf(
            "com.android.chrome", // Chrome has best Custom Tab support.
            "org.mozilla.firefox", // Firefox is second most popular and works good too.
            "com.chrome.beta", // Include Chrome's other release channels.
            "com.chrome.dev",
            "com.chrome.canary",
            "org.mozilla.firefox_beta", // Include Firefox Beta,
            "org.mozilla.fenix", // and Nightly.
        )
        for (packageName in preferredPackageNames) {
            val preferred =
                browsers.indexOfFirst { it is InstalledBrowser && it.packageName == packageName }
            if (preferred != -1) return preferred
        }

        // If none found, pick one that has Custom Tab support.
        val firstWithCustomTabSupport =
            browsers.indexOfFirst { it is InstalledBrowser && it.supportsCustomTabs }
        if (firstWithCustomTabSupport != -1) return firstWithCustomTabSupport

        // If none found, just pick one.
        return browsers.indexOfFirst { it is InstalledBrowser }
    }
}

package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Context about device's browsers.
 * @param defaultBrowser package name of the default browser or null if none set
 * @param defaultBrowserVersion version name of the default browser or null if not available
 * @param defaultBrowserSupportsCustomTabs true if the default browser is set and supports Custom Tabs
 * @param customTabsBrowserCount number of browsers on the device that support Custom Tabs
 */
data class BrowserContext(
    private val defaultBrowser: String?,
    private val defaultBrowserVersion: String?,
    private val defaultBrowserSupportsCustomTabs: Boolean,
    private val customTabsBrowserCount: Int,
) : Entity {
    override fun toSelfDescribingJson(): SelfDescribingJson = SelfDescribingJson(
        schema = "iglu:com.pocket/browser_context/jsonschema/1-0-0",
        data = buildMap {
            put("defaultBrowser", defaultBrowser)
            put("defaultBrowserVersion", defaultBrowserVersion)
            put("defaultBrowserSupportsCustomTabs", defaultBrowserSupportsCustomTabs)
            put("customTabsBrowserCount", customTabsBrowserCount)
        }
    )
}

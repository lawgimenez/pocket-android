package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * A unique piece of content (item) within Pocket, usually represented by a URL.
 * Should be included in all events that relate to content (primarily
 * recommendation card impressions/engagements and item page impressions/engagements).
 */
data class ContentEntity(
    // The full URL of the content.
    val url: String,
    // The backend identifier for a URL.
    private val itemId: String? = null,
) : Entity {

    override fun toSelfDescribingJson(): SelfDescribingJson =
        SelfDescribingJson(
            "iglu:com.pocket/content/jsonschema/1-0-0",
            buildMap {
                put("url", url)
                itemId?.let { put("item_id", itemId) }
            }
        )
}
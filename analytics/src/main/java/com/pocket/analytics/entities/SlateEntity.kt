package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Entity to describe a slate of recommendations. Should be included with any
 * impression or engagement events with recommendations.
 */
data class SlateEntity(
    // A unique slug/id that is used to identify a slate and its specific configuration.
    private val slateId: String,
    // A guid that is unique to every API request that returns slates.
    private val requestId: String,
    // A string identifier of a recommendation experiment.
    private val experiment: String,
    // The zero-based index value of the slate’s display position among other slates in the same lineup.
    private val index: Int,
    // The name to show the user for a slate.
    private val displayName: String? = null,
    // The description of the slate.
    private val description: String? = null,
) : Entity {

    override fun toSelfDescribingJson(): SelfDescribingJson =
        SelfDescribingJson(
            "iglu:com.pocket/slate/jsonschema/1-0-0",
            buildMap {
                put("slate_id", slateId)
                put("request_id", requestId)
                put("experiment", experiment)
                put("index", index)

                displayName?.let { put("display_name", it) }
                description?.let { put("description", it) }
            }
        )
}
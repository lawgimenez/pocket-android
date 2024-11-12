package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * A snowplow entity or context
 */
interface Entity {
    fun toSelfDescribingJson(): SelfDescribingJson
}
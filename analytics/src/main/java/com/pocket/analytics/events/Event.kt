package com.pocket.analytics.events

import com.snowplowanalytics.snowplow.event.SelfDescribing

/**
 * A snowplow event
 */
interface Event {
    fun toSelfDescribing(): SelfDescribing
}
package com.pocket.analytics.events

import com.snowplowanalytics.snowplow.event.SelfDescribing

/**
 * System log message triggered by a Pocket app, possibly not in direct response to user interaction.
 * Entities included: always api_user, user.
 *
 * @param identifier The internal name for the logged event.
 * @param value The final state of the system operation being tracked in this event.
 */
data class SystemLog(
    private val identifier: String,
    private val value: String?,
): Event {
    override fun toSelfDescribing() = SelfDescribing(
        "iglu:com.pocket/system_log/jsonschema/1-0-0",
        buildMap {
            put("identifier", identifier)
            if (value != null) put("value", value)
        }
    )
}

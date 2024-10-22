package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Entity for a flag from a user to Pocket that an item is inappropriate or broken.
 * Should be included with any engagement event where type = report.
 */
data class ReportEntity(
    // The reason for the report selected from a list of options.
    private val reason: Reason,
    // An optional user-provided comment on the reason for the report.
    private val comment: String? = null,
) : Entity {

    override fun toSelfDescribingJson(): SelfDescribingJson =
        SelfDescribingJson(
            "iglu:com.pocket/report/jsonschema/1-0-0",
            buildMap {
                put("reason", reason.value)
                comment?.let { put("comment", it) }
            }
        )

    enum class Reason(val value: String) {
        BROKEN_META("broken_meta"),
        WRONG_CATEGORY("wrong_category"),
        SEXUALLY_EXPLICIT("sexually_explicit"),
        OFFENSIVE("offensive"),
        MISINFORMATION("misinformation"),
        OTHER("other"),
    }
}
package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Entity to describe a front-end user interface. Should be included with any
 * impression, engagement, or custom engagement events.
 */
data class UiEntity(
    // The general UI component type.
    private val type: Type,
    /**
     * The internal name for the specific UI.  The general pattern for naming events is
     * screen.feature.drilldown.action
     * This is a guideline, not a hard rule.
     * And example:
     * home.recentsaves.save.delete
     * https://docs.google.com/spreadsheets/d/10DrvRWaRjHbhvdoetVqeScK452alaSUtXpgdLGtEs3A/edit#gid=778876482
     */
    private val identifier: String,
    // The detailed type of UI component (e.g. standard, radio, checkbox, etc).
    private val componentDetail: String? = null,
    // The en-US display name for the UI, if available.
    private val label: String? = null,
    // The zero-based index value of a UI, if found in a list of similar UI components (e.g. item in a feed).
    private val index: Int? = null,
    // The state of a UI element before the engagement (e.g. the initial value for a setting or filter).
    private val value: String? = null,
) : Entity {

    override fun toSelfDescribingJson(): SelfDescribingJson =
        SelfDescribingJson(
            "iglu:com.pocket/ui/jsonschema/1-0-3",
            buildMap {
                put("hierarchy", 0)
                put("identifier", identifier)
                put("type", type.value)

                componentDetail?.let { put("component_detail", it) }
                label?.let { put("label", it) }
                index?.let { put("index", it) }
                value?.let { put("value", it) }
            }
        )

    enum class Type(val value: String) {
        BUTTON("button"),
        DIALOG("dialog"),
        MENU("menu"),
        CARD("card"),
        LIST("list"),
        READER("reader"),
        PAGE("page"),
        SCREEN("screen"),
        LINK("link"),
        PUSH_NOTIFICATION("push_notification"),
    }
}
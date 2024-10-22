package com.pocket.analytics.events

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.Entity
import com.pocket.analytics.entities.UiEntity
import com.snowplowanalytics.snowplow.event.SelfDescribing

/**
 * Event created when an app initiates the opening a piece of content
 * (triggered by the intent to open an item and does not guarantee that the item was viewed).
 */
data class ContentOpen(
    // Indicates whether the content is being opened within a
    // Pocket property (internal) or offsite / in another app (external
    private val destination: Destination = Destination.INTERNAL,
    // Indicates whether content was opened with direct intent
    // (e.g. user taps vs. next-up in Listen playlist or infinite scroll
    private val trigger: Trigger = Trigger.CLICK,
    val contentEntity: ContentEntity,
    private val uiEntity: UiEntity,
    private val extraEntities: List<Entity> = listOf()
) : Event {
    override fun toSelfDescribing(): SelfDescribing =
        SelfDescribing(
            "iglu:com.pocket/content_open/jsonschema/1-0-0",
            buildMap {
                put("destination", destination.value)
                put("trigger", trigger.value)
            }
        ).apply {
            entities.apply {
                add(contentEntity.toSelfDescribingJson())
                add(uiEntity.toSelfDescribingJson())
                extraEntities.forEach { add(it.toSelfDescribingJson()) }
            }
        }

    enum class Destination(val value: String) {
        INTERNAL("internal"),
        EXTERNAL("external"),
    }

    enum class Trigger(val value: String) {
        CLICK("click"),
        AUTO("auto"),
    }
}
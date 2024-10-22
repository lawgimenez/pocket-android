package com.pocket.analytics.events

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.Entity
import com.pocket.analytics.entities.ReportEntity
import com.pocket.analytics.entities.UiEntity
import com.snowplowanalytics.snowplow.event.SelfDescribing

/**
 * Event triggered when a user engages with a UI element.
 */
data class Engagement(
    // Indicates the type of engagement.
    private val type: Type = Type.General,
    // The new value of a setting/filter, if the user engaged with
    // something and modified its state in doing so.
    private val value: String? = null,
    private val uiEntity: UiEntity,
    private val extraEntities: List<Entity> = listOf(),
) : Event {

    override fun toSelfDescribing(): SelfDescribing =
        SelfDescribing(
            "iglu:com.pocket/engagement/jsonschema/1-0-1",
            buildMap {
                put("type", type.value)
                value?.let { put("value", it) }
            }
        ).apply {
            entities.apply {
                add(uiEntity.toSelfDescribingJson())
                extraEntities.forEach { add(it.toSelfDescribingJson()) }
                type.requiredEntities.forEach { add(it.toSelfDescribingJson()) }
            }
        }

    sealed class Type(
        val value: String,
        val requiredEntities: List<Entity>
    ) {
        data object General : Type(
            value = "general",
            requiredEntities = emptyList()
        )

        data class Save(
            val contentEntity: ContentEntity,
        ) : Type(
            value = "save",
            requiredEntities = listOf(contentEntity)
        )

        data class Report(
            val reportEntity: ReportEntity,
            val contentEntity: ContentEntity,
        ) : Type(
            value = "report",
            requiredEntities = listOf(reportEntity, contentEntity)
        )
    }
}
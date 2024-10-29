package com.pocket.analytics.events

import com.pocket.analytics.entities.ContentEntity
import com.pocket.analytics.entities.Entity
import com.pocket.analytics.entities.UiEntity
import com.snowplowanalytics.snowplow.event.SelfDescribing

/**
 * Event triggered when a user views a UI element.
 */
data class Impression(
    // Indicator of the component that is being viewed.
    private val component: Component,
    // Indicates the requirement before an impression can be triggered (instant: any
    // pixel displayed on screen; viewable: +50% of component displayed for 1+ seconds).
    private val requirement: Requirement,
    private val uiEntity: UiEntity,
    private val extraEntities: List<Entity> = listOf(),
) : Event {

    override fun toSelfDescribing(): SelfDescribing =
        SelfDescribing(
            "iglu:com.pocket/impression/jsonschema/1-0-2",
            buildMap {
                put("component", component.value)
                put("requirement", requirement.value)
            }
        ).apply {
            entities.apply {
                add(uiEntity.toSelfDescribingJson())
                extraEntities.forEach { add(it.toSelfDescribingJson()) }
                component.requiredEntities.forEach { add(it.toSelfDescribingJson()) }
            }
        }

    sealed class Component(
        val value: String,
        val requiredEntities: List<Entity>
    ) {
        object Ui : Component(
            value = "ui",
            requiredEntities = emptyList()
        )

        object Card : Component(
            value = "card",
            requiredEntities = emptyList()
        )

        data class Content(
            val contentEntity: ContentEntity
        ) : Component(
            value = "content",
            requiredEntities = listOf(contentEntity)
        )

        object Screen : Component(
            value = "screen",
            requiredEntities = emptyList()
        )

        object PushNotification : Component(
            value = "push_notification",
            requiredEntities = emptyList()
        )

        object Button : Component(
            value = "button",
            requiredEntities = emptyList()
        )
    }

    enum class Requirement(val value: String) {
        INSTANT("instant"),
        VIEWABLE("viewable"),
    }
}
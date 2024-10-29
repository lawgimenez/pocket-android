package com.pocket.analytics

import com.snowplowanalytics.snowplow.controller.GlobalContextsController
import com.snowplowanalytics.snowplow.globalcontexts.FunctionalGenerator
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

/**
 * Add a GlobalContext generator to the configuration of the tracker.
 *
 * Adapts [GlobalContextsController.add] and [GlobalContext] constructor to an idiomatic Kotlin API.
 */
fun GlobalContextsController.add(
    tag: String,
    functionalGenerator: (InspectableEvent) -> List<SelfDescribingJson>,
): Boolean {
    return add(
        tag,
        GlobalContext(
            object : FunctionalGenerator() {
                override fun apply(event: InspectableEvent) = functionalGenerator(event)
            }
        ),
    )
}

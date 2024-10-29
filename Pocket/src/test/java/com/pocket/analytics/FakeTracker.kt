package com.pocket.analytics

import android.view.View
import com.pocket.analytics.events.Event
import kotlin.test.assertEquals

fun FakeTracker.assertTracked(event: Event) {
    assertEquals(
        1,
        events.count { it == event },
    )
}

class FakeTracker : Tracker {
    val events = mutableListOf<Event>()

    override fun track(event: Event) {
        events += event
    }

    override fun bindUiEntityType(view: View, type: UiEntityType) {
        /* no-op */
    }

    override fun bindUiEntityIdentifier(view: View, identifier: String) {
        /* no-op */
    }

    override fun bindUiEntityValue(view: View, value: String) {
        /* no-op */
    }

    override fun bindContent(view: View, content: Content) {
        /* no-op */
    }

    override fun enableImpressionTracking(
        view: View,
        component: ImpressionComponent,
        uniqueId: Any,
    ) {
        /* no-op */
    }

    override fun trackImpression(
        view: View,
        component: ImpressionComponent,
        requirement: ImpressionRequirement,
        corpusRecommendation: CorpusRecommendation?,
    ) {
        /* no-op */
    }

    override fun trackEngagement(
        view: View,
        type: EngagementType,
        value: String?,
        report: Report?,
        corpusRecommendation: CorpusRecommendation?,
    ) {
        /* no-op */
    }

    override fun trackEngagement(
        externalView: ExternalView,
        type: EngagementType,
        value: String?,
        corpusRecommendation: CorpusRecommendation?,
    ) {
        /* no-op */
    }

    override fun trackContentOpen(
        view: View?,
        destination: ContentOpenDestination,
        trigger: ContentOpenTrigger,
        corpusRecommendation: CorpusRecommendation?,
    ) {
        /* no-op */
    }

    override fun trackContentOpen(
        link: Link,
        destination: ContentOpenDestination,
        trigger: ContentOpenTrigger,
        corpusRecommendation: CorpusRecommendation?,
    ) {
        /* no-op */
    }

    override fun trackAppOpen(deepLink: String?, referrer: String?) {
        /* no-op */
    }

    override fun trackAppBackground() {
        /* no-op */
    }

    override fun trackVariantEnroll(name: String, variant: String, view: View?) {
        /* no-op */
    }
}
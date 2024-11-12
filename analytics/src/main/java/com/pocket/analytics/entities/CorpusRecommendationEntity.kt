package com.pocket.analytics.entities

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Entity to identify a Corpus Item recommendation.
 * Should be included with any impression or engagement events for corpus recommendations.
 */
data class CorpusRecommendationEntity(
    // Uniquely identifies the recommendation of a corpus item at a point in time to a user.
    // Should be set to the `id` field of CorpusRecommendation.
    val corpusRecommendationId: String,
) : Entity {

    override fun toSelfDescribingJson(): SelfDescribingJson =
        SelfDescribingJson(
            "iglu:com.pocket/corpus_recommendation/jsonschema/1-0-0",
            buildMap {
                put("corpus_recommendation_id", corpusRecommendationId)
            }
        )
}
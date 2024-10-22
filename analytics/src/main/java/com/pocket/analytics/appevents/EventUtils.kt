package com.pocket.analytics.appevents

import com.pocket.analytics.entities.CorpusRecommendationEntity
import com.pocket.analytics.entities.Entity

/**
 * Adds a [CorpusRecommendationEntity] to the list if the id is non-null
 */
fun MutableList<Entity>.withCorpusRecommendationEntity(corpusRecommendationId: String?) {
    if (corpusRecommendationId != null) {
        add(
            CorpusRecommendationEntity(
                corpusRecommendationId = corpusRecommendationId,
            )
        )
    }
}

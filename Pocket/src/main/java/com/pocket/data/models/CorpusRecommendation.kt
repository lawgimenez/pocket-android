package com.pocket.data.models

data class CorpusRecommendation(
    val id: String,
    val corpusItem: CorpusItem
)

fun com.pocket.sdk.api.generated.thing.CorpusRecommendation.toDomainCorpusRecommendation() =
    CorpusRecommendation(
        id = id?.id!!,
        corpusItem = corpusItem?.toDomainCorpusItem()!!
    )
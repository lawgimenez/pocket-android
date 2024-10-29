package com.pocket.repository

import com.pocket.data.models.CorpusRecommendation
import com.pocket.data.models.toDomainCorpusRecommendation
import com.pocket.sdk.Pocket
import com.pocket.sync.await
import com.pocket.sync.source.bindLocalAsFlow
import com.pocket.sync.space.Holder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationsRepository @Inject constructor(
    private val pocket: Pocket
) {

    suspend fun refreshEndOfArticleRecommendations(url: String) {
        val recs = buildEndOfArticleObject(url)

        pocket.remember(Holder.session("eoa-$url"), recs)
        pocket.syncRemote(recs).await()
    }

    fun getEndOfArticleRecommendationsFlow(url: String): Flow<List<CorpusRecommendation>> =
        pocket.bindLocalAsFlow(buildEndOfArticleObject(url))
            .map { relatedAfterArticle ->
                relatedAfterArticle?.itemByUrl?.relatedAfterArticle?.map { corpusRecommendation ->
                    corpusRecommendation.toDomainCorpusRecommendation()
                } ?: emptyList()
            }

    private fun buildEndOfArticleObject(url: String) =
        pocket.spec().things().relatedAfterArticle()
            .url(url)
            .count(END_OF_ARTICLE_RECOMMENDATION_COUNT)
            .build()

    companion object {
        const val END_OF_ARTICLE_RECOMMENDATION_COUNT = 4
    }
}
package com.pocket.repository

import com.pocket.data.models.DomainRecommendation
import com.pocket.data.models.DomainSlate
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.Collection
import com.pocket.sdk.api.generated.thing.CorpusItem
import com.pocket.sdk.api.generated.thing.CorpusRecommendation
import com.pocket.sdk.api.generated.thing.CorpusSlate
import com.pocket.sdk.api.generated.thing.Home
import com.pocket.sync.await
import com.pocket.sync.source.bindLocalAsFlow
import com.pocket.sync.space.Holder
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.getValue
import com.pocket.util.prefs.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

private const val RECOMMENDATION_COUNT = 10

@Singleton
class HomeRepository
@Inject constructor(
    private val pocket: Pocket,
    prefs: Preferences,
) {
    private val holder = Holder.persistent("home-8.26.1")

    init {
        bustCachePre826()
        pocket.setup {
            pocket.remember(holder, lineup(currentLocale))
        }
    }

    /**
     * We need to bust cache we had before release 8.26, because we started requiring
     * [CorpusItem.preview] in [toRecommendation], but it wasn't cached before,
     * so it causes a crash on upgrade.
     */
    private fun bustCachePre826() = pocket.forget(Holder.persistent("home"))

    var currentLocale by prefs.forUser("home_locale", null as String?)

    suspend fun refreshLineup(locale: String?) {
        val lineup = lineup(locale)
        if (locale != currentLocale) {
            // If locale changes, clear the currently cached lineup
            // and setup caching for the new locale.
            pocket.forget(holder, lineup(currentLocale))
            currentLocale = locale
            pocket.remember(holder, lineup)
        }

        pocket.syncRemote(lineup).await()
    }

    fun getLineup(locale: String?): Flow<List<DomainSlate>> {
        return pocket.bindLocalAsFlow(lineup(locale))
            .mapNotNull { it.homeSlateLineup?.slates?.map { it.toDomainSlate() } }
    }

    suspend fun hasCachedLineup(locale: String?): Boolean =
        pocket.syncLocal(lineup(locale)).await()?.homeSlateLineup?.slates?.isNotEmpty() ?: false

    private fun lineup(locale: String?): Home {
        return pocket.spec().things().home()
            .recommendationCount(RECOMMENDATION_COUNT)
            .also {
                if (locale != null) {
                    it.locale(locale)
                }
            }
            .build()
    }
}

fun CorpusSlate.toDomainSlate(): DomainSlate =
    DomainSlate(
        title = headline,
        subheadline = subheadline,
        id = id?.id!!,
        recommendations = recommendations?.mapIndexed { index, corpusRecommendation ->
            corpusRecommendation.toRecommendation(index)
        } ?: listOf(),
    )

private fun CorpusRecommendation.toRecommendation(index: Int): DomainRecommendation =
    DomainRecommendation(
        corpusId = id!!.id,
        itemId = corpusItem!!.preview!!._id()!!.id,
        url = corpusItem!!.preview!!._url()!!.url,
        title = this.corpusItem?.preview?._title().orEmpty(),
        domain = corpusItem?.preview?._domain()?.name.orEmpty(),
        imageUrl = corpusItem?.preview?._image()?.url?.url,
        isCollection = corpusItem?.target is Collection,
        isSaved = corpusItem?.savedItem?.item != null,
        excerpt = corpusItem?.preview?._excerpt().orEmpty(),
        index = index,
        viewingTime = null,
    )

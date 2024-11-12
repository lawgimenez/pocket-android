package com.pocket.app.home.details

import com.ideashower.readitlater.R
import com.pocket.data.models.DomainRecommendation
import com.pocket.data.models.toDomainItem
import com.pocket.data.models.viewingTime
import com.pocket.sdk.api.generated.enums.ItemStatus
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.util.StringLoader
import org.threeten.bp.Duration

data class RecommendationUiState(
    val corpusRecommendationId: String?,
    val itemId: String,
    val url: String,
    val title: String,
    val domain: String,
    val timeToRead: String?,
    val imageUrl: String?,
    val isCollection: Boolean,
    val isSaved: Boolean,
    val excerpt: String,
    // used for analytics
    val index: Int,
)

fun DomainRecommendation.toRecommendationUiState(stringLoader: StringLoader) = RecommendationUiState(
    corpusRecommendationId = corpusId,
    itemId = itemId,
    url = url,
    title = title,
    domain = domain,
    timeToRead = viewingTime?.let { duration -> timeToReadText(stringLoader, duration) },
    imageUrl = imageUrl,
    isCollection = isCollection,
    isSaved = isSaved,
    // for some reason some excerpts are "View Original".  Clearly a backend bug
    excerpt = if (excerpt != "View Original") excerpt else "",
    index = index
)

fun Item.toRecommendationUiState(stringLoader: StringLoader, index: Int): RecommendationUiState {
    val domainItem = toDomainItem()
    return RecommendationUiState(
        corpusRecommendationId = null,
        itemId = domainItem.id!!,
        url = domainItem.idUrl,
        title = display_title.orEmpty(),
        domain = display_domain.orEmpty(),
        timeToRead = domainItem.viewingTime?.let { duration -> timeToReadText(stringLoader, duration) },
        imageUrl = display_thumbnail?.url,
        isCollection = collection?.slug != null,
        isSaved = status == ItemStatus.UNREAD
                || status == ItemStatus.ARCHIVED,
        excerpt = excerpt.orEmpty(),
        index = index,
    )
}

private fun timeToReadText(stringLoader: StringLoader, duration: Duration): String {
    val minutes: Int = duration.toMinutes().toInt() +
            if (duration.seconds % SECONDS_PER_MINUTE >= SECONDS_PER_HALF_MINUTE) {
                1
            } else {
                0
            }

    return stringLoader.getQuantityString(R.plurals.nm_time_to_read_estimate, minutes, minutes)
}

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HALF_MINUTE = 30
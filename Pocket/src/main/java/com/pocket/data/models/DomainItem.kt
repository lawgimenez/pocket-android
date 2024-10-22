package com.pocket.data.models

import com.pocket.sdk.api.generated.enums.ItemStatus
import com.pocket.sdk.api.generated.enums.Videoness
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.Position
import com.pocket.sdk.api.generated.thing.Video
import com.pocket.sdk.api.value.Timestamp

/**
 * Represents an item
 *
 * @property id the item id (can be null when
 *     an item is created by saving something locally before we get a chance to sync it with API)
 * @property idUrl the url used to identify the item
 * @property type whether this item is a [ItemType.VIDEO], [ItemType.ARTICLE], [ItemType.INDEX] or [ItemType.OTHER]
 * @property isSaved whether the item is saved (including archived)
 * @property isArchived whether the item is archived
 * @property isFavorited whether the item is favorited
 * @property highlights highlights on the item
 * @property isViewed whether this item ahs been opened in reader view
 * @property positions where the user left off
 */
data class DomainItem(
    val id: String?,
    val idUrl: String,
    val displayTitle: String,
    val type: ItemType,
    val isSaved: Boolean,
    val isArchived: Boolean,
    val isFavorited: Boolean,
    val highlights: List<Highlight>,
    val positions: List<DomainPosition>,
    val isViewed: Boolean,
    val resolvedUrl: String?,
    val wordCount: Int?,
    val videos: List<DomainVideo>?,
)

enum class ItemType {
    VIDEO,
    ARTICLE,
    INDEX,
    OTHER,
}

fun Item.toDomainItem(): DomainItem =
    DomainItem(
        id = item_id,
        idUrl = id_url!!.url,
        displayTitle = display_title.orEmpty(),
        type = itemType(),
        isSaved = status == ItemStatus.ARCHIVED || status == ItemStatus.UNREAD,
        isArchived = status == ItemStatus.ARCHIVED,
        isFavorited = favorite ?: false,
        highlights = annotations?.map { it.toHighlight() } ?: emptyList(),
        isViewed = viewed ?: false,
        positions = positions?.toList()?.map { it.toDomainPosition() } ?: emptyList(),
        resolvedUrl = resolved_url?.url,
        wordCount = word_count,
        videos = videos?.map { it.toDomainVideo() }
    )

data class DomainVideo(val length: Int?)

fun Video.toDomainVideo() = DomainVideo(length = length)


fun Item.itemType(): ItemType = when {
    has_video == Videoness.IS_VIDEO -> ItemType.VIDEO
    is_article == true -> ItemType.ARTICLE
    is_index == true -> ItemType.INDEX
    else -> ItemType.OTHER
}

enum class PositionType {
    ARTICLE, WEB, VIDEO, COLLECTION, OTHER,
}

fun Pair<String?, Position>.toDomainPosition(): DomainPosition = DomainPosition(
    positionType = when (first) {
        com.pocket.sdk.api.generated.enums.PositionType.ARTICLE.id.toString() -> PositionType.ARTICLE
        com.pocket.sdk.api.generated.enums.PositionType.WEB.id.toString() -> PositionType.WEB
        com.pocket.sdk.api.generated.enums.PositionType.VIDEO.id.toString() -> PositionType.VIDEO
        com.pocket.sdk.api.generated.enums.PositionType.COLLECTION.id.toString() -> PositionType.COLLECTION
        else -> PositionType.OTHER
    },
    timeUpdated = second.time_updated,
    scrollPosition = second.scroll_position ?: 0,
    timeSpent = second.time_spent,
    syncPosition = second,
    nodeIndex = second.node_index,
)

data class DomainPosition(
    val positionType: PositionType,
    val timeUpdated: Timestamp?,
    val timeSpent: Int?,
    val scrollPosition: Int,
    val syncPosition: Position,
    @Deprecated("don't use") val nodeIndex: Int?,
)

package com.pocket.sdk.tts

import com.pocket.data.models.*
import com.pocket.data.models.itemType
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.Timestamp

/**
 * A single track of content that can be played in [Listen].
 * For starters this will always be an item for text to speech,
 * but perhaps in the future this can be further abstracted into
 * a general playable type for other media types.
 *
 * @property syncItem backing item for use with sync engine
 * @property itemId for the item represented by the track
 * @property idUrl url used for identifying the item backing the track
 * @property displayThumbnailUrl url for display thumbnail
 * @property displayTitle title to display
 * @property displayUrl the best url for displaying the item to the user
 * @property timeAdded last time this model was added
 * @property idKey a hashed key based on the item's identity
 * @property authors of the content
 * @property positions the progress/scroll position of where the user left off
 * @property listenDurationEstimate estimated time in seconds to listen to article
 * @property itemType of the item
 */
data class Track(
    @JvmField val syncItem: Item,
    @JvmField val itemId: String?,
    @JvmField val idUrl: String,
    @JvmField val openUrl: String?,
    @JvmField val displayThumbnailUrl: String,
    @JvmField val displayTitle: String,
    @JvmField val displayUrl: String,
    @JvmField val timeAdded: Timestamp?,
    @JvmField val idKey: String,
    @JvmField val authors: List<Author>,
    @JvmField val positions: List<DomainPosition>,
    @JvmField val listenDurationEstimate: Int?,
    @JvmField val itemType: ItemType,
    @JvmField val wordCount: Int,
) {
    val isVideo: Boolean
        get() = itemType == ItemType.VIDEO

    val isArticle: Boolean
        get() = itemType == ItemType.ARTICLE

    val articlePosition: DomainPosition?
        get() = positions.firstOrNull { it.positionType == PositionType.ARTICLE }
}

/**
 * Builds a [Track] from an [Item] for listening
 *
 * @return a [Track] representing the [Item] for listening
 */
fun Item.toTrack(): Track = Track(
    syncItem = this,
    itemId = item_id,
    idUrl = id_url?.url!!,
    displayThumbnailUrl = display_thumbnail?.url ?: "",
    displayTitle = display_title ?: "",
    displayUrl = display_url?.url ?: "",
    timeAdded = time_added,
    idKey = idkey(),
    authors = authors?.map { it.toDomainAuthor() } ?: emptyList(),
    positions = positions?.toList()?.map { it.toDomainPosition() } ?: emptyList(),
    itemType = itemType(),
    listenDurationEstimate = listen_duration_estimate,
    openUrl = open_url!!.url,
    wordCount = word_count ?: 0,
)

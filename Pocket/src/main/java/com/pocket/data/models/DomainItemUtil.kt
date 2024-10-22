package com.pocket.data.models

import android.content.Context
import com.ideashower.readitlater.R
import com.pocket.sdk.api.thing.ItemUtil
import org.threeten.bp.Duration

val DomainItem.isVideo: Boolean
    get() = type == ItemType.VIDEO

val DomainItem.articlePosition: DomainPosition?
    get() = positions.firstOrNull { it.positionType == PositionType.ARTICLE }

/**
 * Returns time needed to view or consume the item. For an article it's reading time. For a
 * video it's its length.
 */
val DomainItem.viewingTime: Duration?
    get() = viewingSeconds?.let { Duration.ofSeconds(it) }

private val DomainItem.viewingSeconds: Long?
    get() = if (isViewed && wordCount != null) {
        if (wordCount < ItemUtil.minWordCountForViewingTime()) {
            null
        } else {
            (wordCount * SECONDS_PER_MINUTE / AVERAGE_WORDS_PER_MINUTE).toLong()
        }
    } else if (isVideo && videos != null) {
        videos.getOrNull(0)?.length?.toLong()
    } else {
        null
    }

private const val AVERAGE_WORDS_PER_MINUTE = 220
private const val SECONDS_PER_MINUTE = 60

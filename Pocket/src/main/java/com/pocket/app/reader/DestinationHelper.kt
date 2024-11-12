package com.pocket.app.reader

import android.util.Log
import com.pocket.analytics.events.ContentOpen
import com.pocket.app.reader.internal.article.ArticleViewKillSwitchFlag
import com.pocket.data.models.ItemType
import com.pocket.repository.ItemRepository
import com.pocket.sdk.http.HttpClientDelegate
import com.pocket.sdk.preferences.AppPrefs
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DestinationHelper @Inject constructor(
    private val httpClientDelegate: HttpClientDelegate,
    private val appPrefs: AppPrefs,
    private val itemRepository: ItemRepository,
    private val articleViewKillSwitchFlag: ArticleViewKillSwitchFlag
) {

    @Suppress("ComplexMethod")
    suspend fun getDestination(
        url: String,
        forceOpenInWebView: Boolean = false,
    ): Destination? {

        val item = try {
            itemRepository.getDomainItem(url)
        } catch (e: Exception) {
            Log.e("Reader", e.message ?: "")
            null
        }

        val httpUrl = if (!url.startsWith("https://") && !url.startsWith("http://")) {
            // make sure the link has a schema
            "https://$url".toHttpUrl()
        } else {
            url.toHttpUrl()
        }

        return when {
            forceOpenInWebView -> {
                Destination.ORIGINAL_WEB
            }
            // is video
            item?.type == ItemType.VIDEO -> {
                Destination.ORIGINAL_WEB
            }
            // is collection
            httpUrl.host == "getpocket.com"
                    && httpUrl.pathSegments.size >= 2
                    && httpUrl.pathSegments[0] == "collections" -> {
                Destination.COLLECTION
            }
            // syndicated article
            httpUrl.host == "getpocket.com"
                    && httpUrl.pathSegments.isNotEmpty()
                    && httpUrl.pathSegments[0] == "explore"
                    && !articleViewKillSwitchFlag.isEnabled -> {
                Destination.ARTICLE
            }
            // syndicated article but the article view kill switch is enabled
            httpUrl.host == "getpocket.com"
                    && httpUrl.pathSegments.isNotEmpty()
                    && httpUrl.pathSegments[0] == "explore"
                    && articleViewKillSwitchFlag.isEnabled -> {
                Destination.ORIGINAL_WEB
            }
            // any other type of pocket link
            httpUrl.host == "getpocket.com" -> {
                // ignore the link, we can't handle it
                null
            }
            // online and user setting to always open original
            httpClientDelegate.status().isOnline
                    && appPrefs.ALWAYS_OPEN_ORIGINAL.get() -> {
                Destination.ORIGINAL_WEB
            }
            // article view available
            item?.type == ItemType.ARTICLE &&
                    item.isSaved &&
                    !articleViewKillSwitchFlag.isEnabled -> {
                Destination.ARTICLE
            }
            else -> {
                Destination.ORIGINAL_WEB
            }
        }
    }
}

fun Destination.toContentOpenDestination(): ContentOpen.Destination =
    when (this) {
        Destination.COLLECTION,
        Destination.ARTICLE -> ContentOpen.Destination.INTERNAL
        Destination.ORIGINAL_WEB -> ContentOpen.Destination.EXTERNAL
    }

enum class Destination {
    ARTICLE,
    COLLECTION,
    ORIGINAL_WEB,
}
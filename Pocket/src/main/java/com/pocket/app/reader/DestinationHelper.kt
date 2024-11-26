package com.pocket.app.reader

import android.util.Log
import com.pocket.app.reader.internal.article.ArticleViewKillSwitchFlag
import com.pocket.data.models.ItemType
import com.pocket.repository.ItemRepository
import com.pocket.sdk.http.HttpClientDelegate
import com.pocket.sdk.preferences.AppPrefs
import okhttp3.HttpUrl
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

    suspend fun getDestination(
        url: String,
        forceOpenInWebView: Boolean = false,
    ): Destination {

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
            // video
            item?.type == ItemType.VIDEO -> {
                Destination.ORIGINAL_WEB
            }
            // collection
            httpUrl.isCollection() -> {
                Destination.COLLECTION
            }
            // syndicated article
            httpUrl.isSyndicatedArticle()
                    && !articleViewKillSwitchFlag.isEnabled -> {
                Destination.ARTICLE
            }
            // syndicated article but the article view kill switch is enabled
            httpUrl.isSyndicatedArticle()
                    && articleViewKillSwitchFlag.isEnabled -> {
                Destination.ORIGINAL_WEB
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

    /**
     * * `https://getpocket.com/collections/<slug>` or
     * * `https://getpocket.com/<locale>/collections/<slug>`
     */
    private fun HttpUrl.isCollection(): Boolean {
        return isHostedByPocket() &&
                pathSegments.size >= 2 &&
                encodedPath.contains("/collections/", ignoreCase = true)
    }

    /**
     * * `https://getpocket.com/explore/item/<slug>` or
     * * `https://getpocket.com/<locale>/explore/item/<slug>`
     */
    private fun HttpUrl.isSyndicatedArticle(): Boolean {
        return isHostedByPocket() &&
                pathSegments.size >= 3 &&
                encodedPath.contains("/explore/item/", ignoreCase = true)
    }

    @Suppress("SpellCheckingInspection")
    private fun HttpUrl.isHostedByPocket(): Boolean {
        return host == "getpocket.com"
    }
}

enum class Destination {
    ARTICLE,
    COLLECTION,
    ORIGINAL_WEB,
}

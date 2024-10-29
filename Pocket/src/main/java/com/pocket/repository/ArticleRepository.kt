package com.pocket.repository

import android.util.Log
import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.data.models.ArticleImage
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.PositionType
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.source.V3Source
import com.pocket.sdk.api.thing.VideoUtil
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk.getLocal
import com.pocket.sdk.image.Image
import com.pocket.sdk.offline.OfflineDownloading
import com.pocket.sdk.offline.cache.AssetUser
import com.pocket.sdk.offline.cache.Assets
import com.pocket.sdk.offline.cache.DownloadAuthorization
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val pocket: Pocket,
    private val assets: Assets,
    private val offlineDownloading: OfflineDownloading,
) {

    suspend fun getArticleHtml(
        url: String,
        forceRefresh: Boolean = false
    ): String {
        val item = pocket.getLocal(
            pocket.spec().things().item()
                .given_url(UrlString(url))
                .build()
        ) ?: Item.Builder()
            .given_url(UrlString(url))
            .build()
        val filePath = assets.assetDirectory.pathForText(item)
        val file = File(filePath)
        // Download article view if it isn't already
        if (!file.exists() || forceRefresh) {
            Log.d("ArticleRepository", "downloading html")
            // offline downloading is asynchronous, so use a mutex to make sure we
            // wait until it's finished to proceed
            val lock = Mutex(locked = true)
            offlineDownloading.download(
                item,
                PositionType.ARTICLE,
                forceRefresh
            ) { _, _, _ ->
                Log.d("ArticleRepository", "download complete")
                // continue
                lock.unlock()
            }
            // await
            lock.lock()
        }
        return FileUtils.readFileToString(
            file,
            "UTF-8"
        )
    }

    fun getImages(url: String, imageWidth: Int): Flow<ArticleImage> = callbackFlow {
        val item = pocket.getLocal(
            pocket.spec().things().item()
                .given_url(UrlString(url))
                .build()
        )

        Log.d("ArticleRepository", "fetching images")
        item?.images?.forEach { image ->
            Image.build(image.src, AssetUser.forItem(item.time_added, item.idkey()))
                .fitWidth(imageWidth, false)
                .setDownloadAuthorization(DownloadAuthorization.ALWAYS)
                .cache { request, result ->

                    if (result != Image.Result.SUCCESS) {
                        Log.d("ArticleRepository", "fetch failed for image: ${result.name}")
                        return@cache
                    }
                    Log.d("ArticleRepository", "fetch success for image: ${result.name}")

                    // Protect against XSS attacks in captions/credits
                    val caption = StringEscapeUtils.escapeHtml4(
                        StringUtils.defaultString(image.caption)
                    )
                    val credit = StringEscapeUtils.escapeHtml4(
                        StringUtils.defaultString(image.credit)
                    )

                    trySendBlocking(ArticleImage(
                        imageId = image.image_id!!,
                        localFileUrl = "file://${File(request.assetSizedPath)}",
                        caption = caption,
                        credit = credit,
                        originalUrl = image.src!!
                    ))
                }
        }

        awaitClose {
            // not possible to cancel the image requests from here?
        }
    }

    suspend fun getVideoJson(url: String): List<ObjectNode> {
        val item = pocket.getLocal(
            pocket.spec().things().item()
                .given_url(UrlString(url))
                .build()
        )
        val videoList = mutableListOf<ObjectNode>()
        item?.videos?.forEach { video ->
            videoList.add(VideoUtil.upgradeType(video).toJson(V3Source.JSON_CONFIG))
        }

        return videoList
    }

    fun reportArticle(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().reportArticleView()
                .url(UrlString(url))
                .time(Timestamp.now())
                .build()
        )
    }
}
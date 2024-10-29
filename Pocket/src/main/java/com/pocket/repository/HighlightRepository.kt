package com.pocket.repository

import com.pocket.data.models.Highlight
import com.pocket.data.models.toDomainItem
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.Annotation
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk.update
import com.pocket.sync.await
import com.pocket.sync.source.bindLocalAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HighlightRepository @Inject constructor(
    private val pocket: Pocket,
) {

    suspend fun addHighlight(patch: String, text: String, itemUrl: String) {
        val annotation = Annotation.Builder()
            .annotation_id(UUID.randomUUID().toString())
            .quote(text)
            .patch(patch)
            .version(2)
            .build()

        pocket.update(
            pocket.spec().actions().add_annotation()
                .annotation(annotation)
                .annotation_id(annotation.annotation_id)
                .url(UrlString(itemUrl))
                .time(Timestamp.now())
                .build()
        )
    }

    fun getHighlightsFlow(url: String): Flow<List<Highlight>> =
        pocket.bindLocalAsFlow(
            pocket.spec().things()
                .item()
                .given_url(UrlString(url))
                .build()
        ).map { it.toDomainItem().highlights }

    suspend fun deleteHighlight(highlightId: String, itemUrl: String) {
        pocket.sync(
            null,
            pocket.spec().actions().delete_annotation()
                .url(UrlString(itemUrl))
                .annotation_id(highlightId)
                .time(Timestamp.now())
                .build()
        ).await()
    }
}
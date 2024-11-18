package com.pocket.repository

import com.pocket.data.models.toDomainCollection
import com.pocket.sdk.Pocket
import com.pocket.data.models.Collection
import com.pocket.sdk.get
import com.pocket.sync.space.Holder
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val pocket: Pocket,
) {

    private val holder = Holder.session("collectionsSession")

    suspend fun getCollection(url: String): Collection {
        val slug = url.toHttpUrl().pathSegments.last()
        val collection = pocket.spec()
            .things()
            .collectionBySlug
            .slug(slug)
            .build()
        pocket.setup {
            pocket.remember(holder, collection)
        }
        return pocket.get(collection).collection?.toDomainCollection()!!
    }
}

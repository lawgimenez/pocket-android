package com.pocket.repository

import com.pocket.data.models.DomainItem
import com.pocket.data.models.toDomainItem
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.PositionType
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.PocketShare
import com.pocket.sdk.api.value.IdString
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk.get
import com.pocket.sdk.getLocal
import com.pocket.sync.await
import com.pocket.sync.source.bindLocalAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ItemRepository {
    suspend fun getDomainItem(url: String): DomainItem

    /**
     * Prefer [getItem] which doesn't throw.
     *
     * TODO: migrate all usages to [getItem] and inline this into [getItem].
     */
    suspend fun getItemOrThrow(
        url: String,
        lookupStrategy: LookupStrategy = LookupStrategy.LocalCache,
    ): Item

    suspend fun getItem(
        url: String,
        lookupStrategy: LookupStrategy = LookupStrategy.LocalCache,
    ): Item?

    suspend fun getItemByShareSlug(slug: String): Item?

    fun getDomainItemFlow(url: String): Flow<DomainItem>
    fun toggleFavorite(item: Item)
    fun favorite(url: String)
    fun unfavorite(url: String)
    fun favorite(vararg items: Item)
    fun unFavorite(vararg items: Item)
    fun archive(item: Item)
    fun archive(url: String)
    fun archive(items: List<Item>)
    fun unArchive(item: Item)
    fun unArchive(url: String)
    fun unArchive(items: List<Item>)
    fun unArchive(vararg items: Item)
    fun toggleViewed(item: Item)
    fun markAsViewed(vararg items: Item)
    fun markAsNotViewed(vararg items: Item)
    fun markAsViewed(url: String)
    fun markAsNotViewed(url: String)
    fun delete(item: DomainItem)
    fun delete(item: Item)
    fun delete(url: String)
    fun delete(items: List<Item>)
    suspend fun save(url: String)
    fun setScrollPosition(
        url: String,
        position: Int,
        timeSpentViewing: Int,
    )

    enum class LookupStrategy {
        LocalCache,
        RemoteIfNotCached,
        ForceRemote,
    }
}

@Singleton
class SyncEngineItemRepository @Inject constructor(
    private val pocket: Pocket,
) : ItemRepository {

    override suspend fun getDomainItem(url: String): DomainItem =
        getItemOrThrow(url).toDomainItem()

    override suspend fun getItemOrThrow(
        url: String,
        lookupStrategy: ItemRepository.LookupStrategy,
    ): Item {
        if (lookupStrategy == ItemRepository.LookupStrategy.LocalCache ||
            lookupStrategy == ItemRepository.LookupStrategy.RemoteIfNotCached
        ) {
            val item = pocket.getLocal(
                pocket.spec()
                    .things()
                    .item()
                    .given_url(UrlString(url))
                    .build()
            )
            if (item != null) return item
        }

        if (lookupStrategy == ItemRepository.LookupStrategy.RemoteIfNotCached ||
            lookupStrategy == ItemRepository.LookupStrategy.ForceRemote
        ) {
            val query = pocket.spec()
                .things()
                .getItemByUrl()
                .url(url)
                .build()
            return pocket.get(query).item!!
        }

        throw NoSuchElementException()
    }

    override suspend fun getItem(
        url: String,
        lookupStrategy: ItemRepository.LookupStrategy
    ): Item? {
        return try {
            getItemOrThrow(url, lookupStrategy)
        } catch (e: NoSuchElementException) {
            null
        } catch (e: NullPointerException) {
            null
        }
    }

    override suspend fun getItemByShareSlug(slug: String): Item? {
        val query = pocket.spec()
            .things()
            .getItemByShareSlug()
            .slug(IdString(slug))
            .build()
        return when (val result = pocket.get(query).result) {
            is PocketShare -> result.preview?._item()
            else -> null
        }
    }

    override fun getDomainItemFlow(
        url: String,
    ): Flow<DomainItem> = pocket.bindLocalAsFlow(
        pocket.spec()
            .things()
            .item()
            .given_url(UrlString(url))
            .build()
    ).map { it.toDomainItem() }

    override fun toggleFavorite(item: Item) {
        if (item.favorite == true) {
            pocket.sync(
                null,
                pocket.spec().actions().unfavorite()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build(),
            )
        } else {
            pocket.sync(
                null,
                pocket.spec().actions().favorite()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build(),
            )
        }
    }

    override fun favorite(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().favorite()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build(),
        )
    }

    override fun unfavorite(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().unfavorite()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build(),
        )
    }

    override fun favorite(vararg items: Item) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().favorite()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override fun unFavorite(vararg items: Item) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().unfavorite()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override fun archive(item: Item) {
        pocket.sync(
            null,
            pocket.spec().actions().archive()
                .time(Timestamp.now())
                .item_id(item.item_id)
                .url(item.id_url)
                .build()
        )
    }

    override fun archive(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().archive()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build()
        )
    }

    override fun archive(items: List<Item>) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().archive()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override fun unArchive(item: Item) {
        pocket.sync(
            null,
            pocket.spec().actions().readd()
                .time(Timestamp.now())
                .item_id(item.item_id)
                .url(item.id_url)
                .build()
        )
    }

    override fun unArchive(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().readd()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build()
        )
    }

    override fun unArchive(items: List<Item>) {
        unArchive(*items.toTypedArray())
    }

    override fun unArchive(vararg items: Item) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().readd()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override fun toggleViewed(item: Item) {
        if (item.viewed == true) {
            pocket.sync(
                null,
                pocket.spec().actions().markAsNotViewed()
                    .time(Timestamp.now())
                    .url(item.id_url)
                    .build()
            )
        } else {
            pocket.sync(
                null,
                pocket.spec().actions().markAsViewed()
                    .time(Timestamp.now())
                    .url(item.id_url)
                    .build()
            )
        }
    }

    override fun markAsViewed(vararg items: Item) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().markAsViewed()
                    .time(Timestamp.now())
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override fun markAsNotViewed(vararg items: Item) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().markAsNotViewed()
                    .time(Timestamp.now())
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override fun markAsViewed(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().markAsViewed()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build()
        )
    }

    override fun markAsNotViewed(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().markAsNotViewed()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build()
        )
    }

    override fun delete(item: DomainItem) {
        pocket.sync(
            null,
            pocket.spec().actions().delete()
                .time(Timestamp.now())
                .item_id(item.id)
                .url(UrlString(item.idUrl))
                .build()
        )
    }

    override fun delete(item: Item) {
        pocket.sync(
            null,
            pocket.spec().actions().delete()
                .time(Timestamp.now())
                .item_id(item.item_id)
                .url(item.id_url)
                .build()
        )
    }

    override fun delete(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().delete()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build()
        )
    }

    override fun delete(items: List<Item>) {
        pocket.sync(
            null,
            *items.map { item ->
                pocket.spec().actions().delete()
                    .time(Timestamp.now())
                    .item_id(item.item_id)
                    .url(item.id_url)
                    .build()
            }.toTypedArray()
        )
    }

    override suspend fun save(url: String) {
        pocket.sync(
            null,
            pocket.spec().actions().add()
                .time(Timestamp.now())
                .url(UrlString(url))
                .build()
        ).await()
    }

    override fun setScrollPosition(
        url: String,
        position: Int,
        timeSpentViewing: Int,
    ) {
        pocket.sync(
            null,
            pocket.spec().actions().scrolled()
                .url(UrlString(url))
                .scroll_position(position)
                .time_spent(timeSpentViewing)
                .time(Timestamp.now())
                .view(PositionType.ARTICLE)
                .build()
        )
    }

}

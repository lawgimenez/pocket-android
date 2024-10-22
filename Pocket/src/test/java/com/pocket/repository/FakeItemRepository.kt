package com.pocket.repository

import com.pocket.data.models.DomainItem
import com.pocket.data.models.toDomainItem
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.UrlString
import kotlinx.coroutines.flow.Flow

class FakeItemRepository : ItemRepository {
    private val items = mutableListOf<Item>()
    private val itemsByUrl = mutableMapOf<String, Item>()
    private val itemsByShareSlug = mutableMapOf<String, Item>()

    override suspend fun getDomainItem(url: String): DomainItem {
        return getItemOrThrow(url).toDomainItem()
    }

    override suspend fun getItemOrThrow(
        url: String,
        lookupStrategy: ItemRepository.LookupStrategy,
    ): Item {
        return getItem(url, lookupStrategy) ?: throw NoSuchElementException()
    }

    override suspend fun getItem(
        url: String,
        lookupStrategy: ItemRepository.LookupStrategy
    ): Item? {
        return itemsByUrl[url]
            ?: items.firstOrNull { it.id_url?.url == url }
    }

    override suspend fun getItemByShareSlug(slug: String): Item? {
        return itemsByShareSlug[slug]
    }

    override fun getDomainItemFlow(url: String): Flow<DomainItem> {
        TODO("Not yet implemented")
    }

    override fun toggleFavorite(item: Item) {
        TODO("Not yet implemented")
    }

    override fun favorite(url: String) {
        TODO("Not yet implemented")
    }

    override fun unfavorite(url: String) {
        TODO("Not yet implemented")
    }

    override fun favorite(vararg items: Item) {
        TODO("Not yet implemented")
    }

    override fun unFavorite(vararg items: Item) {
        TODO("Not yet implemented")
    }

    override fun archive(item: Item) {
        TODO("Not yet implemented")
    }

    override fun archive(url: String) {
        TODO("Not yet implemented")
    }

    override fun archive(items: List<Item>) {
        TODO("Not yet implemented")
    }

    override fun unArchive(item: Item) {
        TODO("Not yet implemented")
    }

    override fun unArchive(url: String) {
        TODO("Not yet implemented")
    }

    override fun unArchive(items: List<Item>) {
        TODO("Not yet implemented")
    }

    override fun unArchive(vararg items: Item) {
        TODO("Not yet implemented")
    }

    override fun toggleViewed(item: Item) {
        TODO("Not yet implemented")
    }

    override fun markAsViewed(vararg items: Item) {
        TODO("Not yet implemented")
    }

    override fun markAsNotViewed(vararg items: Item) {
        TODO("Not yet implemented")
    }

    override fun markAsViewed(url: String) {
        TODO("Not yet implemented")
    }

    override fun markAsNotViewed(url: String) {
        TODO("Not yet implemented")
    }

    override fun delete(item: DomainItem) {
        delete(item.idUrl)
    }

    override fun delete(item: Item) {
        delete(item.id_url!!.url)
    }

    override fun delete(url: String) {
        items.removeAll { it.id_url?.url == url }
    }

    override fun delete(items: List<Item>) {
        for (item in items) {
            delete(item)
        }
    }

    override suspend fun save(url: String) {
        items += url.toItem()
    }

    override fun setScrollPosition(url: String, position: Int, timeSpentViewing: Int) {
        TODO("Not yet implemented")
    }

    fun mapItemToUrl(resolvedUrl: String, givenUrl: String) {
        itemsByUrl[givenUrl] = resolvedUrl.toItem()
    }

    fun mapItemToShareSlug(url: String, slug: String) {
        itemsByShareSlug[slug] = url.toItem()
    }

    private fun String.toItem(): Item =
        Item.Builder().given_url(UrlString(this)).build()
}

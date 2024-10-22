package com.pocket.app.reader.queue

import com.pocket.app.list.list.ListManager
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.SearchItem

class SavesListQueueManager(
    private val listManager: ListManager,
    startIndex: Int,
): QueueManager {

    private var currentIndex = startIndex

    private val urls: List<String>
        get() = listManager.list.value.mapNotNull { value ->
            val item: Item? = when (value) {
                is Item -> value
                is SearchItem -> value.item
                else -> null
            }
            item?.id_url?.url
        }

    override fun getPreviousUrl(): String? {
        currentIndex--
        return urls.getOrNull(currentIndex)
    }

    override fun getNextUrl(): String? {
        currentIndex++
        if (currentIndex > urls.size - LOAD_MORE_THRESHOLD) {
            listManager.loadNextPage()
        }
        return urls.getOrNull(currentIndex)
    }

    override fun hasPrevious(): Boolean = urls.getOrNull(currentIndex - 1) != null

    override fun hasNext(): Boolean = urls.getOrNull(currentIndex + 1) != null

    companion object {
        const val LOAD_MORE_THRESHOLD = 10
    }
}
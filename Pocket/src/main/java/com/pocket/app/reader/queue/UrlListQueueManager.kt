package com.pocket.app.reader.queue

class UrlListQueueManager(
    private val urls: List<String>,
    startingIndex: Int,
) : QueueManager {

    private var currentIndex = startingIndex

    override fun getPreviousUrl(): String? {
        currentIndex--
        return urls.getOrNull(currentIndex)
    }

    override fun getNextUrl(): String? {
        currentIndex++
        return urls.getOrNull(currentIndex)
    }

    override fun hasPrevious(): Boolean = urls.getOrNull(currentIndex - 1) != null

    override fun hasNext(): Boolean = urls.getOrNull(currentIndex + 1) != null
}
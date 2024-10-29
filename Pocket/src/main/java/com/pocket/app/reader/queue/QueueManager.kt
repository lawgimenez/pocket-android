package com.pocket.app.reader.queue

/**
 * Interface for getting the previous and next articles
 */
interface QueueManager {
    fun getPreviousUrl(): String? = null
    fun getNextUrl(): String? = null
    fun hasPrevious(): Boolean = false
    fun hasNext(): Boolean = false
}
package com.pocket.app.reader.queue

class EmptyQueueManager : QueueManager {

    override fun getPreviousUrl(): String? = null

    override fun getNextUrl(): String? = null

    override fun hasPrevious(): Boolean = false

    override fun hasNext(): Boolean = false
}
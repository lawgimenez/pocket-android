package com.pocket.util.android.thread

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import java.util.concurrent.atomic.AtomicInteger

class TaskPoolIdlingResource constructor(private val resourceName: String) : IdlingResource {

    private val counter: AtomicInteger = AtomicInteger(0)
    @Volatile private var resourceCallback: ResourceCallback? = null

    override fun getName(): String {
        return resourceName
    }

    override fun isIdleNow(): Boolean {
        return counter.get() == 0
    }

    fun increment() {
        counter.incrementAndGet()
    }

    fun decrement() {
        if (counter.decrementAndGet() == 0) {
            resourceCallback?.onTransitionToIdle()
        }
    }

    fun clear() {
        counter.set(0)
        resourceCallback?.onTransitionToIdle()
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback) {
        resourceCallback = callback
    }
}
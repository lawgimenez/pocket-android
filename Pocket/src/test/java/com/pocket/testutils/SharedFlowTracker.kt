package com.pocket.testutils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SharedFlowTracker<T>(
    private val sharedFlow: SharedFlow<T>
) {
    val sharedFlowUpdates = mutableListOf<T>()

    val lastValue: T
        get() = sharedFlowUpdates.last()
    val firstValue: T
        get() = sharedFlowUpdates.first()

    init {
        CoroutineScope(Dispatchers.Main).launch {
            sharedFlow.collect {
                sharedFlowUpdates.add(it)
            }
        }
    }
}
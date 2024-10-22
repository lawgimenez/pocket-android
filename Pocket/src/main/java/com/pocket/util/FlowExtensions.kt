package com.pocket.util

import androidx.lifecycle.LifecycleOwner
import com.pocket.util.android.repeatOnCreated
import com.pocket.util.android.repeatOnResumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun <T> MutableStateFlow<T>.edit(block: T.() -> T) {
    update {
        it.block()
    }
}

fun <T> Flow<T>.collect(coroutineScope: CoroutineScope, collector: FlowCollector<T>) {
    coroutineScope.launch {
        collect(collector)
    }
}

fun <T> Flow<T>.collectWhenResumed(lifecycleOwner: LifecycleOwner, collector: FlowCollector<T>) {
    lifecycleOwner.repeatOnResumed {
        collect(collector)
    }
}

fun <T> Flow<T>.collectWhenCreated(lifecycleOwner: LifecycleOwner, collector: FlowCollector<T>) {
    lifecycleOwner.repeatOnCreated {
        collect(collector)
    }
}
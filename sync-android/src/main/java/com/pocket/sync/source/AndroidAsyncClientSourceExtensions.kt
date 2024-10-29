package com.pocket.sync.source

import com.pocket.sync.source.threads.AndroidUiThreadPublisher
import com.pocket.sync.source.threads.PendingImpl

/**
 * Allows to call suspending versions of [AppSource] functions
 * proxying the sequence of calls with a shared [PendingResult] which it returns
 * and a default publisher for Android.
 */
inline fun <R> AsyncClientSource.suspending(
    block: SuspendingClientSource<R>.(PendingImpl<R, Throwable>) -> Unit,
): PendingResult<R, Throwable> {
    return suspending(AndroidUiThreadPublisher(), block)
}


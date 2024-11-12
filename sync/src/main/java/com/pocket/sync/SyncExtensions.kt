package com.pocket.sync

import com.pocket.sync.action.Action
import com.pocket.sync.source.PendingResult
import com.pocket.sync.source.result.Status
import com.pocket.sync.source.result.SyncResult
import com.pocket.sync.thing.Thing
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** @see SyncResult.Builder.action */
fun <T : Thing?> SyncResult.Builder<T>.action(
    a: Action,
    status: Status,
    cause: Throwable? = null
) {
    action(a, status, cause, null)
}

/**
 * Suspends until the pending result completes and returns the success value or throws on failure.
 */
suspend fun <T, E : Throwable> PendingResult<T, E>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        onSuccess { continuation.resume(it) }
        onFailure { continuation.resumeWithException(it) }
        continuation.invokeOnCancellation { abandon() }
    }
}

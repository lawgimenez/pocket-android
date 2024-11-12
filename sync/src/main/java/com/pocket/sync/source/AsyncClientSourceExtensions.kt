package com.pocket.sync.source

import com.pocket.sync.action.Action
import com.pocket.sync.await
import com.pocket.sync.source.threads.PendingImpl
import com.pocket.sync.source.threads.Publisher
import com.pocket.sync.thing.Thing
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.cancellation.CancellationException

fun <T : Thing> AsyncClientSource.bindLocalAsFlow(thing: T): Flow<T> = callbackFlow {
    val subscription = bindLocal(
        thing,
        { item ->
            trySendBlocking(item)
        },
        { syncException, _ ->
            //TODO the bind is still emitting values so don't cancel the flow.  What should we do
            // with the exception?
        }
    )
    awaitClose {
        subscription.stop()
    }
}


/**
 * Allows to call suspending versions of [AppSource] functions
 * proxying the sequence of calls with a shared [PendingResult] which it returns.
 */
inline fun <R> AsyncClientSource.suspending(
    publisher: Publisher,
    block: SuspendingClientSource<R>.(PendingImpl<R, Throwable>) -> Unit,
): PendingResult<R, Throwable> {
    val proxy = PendingImpl<R, Throwable>(publisher)
    val pocket = SuspendingClientSource(this, proxy)
    try {
        pocket.block(proxy)
    } catch (e: Throwable) {
        proxy.fail(e)
    }
    return proxy
}

/* For use via [suspending]. */
class SuspendingClientSource<R>(
    private val source: AsyncClientSource,
    private val proxy: PendingImpl<R, Throwable>,
) {
    suspend fun <T : Thing> sync(thing: T, vararg actions: Action): T {
        return source.sync(thing, *actions)
            .also { proxy.proxy(it) }
            .await()
    }

    suspend fun <T : Thing> syncRemote(thing: T, vararg actions: Action): T {
        return source.syncRemote(thing, *actions)
            .also { proxy.proxy(it) }
            .await()
    }
}

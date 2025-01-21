package com.pocket.sdk

import com.pocket.sdk.api.generated.PocketActions
import com.pocket.sync.action.Action
import com.pocket.sync.thing.Thing
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T : Thing> Pocket.getLocal(thing: T): T = suspendCancellableCoroutine { continuation ->
    syncLocal(
        thing,
    ).onSuccess {
        continuation.resume(it)
    }.onFailure {
        continuation.cancel(it)
    }.onComplete {
        //is this the right thing to do here?
        continuation.cancel()
    }
}

suspend fun <T : Thing> Pocket.get(thing: T): T = suspendCancellableCoroutine { continuation ->
    sync(
        thing,
    ).onSuccess {
        continuation.resume(it)
    }.onFailure {
        continuation.cancel(it)
    }.onComplete {
        //is this the right thing to do here?
        continuation.cancel()
    }
}

suspend fun <A : Action> Pocket.update(action: A) = suspendCancellableCoroutine { continuation ->
    sync(
        null,
        action
    ).onSuccess {
        continuation.resume(Unit)
    }.onFailure {
        continuation.cancel(it)
    }.onComplete {
        //is this the right thing to do here?
        continuation.cancel()
    }
}

fun Pocket.send(builderAction: PocketActions.() -> Action) {
    sync(null, spec().actions().builderAction())
}

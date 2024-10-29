package com.pocket.app.share

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.PocketShare
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.api.value.toValidUrl
import com.pocket.sync.source.Subscribeable
import com.pocket.sync.source.result.SyncException
import com.pocket.sync.source.subscribe.Changes
import com.pocket.sync.source.subscribe.Subscription
import com.pocket.sync.source.subscribe.WrappedSubscription
import com.pocket.sync.thing.Thing
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ShareRepository
@Inject constructor(
    private val pocket: Pocket,
) {
    /** Call the API to wrap [targetUrl] in a Share Link. */
    suspend fun createShareLink(targetUrl: String): PocketShare = suspendCancellableCoroutine { cont ->
        val subscription = createShareLink(
            targetUrl = targetUrl,
            onLinkCreated = { cont.resume(it) },
            onFailed = { cont.resumeWithException(it) }
        )
        cont.invokeOnCancellation { subscription.stop() }
    }

    /**
     * Only the suspend version above is intended for direct use.
     * I decided to extract this to separate the coroutines ceremony from
     * the fandangling the sync engine currently requires to return action/mutation result.
     */
    private fun createShareLink(
        targetUrl: String,
        onLinkCreated: (PocketShare) -> Unit,
        onFailed: (SyncException) -> Unit,
    ): Subscription {
        // The code is written kind of "upside down" relative to the actual flow, because of
        // how the callback based sync APIs work. syncRemote will actually run first and when it
        // returns it will trigger the callback passed to subscribeSingle.
        //
        // This works because we pass targetUrl to both places and it is the identity which
        // the sync engine uses to match the response to the subscription.
        val share = PocketShare.Builder()
            .targetUrl(targetUrl.toValidUrl())
            .build()
        val subscription = pocket.subscribeSingle(Changes.of(share), onLinkCreated)
        pocket.syncRemote(
            null,
            pocket.spec().actions().createShareLink()
                .time(Timestamp.now())
                .target(targetUrl.toValidUrl())
                .build()
        ).onFailure(onFailed)
        // Return the subscription in case we want to cancel before the response comes back.
        return subscription
    }

    /**
     * Listens to just the first future state change.
     *
     * This is something we could make public and move to a more appropriate file if more places
     * need this pattern. But for now separated from the above, because it was yet another piece
     * of fandangling that could be self contained.
     * @see Subscribeable.subscribe
     */
    private inline fun <T: Thing> Subscribeable.subscribeSingle(
        change: Changes<T>,
        crossinline onUpdate: (T) -> Unit,
    ): Subscription {
        // Create a wrapper, so we can reference and stop a subscription from its onUpdate callback.
        val wrapper = WrappedSubscription()
        subscribe(change) {
            onUpdate(it)
            // We're only interested in the first update, so stop the subscription (via the wrapper).
            wrapper.stop()
        }.also {
            // Connect the actual subscription with the wrapper, so the .stop() call above works.
            wrapper.setSubscription(it)
        }
        return wrapper
    }
}

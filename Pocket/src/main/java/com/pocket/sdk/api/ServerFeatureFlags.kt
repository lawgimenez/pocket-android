package com.pocket.sdk.api

import android.content.Context
import android.view.View
import com.pocket.analytics.Tracker
import com.pocket.app.*
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.endpoint.AndroidDeviceInfo
import com.pocket.sdk.api.generated.Modeller
import com.pocket.sdk.api.generated.enums.UnleashEnvironment
import com.pocket.sdk.api.generated.thing.GetUnleashAssignments
import com.pocket.sdk.api.generated.thing.UnleashAssignment
import com.pocket.sdk.api.generated.thing.UnleashContext
import com.pocket.sdk.api.generated.thing.UnleashProperties
import com.pocket.sdk.dev.ErrorHandler
import com.pocket.sync.source.PendingResult
import com.pocket.sync.source.result.SyncException
import com.pocket.sync.source.suspending
import com.pocket.sync.space.Holder
import com.pocket.sync.spec.Syncable
import com.pocket.sync.value.SyncableParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature flags and ab tests provided by our backend API, specifically, unleash.
 *
 * In unleash, these are called "Feature Toggles".
 * They are managed via https://featureflags.readitlater.com.
 * See docs on [GetUnleashAssignments] for additional details on creating and managing toggles.
 *
 * ## Important notes on Assignment consistency
 *
 * This class provides methods for querying the current "assignment" of a toggle for the current device or user.
 * Assignment here means whether the flag should be on for this user/device, or in the case of a test, what variant it should see.
 *
 * Assignments are based on the current user/device context and can change as that context changes.
 * If a user logs in or out, the assignments might change.
 * The assignments might also change if the toggle configuration is updated on the admin interface and is synced down.
 *
 * ## Async methods
 *
 * All calls are async, if you need faster access, have a component that queries and caches the value.
 * But make sure you clear that value on log out!
 *
 * ## Analytics / Tracking
 *
 * Each time you check an assignment will automatically fire a tracking event
 * if the assignment exists.
 * We don't want to send tracking events if we don't have the assignment yet,
 * because that's going to skew the results.
 *
 * Currently we only use our custom Snowplow-powered pipeline for tracking.
 * Unleash has its own tracking feature via the metrics endpoint:
 * https://unleash.github.io/docs/api/client/metrics
 *
 * If at some point it proves useful to also support the metrics endpoint,
 * here's some code that implemented it, before we decided not to include it for now:
 * https://github.com/Pocket/Android/pull/1543/files#diff-6c695b6a5d7f838b56c6863bf1259b4ae84c45f24671d95c6d4bb333e983fe25R6155
 * https://github.com/Pocket/Android/pull/1543/files#diff-0181435323ccd40336bf9e2f67b822fb583ad56fed5d6c3c3d6db8cfb2a3eed9R131
 */
@Singleton
class ServerFeatureFlags @Inject constructor(
    private val pocket: Pocket,
    appSync: AppSync,
    private val mode: AppMode,
    @ApplicationContext context: Context,
    private val tracker: Tracker,
    private val appScope: AppScope,
    private val errorReporter: ErrorHandler,
    dispatcher: AppLifecycleEventDispatcher
) : AppLifecycle {

    private val baseContext = UnleashContext.Builder()
        .appName("android")
        .environment(when (mode) {
            AppMode.PRODUCTION -> UnleashEnvironment.PROD
            AppMode.TEAM_ALPHA, AppMode.DEV -> UnleashEnvironment.ALPHA
        })
        .properties(UnleashProperties.Builder()
            .locale(AndroidDeviceInfo.localeFrom(context)) // TODO register for ACTION_LOCALE_CHANGED to update this when device language changes.
            .build())
        .build()

    init {
        dispatcher.registerAppLifecycleObserver(this)
        pocket.setup {
            val unleashThing = pocket.spec().things().unleash()
                .current_assignments(emptyMap())
                .build()
            pocket.remember(Holder.persistent("unleash"), unleashThing)
            pocket.initialize(unleashThing)
        }
        // Update during sync.
        appSync.addWork { _, _, _ -> refresh() }
    }

    override fun onLoggingIn(isNewUser: Boolean) {
        super.onLoggingIn(isNewUser)
        // Best attempt to make sure we have the user toggles before we show any post-log ui.
        try {
            refresh().get()
        } catch (e: SyncException) {
            errorReporter.reportError(RuntimeException("Failed to get Unleash assignments on login.", e))
        }
    }

    /**
     * Returns the assignment info for [flag] automatically enrolling the user in the A/B test.
     *
     * @param view If available a view to attach additional UI context in the tracking information.
     */
    fun get(flag: String, view: View? = null): PendingResult<UnleashAssignment?, Throwable> {
        return get(flag) { enroll(it, view) }
    }

    /**
     * **WARNING: Use very carefully!**
     *
     * [get]s without automatically enrolling the user in the A/B test. Don't do this unless you
     * have a good reason and ideally you consulted this with the Data Analytics Team.
     * By default just use [get]. And if you do use this method, make sure you also use [get] later
     * to correctly enroll the user at some point.
     *
     * ## When would it make sense to do this?
     * Well, first (just to reiterate) after you talked to your data scientist and they confirmed
     * this makes sense and is safe.
     *
     * ## But what makes sense and is safe?
     * That you want to check the A/B test variant assignment before the user is going to see any
     * change in UX. The theory is that you should enroll the user as close to the actual difference
     * they notice.
     *
     * ## But why would I want to check the flag if I don't want to do anything visibly different?
     * The key is in the word "visibly". So if you want to do something transparent to the user that's
     * different based on which test variant they are in, then you shouldn't enroll them to the test yet.
     *
     * ## What would be an example of that?
     * If you want to preload data that's different based on the A/B test variant. Since you are
     * preloading, they won't actually see the data or any difference at all at this point.
     * They will see the difference once they open a screen that displays the preloaded data.
     * And at the point of opening the screen you should check the A/B test variant again,
     * using [get] this time, which will enroll the user.
     *
     * There might be other cases, but this is one we've used so far and ran it by the Data Analytics Team.
     */
    @Suppress("unused")
    fun getWithoutEnrolling(flag: String): PendingResult<UnleashAssignment?, Throwable> {
        return get(flag, enroll = {})
    }

    private inline fun get(
        flag: String,
        crossinline enroll: (UnleashAssignment?) -> Unit,
    ): PendingResult<UnleashAssignment?, Throwable> {
        return pocket.suspending { pending ->
            appScope.launch {
                val unleash = sync(pocket.spec().things().unleash().build())
                val overrides = if (mode.isForInternalCompanyOnly) {
                    unleash.overridden_assignments ?: emptyMap()
                } else {
                    emptyMap()
                }
                val assignment = overrides[flag] ?: unleash.current_assignments?.get(flag)
                enroll(assignment)
                pending.success(assignment)
            }
        }
    }

    private fun enroll(
        assignment: UnleashAssignment?,
        view: View?,
    ) {
        if (assignment?.assigned != true) {
            // Not eligible for the test, don't enroll.
            return
        }
        if (assignment.variant == null) {
            // All A/B tests have to configure variants.
            return
        }
        tracker.trackVariantEnroll(assignment.name!!, assignment.variant!!, view)
    }

    /**
     * Refresh all assignments from the server.
     *
     * If a guid hasn't been obtained yet, this will attempt to get it first.
     * If it isn't able to obtain a guid or the toggles, this will fail.
     */
    fun refresh(): PendingResult<Any, Throwable> {
        return pocket.suspending { result ->
            appScope.launch {
                try {
                    // 1. First get the user id.
                    val login = sync(pocket.spec().things().loginInfo().build())
                    // 2. Then also get the guid.
                    val guid = sync(pocket.spec().things().guid().build())
                    // 3. Then request the assignments for this user
                    val assignments = syncRemote(
                        pocket.spec().things().unleashAssignments
                            .context(run {
                                val context = baseContext.builder()
                                context.sessionId(guid.guid)
                                if (login.account?.user_id != null) {
                                    context.userId(login.account!!.user_id)
                                }
                                context.properties(
                                    baseContext.properties?.builder()
                                        ?.build()!!
                                )
                                context.build()
                            })
                            .build())
                    result.success(assignments)
                } catch (e: Exception) {
                    result.fail(e)
                }
            }
        }
    }
}

/**
 * Helper for parsing the [UnleashAssignment.payload] after [get]ting an assignment.
 *
 * It's recommended to define your payload as a type in `local.graphqls`.
 * In this case [payloadCreator] is the generated model's `JSON_CREATOR`.
 */
fun <Payload> UnleashAssignment?.parsePayload(payloadCreator: SyncableParser<Payload>): Payload? {
    if (this == null || assigned != true || payload == null) return null

    return payload
        .let { Modeller.OBJECT_MAPPER.readTree(it) }
        .let { payloadCreator.create(it, Syncable.NO_ALIASES) }
}

package com.pocket.analytics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.iterator
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.pocket.analytics.api.*
import com.pocket.analytics.api.UiEntityable.Companion.identifierFromReferrer
import com.pocket.analytics.entities.BrowserContext
import com.pocket.analytics.tools.WindowViewListener
import com.pocket.util.prefs.Preferences
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import org.threeten.bp.Clock
import java.util.*

private object Schema {
    // Events:
    const val Impression = "iglu:com.pocket/impression/jsonschema/1-0-2"
    const val Engagement = "iglu:com.pocket/engagement/jsonschema/1-0-1"
    const val ContentOpen = "iglu:com.pocket/content_open/jsonschema/1-0-0"
    const val AppOpen = "iglu:com.pocket/app_open/jsonschema/1-0-0"
    const val AppBackground = "iglu:com.pocket/app_background/jsonschema/1-0-0"
    const val VariantEnroll = "iglu:com.pocket/variant_enroll/jsonschema/1-0-0"

    // Entities/contexts:
    const val Ui = "iglu:com.pocket/ui/jsonschema/1-0-3"
    const val Content = "iglu:com.pocket/content/jsonschema/1-0-0"
    const val User = "iglu:com.pocket/user/jsonschema/1-0-1"
    const val ApiUser = "iglu:com.pocket/api_user/jsonschema/1-0-1"
    const val FeatureFlag = "iglu:com.pocket/feature_flag/jsonschema/1-0-0"
    const val Report = "iglu:com.pocket/report/jsonschema/1-0-0"
    const val CorpusRecommendation = "iglu:com.pocket/corpus_recommendation/jsonschema/1-0-0"

}

/**
 * [Tracker] that sends Snowplow events.
 * 
 * Automatically tracks impressions and general engagements for main use cases.
 * Automatically attaches api_user and user contexts to all events.
 * If a view is provided for an event it also attaches ui and content contexts.
 * 
 * In an ideal world here's how you use the tracker to send events to a Snowplow collector.
 * 
 * ## Tracking events for a new view
 * The easiest and most common case. As you're working on a new feature, you're adding new views
 * and want to send Snowplow events for them. (A related scenario we're going to have for a while
 * is adding Snowplow tracking to existing features.) There's basically only two things to do:
 * 1. Add a an identifier to a view that will be used to build a UI entity.
 * 2. Enable tracking of the required event (or events). This works different for different events
 * and even in some cases there might be nothing to do.
 * 
 * ### Ui entity identifier
 * There are a few ways to configure an identifier for a view. Here they are in order from most
 * recommended to least:
 * 1. If it's one of our UI components that is [UiEntityable] set it in XML using
 * `app:uiEntityIdentifier` ([R.attr.uiEntityIdentifier]).
 * 2. If you can't set it in XML (because for example the view is created dynamically in code)
 * set it as soon after creation as possible by setting [UiEntityable.uiEntityIdentifier]
 * (`.setUiEntityIdentifier()` from the Java Programming Language).
 * 3. For composite UI components made up of multiple views that you would want to set different
 * identifiers for, look for binder methods that accept identifiers.
 * 4. It's possible that some very specialized components may set a default identifier that may
 * be okay for your case. Like a `SubmitButton` might set it to `submit`. But you should still be
 * able to set [UiEntityable.uiEntityIdentifier] again to overwrite the default value.
 * 5. If it's a view that doesn't implement [UiEntityable], but it's something we wrote or we can
 * modify, then implement it and then use one of the above options.
 * 6. If it's not possible, because it's a view we don't control and don't want to subclass or wrap,
 * then use [bindUiEntityIdentifier], preferably as close to inflating/creating/binding this view
 * as possible, not waiting till just before the event happens.
 * 
 * Here are some edge cases:
 * * Some views are dynamic, in the sense that for performance or whatever reason there's a single
 * view which can change/re-bind and be used for something that is logically/semantically a different
 * view to the user. It's totally okay to set the identifier multiple times, preferably as close
 * to updating/re-binding the view as possible. A classic example is a play/pause button that
 * changes the icon and the action based on the state of the player. Just update the identifier
 * at the same time you update the icon, label, accessibility content description, etc.
 *
 * * For some extremely dynamic views maybe it's only in the on click listener that you can decide
 * what this view is at the moment. Or for some weird APIs (that we don't control and can't update)
 * maybe you never get to even touch the view and can only pass in a click listener. In these cases
 * wrap the listener in `OnClickEngagementListener` (available in the `Pocket` module).
 *
 *    **Be warned** that this approach works only for tracking engagements (clicks).
 *    Other events like impressions might pick up an outdated identifier.
 *
 * ### Engagements
 * Engagements at the moment literally mean clicks. There's only two cases:
 * 1. If it's our UI Component it should already implement [Engageable] and the tracker should
 * automatically be notified about its clicks.
 * 2. If it's a view we don't control, call [trackEngagement] directly from the click listener
 * you set on it.
 * 
 * Also be sure you declared an identifier for the UI entity representing the clicked view
 * (see section above).
 * 
 * ### Impressions
 * We have two main use cases for impressions.
 * 
 * First is rows/tiles/pages in scrolling views backed by adapters (so mostly really just
 * [RecyclerView], another example would be a `ViewPager`, but nowadays it's actually also backed
 * by a recycler). When creating a view in the adapter call [enableImpressionTracking] to turn on
 * and configure automatic impression tracking. Be sure to also set/bind an identifier for the UI
 * entity representing this view when binding data to it.
 * 
 * Second is screen impressions. If you use a standard fragment for a screen this is handled by
 * the base class. But basically it is just a manual call to [trackImpression] when the screen is
 * shown or re-shown (so for example [Fragment.onStart]). And of course setting an identifier
 * (`AbsPocketFragment` handles it in `onViewCreatedImpl` by binding a value declared in
 * `getScreenIdentifier()`)
 * 
 * ### Content opens
 * Content opens are always tracked manually by calling [trackContentOpen]. If initiated by user's
 * click include the clicked view and be sure to configure the UI entity identifier for it.
 * 
 * ## Supporting automatic tracking in new UI components
 * A more complicated case is when we create or add to a project a new UI component or want to
 * start tracking an existing component for the first time.
 * 
 * ### Implementing [UiEntityable] and [Engageable]
 * For components that we control and can extend implementing [UiEntityable] and [Engageable]
 * provides the most seamless tracking configuration by the code using them and in some cases
 * automatic tracking. [UiEntityableHelper] and [EngageableHelper] are provided for ease and
 * consistency of implementations.
 * 
 * ### Configuring UI entity type
 * Another thing besides the identifier that is required in UI entities is the type. Type generally
 * is the same for all usages of a given UI component, so usually it's enough to set it once
 * and there's no need to worry about it with each new use of the component.
 * 
 * If the component supports it set [UiEntityable.uiEntityType] (and optionally depending on the
 * requirements [UiEntityable.uiEntityComponentDetail]). Preferably do this in component's
 * constructor.
 * 
 * If the component doesn't support it use [bindUiEntityType]. Preferably do this in a single
 * centralized place with all of the app-specific tracker configuration (see `PocketTracker.kt`'s
 * `configure()` function).
 */
class SnowplowTracker(
    prefs: Preferences,
    context: Context,
    private val clock: Clock,
    collectorEndpoint: String,
    collectorPath: String,
    private val isInternalBuild: Boolean,
    private val getAdjustId: () -> String?,
    private val apiId: Int,
    private val clientVersion: String,
) : TrackerConfig, Tracker {

    var browserContext: BrowserContext? = null

    private val tracker by lazy {
        Snowplow.createTracker(
            context,
            "default",
            NetworkConfiguration(collectorEndpoint, HttpMethod.POST).customPostPath(collectorPath),
            TrackerConfiguration(if (isInternalBuild) "pocket-android-dev" else "pocket-android")
        ).apply {
            try {
                globalContexts.add("browserContext") {
                    buildList {
                        browserContext?.let {
                            add(it.toSelfDescribingJson())
                        }
                    }
                }
            } catch (ignored: Throwable) {
                // This crashes on SDK 23 (Android 6), because it uses java.util.function under the hood.
                // It is available natively from SDK 24. It requires coreLibraryDesugaring on older SDKs.
                // We're not using it at the moment, have no plan to start using it
                // and working around this issue isn't a good enough reason to start.
                // More likely we'll stop supporting SDK 23.

                // This means global browser_context won't work on SDK 23 (or wherever it crashes).
            }
        }
    }

    private var guid: String? = null
    private var userId: String? = null
    private var userEmail: String? = null

    private val lastAppOpen = prefs.forApp("snwplw_lao", 0L)
    private val lastAppBackground = prefs.forApp("snwplw_lab", 0L)

    private val uiTypesByInstance = WeakHashMap<View, UiEntityType>()
    private val uiTypesByClass = mutableMapOf<Class<*>, UiEntityType>()
    private val uiDetails = mutableMapOf<Class<*>, String>()
    private val uiIdentifiers = WeakHashMap<View, String>()
    private val uiValues = WeakHashMap<View, String>()
    private val contents = WeakHashMap<View, Content>()
    private val indexProviders = mutableListOf<TrackerConfig.IndexProvider>()
    private val impressionComponents = WeakHashMap<View, ImpressionComponent>()
    private val impressionRequirements =
        mutableMapOf<ImpressionRequirement, TrackerConfig.ImpressionRequirementChecker>()

    // The snowplow sdk Verbose log level is too verbose, and Debug isn't verbose enough
    // Just printing the event context for easy debugging
    private fun SelfDescribing.log() {
        Log.d(SnowplowTracker::class.simpleName,  "{\"event\": $eventData, \"contexts\": $entities}")
    }

    override fun track(event: com.pocket.analytics.events.Event) {
        tracker.trackAndLog(
            event.toSelfDescribing().apply {
                entities.addUser()
                entities.addApiUser()
            }
        )
    }

    private fun TrackerController.trackAndLog(event: SelfDescribing): UUID? {
        if (isInternalBuild) event.log()
        return track(event)
    }

    override fun registerIndexProvider(indexProvider: TrackerConfig.IndexProvider) {
        indexProviders.add(indexProvider)
    }

    override fun registerImpressionRequirement(
        requirement: ImpressionRequirement,
        checker: TrackerConfig.ImpressionRequirementChecker,
    ) {
        impressionRequirements[requirement] = checker
    }

    override fun bindUiEntityType(view: View, type: UiEntityType) {
        uiTypesByInstance[view] = type
    }

    override fun bindUiEntityType(component: Class<*>, type: UiEntityType, detail: String?) {
        uiTypesByClass[component] = type
        if (detail != null) uiDetails[component] = detail
    }

    override fun bindUiEntityIdentifier(view: View, identifier: String) {
        uiIdentifiers[view] = identifier
    }
    
    override fun bindUiEntityValue(view: View, value: String) {
        uiValues[view] = value
    }
    
    override fun bindContent(view: View, content: Content) {
        contents[view] = content
    }

    override fun enableImpressionTracking(
        view: View,
        component: ImpressionComponent,
        uniqueId: Any,
    ) {
        impressionComponents[view] = component
        for (requirement in impressionRequirements.values) {
            requirement.enableFor(view, uniqueId)
        }
        checkImpressions(view)
    }

    override fun trackImpression(
        view: View,
        component: ImpressionComponent,
        requirement: ImpressionRequirement,
        corpusRecommendation: CorpusRecommendation?
    ) {
        // Only track when there is a valid UI entity for [view]
        if (view.toCustomContext(0) == null) return

        tracker.trackAndLog(
            SelfDescribing(
                Schema.Impression,
                mapOf("component" to component.value, "requirement" to requirement.value)
            ).apply {
                entities.addStandardCustomContexts(view)
                if (corpusRecommendation != null) {
                    entities.add(corpusRecommendation.toCustomContext())
                }
            }
        )
    }

    override fun trackEngagement(
        view: View,
        type: EngagementType,
        value: String?,
        report: Report?,
        corpusRecommendation: CorpusRecommendation?
    ) {
        // Only track when there is a valid UI entity for [view]
        if (view.toCustomContext(0) == null) return

        tracker.trackAndLog(
            SelfDescribing(
                Schema.Engagement,
                buildMap {
                    put("type", type.value)
                    if (value != null) put("value", value)
                }
            ).apply {
                entities.addStandardCustomContexts(view)
                if (corpusRecommendation != null) {
                    entities.add(corpusRecommendation.toCustomContext())
                }
                report?.toCustomContext()?.let { entities.add(it) }
            }
        )
    }

    override fun trackEngagement(externalView: ExternalView, type: EngagementType, value: String?, corpusRecommendation: CorpusRecommendation?) {
        tracker.trackAndLog(
            SelfDescribing(
                Schema.Engagement,
                buildMap {
                    put("type", type.value)
                    if (value != null) put("value", value)
                }
            ).apply {
                entities.add(externalView.toCustomContext())
                if (corpusRecommendation != null) {
                    entities.add(corpusRecommendation.toCustomContext())
                }
                entities.addUser()
                entities.addApiUser()
            }
        )
    }

    override fun trackContentOpen(
        view: View?,
        destination: ContentOpenDestination,
        trigger: ContentOpenTrigger,
        corpusRecommendation: CorpusRecommendation?
    ) {
        tracker.trackAndLog(
            SelfDescribing(
                Schema.ContentOpen,
                mapOf("destination" to destination.value, "trigger" to trigger.value)
            ).apply {
                entities.addStandardCustomContexts(view)
                if (corpusRecommendation != null) {
                    entities.add(corpusRecommendation.toCustomContext())
                }
            }
        )
    }

    override fun trackContentOpen(
        link: Link,
        destination: ContentOpenDestination,
        trigger: ContentOpenTrigger,
        corpusRecommendation: CorpusRecommendation?
    ) {
        tracker.trackAndLog(
            SelfDescribing(
                Schema.ContentOpen,
                mapOf("destination" to destination.value, "trigger" to trigger.value)
            ).apply {
                entities.add(link.toCustomContext())
                entities.addStandardCustomContexts(link.view)
                if (link.referrer != null) {
                    val hierarchy = entities.count {
                        it.map[Parameters.SCHEMA] == Schema.Ui
                    }
                    entities.add(link.toReferrerContext(hierarchy))
                }
                if (corpusRecommendation != null) {
                    entities.add(corpusRecommendation.toCustomContext())
                }
            }
        )
    }

    override fun trackAppOpen(deepLink: String?, referrer: String?) {
        tracker.trackAndLog(
            SelfDescribing(
                Schema.AppOpen,
                buildMap {
                    put("seconds_since_last_open", secondsSinceLastAppOpen())
                    put("seconds_since_last_background", secondsSinceLastAppBackground())
                    if (deepLink != null) put("deep_link", deepLink)
                    if (referrer != null) put("referring_app", referrer)
                }
            ).apply {
                entities.addStandardCustomContexts()
            }
        )

        lastAppOpen.set(clock.instant().epochSecond)
    }

    override fun trackAppBackground() {
        tracker.trackAndLog(
            SelfDescribing(
                Schema.AppBackground,
                buildMap {
                    put("seconds_since_last_open", secondsSinceLastAppOpen())
                    put("seconds_since_last_background", secondsSinceLastAppBackground())
                }
            ).apply {
                entities.addStandardCustomContexts()
            }
        )

        lastAppBackground.set(clock.instant().epochSecond)
    }

    private fun secondsSinceLastAppOpen(): Int? {
        return if (lastAppOpen.isSet) {
            (clock.instant().epochSecond - lastAppOpen.get()).toInt()
        } else {
            null
        }
    }

    private fun secondsSinceLastAppBackground(): Int? {
        return if (lastAppBackground.isSet) {
            (clock.instant().epochSecond - lastAppBackground.get()).toInt()
        } else {
            null
        }
    }

    override fun trackVariantEnroll(name: String, variant: String, view: View?) {
        tracker.trackAndLog(
            SelfDescribing(
                Schema.VariantEnroll,
                emptyMap()
            ).apply {
                entities.add(
                    SelfDescribingJson(
                        Schema.FeatureFlag,
                        mapOf("name" to name, "variant" to variant)
                    )
                )
                entities.addStandardCustomContexts(view)
            }
        )
    }

    override fun setGuid(guid: String?) {
        this.guid = guid
    }

    override fun setAccount(userId: String?, email: String?) {
        this.userId = userId
        userEmail = email
    }

    private val engagementListener = EngagementListener { view, value ->
        trackEngagement(view, EngagementType.GENERAL, value)
    }

    private val impressionListener = object :
        RecyclerView.OnScrollListener(),
        NestedScrollView.OnScrollChangeListener {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            dispatchCheckImpressionsToChildren(recyclerView)
        }

        override fun onScrollChange(v: NestedScrollView, x: Int, y: Int, oldX: Int, oldY: Int) {
            dispatchCheckImpressionsToChildren(v)
        }

        private fun dispatchCheckImpressionsToChildren(parent: ViewGroup) {
            for (child in parent) {
                child.dispatchCheckImpressions()
            }
        }
    }

    init {
        val app = context.applicationContext as? Application
        app?.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // attach the tracker onActivityCreated. This may have already occurred
                // when the Window was created via WindowViewListener, however since that
                // method uses reflection, we do so here in order to prevent most events from
                // breaking in the case where the reflection fails for any reason.
                activity.window.decorView.attachTracker()
            }
            override fun onActivityStarted(activity: Activity) {
                // Window visibility is still INVISIBLE at this point, so post the dispatch.
                activity.window.decorView.post {
                    activity.window.decorView.dispatchCheckImpressions()
                }
            }
            override fun onActivityStopped(activity: Activity) {
                activity.window.decorView.dispatchCheckImpressions()
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        // listen for Views added to new Windows, including activities, dialogs, popup menus, etc.
        WindowViewListener.onViewAdded {
            it.attachTracker()
        }
    }

    private fun View.attachTracker() {
        // Ensure that multiple attempts to attach the tracker to this and its subviews are ignored.
        val tag = getTag(R.id.TAG_TRACKED)
        if (tag is Boolean && tag) {
            return
        }
        setTag(R.id.TAG_TRACKED, true)

        // Attach to this.
        if (this is Engageable) setEngagementListener(engagementListener)

        if (this is ViewGroup) {
            // Attach to existing children.
            for (child in this) child.attachTracker()

            setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View) {
                    // Attach to future children.
                    child.attachTracker()
                }

                override fun onChildViewRemoved(parent: View?, child: View) {
                    // Detach from removed children.
                    child.detachTracker()
                }
            })
        }

        // Check impressions now.
        checkImpressions(this)
        // Recheck impressions on scroll, because some conditions might become satisfied.
        if (this is RecyclerView) {
            addOnScrollListener(impressionListener)
        }
        if (this is NestedScrollView) {
            // This is dangerous, because someone can overwrite this listener.
            // But looks like we don't have a better API.
            setOnScrollChangeListener(impressionListener)
        }
        // Handle other scrollable containers as needed.
    }

    private fun View.detachTracker() {
        setTag(R.id.TAG_TRACKED, false)

        if (this is Engageable) setEngagementListener(null)
        if (this is ViewGroup) setOnHierarchyChangeListener(null)

        // Trigger impression check, so that conditions can register the view isn't visible any more.
        dispatchCheckImpressions()
    }

    private fun checkImpressions(view: View) {
        val component = impressionComponents[view] ?: return

        for ((requirement, implementation) in impressionRequirements) {
            implementation.whenRequirementSatisfied(view) {
                trackImpression(view, component, requirement)
            }
        }
    }
    
    private fun View.dispatchCheckImpressions() {
        checkImpressions(this)
        
        if (this is ViewGroup) {
            for (child in this) child.dispatchCheckImpressions()
        }
    }

    private fun MutableList<SelfDescribingJson>.addStandardCustomContexts(
        view: View? = null,
        customContent: SelfDescribingJson? = null,
    ) {
        addFromViewHierarchy(view, customContent)

        // NOTE: For user and api_user it might be possible to use tracker.globalContexts
        // instead of adding them manually like this.
        addUser()
        addApiUser()
    }

    private fun MutableList<SelfDescribingJson>.addFromViewHierarchy(
        leaf: View?,
        customContent: SelfDescribingJson?,
    ) {
        var contentFound = false
        if (customContent != null) {
            add(customContent)
            contentFound = true
        }

        var hierarchy = count { it.map[Parameters.SCHEMA] == Schema.Ui }

        var view: View? = leaf
        while (view != null) {
            contents[view]?.let { content ->
                if (!contentFound) {
                    add(content.toCustomContext())
                    contentFound = true
                }
            }

            view.toCustomContext(hierarchy)
                ?.let {
                    hierarchy++
                    add(it)
                }

            view = view.parent as? View
        }
    }

    private fun MutableList<SelfDescribingJson>.addUser() {
        val hashedGuid = guid
        val hashedUserId = userId
        val email = userEmail
        val adjustId = getAdjustId()
        hashedGuid ?: hashedUserId ?: email ?: adjustId ?: return

        add(
            SelfDescribingJson(
                Schema.User,
                buildMap {
                    if (hashedGuid != null) put("hashed_guid", hashedGuid)
                    if (hashedUserId != null) put("hashed_user_id", hashedUserId)
                    if (email != null) put("email", email)
                    if (adjustId != null) put("adjust_id", adjustId)
                }
            )
        )
    }

    private fun MutableList<SelfDescribingJson>.addApiUser() {
        add(
            SelfDescribingJson(
                Schema.ApiUser,
                mapOf("api_id" to apiId, "client_version" to clientVersion)
            )
        )
    }

    private fun View.toCustomContext(hierarchy: Int): SelfDescribingJson? {
        val identifier = identifier() ?: return null
        val type = type() ?: return null

        return SelfDescribingJson(
            Schema.Ui,
            buildMap {
                put("hierarchy", hierarchy)
                put("identifier", identifier)
                put("type", type.value)

                details()?.let { put("component_detail", it) }
                enUsLabel()?.let { put("label", it) }
                index()?.let { put("index", it) }
                value()?.let { put("value", it) }
            }
        )
    }

    private fun View.identifier(): String? {
        if (this in uiIdentifiers) return uiIdentifiers[this]
        if (this is UiEntityable && uiEntityIdentifier != null) return uiEntityIdentifier

        return null
    }

    private fun View.type(): UiEntityType? {
        if (this in uiTypesByInstance) return uiTypesByInstance[this]
    
        val byClass = uiTypesByClass.recursiveGet(this::class.java)
        if (byClass != null) return byClass
    
        if (this is UiEntityable && uiEntityType != null) {
            return uiEntityType?.toSnowplowUiType()
        }
        
        return null
    }
    
    private fun View.details(): String? {
        val byClass = uiDetails.recursiveGet(this::class.java)
        if (byClass != null) return byClass
        
        if (this is UiEntityable && uiEntityComponentDetail != null) return uiEntityComponentDetail
        
        return null
    }
    
    private fun View.enUsLabel() = (this as? UiEntityable)?.uiEntityLabel
    
    private fun View.index(): Int? {
        for (provider in indexProviders) {
            provider.indexFor(this)?.let {
                return it
            }
        }

        return null
    }
    
    private fun View.value(): String? {
        return when (this) {
            in uiValues -> uiValues[this]
            is UiEntityable -> uiEntityValue
            else -> null
        }
    }
}

private fun UiEntityable.Type.toSnowplowUiType() = when (this) {
    UiEntityable.Type.BUTTON -> UiEntityType.BUTTON
    UiEntityable.Type.DIALOG -> UiEntityType.DIALOG
    UiEntityable.Type.MENU -> UiEntityType.MENU
    UiEntityable.Type.CARD -> UiEntityType.CARD
    UiEntityable.Type.LIST -> UiEntityType.LIST
    UiEntityable.Type.SCREEN -> UiEntityType.SCREEN
    UiEntityable.Type.PAGE -> UiEntityType.PAGE
    UiEntityable.Type.READER -> UiEntityType.READER
}

/**
 * Return a value associated with [clazz] in this map.
 * If there is none repeat for its super class.
 */
private fun <T, R> Map<Class<*>, R>.recursiveGet(clazz: Class<in T>): R? {
    if (clazz in this) return getValue(clazz)
    
    val superclass = clazz.superclass ?: return null
    return recursiveGet(superclass)
}

private fun Report.toCustomContext() = SelfDescribingJson(
    Schema.Report,
    buildMap {
        put("reason", reason.name)
        if (comment != null) put("comment", comment)
    }
)

private fun Link.toCustomContext(hierarchy: Int = 0) = SelfDescribingJson(
    Schema.Ui,
    mapOf(
        "hierarchy" to hierarchy,
        "type" to UiEntityType.LINK.value,
        "identifier" to identifier,
    )
)
private fun Link.toReferrerContext(hierarchy: Int) = SelfDescribingJson(
    Schema.Ui,
    mapOf(
        "hierarchy" to hierarchy,
        "type" to UiEntityType.SCREEN.value,
        "identifier" to identifierFromReferrer(referrer),
    )
)
private fun ExternalView.toCustomContext(hierarchy: Int = 0) = SelfDescribingJson(
    Schema.Ui,
    mapOf(
        "hierarchy" to hierarchy,
        "identifier" to identifier,
        "type" to type.value,
    )
)

private fun CorpusRecommendation.toCustomContext() = SelfDescribingJson(
    Schema.CorpusRecommendation,
    buildMap {
        put("corpus_recommendation_id", corpusRecommendationId)
    }
)

private fun Content.toCustomContext() = SelfDescribingJson(Schema.Content, mapOf("url" to url))

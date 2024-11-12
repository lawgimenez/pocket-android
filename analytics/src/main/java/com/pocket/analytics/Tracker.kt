package com.pocket.analytics

import android.net.Uri
import android.view.View
import com.pocket.analytics.api.UiEntityable
import com.pocket.analytics.api.UiEntityableHelper
import com.pocket.analytics.events.Event

/**
 * Tracker configuration API.
 *
 * For most of these methods it should be enough to use them only at
 * app startup (or in case of [setGuid]/[setAccount] setup a subscription at app startup that
 * will keep these values up to date).
 */
interface TrackerConfig {
    /**
     * Declare ui.type and optionally ui.component_detail for all instances of a given [component].
     */
    fun bindUiEntityType(component: Class<*>, type: UiEntityType, detail: String? = null)

    /** Teach the tracker how to calculate ui.index for a view. */
    fun registerIndexProvider(indexProvider: IndexProvider)

    /** Teach the tracker how to check impression requirement for a view. */
    fun registerImpressionRequirement(
        requirement: ImpressionRequirement,
        checker: ImpressionRequirementChecker
    )

    /** Set the current [guid] or null to clear out the previous value. */
    fun setGuid(guid: String?)

    /** Set the current logged-in account or null if logged out. */
    fun setAccount(userId: String?, email: String?)

    /** Lets the tracker calculate a value for ui.index for a given view. */
    interface IndexProvider {
        /** Calculate an integer 0-based index of this view within its parent (or null if n/a) */
        fun indexFor(view: View): Int?
    }

    /** Lets the tracker check impressions for a given [ImpressionRequirement]. */
    interface ImpressionRequirementChecker {
        /** Call [onImpression] when requirement is satisfied for [view] */
        fun whenRequirementSatisfied(view: View, onImpression: () -> Unit)

        /**
         * Opt into checking this requirement for a [view] with [uniqueId].
         * 
         * Don't keep a strong reference to the [view].
         * 
         * @see [Tracker.enableImpressionTracking] for more information on [uniqueId].
         */
        fun enableFor(view: View, uniqueId: Any)
    }
}

/**
 * Tracker APIs for app-wide use, including binding entity params to view instances
 * and manually triggering events.
 */
interface Tracker {

    fun track(event: Event)

    /**
     * Override ui.type for a [view]. To set a default for a view we control,
     * implement [UiEntityable] and use [UiEntityableHelper.uiEntityType].
     *
     * For use in case of view classes that are generic, like different types of layouts,
     * that can serve as general purpose containers, screen root, but sometimes also smaller
     * focus components like non-trivial buttons.
     * 
     * Implementations should only hold a weak reference to the view, so there is no need to unbind.
     */
    fun bindUiEntityType(view: View, type: UiEntityType)

    /**
     * Override ui.identifier for a [view]. To set a default for a view we control,
     * implement [UiEntityable] and use [UiEntityableHelper.uiEntityIdentifier]
     * (or declare in XML via [R.styleable.UiEntityable_uiEntityIdentifier]).
     *
     * Implementations should only hold a weak reference to the view, so there is no need to unbind.
     */
    fun bindUiEntityIdentifier(view: View, identifier: String)
    
    /**
     * Declare ui.value for a [view].
     * 
     * Implementations should only hold a weak reference to the view, so there is no need to unbind.
     */
    fun bindUiEntityValue(view: View, value: String)

    /**
     * Bind an instance of [content] to a [view] to build a content context/entity out of it.
     *
     * Implementations should only hold a weak reference to the view, so there is no need to unbind.
     */
    fun bindContent(view: View, content: Content)

    /**
     * Opt into tracking impressions for a [view] with a given [component].
     * 
     * For comparing views we want to be able to tell if the user looking at two of them would
     * say they are different or the same. In some cases it's not enough to compare instances of
     * [View] subclasses. The same [View] can look like two different views if you rebind it with
     * new data. Two different [View]s can look the same if you rebind the same data to a different
     * [View]. If this is possible for this [view] pass the optional [uniqueId] and the tracker
     * will use it for comparisons instead of [view].
     * 
     * By default checks all registered [ImpressionRequirement]s. If we have a use case for checking
     * only specific requirements, we'll have to add a new method for that.
     */
    fun enableImpressionTracking(
        view: View,
        component: ImpressionComponent,
        uniqueId: Any = Unit,
    )

    /** Manually fire an impression for a [view]. */
    fun trackImpression(
        view: View,
        component: ImpressionComponent,
        requirement: ImpressionRequirement,
        corpusRecommendation: CorpusRecommendation? = null
    )

    /** Manually fire an engagement for a [view]. */
    fun trackEngagement(
        view: View,
        type: EngagementType = EngagementType.GENERAL,
        value: String? = null,
        report: Report? = null,
        corpusRecommendation: CorpusRecommendation? = null
    )

    /** Manually fire an engagement for some [externalView] like system UI. */
    fun trackEngagement(
        externalView: ExternalView,
        type: EngagementType,
        value: String?,
        corpusRecommendation: CorpusRecommendation? = null
    )

    /** Manually fire a content_open for a [view]. */
    fun trackContentOpen(
        view: View?,
        destination: ContentOpenDestination,
        trigger: ContentOpenTrigger,
        corpusRecommendation: CorpusRecommendation? = null
    )

    /** Manually fire a content_open for a [link]. */
    fun trackContentOpen(
        link: Link,
        destination: ContentOpenDestination,
        trigger: ContentOpenTrigger,
        corpusRecommendation: CorpusRecommendation? = null
    )

    /** Manually fire an app_open. */
    fun trackAppOpen(deepLink: String?, referrer: String?)
    
    /** Manually fire an app_background. */
    fun trackAppBackground()
    
    /** Manually fire a variant_enroll. */
    fun trackVariantEnroll(
        name: String,
        variant: String,
        view: View?
    )
}

@Suppress("unused")
enum class UiEntityType(val value: String) {
    BUTTON("button"),
    DIALOG("dialog"),
    MENU("menu"),
    CARD("card"),
    LIST("list"),
    READER("reader"),
    PAGE("page"),
    SCREEN("screen"),
    LINK("link"),
    PUSH_NOTIFICATION("push_notification"),
}
enum class ImpressionRequirement(val value: String) {
    INSTANT("instant"),
    VIEWABLE("viewable"),
}
@Suppress("unused")
enum class ImpressionComponent(val value: String) {
    UI("ui"),
    CARD("card"),
    CONTENT("content"),
    SCREEN("screen"),
    PUSH_NOTIFICATION("push_notification"),
    BUTTON("button"),
}

@Suppress("unused")
enum class EngagementType(val value: String) {
    GENERAL("general"),
    SAVE("save"),
    REPORT("report"),
    DISMISS("dismiss"),
}

@Suppress("unused")
enum class ContentOpenDestination(val value: String) {
    INTERNAL("internal"),
    EXTERNAL("external"),
}
@Suppress("unused")
enum class ContentOpenTrigger(val value: String) {
    CLICK("click"),
    AUTO("auto"),
}

interface Content {
    val url: String?
}

sealed class Link(
    val content: Content,
    val identifier: String,
    val view: View? = null,
    val referrer: Uri? = null,
)
class PocketCoLink(
    content: Content,
    referrer: Uri?,
) : Link(
    content,
    "pocket.co",
    referrer = referrer
)
class ArticleLink(
    content: Content,
    view: View,
) : Link(
    content,
    "article_link",
    view = view
)

class ExternalView(
    val identifier: String,
    val type: UiEntityType,
)

class Report(
    val reason: Reason,
    val comment: String?,
) {
    @Suppress("EnumEntryName")
    enum class Reason {
        broken_meta,
        wrong_category,
        sexually_explicit,
        offensive,
        misinformation,
        other
    }
}

class CorpusRecommendation(val corpusRecommendationId: String)
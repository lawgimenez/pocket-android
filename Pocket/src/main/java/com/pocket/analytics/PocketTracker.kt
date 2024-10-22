package com.pocket.analytics

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import com.pocket.analytics.entities.BrowserContext
import com.pocket.app.AppLifecycle
import com.pocket.app.AppLifecycleEventDispatcher
import com.pocket.app.AppOpen
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.*
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.sync.source.subscribe.Changes
import com.pocket.util.android.ViewUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import java.util.*

/**
 * Configures [Tracker] for the Pocket app, sets up some additional automatic
 * app-dependent tracking and exposes [Tracker] API for the rest of the app.
 */
class PocketTracker(
    private val pocket: Pocket,
    private val appOpen: AppOpen,
    private val browserAnalytics: BrowserAnalytics,
    pktCache: PocketCache,
    private val tracker: SnowplowTracker,
    dispatcher: AppLifecycleEventDispatcher
) : AppLifecycle, Tracker by tracker {

    init {
        dispatcher.registerAppLifecycleObserver(this)
        tracker.configure()

        tracker.setGuid(pktCache.loginInfo().guid)
        tracker.setAccount(pktCache.account()?.user_id, pktCache.account()?.email)
        pocket.setup {
            pocket.subscribe(Changes.of(LoginInfo.Builder().build())) {
                tracker.setGuid(it?.guid)
                tracker.setAccount(it?.account?.user_id, it?.account?.email)
            }
        }
    }

    override fun onUserPresent() {
        super.onUserPresent()
        val defaultBrowser = browserAnalytics.getDefaultBrowserInfo()
        tracker.browserContext = BrowserContext(
            defaultBrowser = defaultBrowser?.name,
            defaultBrowserVersion = defaultBrowser?.version,
            defaultBrowserSupportsCustomTabs = browserAnalytics.getDefaultCustomTabsPackageName() != null,
            customTabsBrowserCount = browserAnalytics.getCustomTabsPackageNames().count(),
        )
        trackAppOpen(appOpen.deepLink, appOpen.referrer?.toString())
    }

    override fun onUserGone(context: Context?) {
        super.onUserGone(context)
        trackAppBackground()
    }

    override fun onLogoutStarted(): AppLifecycle.LogoutPolicy {
        return object : AppLifecycle.LogoutPolicy {
            override fun deleteUserData() {
                tracker.setGuid(null)
                tracker.setAccount(null, null)
            }

            override fun onLoggedOut() {}
            override fun restart() {}
            override fun stopModifyingUserData() {}
        }
    }
}

private fun TrackerConfig.configure() {
    bindUiEntityType(Button::class.java, UiEntityType.BUTTON)

    // Use adapter position in [RecyclerView] adapters.
    registerIndexProvider(object : TrackerConfig.IndexProvider {
        override fun indexFor(view: View): Int? {
            val list = view.parent
            if (list !is RecyclerView) return null
    
            val adapterPosition = list.getChildAdapterPosition(view)
            if (adapterPosition == RecyclerView.NO_POSITION) return null
            
            return adapterPosition + 1
        }
    })
    
    registerImpressionRequirement(
        ImpressionRequirement.INSTANT,
        object : TrackerConfig.ImpressionRequirementChecker {
            private val enabledFor = WeakHashMap<View, Any>()
            private val alreadyTracked = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
            
            override fun whenRequirementSatisfied(view: View, onImpression: () -> Unit) {
                if (!enabledFor.containsKey(view)) return

                // Wait until laid out to check visibility reliably.
                view.doOnLayout {
                    if (ViewUtil.getPercentVisible(view) <= 0) {
                        alreadyTracked.remove(view)

                    } else if (!alreadyTracked.contains(view)) {
                        alreadyTracked.add(view)
                        onImpression()

                    } else {
                        // Visible, but we already tracked an impression.
                    }
                }
            }

            override fun enableFor(view: View, uniqueId: Any) {
                if (enabledFor[view] != uniqueId) {
                    enabledFor[view] = uniqueId

                    // If enabling again for the same view but with different content
                    // (represented by different `uniqueId`), reset already tracked flag for it.
                    alreadyTracked.remove(view)
                }
            }
        })
    
    registerImpressionRequirement(
        ImpressionRequirement.VIEWABLE,
        object : TrackerConfig.ImpressionRequirementChecker {
            private val minVisiblePercent = 0.5
            private val minVisibleDuration = Duration.ofSeconds(1)

            private val enabledFor = WeakHashMap<View, Any>()
            private val impressionStarts = WeakHashMap<View, Instant>()
            private val requirementCheckerScope = MainScope()
            
            override fun whenRequirementSatisfied(view: View, onImpression: () -> Unit) {
                if (!enabledFor.containsKey(view)) return

                // Wait until laid out to check visibility reliably.
                view.doOnLayout {
                    if (ViewUtil.getPercentVisible(view) <= 0) {
                        // No longer visible.
                        impressionStarts.remove(view)

                    } else if (ViewUtil.getPercentVisible(view) <= minVisiblePercent) {
                        // Not enough of the view is visible.

                    } else if (!impressionStarts.containsKey(view)) {
                        // Enough of the view became visible. Start tracking visible duration.
                        impressionStarts[view] = Instant.now()

                        // Schedule a re-check when it'll be visible long enough
                        requirementCheckerScope.launch {
                            delay(minVisibleDuration.toMillis())
                            whenRequirementSatisfied(view, onImpression)
                        }

                    } else if (
                        Duration.between(impressionStarts[view], Instant.now()) > minVisibleDuration
                    ) {
                        // Visible long enough to track.
                        impressionStarts[view] = Instant.MAX // So we don't track again.
                        onImpression()

                    } else {
                        // Previously visible, but not long enough. There is a re-check scheduled.
                    }
                }
            }

            override fun enableFor(view: View, uniqueId: Any) {
                if (enabledFor[view] != uniqueId) {
                    enabledFor[view] = uniqueId

                    // If enabling again for the same view but with different content
                    // (represented by different `uniqueId`), reset already tracked flag for it.
                    impressionStarts.remove(view)
                }
            }
        })
}
class ItemContent(
    override val url: String
) : Content

class UrlContent(override val url: String) : Content

package com.pocket.app

import com.pocket.app.build.Versioning
import com.pocket.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls the maximum number of unread items we'll keep around locally.
 * If we keep too many around we'll risk performance degradation or out of memory errors.
 * The app hasn't really designed to handle massive unread lists and unread is meant to be items you might get to, hence that's why we keep quickly accessibly and offline.
 * The backend's [com.pocket.sdk.api.generated.thing.Fetch] and [com.pocket.sdk.api.generated.thing.Get] will only sync down 5000 items at most, but if they continue to save, we'll continue to track them locally,
 * so the app can end up with many more than 5000. Before the sync release, some users had thousands to over 10,000 items!
 * See further discussion in https://github.com/Pocket/Android/pull/1427
 *
 * Short term, to avoid OOMEs that prevent some of these users from using the app, we'll enforce an local item cap.
 * For existing installs, we'll continue trying using a high cap, but we have seen that a dozen or 2 dozen users still hit OOMEs.
 * For new installs, we'll use a lower cap. So if users get into problems, when they reinstall they will have a lower, safer cap.
 *
 * Long term the better way to handle this is to have a much lower cap, but in various parts of the app switch over to loading from the server for filtering, searching beyond the local cache.
 */
@Singleton
class ItemCap @Inject constructor(versioning: Versioning, prefs: Preferences) {

    val cap: Int

    init {
        // Decide on a cap or load it.
        // This should only be set once per install, and not change later
        // otherwise you'll need to carefully consider what components might have used this value in a way that could get messed up if it changes, such as a thing's identity changing.
        val pref = prefs.forApp("item_cap", 0)
        if (!pref.isSet) {
            if (versioning.isFirstRun) {
                pref.set(5500) // Lower/safer cap for new installs
            } else {
                pref.set(8000) // Keep the original, higher cap that was already implemented in the app so it doesn't change
            }
        }
        cap = pref.get()
    }

}

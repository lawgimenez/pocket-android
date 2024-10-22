package com.pocket.app.reader.internal.article

import com.pocket.sdk.api.ServerFeatureFlags
import com.pocket.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kill switch to disable article view in case the parser dies for some reason.
 * Off by default.  Enabling this in unleash will disable article view.
 */
@Singleton
class ArticleViewKillSwitchFlag @Inject constructor(
    serverFeatureFlags: ServerFeatureFlags,
    val prefs: Preferences,
) {

    init {
        serverFeatureFlags.get(FLAG, null).onSuccess {
            prefs.forUser(FLAG, false).set(it?.assigned ?: false)
        }
    }

    val isEnabled: Boolean
        get() = prefs.forUser(FLAG, false).get()

    companion object {
        const val FLAG = "perm.android.disableArticleView"
    }
}
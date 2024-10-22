package com.pocket.app

import com.pocket.app.build.Versioning
import com.pocket.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveExtension @Inject constructor(
    prefs: Preferences,
    versioning: Versioning,
) {

    private val pref = prefs.forUser("add_overlay", true)

    init {
        // 7.25.0.0 contained a bug which switched all upgraded user's quick save actions setting to false.
        // Unfortunately we don't know what setting they had before upgrading, but users who upgrade from that
        // version will have them turned on by default.
        if (versioning.isUpgrade && versioning.from() == VersionUtil.toVersionCode(7, 25, 0, 0)) {
            pref.set(true)
        }
    }

    var isOn
        get() = pref.get()
        set(value) = pref.set(value)
}

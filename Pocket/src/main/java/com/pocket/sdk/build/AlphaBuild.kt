package com.pocket.sdk.build

import android.app.Application
import com.ideashower.readitlater.BuildConfig
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.distribute.Distribute
import com.pocket.app.AppMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logic specific to Alpha (aka Nightly) builds. For example initializing SDK for Alpha updates.
 *
 * Note: not all alpha things are encapsulated here. There's still a bunch of legacy logic spread
 * across the app.
 */
@Singleton
class AlphaBuild
@Inject constructor(
    private val app: Application,
    private val appMode: AppMode,
) : Nightly {
    fun setup() {
        if (appMode.isForInternalCompanyOnly) {
            if (!AppCenter.isConfigured()) {
                AppCenter.configure(app, BuildConfig.AC_I)
            }
            AppCenter.start(Distribute::class.java)
        }
    }
}

/**
 * Proxy for [AlphaBuild].
 * Historically we call the internal/team builds "alpha". But the more common name these days is
 * "nightly". This is here to make searching for this class easier.
 */
interface Nightly

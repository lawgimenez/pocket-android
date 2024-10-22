package com.pocket.app

import android.content.Context
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple app component for storing information related to the most recent app open
 * and providing easy access to this information throughout the app where it's needed.
 */
@Singleton
class AppOpen @Inject constructor(dispatcher: AppLifecycleEventDispatcher) : AppLifecycle {
    var deepLink: String? = null
    var referrer: Uri? = null

    init {
        dispatcher.registerAppLifecycleObserver(this)
    }

    override fun onUserGone(context: Context?) {
        super.onUserGone(context)
        deepLink = null
        referrer = null
    }
}

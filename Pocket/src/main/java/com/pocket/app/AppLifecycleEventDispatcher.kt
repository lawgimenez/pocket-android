package com.pocket.app

import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for sending [AppLifecycle] events.
 */
@Singleton
class AppLifecycleEventDispatcher @Inject constructor() {

    private val appLifecycleObservers: MutableSet<AppLifecycle> = HashSet()

    fun <C : AppLifecycle> registerAppLifecycleObserver(observer: C): C {
        appLifecycleObservers.add(observer)
        return observer
    }

    /**
     * Dispatch an [AppLifecycle] event to all observers.
     * @param dispatch The implementation to invoke on each observer. See [Dispatch.dispatch] for more details.
     */
    fun dispatch(dispatch: Dispatch?) {
        appLifecycleObservers.forEach {
            dispatch?.dispatch(it)
        }
    }
    interface Dispatch {
        /**
         * Invoke your event on the provided component. This is called on every registered observer separately.
         * @param appLifecycle The observer to invoke your method on.
         */
        fun dispatch(appLifecycle: AppLifecycle)
    }
}
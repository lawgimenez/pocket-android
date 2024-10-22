package com.pocket.app

import com.pocket.app.AppLifecycle.LogoutPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class AppScope
@Inject constructor() : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    private val lock = Any()
    private var _scope: CoroutineScope? = null
    private val scope: CoroutineScope
        get() {
            return synchronized(lock) {
                _scope
                    ?: CoroutineScope(SupervisorJob() + Dispatchers.Default).also { _scope = it }
            }
        }

    /**
     * This does not use [AppLifecycle.onLogoutStarted] because [com.pocket.app.UserManager]
     * wants to run this policy at the very end, because logout processes might actually need
     * to use this to run async tasks.
     */
    @Suppress("EmptyFunctionBlock")
    fun getLogoutPolicy(): LogoutPolicy {
        return object : LogoutPolicy {
            override fun stopModifyingUserData() {
                synchronized(lock) {
                    _scope?.cancel("Logout.")
                }
            }

            override fun deleteUserData() {}
            override fun restart() {
                _scope = null
            }

            override fun onLoggedOut() {}
        }
    }
}

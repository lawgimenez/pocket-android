package com.pocket.app

import android.app.Activity
import android.content.Context
import com.pocket.sdk.util.AbsPocketActivity
import android.content.Intent
import android.content.res.Configuration

/**
 * Major App events.
 */
@Suppress("TooManyFunctions")
interface AppLifecycle {
    /**
     * Invoked from within a [android.content.Intent.ACTION_BOOT_COMPLETED] action receiver.
     * This runs in a Broadcast Receiver and must be extremely quick.
     */
    fun onDeviceBoot() {}

    /**
     * The user has begun interacting with an Activity in the foreground.
     */
    fun onUserPresent() {}

    /**
     * Invoked when a Pocket activity resumes.
     */
    fun onActivityResumed(activity: Activity?) {}

    /**
     * Invoked when the current Pocket activity has an [Activity.onActivityResult].
     */
    fun onActivityResult(
        activity: AbsPocketActivity?,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {}

    /**
     * Invoked when a Pocket activity pauses.
     */
    fun onActivityPaused(activity: Activity?) {}

    /**
     * Invoked during [android.app.Application.onConfigurationChanged]
     */
    fun onConfigurationChanged(configuration: Configuration?) {}

    /** Invoked when [Application.onLowMemory] is.  */
    fun onLowMemory() {}

    /**
     * The user has left the app's foreground Activities completely.
     */
    fun onUserGone(context: Context?) {}

    /**
     * Invoked off the ui thread after the user has successfully logged into (or signed up) the [Pocket] instance,
     * but before the log in progress ui goes away. This is provided as an opportunity to do a blocking operation
     * that is ideally completed before the app ui moves into the logged in experience.
     *
     *
     * Operations done here will prolong the users wait time when logging in, so be mindful.
     * Also, any exceptions thrown here will be ignored, the app will still move into the logged in state.
     * This is not meant to host required tasks, just best attempt access. If your work absolutely must be
     * completed as part of the login process such that it would require failing the login, then you likely
     * want to modify [UserManager] instead.
     *
     * @see .onLoggedIn
     */
    fun onLoggingIn(isNewUser: Boolean) {}

    /**
     * The user has successfully logged into the app and the ui is moving to the logged in experience.
     * @see .onLoggingIn
     */
    fun onLoggedIn(isNewUser: Boolean) {}

    /**
     * A logout has begun.
     * Return a [LogoutPolicy] that will help you ensure you clean up your component properly.
     */
    fun onLogoutStarted(): LogoutPolicy? = null

    /**
     * An interface that will help you clean up your component when a user logs out,
     * and ensure no user data accidentally leaks past logout.
     * See each method for more details on how to implement.
     *
     *
     * If you are relying on other components or using logout safe apis like the forUser() pref apis
     * or [AppThreads.async], you may not need a logout policy, but checkout the methods
     * and consider if you do.
     *
     *
     * Note: this is intentionally an abstract class rather than trying to allow lambdas
     * to help force developers to consider each method.
     */
    interface LogoutPolicy {
        /**
         * Attempt to stop and/or wait for all work that might modify or add state/data
         * that belongs to the current logged in user.
         * Also stop accepting requests from outside this component that would trigger further work
         * until after [.restart] is invoked.
         * Avoid interacting with other components as they may have also already stopped.
         * Block this thread until complete.
         * Exceptions thrown here will be ignored.
         */
        fun stopModifyingUserData()

        /**
         * Remove any user specific state in variables, caches, files, databases etc that you control.
         * You should not interact with other components during this step.
         * Block this thread until complete.
         * If you created a preference using a forUser() like method, that state will be cleared for
         * you by [com.pocket.sdk.preferences.AppPrefs]
         * Exceptions thrown here will be ignored.
         */
        fun deleteUserData()

        /**
         * Start accepting requests again if you stopped in [.stopModifyingUserData].
         * Do whatever you need to do so other components and features can start interacting with you again.
         * This should not be a long running operation. Should be very fast.
         * Don't interact with other components yet, they may not all be restarted.
         * See [.onLoggedOut] for when all log out processes are complete and it is safe to use other components.
         * Exceptions thrown here will be ignored.
         */
        fun restart()

        /**
         * Log out complete. It is safe to use any component again.
         */
        fun onLoggedOut()
    }
}
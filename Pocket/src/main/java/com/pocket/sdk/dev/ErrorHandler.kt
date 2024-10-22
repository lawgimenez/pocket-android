package com.pocket.sdk.dev

import com.pocket.app.AppMode
import io.sentry.Sentry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionThrown")
class ErrorHandler @Inject constructor(
    private val mode: AppMode,
) {

    /** Report a caught/handled exception. */
    fun reportError(throwable: Throwable) {
        throwable.printStackTrace()
        Sentry.captureException(throwable)
    }

    fun reportOnProductionOrThrow(throwable: Throwable) {
        if (mode.isForInternalCompanyOnly) {
            throw throwable
        } else {
            reportError(throwable)
        }
    }
}

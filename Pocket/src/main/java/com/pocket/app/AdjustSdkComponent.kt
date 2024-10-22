package com.pocket.app

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.ideashower.readitlater.BuildConfig
import com.pocket.util.java.Logs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdjustSdkComponent
@Inject constructor(
	private val mode: AppMode,
	@ApplicationContext context: Context,
) {

	companion object {
		private const val APP_TOKEN = BuildConfig.ADJUST_APP_TOKEN
		private const val SIGN_UP_EVENT_TOKEN = BuildConfig.ADJUST_SIGN_UP_EVENT_TOKEN
	}

	init {
		callAdjustSafely {
			val environment = if (mode.isForInternalCompanyOnly)
				AdjustConfig.ENVIRONMENT_SANDBOX
			else AdjustConfig.ENVIRONMENT_PRODUCTION
			Adjust.onCreate(AdjustConfig(context, APP_TOKEN, environment, true))
			registerAdjustSdkLifecycleCallbacks(App.from(context))
		}
	}

	private fun registerAdjustSdkLifecycleCallbacks(app: App) {
		app.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
			override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
			override fun onActivityStarted(activity: Activity) {}
			override fun onActivityResumed(activity: Activity) {
				try {
					Adjust.onResume()
				} catch (e: Throwable) {
					handle(e)
				}
			}

			override fun onActivityPaused(activity: Activity) {
				try {
					Adjust.onPause()
				} catch (e: Throwable) {
					handle(e)
				}
			}

			override fun onActivityStopped(activity: Activity) {}
			override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
			override fun onActivityDestroyed(activity: Activity) {}
		})
	}

	fun trackSignUp() {
		callAdjustSafely {
			Adjust.trackEvent(AdjustEvent(SIGN_UP_EVENT_TOKEN))
		}
	}

	fun getAdId(): String? {
		callAdjustSafely {
			return Adjust.getAdid()
		}
		// If Adjust throws and the above block fails to return, then return null.
		return null
	}

	private inline fun callAdjustSafely(block: () -> Unit) {
		try {
			block();
		} catch (e: Throwable) {
			handle(e);
		}
	}

	private fun handle(e: Throwable) {
		Logs.printStackTrace(e)
		if (mode.isForInternalCompanyOnly) {
			throw RuntimeException(e)
		}
	}
}

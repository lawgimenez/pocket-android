package com.pocket.app

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.ideashower.readitlater.BuildConfig
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.CxtEvent
import com.pocket.sdk.api.generated.enums.CxtPage
import com.pocket.sdk.api.generated.enums.CxtSection
import com.pocket.sdk.api.generated.enums.CxtView
import com.pocket.sdk.dev.ErrorHandler
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.util.java.Logs
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.StringPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject
import javax.inject.Singleton

private val DEBUG = BuildConfig.DEBUG
private val LOG_TAG = InstallReferrer::class.java.simpleName

/**
 * Checks the Google Play install referrer, stores it for later reference and sends off to Pocket
 * servers.
 * 
 * Uses Install Referrer Library. Based on this documentation:
 * [https://developer.android.com/google/play/installreferrer/library]
 * 
 * You can test on a device using these steps:
 * 1. Uninstall previous build if necessary.
 * 2. Open Pocket on the Play Store using a special link like:
 *    https://play.google.com/store/apps/details?id=com.ideashower.readitlater.pro&referrer=utm_source%3Dsrc%26utm_content%3Dcntnt&hl=en
 * 3. Install from adb or Android Studio (but just install, don't run).
 * 4. Play Store should change from "Install" button to "Open" button.
 * 5. Open from Play Store.
 * 6. Use debugger or logs to verify received referrer.
 * 
 * (Testing steps based on this SO answer: [https://stackoverflow.com/a/58621059/1402641].)
 */
@Singleton
class InstallReferrer @Inject constructor(
    @ApplicationContext context: Context,
    pocket: Pocket,
    errorHandler: ErrorHandler,
    prefs: Preferences
) {
    private val googlePlayReferrer: StringPreference = prefs.forApp("rffrgp", null as String?)
    
    init {
        processGooglePlayReferrer(prefs, context, pocket, errorHandler)
    }
    
    private fun processGooglePlayReferrer(
        prefs: Preferences,
        context: Context,
        pocket: Pocket,
        errorHandler: ErrorHandler,
    ) {
        val alreadySent = prefs.forApp("rffrpv", false)
        if (alreadySent.get()) {
            if (DEBUG) Logs.d(LOG_TAG, "Already sent. Nothing more to do.")
            return
        }
        if (DEBUG) Logs.d(LOG_TAG, "Not sent yet, processing.")
        
        val client = InstallReferrerClient.newBuilder(context).build()
        try {
            client.startConnection(object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    alreadySent.set(true) // We don't want to retry even if responseCode != OK.

                    if (DEBUG) Logs.d(LOG_TAG, "responseCode = $responseCode")
                    if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) return

                    try {
                        val referrer = client.installReferrer.installReferrer
                        if (DEBUG) Logs.d(LOG_TAG, "referrer = $referrer")
                        if (!StringUtils.isBlank(referrer)) {
                            googlePlayReferrer.set(referrer)
                        }

                        val it = Interaction.on(context)
                        pocket.sync(null, pocket.spec().actions().pv_wt()
                                .view(CxtView.MOBILE)
                                .section(CxtSection.CORE)
                                .action_identifier(CxtEvent.REFERRER)
                                .page(CxtPage.INSTALLATION)
                                .type_id(3)
                                .time(it.time)
                                .context(it.context)
                                .page_params(referrer)
                                .build())
                    } catch (ignored: Exception) {}

                    client.endConnection()
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // Ignore, we will retry the connection on next app start.
                }
            })
        } catch (t:Throwable) {
            // There are some cases where this is crashing due to an error that doesn't have much clarity available. See commit that introduced this line for links and discussion.
            // We likely need to see if a later version of this will fix it, but we won't break the app for analytics, so just ignore this but report it.
            errorHandler.reportError(t)
            alreadySent.set(true)
        }
    }
    
    fun getGooglePlayReferrer(): String? = googlePlayReferrer.get()
}

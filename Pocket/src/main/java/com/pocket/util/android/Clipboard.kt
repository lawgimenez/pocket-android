package com.pocket.util.android

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassifier
import android.widget.Toast
import com.ideashower.readitlater.R
import com.pocket.app.AppLifecycle
import com.pocket.app.AppLifecycleEventDispatcher
import com.pocket.app.AppMode
import com.pocket.sdk.dev.ErrorHandler
import com.pocket.util.java.UrlFinder
import com.pocket.util.prefs.IntPreference
import com.pocket.util.prefs.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides easy access to the Clipboard based on whatever ClipboardManager is available for this device's api level.
 */
@Singleton
class Clipboard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mode: AppMode,
    private val errorReporter: ErrorHandler,
    prefs: Preferences,
    dispatcher: AppLifecycleEventDispatcher
) : AppLifecycle {

    private val lastUrlHash: IntPreference = prefs.forUser("lastClipUrlHash", 0)
    private val manager: ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    init {
        dispatcher.registerAppLifecycleObserver(this)
    }

    override fun onLoggedIn(isNewUser: Boolean) {
        super.onLoggedIn(isNewUser)

        // Prevent the save from clipboard prompt from showing immediately after sign up or login
        // if the user has a url in their clipboard.
        getUrl()
    }

    /**
     * Get URL from clipboard or null if there is none or this URL was returned previously already.
     */
    fun getUrl(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.primaryClipDescription?.let { clipDescription ->
                if (clipDescription.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE &&
                    clipDescription.getConfidenceScore(TextClassifier.TYPE_URL) < 0.5) {
                    return null
                }
            }
        }
        return UrlFinder.getFirstUrlOrNull(getText())?.let { url ->
            // if the url is the same as the last one, return null
            if (url.hashCode() == lastUrlHash.get()) {
                null
            } else {
                lastUrlHash.set(url.hashCode())
                url
            }
        }
    }

    fun getText(): String? {
        var clipData: ClipData? = null
        try {
            clipData = manager.primaryClip
        } catch (t: Throwable) {
            // Looks like just checking clipboard contents can crash the app on some devices.
            // https://appcenter.ms/orgs/pocket-app/apps/Android-Production-Google-Play-Amazon-App-Store/crashes/errors/4025504882u/overview
            if (mode.isForInternalCompanyOnly) {
                throw t
            } else {
                errorReporter.reportError(t)
            }
        }

        if (clipData?.description == null
            || clipData.description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == false
            || clipData.itemCount == 0
            || clipData.getItemAt(0) == null
        ) {
            return null
        }

        val item = clipData.getItemAt(0)
        return item.text?.toString() ?: item.uri?.toString()
    }

    /**
     * Paste some text to the clipboard.
     *
     * @param text
     * @param name Optional. If not null a toast will be shown along the lines of
     * "name copied to clipboard". If null no toast is shown.
     */
    fun setText(text: String, name: String?) {
        performSetText(text)
        if (name == null) return
        Toast
            .makeText(context, context.getString(R.string.ts_copied, name), Toast.LENGTH_SHORT)
            .show()
    }

    /**
     * Safely sets a url to the clip avoiding the app prompting to save it.
     * @param url
     * @param name
     */
    fun setUrl(url: String, name: String?) {
        // Save this url hash so the clipboard url detection doesn't ask about it.
        lastUrlHash.set(url.hashCode())
        setText(url, name)
    }

    private fun performSetText(text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label_url), text)
        manager.setPrimaryClip(clip)
    }

}
package com.pocket.app.share

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.IntentCompat
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ShareEvents
import com.pocket.util.android.PendingIntentUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val SHARE_RECEIVER_REQUEST_CODE = 958489983
private const val EXTRA_SHARE_URL = "share_url"

object ShareSheet {
    /**
     * @param context context to create the share intent with
     * @param url the url being shared
     * @param shareLink optionally a Share Link that wraps [url] and opens in Pocket
     * @param quote optionally a quote to send along with the link
     * @param quote optionally a title to show in the system share sheet
     */
    fun show(
        context: Context,
        url: String,
        shareLink: String = url,
        quote: String? = null,
        title: String? = null,
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (!title.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TITLE, title)
            }
            putExtra(Intent.EXTRA_TEXT, if (quote != null) "\"$quote\"\n\n$shareLink" else shareLink)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        val pendingShareBroadcast = PendingIntent.getBroadcast(
            context,
            SHARE_RECEIVER_REQUEST_CODE,
            Intent(context, ShareReceiver::class.java).apply {
                putExtra(EXTRA_SHARE_URL, shareLink)
            },
            PendingIntentUtils.addMutableFlag(PendingIntent.FLAG_UPDATE_CURRENT)
        )
        val shareSheetIntent = Intent.createChooser(
            shareIntent,
            null,
            pendingShareBroadcast.intentSender,
        ).apply {
            excludeOurShareExtension(context)
            // Customize share text for specific apps.
            putExtra(
                Intent.EXTRA_REPLACEMENT_EXTRAS,
                Bundle().apply {
                    sendOriginalUrlToBrowsers(url)
                }
            )
        }
        context.startActivity(shareSheetIntent)
    }

    /**
     * Exclude the Add to Pocket option,
     * using the alias in the manifest (not the actual/current class package).
     */
    private fun Intent.excludeOurShareExtension(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val addActivity = "com.ideashower.readitlater.activity.AddActivity"
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(ComponentName(context, addActivity))
            )
        }
    }

    private fun Bundle.sendOriginalUrlToBrowsers(originalUrl: String) {
        // Top 10 browsers (with their alternative channels/builds).
        // https://app.mode.com/getpocket/reports/b455ccc51a3e
        replaceShareText(
            listOf(
                "com.android.chrome", "org.chromium.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
                "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix",
                "com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser.beta", "com.sec.android.app.sbrowser.lite",
                "com.brave.browser", "com.brave.browser_beta", "com.brave.browser_nightly",
                "com.duckduckgo.mobile.android",
                "com.microsoft.emmx", "com.microsoft.emmx.beta", "com.microsoft.emmx.dev", "com.microsoft.emmx.canary",
                "com.opera.browser", "com.opera.browser.beta",
                "com.mi.globalbrowser", "com.mi.globalbrowser.mini",
                "com.vivaldi.browser", "com.vivaldi.browser.snapshot",
                "org.mozilla.focus", "org.mozilla.klar",
            ),
            originalUrl,
        )
    }

    private fun Bundle.replaceShareText(apps: List<String>, text: String) {
        val replacement = Bundle().apply { putString(Intent.EXTRA_TEXT, text) }
        for (app in apps) {
            putBundle(app, replacement)
        }
    }
}

@AndroidEntryPoint
class ShareReceiver : BroadcastReceiver() {
    @Inject
    lateinit var tracker: Tracker

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        val clickedComponent = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_CHOSEN_COMPONENT,
            ComponentName::class.java,
        )
        if (clickedComponent != null) {
            tracker.track(
                ShareEvents.shareSheetAppClicked(
                    clickedComponent.packageName,
                    intent.getStringExtra(EXTRA_SHARE_URL),
                ),
            )
        }
    }
}
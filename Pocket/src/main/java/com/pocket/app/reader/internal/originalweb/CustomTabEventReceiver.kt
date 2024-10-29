package com.pocket.app.reader.internal.originalweb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID
import com.ideashower.readitlater.R
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.OriginalWebViewEvents
import com.pocket.app.MainActivity
import com.pocket.app.reader.internal.originalweb.overlay.OriginalWebOverlayActivity
import com.pocket.sdk.util.UrlUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CustomTabEventReceiver : BroadcastReceiver() {

    @Inject lateinit var tracker: Tracker

    override fun onReceive(context: Context?, intent: Intent?) {
        val overlayIntent = Intent(context, OriginalWebOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        when (intent?.action) {
            OriginalWebFragment.ACTION_OPEN_MENU -> {
                tracker.track(OriginalWebViewEvents.pocketMenuClicked())
                intent.dataString?.let { url ->
                    // the custom tab browser might have changed the url slightly,
                    // i.e. changing http to https.  If the url is still the "same" but slightly
                    // different, we want to use the original url the custom tab was launched with
                    // because that might have come from a recommendation somewhere and we want the
                    // url to match the recommendation if that is the case.
                    if (
                        UrlUtil.areUrlsTheSame(OriginalWebFragment.urlCustomTabsWasLaunchedWith, url)
                        || UrlUtil.areUrlsTheSame(OriginalWebFragment.resolvedUrlCustomTabsWasLaunchedWith, url)
                    ) {
                        overlayIntent.putExtra(
                            OriginalWebOverlayActivity.URL_EXTRA,
                            OriginalWebFragment.urlCustomTabsWasLaunchedWith
                        )
                    } else {
                        overlayIntent.putExtra(OriginalWebOverlayActivity.URL_EXTRA, url)
                    }
                    context?.startActivity(overlayIntent)
                }

            }
            OriginalWebFragment.ACTION_PREVIOUS_NEXT_CLICKED -> {
                when (intent.extras?.getInt(EXTRA_REMOTEVIEWS_CLICKED_ID)) {
                    R.id.previousItem -> {
                        OriginalWebFragment.resumeAction = OriginalWebFragment.ResumeAction.PREVIOUS
                    }
                    R.id.nextItem -> {
                        OriginalWebFragment.resumeAction = OriginalWebFragment.ResumeAction.NEXT
                    }
                }
                context?.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
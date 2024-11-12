package com.pocket.sdk.tts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pocket.app.App
import com.pocket.sdk.api.generated.enums.CxtUi
import com.pocket.sdk.api.generated.thing.ActionContext
import com.pocket.sdk.util.DeepLinks
import com.pocket.sync.value.Parceller

/**
 * Handles routing listen related [DeepLinks], since deep links open activities.
 * Should be transparent Activity, it will close itself immediately.
 *
 *
 * A few cases to handle:
 *
 *  * /listen when Pocket is closed, will start the playlist from the beginning and fire a general app launch intent.
 *  * /listen when Pocket is open, will start the playlist from the beginning and will leave the app at its current screen.
 *
 */
class ListenDeepLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            App.from(this)
                .listen()
                .trackedControls(null, CxtUi.DEEP_LINK)
                .play() // TODO pass uiContext?
            if (!App.isUserPresent()) {
                startActivity(
                    DeepLinks.newOpenAppIntent(
                        this,
                        Parceller.get(intent, EXTRA_UI_CONTEXT, ActionContext.JSON_CREATOR)
                    )
                )
            }
        }
        finish()
    }

    companion object {
        private const val EXTRA_UI_CONTEXT = "uiContext"
        @JvmStatic fun newStartIntent(context: Context?, uiContext: ActionContext?): Intent {
            val intent = Intent(context, ListenDeepLinkActivity::class.java)
            Parceller.put(intent, EXTRA_UI_CONTEXT, uiContext)
            return intent
        }
    }
}
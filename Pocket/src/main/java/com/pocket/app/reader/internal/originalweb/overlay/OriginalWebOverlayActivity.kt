package com.pocket.app.reader.internal.originalweb.overlay

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.StyleRes
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ActivityOriginalWebOverlayBinding
import com.pocket.app.listen.ListenView.PlayerState
import com.pocket.app.reader.internal.originalweb.overlay.bottomsheet.OriginalWebBottomSheetFragment
import com.pocket.app.settings.Theme
import com.pocket.sdk.tts.Listen
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.ui.view.button.IconButton
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@AndroidEntryPoint
class OriginalWebOverlayActivity : AbsPocketActivity() {

    @Inject lateinit var theme: Theme
    @Inject lateinit var listen: Listen

    private lateinit var binding: ActivityOriginalWebOverlayBinding

    override fun getAccessType(): ActivityAccessRestriction = ActivityAccessRestriction.ANY

    override fun getActivityBackground(): Drawable? = null

    private var listenObserver: Disposable? = null

    @StyleRes override fun themeOverride(): Int = R.style.Theme_Transparent_StandaloneDialogActivity2

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(window.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        lp.windowAnimations = android.R.style.Animation_Dialog
        window.attributes = lp

        binding = ActivityOriginalWebOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.setOnTouchListener { _: View?, _: MotionEvent? ->
            finish()
            true
        }

        intent.getStringExtra(URL_EXTRA)?.let { url ->
            OriginalWebBottomSheetFragment.newInstance(url).show(
                supportFragmentManager,
                OriginalWebBottomSheetFragment::class.simpleName
            )
        } ?: finish()

        setupListen()
    }

    // we need to setup click listeners to finish the activity when the user closes listen
    private fun setupListen() {
        // listen might not be open when first starting the activity, so subscribe to it
        // and setup the click listener when it's showing
        listenObserver = listenViewStates.subscribe { state ->
            if (state.player == PlayerState.MINI) {
                setupListenCloseButtonClickListener()
            }
        }

        // also try to set up the click listener right off the bat
        Handler(Looper.getMainLooper()).post {
            setupListenCloseButtonClickListener()
        }
    }

    private fun setupListenCloseButtonClickListener() {
        val closeButton = findViewById<IconButton>(R.id.listen_mini_close)
        closeButton?.setOnClickListener {
            listen.trackedControls(null, null).off()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenObserver?.dispose()
    }

    companion object {
        const val URL_EXTRA = "url"
    }
}
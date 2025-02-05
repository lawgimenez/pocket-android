package com.pocket.app.listen

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewListenBinding
import com.pocket.analytics.ItemContent
import com.pocket.app.App
import com.pocket.app.PocketApp
import com.pocket.sdk.api.generated.enums.CxtSection
import com.pocket.sdk.api.generated.enums.CxtUi
import com.pocket.sdk.api.generated.enums.CxtView
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier
import com.pocket.sdk.api.generated.thing.ActionContext
import com.pocket.sdk.tts.*
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sdk.util.view.tooltip.Tooltip.TooltipController
import com.pocket.sdk2.analytics.context.Contextual
import com.pocket.sdk2.analytics.context.ContextualRoot
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.sync.thing.Thing
import com.pocket.ui.util.PocketUIViewUtil
import com.pocket.ui.view.bottom.BottomSheetBackgroundDrawable
import com.pocket.ui.view.notification.PktSnackbar
import com.pocket.ui.view.themed.ThemedCoordinatorLayout
import com.pocket.util.DisplayUtil.displayAuthors
import com.pocket.util.DisplayUtil.displayHost
import com.pocket.util.android.AccessibilityUtils.BottomSheetHelper
import com.pocket.util.android.ViewUtil
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.*

/**
 * The main in-app control panel for [Listen].
 * User can collapse for access to the app, or expand for more detailed controls.
 */
class ListenView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    ThemedCoordinatorLayout(
        context!!, attrs
    ), Contextual {
    class State(@JvmField val player: PlayerState, @JvmField val interaction: Interaction)
    enum class PlayerState {
        FULLSCREEN, MINI
    }

    private val views: ViewListenBinding
    private val controls: Controls
    private val playlistControls: Controls
    private val miniPlayerControls: Controls
    private val errorControls: Controls
    private val app: PocketApp
    private val bottomSheetStates = PublishSubject.create<Int>()
    private var state: ListenState? = null
    private var error: ListenError? = null
    private var offlineHint: TooltipController? = null
    private val playlistAdapter: ListenItemAdapter
    private val bottomSheetBehavior: BottomSheetBehavior<View>
    private val playerView: ListenPlayerView
    private val miniLoadingSpinnerDrawable: Animatable
    private var isLaidOut = false

    init {
        app = App.from(context)
        views = ViewListenBinding.inflate(LayoutInflater.from(context), this)
        val analytics: ContextualRoot? = AbsPocketActivity.from(getContext())
        if (analytics != null) {
            analytics.bindViewContext(
                views.listenPlaylist,
                Contextual { ActionContext.Builder().cxt_ui(CxtUi.PLAYLIST).build() })
            analytics.bindViewContext(
                views.listenMiniPlayer,
                Contextual { ActionContext.Builder().cxt_ui(CxtUi.MINI_PLAYER).build() })
        }
        val listen = app.listen()
        controls = listen.trackedControls(this, null)
        playlistControls = listen.trackedControls(views.listenPlaylist, null)
        miniPlayerControls = listen.trackedControls(views.listenMiniPlayer, null)
        errorControls = listen.controls()
        views.listenStickyPlayer.background = BottomSheetBackgroundDrawable(getContext())
        views.mediaBottomSheet.background = BottomSheetBackgroundDrawable(getContext())
        bottomSheetBehavior = BottomSheetBehavior.from(views.mediaBottomSheet)
        bottomSheetBehavior.setBottomSheetCallback(BottomSheetCallback())
        playerView = ListenPlayerView(getContext())
        playlistAdapter =
            ListenItemAdapter(getContext(), playerView) { clicked: View?, position: Int ->
                if (state!!.index != position) {
                    playlistControls.moveTo(position)
                    playlistControls.play()
                }
            }
        views.listenPlaylist.adapter = playlistAdapter
        views.listenPlaylist.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val scrolled = -playerView.top
                if (scrolled > playerView.stickyOffset - views.listenStickyPlayer.height) {
                    views.listenStickyPlayer.visibility = VISIBLE
                } else {
                    views.listenStickyPlayer.visibility = INVISIBLE
                }
            }
        })
        views.listenMiniClose.setOnClickListener { v: View? -> miniPlayerControls.off() }
        views.listenMiniPlayPause.setOnClickListener { v: View? -> miniPlayerControls.playToggle() }
        views.listenMiniTitle.setOnClickListener { v: View? ->
            bottomSheetBehavior.setState(
                BottomSheetBehavior.STATE_EXPANDED
            )
        }
        miniLoadingSpinnerDrawable =
            (findViewById<View>(R.id.listen_mini_play_pause_ring) as ImageView).drawable as Animatable
        views.listenError.bind().type(PktSnackbar.Type.ERROR_EXCLAIM)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        isLaidOut = true
    }

    val isExpanded: Boolean
        get() = bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
    val states: Observable<State>
        get() = bottomSheetStates
            .filter { state: Int -> state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_EXPANDED }
            .map { state: Int ->
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    return@map State(PlayerState.MINI, Interaction.on(this))
                } else if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    return@map State(PlayerState.FULLSCREEN, Interaction.on(this))
                }
                throw AssertionError("Other states should've been filtered out.")
            }

    fun collapse() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun expand() {
        if (isLaidOut) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        } else {
            PocketUIViewUtil.runAfterNextLayoutOf(
                this
            ) { bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED) }
        }
    }

    /** This view is currently unused and can release resources or clean up. [will be invoked in the future if it is needed again. ][.bind] */
    fun unbind() {
        state = null
        error = null
        views.listenError.bind().dismiss()
    }

    fun bind(state: ListenState) {
        this.state = state
        if (state.playstate == PlayState.PLAYING || state.playstate == PlayState.BUFFERING) {
            views.listenMiniPlayPause.setImageResource(com.pocket.ui.R.drawable.ic_pkt_pause_mini)
            views.listenMiniPlayPause.contentDescription = resources.getString(com.pocket.ui.R.string.ic_pause)
            views.listenMiniPlayPause.uiEntityIdentifier =
                UiEntityIdentifier.LISTEN_MINI_PAUSE.value
        } else {
            views.listenMiniPlayPause.setImageResource(com.pocket.ui.R.drawable.ic_pkt_play_mini)
            views.listenMiniPlayPause.contentDescription = resources.getString(com.pocket.ui.R.string.ic_play)
            views.listenMiniPlayPause.uiEntityIdentifier = UiEntityIdentifier.LISTEN_MINI_PLAY.value
        }
        val maxProgress = resources.getInteger(R.integer.listen_max_progress)
        if (state.duration.seconds == 0L) {
            views.listenMiniProgress.progress = 0
        } else {
            val elapsed = state.elapsed.seconds
            val duration = state.duration.seconds
            views.listenMiniProgress.progress = (elapsed * maxProgress / duration).toInt()
        }
        views.listenMiniProgress.secondaryProgress = (state.bufferingProgress * maxProgress).toInt()
        views.listenMiniTitle.text = if (state.current == null) null else state.current.displayTitle
        if (state.current != null) {
            val (_, _, idUrl, _, _, displayTitle, displayUrl, _, _, authors) = state.current
            views.listenStickyHeadline.text = displayTitle
            if (authors.isEmpty()) {
                views.listenStickySubhead.text = displayHost(displayUrl)
            } else {
                views.listenStickySubhead.text = String.format(
                    Locale.getDefault(),
                    "%1\$s · %2\$s",
                    displayHost(displayUrl),
                    displayAuthors(authors)
                )
            }
            app.tracker().bindContent(this, ItemContent(idUrl))
        }
        views.listenStickyControls.bind(state, controls, shouldShowDegradedPlayer(state))
        playlistAdapter.bind(state, controls, shouldShowDegradedPlayer(state))
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            applyBottomSheetOffset(1f)
        } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            applyBottomSheetOffset(0f)
        } else {
            // Leave as is
        }
        if (state.error != null && error != state.error) {
            error =
                state.error // Cache the error so we know what we have already displayed to the user.
            when (error) {
                ListenError.INIT_FAILED, ListenError.NO_TTS_INSTALLED, ListenError.NO_VOICES -> AlertDialog.Builder(
                    context
                )
                    .setTitle(R.string.tts_dg_t)
                    .setMessage(R.string.tts_dg_not_available_m)
                    .setNeutralButton(R.string.ac_cancel, null)
                    .setPositiveButton(
                        R.string.tts_settings
                    ) { dialog: DialogInterface?, which: Int ->
                        TTSUtils.openTTSSettings(
                            AbsPocketActivity.from(
                                context
                            )
                        )
                    }
                    .show()
                ListenError.ARTICLE_NOT_DOWNLOADED -> {
                    var error1 = resources.getString(R.string.tts_article_unavailable)
                    if (state.autoPlay) {
                        error1 = error1 + " " + resources.getString(R.string.tts_skipping_to_next)
                        errorControls.next()
                    }
                    announceError(error1)
                }
                ListenError.ARTICLE_PARSING_FAILED -> {
                    var error2 = resources.getString(R.string.tts_article_failed)
                    if (state.autoPlay) {
                        error2 = error2 + " " + resources.getString(R.string.tts_skipping_to_next)
                        errorControls.next()
                    }
                    announceError(error2)
                }
                ListenError.SPEECH_ERROR -> {
                    announceError(R.string.tts_speech_error)
                    errorControls.pause()
                }
                ListenError.EMPTY_LIST -> {
                    Toast.makeText(context, R.string.tts_empty_list, Toast.LENGTH_LONG).show()
                    errorControls.off()
                }
                ListenError.NETWORK_ERROR, ListenError.TIMED_OUT -> {
                    if (!app.http().status().isOnline && state.voice.isNetworkConnectionRequired) {
                        offlineHint = playerView.showOfflineHint(views.mediaBottomSheet)
                    } else {
                        announceError(if (error == ListenError.TIMED_OUT) R.string.listen_error_timeout else R.string.dg_api_generic_error)
                    }
                    errorControls.pause()
                }
                ListenError.SERVER_ERROR -> {
                    announceError(R.string.listen_error_server)
                    errorControls.pause()
                }
                ListenError.MEDIA_PLAYER -> {
                    announceError(R.string.listen_error_media_player)
                    if (state.playstate == PlayState.PLAYING) controls.pause()
                }
                ListenError.LOGGED_OUT ->                    // Not expecting the logged out case, and for any other unknowns, just close the player for now.
                    errorControls.off()
                else -> errorControls.off()
            }
        } else if (state.error == null) {
            // Clear the cached error, so we can display it again if the same error happens again in the future.
            error = null
            dismissError()
        }
        if (state.playstate == PlayState.BUFFERING) {
            if (!miniLoadingSpinnerDrawable.isRunning) miniLoadingSpinnerDrawable.start()
        } else {
            miniLoadingSpinnerDrawable.stop()
        }
    }

    private fun shouldShowDegradedPlayer(state: ListenState): Boolean {
        return !state.supportedFeatures.contains(ListenState.Feature.ACCURATE_DURATION_AND_ELAPSED)
    }

    private fun announceError(stringRes: Int) {
        announceError(resources.getString(stringRes))
    }

    private fun announceError(value: CharSequence) {
        // TODO Consider announcing over audio if user is not active on this screen.
        views.listenError.bind()
            .message(value)
            .onAction(R.string.ac_retry) { v: View? -> controls.playToggle() }
            .show()
        views.listenError.visibility = VISIBLE
    }

    private fun dismissError() {
        if (views.listenError.visibility == VISIBLE) {
            views.listenError.bind().dismiss()
        }
        if (offlineHint != null) {
            offlineHint!!.dismiss()
            offlineHint = null
        }
    }

    private fun applyBottomSheetOffset(slideOffset: Float) {
        playlistAdapter.applyBottomSheetOffset(slideOffset)
        if (slideOffset >= 0.25f) {
            views.listenMiniPlayer.visibility = GONE
        } else {
            views.listenMiniPlayer.visibility = VISIBLE
            views.listenMiniPlayer.alpha = 1 - slideOffset * 4
        }
        if (slideOffset == 0f) {
            views.scrim.visibility = GONE
        } else {
            views.scrim.visibility = VISIBLE
            views.scrim.background.alpha = (255 * 0.2 * slideOffset).toInt()
        }
    }

    override fun isPointInChildBounds(child: View, x: Int, y: Int): Boolean {
        // BottomSheetBehaviour uses this method to check if the user is touching a nested scrolling view. It wasn't
        // accounting for views obscuring the scrolling view.
        // For our case we work around it by checking if the sticky player obscures the top of the playlist.
        return if (child === views.listenPlaylist && ViewUtil.isVisible(views.listenStickyPlayer) && super.isPointInChildBounds(
                views.listenStickyPlayer,
                x,
                y
            )
        ) {
            false
        } else super.isPointInChildBounds(child, x, y)
    }

    override fun getActionContext(): ActionContext {
        val actionContext = ActionContext.Builder()
            .cxt_view(CxtView.LISTEN)
            .cxt_ui(CxtUi.PLAYER)
        if (state != null) {
            actionContext
                .cxt_index(state!!.index + 1)
                .cxt_progress(state!!.progressPercent)
            if (state!!.current != null) {
                val sessionId = app.itemSessions().getSessionId(state!!.current.idUrl)
                if (sessionId != null) {
                    actionContext.item_session_id(sessionId.toString())
                }
            }
        }
        return actionContext.build()
    }

    private inner class BottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        private val accessibilityHelper = BottomSheetHelper()
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            showCellularDataAlertIfNeeded(newState)
            trackBottomSheetStateChange(newState)
            bottomSheetStates.onNext(newState)
            // It's important to track session start after pushing the state to the subject,
            // so that Reader can catch the change first and close its session.
            trackItemSessionStartIfNeeded(newState)
            accessibilityHelper.updateAccessibilityState(this@ListenView, newState, false)
        }

        private fun showCellularDataAlertIfNeeded(newState: Int) {
            if (newState != BottomSheetBehavior.STATE_EXPANDED) {
                return  // Show only when user opens Listen...
            }
            if (state == null) {
                return  // ... and not before Listen has gotten setup...
            }
            if (!app.prefs().SHOW_LISTEN_DATA_ALERT.get()) {
                return  // ... for the first time ...
            }
            if (app.http().status().isWifi || !app.http().status().isOnline) {
                return  // ... while on cellular.
            }
            if (state!!.voice == null || !state!!.voice.isNetworkConnectionRequired) {
                return  // Also don't show it if using an offline voice.
            }

            // After seeing it once, it won’t show again (unless you logout, then log back in).
            app.prefs().SHOW_LISTEN_DATA_ALERT.set(false)
            playerView.showDataAlert()
        }

        private fun trackBottomSheetStateChange(newState: Int) {
            val pocket = app.pocket()
            val it = Interaction.on(this@ListenView)
            val pv = pocket.spec().actions().pv()
                .context(it.context)
                .time(it.time)
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                pv.view(CxtView.LISTEN_MINI_PLAYER).section(CxtSection.MAXIMIZE)
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                pv.view(CxtView.LISTEN_PLAYER).section(CxtSection.MINIMIZE)
            } else {
                // In this case we don't want to track any action.
                return
            }
            pocket.sync<Thing?>(null, pv.build())
        }

        private fun trackItemSessionStartIfNeeded(newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                controls.foreground()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            applyBottomSheetOffset(slideOffset)
        }
    }
}
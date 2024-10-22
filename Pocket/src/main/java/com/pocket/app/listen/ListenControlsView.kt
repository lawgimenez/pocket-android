package com.pocket.app.listen

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.ideashower.readitlater.R
import com.ideashower.readitlater.databinding.ViewListenControlsBinding
import com.pocket.app.App
import com.pocket.app.undobar.UndoableItemAction.Companion.fromTrack
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier
import com.pocket.sdk.tts.Controls
import com.pocket.sdk.tts.ListenState
import com.pocket.sdk.tts.PlayState
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.ui.view.themed.ThemedConstraintLayout
import com.pocket.util.android.ViewUtil
import org.threeten.bp.Duration
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

/** A row of control buttons for Listen, like play/pause, skip, change speed, etc.  */
class ListenControlsView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    ThemedConstraintLayout(context, attrs) {
    private val views: ViewListenControlsBinding
    private val speedPopup: ListenSpeedControlsPopup
    private val format = NumberFormat.getNumberInstance(Locale.getDefault())
    private var state: ListenState? = null
    private var controls: Controls? = null
    private val loadingSpinnerDrawable: Animatable

    init {
        clipChildren = false
        views = ViewListenControlsBinding.inflate(LayoutInflater.from(context), this)
        val speedPopupWidth = resources.getDimensionPixelSize(R.dimen.listen_speed_popup_width)
        val speedPopupHeight = resources.getDimensionPixelSize(R.dimen.listen_speed_popup_height)
        speedPopup = ListenSpeedControlsPopup(context, speedPopupWidth, speedPopupHeight)
        views.listenSpeed.setOnClickListener { v: View? ->
            speedPopup.showAsDropDown(
                views.listenSpeed,
                (views.listenSpeed.width - speedPopupWidth) / 2,
                views.listenSpeed.height / 2 - speedPopupHeight
            )
        }
        speedPopup.setOnPlusClickListener { v: View? ->
            controls!!.setSpeed(
                Math.min(
                    state!!.speed + SPEED_INCREMENT, SPEED_MAX
                )
            )
        }
        speedPopup.setOnMinusClickListener { v: View? ->
            controls!!.setSpeed(
                Math.max(
                    state!!.speed - SPEED_INCREMENT, SPEED_MIN
                )
            )
        }
        views.listenPlayPause.setOnClickListener { v: View? -> controls!!.playToggle() }
        views.listenSkipBack.setOnClickListener { v: View? ->
            controls!!.seekTo(
                state!!.elapsed.minus(SEEK_DURATION)
            )
        }
        views.listenSkip.setOnClickListener { v: View? ->
            controls!!.seekTo(
                state!!.elapsed.plus(SEEK_DURATION)
            )
        }
        views.listenPrev.setOnClickListener { v: View? -> controls!!.previous() }
        views.listenNext.setOnClickListener { v: View? -> controls!!.next() }
        views.listenArchive.setOnClickListener { v: View? ->
            App.from(context).undo().archive(
                fromTrack(
                    state!!.current, Interaction.on(this).context
                )
            )
            controls!!.remove(state!!.current)
        }
        loadingSpinnerDrawable =
            (findViewById<View>(R.id.listen_play_pause_ring) as ImageView).drawable as Animatable
        initSpeedButtonFormat()
    }

    private fun initSpeedButtonFormat() {
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 1
        if (format is DecimalFormat) {
            format.positiveSuffix = "x"
        }
    }

    fun bind(state: ListenState, controls: Controls?, shouldShowDegradedPlayer: Boolean) {
        this.state = state
        this.controls = controls
        val speed = format.format(state.speed.toDouble())
        views.listenSpeed.text = speed
        speedPopup.setSpeed(speed)
        views.listenArchive.isEnabled =
            state.current != null // Can happen when the player is loading/initializing
        when (state.playstate) {
            PlayState.STARTING -> {
                loadingSpinnerDrawable.stop()
                ViewUtil.setVisibility(
                    INVISIBLE,
                    views.listenPrev,
                    views.listenSkipBack,
                    views.listenSkip,
                    views.listenNext
                )
            }
            PlayState.BUFFERING -> {
                if (!loadingSpinnerDrawable.isRunning) loadingSpinnerDrawable.start()
                ViewUtil.setVisibility(INVISIBLE, views.listenSkipBack, views.listenSkip)
                ViewUtil.setVisibility(VISIBLE, views.listenPrev, views.listenNext)
                views.listenPlayPause.setImageResource(com.pocket.ui.R.drawable.ic_pkt_pause_solid)
                views.listenPlayPause.contentDescription = resources.getString(com.pocket.ui.R.string.ic_pause)
                views.listenPlayPause.uiEntityIdentifier = UiEntityIdentifier.LISTEN_PAUSE.value
            }
            PlayState.PLAYING -> {
                loadingSpinnerDrawable.stop()
                ViewUtil.setVisibility(VISIBLE, views.listenSkipBack, views.listenSkip)
                ViewUtil.setVisibility(INVISIBLE, views.listenPrev, views.listenNext)
                views.listenPlayPause.setImageResource(com.pocket.ui.R.drawable.ic_pkt_pause_solid)
                views.listenPlayPause.contentDescription = resources.getString(com.pocket.ui.R.string.ic_pause)
                views.listenPlayPause.uiEntityIdentifier = UiEntityIdentifier.LISTEN_PAUSE.value
            }
            PlayState.PAUSED, PlayState.ERROR -> {
                loadingSpinnerDrawable.stop()
                ViewUtil.setVisibility(VISIBLE, views.listenPrev, views.listenNext)
                ViewUtil.setVisibility(INVISIBLE, views.listenSkipBack, views.listenSkip)
                views.listenPlayPause.setImageResource(com.pocket.ui.R.drawable.ic_pkt_play_solid)
                views.listenPlayPause.contentDescription = resources.getString(com.pocket.ui.R.string.ic_play)
                views.listenPlayPause.uiEntityIdentifier = UiEntityIdentifier.LISTEN_PLAY.value
            }
            else -> {
                loadingSpinnerDrawable.stop()
                ViewUtil.setVisibility(
                    INVISIBLE,
                    views.listenPrev,
                    views.listenSkipBack,
                    views.listenSkip,
                    views.listenNext
                )
            }
        }
        if (shouldShowDegradedPlayer) {
            views.listenSkipBack.setImageResource(com.pocket.ui.R.drawable.ic_pkt_skip_back_line)
            views.listenSkip.setImageResource(com.pocket.ui.R.drawable.ic_pkt_skip_line)
        } else {
            views.listenSkipBack.setImageResource(com.pocket.ui.R.drawable.ic_pkt_15_back_line)
            views.listenSkip.setImageResource(com.pocket.ui.R.drawable.ic_pkt_15_skip_line)
        }
    }

    companion object {
        private val SEEK_DURATION = Duration.ofSeconds(15)
        private const val SPEED_INCREMENT = 0.1f
        private const val SPEED_MIN = 0.5f
        private const val SPEED_MAX = 4f
    }
}
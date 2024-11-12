package com.pocket.ui.view.info

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.StringRes
import com.airbnb.lottie.LottieAnimationView
import com.pocket.ui.R
import com.pocket.ui.view.themed.ThemedConstraintLayout

/**
 * A footer view for the end of a feed.
 *
 * https://www.figma.com/file/K70GjbldhXypBlshbeciA0/Android-MVP
 */
class FeedFooterView : ThemedConstraintLayout {

    val binder = Binder()

    private val text: TextView
    private val animation: LottieAnimationView

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_feed_footer, this, true)
        text = findViewById(R.id.text)
        animation = findViewById(R.id.animation)
    }

    inner class Binder {
        fun clear() : Binder  {
            text(null)
            return this
        }
        fun text(message: CharSequence?) : Binder {
            text.text = message
            return this
        }
        fun text(@StringRes message: Int) : Binder {
            text.text = resources.getText(message)
            return this
        }
    }

    fun playAnimation() {
        animation.playAnimation()
    }
}
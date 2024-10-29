package com.pocket.ui.view.animated

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.pocket.ui.R
import com.pocket.ui.view.themed.ThemedLottieAnimationView

class BottomFeedAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ThemedLottieAnimationView(context, attrs, defStyleAttr) {

    private var hasPlayed = false

    override fun asset(): String {
        return "pkt_feed_footer_anim.json"
    }

    override fun light() : List<ColorChange> {
        return listOf(
                ColorChange(
                    KeyPath(
                        "Book Animation - DYNAMIC",
                        "PAGE_COLOR",
                        "Group 1",
                        "Stroke 1",
                    ),
                    LottieProperty.STROKE_COLOR,
                    ContextCompat.getColor(context, R.color.black),
                )
        )
    }

    override fun dark() : List<ColorChange> {
        return listOf(
                ColorChange(
                    KeyPath(
                        "Book Animation - DYNAMIC",
                        "PAGE_COLOR",
                        "Group 1",
                        "Stroke 1",
                    ),
                    LottieProperty.STROKE_COLOR,
                    ContextCompat.getColor(context, R.color.white),
                )
        )
    }

    override fun playAnimation() {
        if (!hasPlayed) {
            hasPlayed = true
            super.playAnimation()
        }
    }
}
package com.pocket.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.model.KeyPath
import com.pocket.ui.R
import com.pocket.util.java.Logs

/**
 * Lottie animations are delivered as rather large JSON files (70kb for simple cases).  Rather than add multiple files
 * for each theme, specific color groups are modified programmatically, depending on the theme.
 *
 * In order to implement, simply subclass this one, and implement what specific color groups should look like for each theme.
 * This includes:
 *      * KeyPath - the keypath within the lottie file that refers to the element you would like to change.
 *      * LottieProperty - the color property it represents, typically LottieProperty.STROKE_COLOR or LottieProperty.COLOR
 *      * Color - the color to replace with
 *
 * The design team should provide a list of color groups to modify, but see
 * https://medium.com/comparethemarket/lottie-on-android-part-3-dynamic-properties-8aa566ba4fbf
 * for examples and insight into debugging Lottie color keypath / groups.
 */
abstract class ThemedLottieAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LottieAnimationView(context, attrs, defStyleAttr) {

    data class ColorChange(val keypath: KeyPath, val lottieProperty: Int, @ColorInt val color: Int)

    init {
        enableMergePathsForKitKatAndAbove(true)
        setAnimation(asset())
    }

    /**
     * Lottie animation views seem to have a bug in layout, where their width/height is still zero even after
     * a layout pass.  Various methods, such as post, onLayout, ViewCompat.isLaidOut, all return a height / width
     * of zero until about 200ms after attaching the View programmatically.
     *
     * This method simply checks if width/height are both greater than zero, which is what ViewCompat.isLaidOut
     * actually does for lower API levels, although that method doesn't work for Lottie Views at API 19+.
     */
    private fun isLaidOutCompat() : Boolean {
        return height > 0 && width > 0
    }

    /**
     * Posts a runnable when we're sure this Lottie animation view has been laid out on the screen.
     */
    fun postOnLayoutCompat(runnable: Runnable) {
        post {
            if (isLaidOutCompat()) {
                runnable.run()
            } else {
                postOnLayoutCompat(runnable)
            }
        }
    }

    /**
     * Override and set to true in order to output Lottie animation keypaths for this animation.  Useful
     * for finding specific elements you'd like to modify in updateTheme()
     */
    open fun debugKeypaths() : Boolean {
        return false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (isShown && debugKeypaths()) {
            resolveKeyPath(KeyPath("**")).forEach {
                Logs.i("KeyPath", it.toString())
            }
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        when (AppThemeUtil.getState(this).firstOrNull()) {
            R.attr.state_light -> {
                updateTheme(light())
            }
            R.attr.state_dark -> {
                updateTheme(dark())
            }
        }
    }

    private fun updateTheme(changes: List<ColorChange>) {
        changes.forEach {
            addValueCallback(it.keypath, it.lottieProperty, { _ -> it.color })
        }
    }

    abstract fun asset() : String

    abstract fun light() : List<ColorChange>
    abstract fun dark() : List<ColorChange>
}
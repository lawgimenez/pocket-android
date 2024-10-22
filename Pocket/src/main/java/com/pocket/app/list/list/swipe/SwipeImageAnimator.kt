package com.pocket.app.list.list.swipe

import android.widget.ImageView
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

object SwipeImageAnimator {

    /**
     * Sets the proper alpha and scale for the left and right swipe action images
     *
     * @param swipeThresholdPercent the percent representing how far the swiped view has been moved towards
     * the swiping threshold where 0.5 is 50%
     * @param leftImage the image on the left that represents the action when swiping right
     * @param rightImage the image on the right that represents the action when swiping left
     */
    @Suppress("MagicNumber")
    fun updateImage(
        swipeThresholdPercent: Float,
        leftImage: ImageView,
        rightImage: ImageView,
    ) {
        val absVal = swipeThresholdPercent.absoluteValue
        // y = 12x^4
        // good formula for this.  Slow start with a sharp rise
        val y = 12 * absVal * absVal * absVal * absVal
        if (swipeThresholdPercent > 0) {
            leftImage.scaleX = min(1 + y, 2f)
            leftImage.scaleY = min(1 + y, 2f)
            leftImage.alpha = max(min(y, 1f), 0f)
            rightImage.alpha = 0f
        } else {
            rightImage.scaleX = min(1 + y, 2f)
            rightImage.scaleY = min(1 + y, 2f)
            rightImage.alpha = max(min(y, 1f), 0f)
            leftImage.alpha = 0f
        }
    }
}
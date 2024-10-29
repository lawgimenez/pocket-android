package com.pocket.ui.view.themed

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.pocket.analytics.api.EngageableHelper
import com.pocket.analytics.api.UiEntityable
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * A themed constraint layout that a user can swipe left or right
 */
open class ThemedSwipeConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    engageable: EngageableHelper = EngageableHelper(),
    entityType: UiEntityable.Type? = null,
) : ThemedConstraintLayout2(
    context,
    attrs,
    defStyleAttr,
    engageable,
    entityType
) {

    private var touchXStart = 0f
    private var currentX = 0f
    private var isMoving = false
        set(value) {
            field = value
            requestDisallowInterceptTouchEvent(field)
        }
    private val swipeThreshold: Float
        get() {
            val parentChildDifference = (maxSwipeDistance - width)
            return max((maxSwipeDistance - parentChildDifference) * swipeThresholdPercent, 1f)
        }
    private val maxSwipeDistance: Int
        get() = (parent as? View)?.width ?: width

    var swipeListener: SwipeListener? = null
    var swipeThresholdPercent: Float = 0.5f
    var allowSwiping = true

    fun reset() {
        resetPosition(0)
    }

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("ReturnCount")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!allowSwiping) {
            return super.onTouchEvent(event)
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchXStart = event.rawX
            }
            MotionEvent.ACTION_UP -> {
                val wasMoving = isMoving
                val touchXEnd = event.rawX
                when {
                    (touchXEnd - touchXStart) > swipeThreshold -> {
                        moveOffscreenRight()
                    }
                    (touchXStart - touchXEnd) > swipeThreshold -> {
                        moveOffscreenLeft()
                    }
                    else -> {
                        resetPosition()
                    }
                }
                return if (wasMoving) {
                    true
                } else {
                    super.onTouchEvent(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {

                val realX = event.rawX - touchXStart
                if (realX.absoluteValue >= MIN_MOVEMENT || isMoving) {
                    currentX = event.rawX - touchXStart
                    ObjectAnimator.ofFloat(this, "translationX", currentX).apply {
                        duration = 0
                        addUpdateListener(MovementUpdateListener())
                        start()
                    }
                    isMoving = true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                resetPosition()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun resetPosition(duration: Long = 50L) {
        isMoving = false
        ObjectAnimator.ofFloat(
            this, "translationX",
            currentX,
            0f
        ).apply {
            setDuration(duration)
            addUpdateListener(MovementUpdateListener())
            start()
        }
        currentX = 0f
    }

    private fun moveOffscreenLeft(duration: Long = DEFAULT_DURATION) {
        ObjectAnimator.ofFloat(
            this,
            "translationX",
            currentX,
            -maxSwipeDistance.toFloat(),
        ).apply {
            setDuration(duration)
            addUpdateListener(OffScreenUpdateListener {
                swipeListener?.onSwipedLeft()
            })
            addUpdateListener(MovementUpdateListener())
            start()
        }
    }

    private fun moveOffscreenRight(duration: Long = DEFAULT_DURATION) {
        ObjectAnimator.ofFloat(
            this, "translationX",
            currentX,
            maxSwipeDistance.toFloat(),
        ).apply {
            setDuration(duration)
            addUpdateListener(OffScreenUpdateListener {
                swipeListener?.onSwipedRight()
            })
            addUpdateListener(MovementUpdateListener())
            start()
        }
    }

    inner class OffScreenUpdateListener(
        private val onFinished: () -> Unit
    ) : ValueAnimator.AnimatorUpdateListener {

        override fun onAnimationUpdate(animation: ValueAnimator) {
            if (animation.animatedFraction == 1f) {
                onFinished.invoke()
            }
        }
    }

    inner class MovementUpdateListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            swipeListener?.onMovement(x / swipeThreshold)
        }
    }

    companion object {
        const val DEFAULT_DURATION = 200L
        const val MIN_MOVEMENT = 25
    }
}

interface SwipeListener {
    fun onSwipedRight()
    fun onSwipedLeft()

    /**
     * @param percentToSwipeThreshold the fraction of movement of the X coordinate from the starting
     * position to the swiping threshold
     */
    fun onMovement(percentToSwipeThreshold: Float)
}
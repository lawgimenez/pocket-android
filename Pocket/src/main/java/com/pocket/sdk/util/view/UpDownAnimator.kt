package com.pocket.sdk.util.view

import android.animation.ObjectAnimator
import android.view.View

class UpDownAnimator(
    private val view: View,
    private val animateDirection: Direction = Direction.UP
) {

    private var currentValue: Float = 0f
    private var currentState: State = State.SHOWING

    init {
        view.translationY = currentValue
    }

    fun show() {
        if (currentState == State.HIDDEN) {
            setupAnimator(0f)
            currentState = State.SHOWING
        }
    }

    fun hide() {
        if (currentState == State.SHOWING) {
            when (animateDirection) {
                Direction.UP -> setupAnimator(-view.height.toFloat())
                Direction.DOWN -> setupAnimator(view.height.toFloat())
            }
            currentState = State.HIDDEN
        }
    }

    private fun setupAnimator(targetValue: Float) {
        ObjectAnimator.ofFloat(
            view,
            "translationY",
            currentValue,
            targetValue
        ).apply {
            duration = ANIMATION_DURATION
            addUpdateListener { currentValue = it.animatedValue as Float }
            start()
        }
    }

    enum class State {
        HIDDEN,
        SHOWING,
    }

    enum class Direction {
        UP,
        DOWN,
    }

    companion object {
        private const val ANIMATION_DURATION = 250L
    }
}
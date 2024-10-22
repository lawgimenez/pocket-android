package com.pocket.app.list.bulkedit

import android.animation.ObjectAnimator
import com.ideashower.readitlater.databinding.ViewBulkEditSnackBarBinding
import com.pocket.ui.util.toPx

class BulkEditSnackBarAnimator(
    private val binding: ViewBulkEditSnackBarBinding
) {

    private var hidden = 75f.toPx(binding.root.context)
    private var currentValue: Float = hidden

    init {
        binding.root.translationY = currentValue
    }

    fun show() {
        setupAnimator(SHOWING)
    }

    fun hide() {
        setupAnimator(hidden)
    }

    private fun setupAnimator(targetValue: Float) {
        ObjectAnimator.ofFloat(
            binding.root,
            "translationY",
            currentValue,
            targetValue
        ).apply {
            duration = ANIMATION_DURATION
            addUpdateListener { currentValue = it.animatedValue as Float }
            start()
        }
    }

    companion object {
        private const val SHOWING = 0f
        private const val ANIMATION_DURATION = 250L
    }
}
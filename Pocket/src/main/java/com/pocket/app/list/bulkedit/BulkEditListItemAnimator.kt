package com.pocket.app.list.bulkedit

import android.animation.ValueAnimator
import android.view.View
import com.ideashower.readitlater.databinding.ViewListItemRowBinding
import javax.inject.Inject

class BulkEditListItemAnimator @Inject constructor() {

    private var valueAnimator: ValueAnimator? = null
    private var currentValue: Float = HIDDEN
    private var maxTranslationX: Float = 0f
    private var isShowing = false

    fun showBulkEdit(binding: ViewListItemRowBinding) {
        setCurrentState(binding)
        val listener = ValueAnimator.AnimatorUpdateListener { setCurrentState(binding) }
        if (isShowing) {
            if (currentValue != SHOWING) {
                valueAnimator?.addUpdateListener(listener)
            }
        } else {
            isShowing = true
            setupValueAnimator(SHOWING)
            valueAnimator?.addUpdateListener(listener)
        }
    }

    fun hideBulkEdit(binding: ViewListItemRowBinding) {
        setCurrentState(binding)
        val listener = ValueAnimator.AnimatorUpdateListener { setCurrentState(binding) }
        if (!isShowing) {
            if (currentValue != HIDDEN) {
                valueAnimator?.addUpdateListener(listener)
            }
        } else {
            isShowing = false
            setupValueAnimator(HIDDEN)
            valueAnimator?.addUpdateListener(listener)
        }
    }

    fun reset() {
        isShowing = false
        currentValue = 0f
        valueAnimator?.removeAllUpdateListeners()
        valueAnimator = null
    }

    private fun setCurrentState(binding: ViewListItemRowBinding) {
        if (maxTranslationX == 0f) {
            maxTranslationX = binding.actionLayout.width.toFloat()
        }
        binding.metaLayout.translationX = maxTranslationX * currentValue
        binding.actionLayout.alpha = 1 - currentValue
        binding.bulkEditRadioButton.alpha = currentValue
        binding.actionLayout.visibility = if (currentValue == SHOWING) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.bulkEditRadioButton.visibility = if (currentValue == HIDDEN) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun setupValueAnimator(endValue: Float) {
        valueAnimator?.removeAllUpdateListeners()
        valueAnimator = ValueAnimator.ofFloat(
            currentValue,
            endValue
        ).apply {
            duration = ANIMATION_DURATION
            addUpdateListener { currentValue = it.animatedValue as Float }
            start()
        }
    }

    companion object {
        private const val HIDDEN = 0f
        private const val SHOWING = 1f
        private const val ANIMATION_DURATION = 250L
    }
}
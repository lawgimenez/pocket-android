package com.pocket.app.home.loading

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.ideashower.readitlater.databinding.ViewHomeSlatesSkeletonBinding
import com.ideashower.readitlater.databinding.ViewHomeTabletSlatesSkeletonBinding
import com.pocket.ui.view.themed.ThemedConstraintLayout2
import com.pocket.util.android.FormFactor

class SlatesSkeletonView(
    context: Context,
    attrs: AttributeSet?,
) : ThemedConstraintLayout2(
    context,
    attrs,
) {

    init {
        if (!isInEditMode && FormFactor.isTablet(context)) {
            ViewHomeTabletSlatesSkeletonBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )
        } else {
            ViewHomeSlatesSkeletonBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )
        }
    }

    override fun setVisibility(visibility: Int) {
        // fade in to reduce jarring visibility differences between
        // loading skeleton and real data
        if (visibility == View.VISIBLE) {
            ObjectAnimator.ofFloat(
                this,
                "alpha",
                0f,
                1f,
            ).apply {
                duration = 1000
                start()
            }
        }
        super.setVisibility(visibility)
    }
}
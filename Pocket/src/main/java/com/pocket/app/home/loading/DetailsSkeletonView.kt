package com.pocket.app.home.loading

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.ideashower.readitlater.databinding.ViewHomeDetailsSkeletonBinding
import com.ideashower.readitlater.databinding.ViewHomeTabletDetailsSkeletonBinding
import com.pocket.ui.view.themed.ThemedLinearLayout
import com.pocket.util.android.FormFactor

class DetailsSkeletonView(
    context: Context,
    attrs: AttributeSet?,
) : ThemedLinearLayout(
    context,
    attrs,
) {

    init {
        if (FormFactor.isTablet(context)) {
            ViewHomeTabletDetailsSkeletonBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
            )
        } else {
            ViewHomeDetailsSkeletonBinding.inflate(
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
package com.pocket.app.home.loading

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.ideashower.readitlater.databinding.ViewHomeRecentSavesSkeletonBinding
import com.pocket.ui.view.themed.ThemedConstraintLayout2

class RecentSavesSkeletonView(
    context: Context,
    attrs: AttributeSet?,
) : ThemedConstraintLayout2(
    context,
    attrs,
) {
    val binding = ViewHomeRecentSavesSkeletonBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

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
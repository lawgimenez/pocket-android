package com.pocket.app.home.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.ideashower.readitlater.databinding.ViewHomeSlateWideHeroCardBinding
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.util.DimenUtil
import com.pocket.ui.view.themed.ThemedCardView

class WideHeroCardView(
    context: Context,
    attrs: AttributeSet?,
) : ThemedCardView(
    context,
    attrs,
) {

    @Suppress("MagicNumber")
    val binding = ViewHomeSlateWideHeroCardBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).apply {
        radius = DimenUtil.dpToPx(context, 16f)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        (layoutParams as LayoutParams).topMargin = DimenUtil.dpToPxInt(context, 1f)
    }

    init {
        setupAnalytics()
    }

    private fun setupAnalytics() {
        binding.saveLayout.setUiEntityType(UiEntityable.Type.BUTTON)
    }
}
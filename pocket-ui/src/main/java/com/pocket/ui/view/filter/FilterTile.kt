package com.pocket.ui.view.filter

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.databinding.ViewFilterTileBinding
import com.pocket.ui.util.DimenUtil
import com.pocket.ui.view.themed.ThemedConstraintLayout2

class FilterTile(
    context: Context,
    attrs: AttributeSet?,
) : ThemedConstraintLayout2(
    context,
    attrs,
    entityType = UiEntityable.Type.BUTTON,
) {

    private val binding: ViewFilterTileBinding = ViewFilterTileBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).apply {
        outlineProvider = object : ViewOutlineProvider() {
            @Suppress("MagicNumber")
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    view.height,
                    DimenUtil.dpToPx(context, 4f)
                )
            }
        }
        clipToOutline = true
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FilterTile,
            0,
            0
        ).apply {
            binding.title.text = getString(R.styleable.FilterTile_titleText)
            binding.description.text = getString(R.styleable.FilterTile_descriptionText)
            val foregroundColor = getColorStateList(R.styleable.FilterTile_foregroundColor)
            binding.title.setTextColor(foregroundColor)
            binding.description.setTextColor(foregroundColor)
        }
    }
}
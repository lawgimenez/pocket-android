package com.pocket.ui.view.badge

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.databinding.ViewBadgeBinding
import com.pocket.ui.util.toPxInt
import com.pocket.ui.view.themed.ThemedLinearLayout

class BadgeView(
    context: Context,
    attrs: AttributeSet? = null,
) : ThemedLinearLayout(
    context,
    attrs,
    entityType = UiEntityable.Type.BUTTON
) {

    private val binding: ViewBadgeBinding = ViewBadgeBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).also {
        // setting params here because our root layout in xml is a <merge> tag
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun setValues(type: Type, text: String) {
        when (type) {
            Type.TAG -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_badge_tag)
                binding.text.setTextColor(
                    ContextCompat.getColorStateList(context, R.color.pkt_badge_tag_foreground)
                )
                binding.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pkt_tag_line))
                binding.icon.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.pkt_badge_tag_foreground)
                engageable.uiEntityIdentifier = "tagBadge"
            }
            Type.EMPHASIZED_TAG -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_badge_tag_emphasized)
                binding.text.setTextColor(
                    ContextCompat.getColorStateList(context, R.color.pkt_badge_tag_emphasized_foreground)
                )
                binding.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pkt_tag_line))
                binding.icon.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.pkt_badge_tag_emphasized_foreground)
                engageable.uiEntityIdentifier = "tagBadge"
            }
            Type.HIGHLIGHT -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_badge_highlights)
                binding.text.setTextColor(
                    ContextCompat.getColorStateList(context, R.color.pkt_themed_amber_2)
                )
                binding.icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pkt_highlights_line))
                binding.icon.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.pkt_themed_amber_2)
                engageable.uiEntityIdentifier = null
            }
        }
        binding.text.text = text
        updatePadding(
            left = 5f.toPxInt(context),
            right = 5f.toPxInt(context),
            top = 5f.toPxInt(context),
            bottom = 5f.toPxInt(context),
        )
    }

    enum class Type {
        TAG,
        HIGHLIGHT,
        EMPHASIZED_TAG
    }
}
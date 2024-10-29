package com.pocket.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.databinding.ViewBottomNavigationButtonBinding
import com.pocket.ui.text.Fonts
import com.pocket.ui.util.CheckableHelper
import com.pocket.ui.view.themed.ThemedConstraintLayout2

/**
 * An IconButton with a text label, for use in tabbed navigation.
 *
 * https://www.figma.com/file/K70GjbldhXypBlshbeciA0/Android-MVP
 */
class BottomNavigationButton(
    context: Context,
    attrs: AttributeSet?,
) : ThemedConstraintLayout2(
    context = context,
    attrs = attrs,
    entityType = UiEntityable.Type.BUTTON
), CheckableHelper.Checkable  {

    private val checkable = CheckableHelper(this).apply {
        isCheckable = true
    }

    private val binding: ViewBottomNavigationButtonBinding = ViewBottomNavigationButtonBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    var badgeVisible: Boolean = false
        set(value) {
            binding.badge.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BottomNavigationButton,
            0,
            0
        ).apply {
            binding.icon.setImageResource(getResourceId(R.styleable.BottomNavigationButton_imageSrc, 0))
            binding.label.text = getText(R.styleable.BottomNavigationButton_labelText)
            binding.root.contentDescription = getText(R.styleable.BottomNavigationButton_labelText)
            recycle()
        }
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        TooltipCompat.setTooltipText(this, contentDescription)
    }

    override fun isChecked(): Boolean {
        return checkable.isChecked
    }

    override fun isCheckable(): Boolean {
        return checkable.isCheckable
    }

    override fun toggle() {
        checkable.toggle()
    }

    override fun setCheckable(isCheckable: Boolean) {
        checkable.isCheckable = isCheckable
    }

    override fun setChecked(checked: Boolean) {
        checkable.isChecked = checked
        binding.label.typeface = if (checked) {
            Fonts.get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM)
        } else {
            Fonts.get(context, Fonts.Font.GRAPHIK_LCG_REGULAR)
        }
        binding.icon.setDrawableColor(
            if (checked) {
                ContextCompat.getColorStateList(context, R.color.pkt_themed_grey_1)
            } else {
                ContextCompat.getColorStateList(context, R.color.pkt_themed_grey_3)
            }
        )
        binding.label.setTextColor(
            if (checked) {
                ContextCompat.getColorStateList(context, R.color.pkt_themed_grey_1)
            } else {
                ContextCompat.getColorStateList(context, R.color.pkt_themed_grey_3)
            }
        )
    }

    override fun setOnCheckedChangeListener(listener: CheckableHelper.OnCheckedChangeListener?) {
        checkable.setOnCheckedChangeListener(listener)
    }

    companion object {

        @JvmStatic
        @BindingAdapter("isChecked")
        fun setChecked(view: BottomNavigationButton, isChecked: Boolean) {
            view.isChecked = isChecked
        }

        @JvmStatic
        @BindingAdapter("badgeVisible")
        fun setBadgeVisibility(view: BottomNavigationButton, badgeVisible: Boolean) {
            view.badgeVisible = badgeVisible
        }
    }
}
package com.pocket.ui.view.bottom

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.databinding.ViewSimpleDrawerRowBinding
import com.pocket.ui.view.checkable.CheckableConstraintLayout

class SimpleDrawerRow
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.simpleDrawerRowStyle,
) : CheckableConstraintLayout(context, attrs, defStyleAttr) {
    
    init {
        engageable.uiEntityType = UiEntityable.Type.MENU
    }

    private val views = ViewSimpleDrawerRowBinding.inflate(LayoutInflater.from(getContext()), this)
    private val binder = Binder()

    fun bind() = binder
    override val uiEntityLabel: String?
        get() = views.text.uiEntityLabel

    inner class Binder {
        fun icon(@DrawableRes resId: Int): Binder {
            views.icon.visibility = if (resId != 0) VISIBLE else GONE
            views.icon.setImageResource(resId)
            return this
        }

        fun text(@StringRes resId: Int): Binder {
            views.text.setTextAndUpdateEnUsLabel(resId)
            return this
        }
        
        /** Prefer passing `R.string` resource id for better tracking. */
        fun text(text: String?): Binder {
            views.text.text = text
            return this
        }

        fun span(span: Any): Binder {
            val spannable = SpannableString(views.text.text)
            spannable.setSpan(span, 0, spannable.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            views.text.text = spannable
            return this
        }

        fun onClick(onClick: OnClickListener): Binder {
            setOnClickListener(onClick)
            return this
        }

        fun setStyle(@StyleRes resId: Int): Binder {
            views.text.setTextAppearance(views.text.context, resId)
            return this
        }

        fun setBold(): Binder {
            views.text.setBold(true)
            return this
        }

        fun showCloseIcon(show: Boolean): Binder {
            views.closeButton.visibility = if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
            return this
        }

        fun uiEntityIdentifier(id: String): Binder {
            engageable.uiEntityIdentifier = id
            return this
        }
    }
}
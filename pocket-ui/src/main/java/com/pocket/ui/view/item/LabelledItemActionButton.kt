package com.pocket.ui.view.item

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.view.button.IconButton
import com.pocket.ui.view.themed.ThemedConstraintLayout

/**
 * An IconButton, with optional horizontal text label.
 */
class LabelledItemActionButton : ThemedConstraintLayout {

    val binder = Binder()

    private val icon: IconButton
    private val label: TextView

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_labelled_item_action, this, true)

        setBackgroundResource(R.drawable.pkt_ripple_borderless)
        icon = findViewById(R.id.icon)
        label = findViewById(R.id.label)

        icon.isLongClickable = false // By default, IconButtons have tooltips on long press, we don't want this icon to take touches
        binder.clear()
        engageable.uiEntityType = UiEntityable.Type.BUTTON
    }

    inner class Binder {
        fun clear() {
            icon(0)
            description(0)
            onClick(null)
        }

        fun icon(@DrawableRes drawable: Int) : Binder {
            if (drawable != 0) {
                icon.setImageResource(drawable)
            } else {
                icon.setImageDrawable(null)
            }
            return this
        }

        fun description(@StringRes text: Int) : Binder {
            if (text != 0) {
                val textLabel = resources.getText(text)
                label.text = textLabel
                TooltipCompat.setTooltipText(this@LabelledItemActionButton, textLabel)
                this@LabelledItemActionButton.contentDescription = textLabel
            } else {
                TooltipCompat.setTooltipText(this@LabelledItemActionButton, null)
                this@LabelledItemActionButton.contentDescription = null
            }
            return this
        }

        fun label(visible: Boolean) : Binder {
            label.visibility = if (visible) View.VISIBLE else View.GONE
            return this
        }

        fun onClick(listener: OnClickListener?) : Binder {
            setOnClickListener(listener)
            return this
        }
    }
}
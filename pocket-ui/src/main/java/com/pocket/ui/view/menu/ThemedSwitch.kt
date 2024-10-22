package com.pocket.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pocket.analytics.api.Engageable
import com.pocket.analytics.api.EngageableHelper
import com.pocket.ui.R
import com.pocket.ui.util.NestedColorStateList
import com.pocket.ui.view.themed.AppThemeUtil

/**
 * A [SwitchCompat] pre-styled for Pocket.
 */
class ThemedSwitch
private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = androidx.appcompat.R.attr.switchStyle,
    private val engageable: EngageableHelper,
) : SwitchCompat(context, attrs, defStyle), Engageable by engageable {

    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = androidx.appcompat.R.attr.switchStyle,
    ) : this(
        context,
        attrs,
        defStyle,
        EngageableHelper()
    )

    init {
        DrawableCompat.setTintList(thumbDrawable, NestedColorStateList.get(
            context, R.color.pkt_switch_thumb))
        DrawableCompat.setTintList(trackDrawable, NestedColorStateList.get(
            context, R.color.pkt_switch_track))
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }

    override val uiEntityValue: String
        get() = isChecked.toString()

    override val engagementValue: String
        get() {
            return isChecked
                .not() // Assume clicking a switch has the intention of flipping it to the opposite.
                .toString()
        }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(engageable.getWrappedClickListener(l))
    }
}
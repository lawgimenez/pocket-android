package com.pocket.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.pocket.analytics.api.Engageable
import com.pocket.analytics.api.EngageableHelper
import com.pocket.analytics.api.UiEntityable

open class ThemedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val engageable: EngageableHelper = EngageableHelper(),
    entityType: UiEntityable.Type? = null,
) :
    LinearLayout(context, attrs, defStyleAttr),
    Engageable by engageable
{
    init {
        engageable.obtainStyledAttributes(context, attrs)
        entityType?.let { setUiEntityType(it) }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(engageable.getWrappedClickListener(listener))
    }

    fun setUiEntityType(type: UiEntityable.Type?) {
        engageable.uiEntityType = type
    }
}

package com.pocket.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.pocket.analytics.api.Engageable
import com.pocket.analytics.api.EngageableHelper
import com.pocket.analytics.api.UiEntityable

open class ThemedConstraintLayout2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val engageable: EngageableHelper = EngageableHelper(), // TODO: This should be private
    entityType: UiEntityable.Type? = UiEntityable.Type.BUTTON,
) :
    ConstraintLayout(context, attrs, defStyleAttr),
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
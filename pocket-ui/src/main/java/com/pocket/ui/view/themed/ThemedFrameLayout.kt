package com.pocket.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.pocket.analytics.api.Engageable
import com.pocket.analytics.api.EngageableHelper
import com.pocket.analytics.api.UiEntityable

open class ThemedFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val engageableHelper: EngageableHelper = EngageableHelper(),
    entityType: UiEntityable.Type? = null,
) : FrameLayout(context, attrs, defStyleAttr),
    Engageable by engageableHelper
{

    init {
        engageableHelper.obtainStyledAttributes(context, attrs)
        entityType?.let { engageableHelper.uiEntityType = it }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(engageableHelper.getWrappedClickListener(listener))
    }
}
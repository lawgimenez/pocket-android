package com.pocket.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

open class ThemedNestedScrollView(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int
) : NestedScrollView(
    context,
    attrs,
    defStyle
) {

    constructor(
        context: Context,
        attrs: AttributeSet?,
    ): this(context, attrs, 0)

    constructor(context: Context): this(context, null)

    // NestedScrollView only allows one scroll listener for some reason
    // This allows for multiple
    private val scrollListeners = mutableListOf<OnScrollChangeListener>()

    private val realScrollListener = OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
        scrollListeners.forEach {
            it.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY)
        }
    }

    override fun setOnScrollChangeListener(listener: OnScrollChangeListener?) {
        listener?.let { scrollListeners.add(it) }
        super.setOnScrollChangeListener(realScrollListener)
    }

    fun removeOnScrollChangedListener(listener: OnScrollChangeListener) {
        scrollListeners.remove(listener)
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }
}
package com.pocket.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton

class ThemedRadioButton(
    context: Context?,
    attrs: AttributeSet? = null,
) : AppCompatRadioButton(
    context,
    attrs
) {

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }
}
package com.pocket.ui.view.checkable

import android.content.Context
import android.util.AttributeSet
import com.pocket.ui.view.themed.ThemedTextView
import com.pocket.ui.util.CheckableHelper
import android.view.SoundEffectConstants
import androidx.databinding.BindingAdapter

/**
 * A [ThemedTextView] that implements [CheckableHelper.Checkable].
 */
open class CheckableTextView : ThemedTextView, CheckableHelper.Checkable {
    private val checkableHelper: CheckableHelper? = CheckableHelper(this)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        checkableHelper?.initAttributes(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        checkableHelper?.initAttributes(context, attrs)
    }

    constructor(context: Context?) : super(context) {
        checkableHelper?.initAttributes(context, null)
    }

    override fun setChecked(checked: Boolean) {
        checkableHelper?.isChecked = checked
    }

    override fun setCheckable(value: Boolean) {
        checkableHelper?.isCheckable = value
    }

    override fun isChecked(): Boolean = checkableHelper?.isChecked ?: false

    override fun isCheckable(): Boolean = checkableHelper?.isCheckable ?: false

    override fun performClick(): Boolean {
        toggle()
        val handled = super.performClick()
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK)
        }
        return handled
    }

    override fun toggle() {
        checkableHelper?.toggle()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        if (isChecked) {
            mergeDrawableStates(drawableState, CheckableHelper.CHECKED_STATE_SET)
        }
        if (isCheckable) {
            mergeDrawableStates(drawableState, CheckableHelper.CHECKABLE_STATE_SET)
        }
        return drawableState
    }

    override fun setOnCheckedChangeListener(listener: CheckableHelper.OnCheckedChangeListener?) {
        checkableHelper?.setOnCheckedChangeListener(listener)
    }

    companion object {
        @JvmStatic
        @BindingAdapter("checked")
        fun isChecked(view: CheckableTextView, isChecked: Boolean) {
            view.isChecked = isChecked
        }
    }
}
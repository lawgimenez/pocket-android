package com.pocket.ui.view.themed

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import com.pocket.analytics.api.Engageable
import com.pocket.analytics.api.EngageableHelper
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.text.Fonts
import com.pocket.ui.util.NestedColorStateList

open class ThemedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = androidx.appcompat.R.attr.editTextStyle,
    protected val engageable: EngageableHelper = EngageableHelper(),
) :
    AppCompatEditText(context, attrs, defStyle),
    Engageable by engageable
{

    init {
        engageable.obtainStyledAttributes(context, attrs)
        engageable.uiEntityType = UiEntityable.Type.BUTTON

        paintFlags = paintFlags or Paint.SUBPIXEL_TEXT_FLAG
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ThemedEditText)
            applyTextAppearanceFromAttributes(a)
            a.recycle()
        }
    }

    @Deprecated("Use setTextAppearance(int) instead")
    override fun setTextAppearance(context: Context, resid: Int) {
        super.setTextAppearance(context, resid)
        val a = getContext().obtainStyledAttributes(resid, R.styleable.ThemedEditText)
        applyTextAppearanceFromAttributes(a)
        a.recycle()
    }

    private fun applyTextAppearanceFromAttributes(a: TypedArray) {
        if (a.hasValue(R.styleable.ThemedEditText_typeface)) {
            typeface = Fonts.get(context, a.getInt(R.styleable.ThemedEditText_typeface, 0))
        }
        var colors = a.getResourceId(R.styleable.ThemedEditText_compatEditTextColor, 0)
        if (colors != 0) {
            setTextColor(NestedColorStateList.get(context, colors))
        }
        colors = a.getResourceId(R.styleable.ThemedEditText_compatEditTextHintColor, 0)
        if (colors != 0) {
            setHintTextColor(NestedColorStateList.get(context, colors))
        }
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }
}
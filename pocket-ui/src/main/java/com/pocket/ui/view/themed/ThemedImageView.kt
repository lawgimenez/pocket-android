package com.pocket.ui.view.themed

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.pocket.analytics.api.Engageable
import com.pocket.analytics.api.EngageableHelper
import android.content.res.ColorStateList
import android.graphics.Color
import com.pocket.ui.R
import com.pocket.ui.util.NestedColorStateList
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.databinding.BindingAdapter
import com.pocket.analytics.api.EngagementListener
import com.pocket.analytics.api.UiEntityable

open class ThemedImageView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val engageable: EngageableHelper = EngageableHelper(), // TODO: This should be private
): AppCompatImageView(context, attrs, defStyleAttr), Engageable by engageable {
    private var mColors: ColorStateList? = null
    private var mHeightRatio = 0f
    private var mDrawableColorOverride: ColorOverride? = null

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ThemedImageView)

            // Something in NestedColorStateList trips up layout preview so skip it when in preview.
            if (!isInEditMode) {
                val colors = a.getResourceId(R.styleable.ThemedImageView_drawableColor, 0)
                if (colors != 0) {
                    mColors = NestedColorStateList.get(context, colors)
                }
            }
            mHeightRatio = a.getFloat(R.styleable.ThemedImageView_heightRatio, 0f)
            a.recycle()
            engageable.obtainStyledAttributes(context, attrs)
            if (mColors != null && drawable is BitmapDrawable) {
                // Recreate the drawable with the new colors
                setImageDrawable(drawable)
            }
        }
        engageable.uiEntityType = UiEntityable.Type.BUTTON
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mHeightRatio > 0) {
            setMeasuredDimension(measuredWidth, (measuredWidth * mHeightRatio).toInt())
        }
    }

    open fun setImageResourceTinted(resId: Int) {
        if (resId != 0) {
            setImageDrawable(ContextCompat.getDrawable(context, resId))
        } else {
            setImageDrawable(null)
        }
    }

    /**
     * This will not change the current drawable. To have it effect an already set drawable,
     * you will need to set it again.
     */
    fun setDrawableColor(colorRes: Int) {
        if (colorRes != 0) {
            setDrawableColor(ContextCompat.getColorStateList(context, colorRes))
        } else {
            setDrawableColor(null)
        }
    }

    fun setDrawableColor(colors: ColorStateList?) {
        mColors = colors
        refreshDrawableState()
    }

    /**
     * This lets you add a custom handler that overrides which color is used.
     * This is a ugly hack until we find a better way to extend a color state list just to change one color.
     */
    fun setDrawableColorOverride(override: ColorOverride?) {
        mDrawableColorOverride = override
        refreshDrawableState()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 2)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }

    override fun refreshDrawableState() {
        super.refreshDrawableState()
        applyDrawableColor()
        invalidate()
    }

    private fun applyDrawableColor() {
        var color = 0
        val state = drawableState
        mColors?.let { color = it.getColorForState(state, Color.TRANSPARENT) }
        mDrawableColorOverride?.let { color = it.getColor(state, color) }
        if (color != 0) {
            setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            colorFilter = null
        }
    }

    override var uiEntityIdentifier: String?
        get() = engageable.uiEntityIdentifier
        set(uiEntityIdentifier) {
            engageable.uiEntityIdentifier = uiEntityIdentifier
        }
    override var uiEntityLabel: String?
        get() = engageable.uiEntityLabel
        set(label) {
            engageable.uiEntityLabel = label
        }

    override fun setEngagementListener(listener: EngagementListener?) {
        engageable.setEngagementListener(listener)
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(engageable.getWrappedClickListener(listener))
    }

    interface ColorOverride {
        fun getColor(state: IntArray?, color: Int): Int
    }

    companion object {
        @JvmStatic
        @BindingAdapter("drawable")
        fun setDrawable(view: ThemedImageView, drawable: Drawable) {
            view.setImageDrawable(drawable)
        }
    }
}
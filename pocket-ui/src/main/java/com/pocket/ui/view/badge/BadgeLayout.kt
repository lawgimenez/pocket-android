package com.pocket.ui.view.badge

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.pocket.ui.R
import com.pocket.ui.view.themed.ThemedTextView
import com.pocket.ui.view.themed.ThemedViewGroup
import kotlin.math.max

/**
 * Takes any number of badges and displays as many as can fit.  Adds a text view with the number
 * of badges that didn't fit.
 */
class BadgeLayout(
    context: Context,
    attrs: AttributeSet? = null,
) : ThemedViewGroup(
    context,
    attrs,
) {

    private val staged: MutableList<View> = mutableListOf()
    private var overflow: ThemedTextView? = null
    private val tags: MutableList<BadgeView> = mutableListOf()
    private val spacing = resources.getDimensionPixelSize(R.dimen.pkt_space_sm);

    init {
        overflow = ThemedTextView(getContext())
        overflow?.layoutParams = generateDefaultLayoutParams()
        overflow?.setTextAppearance(getContext(), R.style.Pkt_Text_Small_LightTitle)
    }

    fun setBadges(badges: List<BadgeView>) {
        tags.clear()
        tags.addAll(badges)
        invalidate()
        requestLayout()
    }

    @Suppress("LongMethod", "NestedBlockDepth")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        removeAllViews()

        // Measure and create views that will fit the space
        val width = MeasureSpec.getSize(widthMeasureSpec) - this.paddingRight - this.paddingLeft
        if (width > 0) {
            var x = 0
            val childUnboundWidthSpec = getChildMeasureSpec(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                0,
                WRAP_CONTENT
            )
            val childHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, WRAP_CONTENT)
            var i = 0
            while (i < tags.size) {
                if (i > 0) {
                    x += spacing
                }
                val badge: BadgeView = tags[i]
                if (tags.size == 1) {
                    badge.measure(MeasureSpec.makeMeasureSpec(width - x, MeasureSpec.AT_MOST),
                        childHeightSpec)
                } else {
                    badge.measure(childUnboundWidthSpec, childHeightSpec)
                }
                val measured = badge.measuredWidth
                if (measured > 0 && x + measured <= width) {
                    // Fits
                    stage(badge, x)
                    x += badge.measuredWidth
                } else {
                    // Does not fit, back track until the overflow fits
                    do {
                        val remaining = tags.size - i
                        if (remaining > 0) {
                            val label = "+$remaining"
                            overflow?.text = label
                            overflow?.measure(MeasureSpec.makeMeasureSpec(childUnboundWidthSpec,
                                MeasureSpec.AT_MOST), childHeightSpec)
                            if (x + overflow!!.measuredWidth <= width) {
                                stage(overflow!!, x)
                                break
                            }
                        }
                        if (i > 0) {
                            x = (staged.removeAt(i - 1).layoutParams as LayoutParams).x
                        }
                        i--
                    } while (i >= 0)
                    break
                }
                i++
            }
        }
        var maxHeight = 0
        var maxRight = 0
        for ((i, child) in staged.withIndex()) {
            maxHeight = max(maxHeight, child.measuredHeight)
            maxRight = max(maxRight,
                (child.layoutParams as LayoutParams).x + child.measuredWidth)
            addViewInLayout(child, i, child.layoutParams)
        }
        staged.clear()
        setMeasuredDimension(
            resolveSize(maxRight, widthMeasureSpec),
            resolveSize(maxHeight, heightMeasureSpec)
        )
    }

    private fun stage(view: View, x: Int) {
        view.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { this.x = x }
        staged.add(view)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val height = b - t
        for (i in 0 until count) {
            val child = getChildAt(i)
            val layoutParams = child.layoutParams as LayoutParams
            val left = layoutParams.x
            val right = left + child.measuredWidth

            // center vertically
            val top = ((height - child.measuredHeight) / 2f).toInt()
            val bottom = top + child.measuredHeight
            child.layout(left, top, right, bottom)
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun generateLayoutParams(attributeSet: AttributeSet?): LayoutParams {
        return LayoutParams(context, attributeSet)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams {
        return LayoutParams(p)
    }

    class LayoutParams : ViewGroup.LayoutParams {
        var x = 0

        constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(layoutParams: ViewGroup.LayoutParams?) : super(layoutParams)
    }
}
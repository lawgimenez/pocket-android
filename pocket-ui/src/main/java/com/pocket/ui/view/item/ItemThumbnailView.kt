package com.pocket.ui.view.item

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.pocket.ui.R
import com.pocket.ui.util.*
import com.pocket.ui.util.DimenUtil.dpToPx
import com.pocket.ui.util.EmptiableView.OnEmptyChangedListener
import com.pocket.ui.util.LazyBitmapDrawable.SupportsPlaceholder
import com.pocket.ui.view.themed.ThemedImageView

/**
 * TODO Handle the various dynamic sizing rules
 */
class ItemThumbnailView : ThemedImageView, EmptiableView, SupportsPlaceholder {
    private val emptyHelper = EmptiableViewHelper(this, EmptiableView.GONE_WHEN_EMPTY)
    private val sizeHelper = IntrinsicSizeHelper(
        90F.toPxInt(context),
        60F.toPxInt(context)
    )
    private val corners = dpToPx(context, CORNER_RADIUS)
    private val rect = Rect()
    private var placeholder: ColorStateListDrawable? = null
    private var videoIndicator: Drawable? = null
    private var videoIndicatorStyle: VideoIndicator? = null

    enum class VideoIndicator {
        LIST, TILE, DISCOVER
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context,
        attrs,
        defStyle) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    private fun init() {
        scaleType = ScaleType.CENTER
        placeholder = ColorStateListDrawable(context, R.color.pkt_themed_grey_6)
        setImageDrawable(null)
    }

    fun setVideoIndicatorStyle(style: VideoIndicator?) {
        if (videoIndicatorStyle != style) {
            videoIndicatorStyle = style
            videoIndicator = null
            if (style != null) {
                videoIndicator = when (style) {
                    VideoIndicator.LIST -> ItemVideoIndicatorDrawable.forItemRow(context)
                    VideoIndicator.TILE -> ItemVideoIndicatorDrawable.forItemTile(context)
                    VideoIndicator.DISCOVER -> ItemVideoIndicatorDrawable.forDiscoverTile(context)
                }
            }
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(
            sizeHelper.applyWidth(widthMeasureSpec),
            sizeHelper.applyHeight(heightMeasureSpec)
        )

    override fun onDraw(canvas: Canvas) {
        if (drawable != null) {
            super.onDraw(canvas)
        } else {
            rect.set(0, 0, width, height)
            drawPlaceholder(canvas, rect, drawableState)
        }

        videoIndicator?.let {
            it.setBounds(0, 0, width, height)
            it.state = drawableState
            it.draw(canvas)
        }

        roundCanvasCorners(canvas, corners)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        // This override covers most but not all possible setters,
        // if we end up using a different setter we'll need to capture that case as well.
        if (!isInEditMode) {
            emptyHelper.setEmpty(getDrawable() == null)
        }
    }

    override fun setOnEmptyChangedListener(listener: OnEmptyChangedListener?) {
        emptyHelper.setOnEmptyChangedListener(listener)
    }

    override fun drawPlaceholder(canvas: Canvas, bounds: Rect, state: IntArray) {
        placeholder?.bounds = bounds
        placeholder?.state = state
        placeholder?.draw(canvas)
    }

    companion object {
        const val CORNER_RADIUS = 16f
    }
}
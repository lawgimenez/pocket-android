package com.pocket.ui.view.item

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.pocket.ui.view.checkable.CheckableConstraintLayout
import com.pocket.ui.view.visualmargin.VisualMargin
import android.view.LayoutInflater
import com.pocket.ui.R
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.databinding.ViewItemRowBinding
import com.pocket.ui.util.LazyBitmap
import com.pocket.ui.util.LazyBitmapDrawable

/**
 * Displays an Item as a row, with an optional thumbnail and actions bar.
 */
class ItemRowView : CheckableConstraintLayout, VisualMargin {
    private val binder: Binder = Binder()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val binding: ViewItemRowBinding = ViewItemRowBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    init {
        bind().clear()
        setBackgroundResource(R.drawable.cl_pkt_touchable_area)
        engageable.uiEntityType = UiEntityable.Type.CARD
        engageable.uiEntityComponentDetail = "item_row"
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            enabled(clicksEnabled = true, metaEnabled = true)
            dividerVisible(true)
            meta().clear()
            thumbnail(null as LazyBitmap?, false)
            actions().clear()
            actionsVisible(false)
            return this
        }

        /**
         * @param clicksEnabled Allow clicks, long clicks, checked toggling etc.
         * @param metaEnabled Visually enabled or not
         */
        fun enabled(clicksEnabled: Boolean, metaEnabled: Boolean): Binder {
            isEnabled = clicksEnabled
            binding.meta.isEnabled = metaEnabled
            binding.thumbnail.isEnabled = metaEnabled
            return this
        }

        fun dividerVisible(visible: Boolean): Binder {
            binding.divider.visibility = if (visible) VISIBLE else GONE
            return this
        }

        fun meta(): ItemMetaView.Binder {
            return binding.meta.bind()
        }

        fun thumbnail(
            value: LazyBitmap?,
            isVideo: Boolean
        ): Binder { // TODO make isVideo a separate method
            binding.thumbnail.setImageDrawable(value?.let { LazyBitmapDrawable(it) })
            binding.thumbnail.setVideoIndicatorStyle(if (isVideo) ItemThumbnailView.VideoIndicator.LIST else null)
            return this
        }

        fun thumbnail(drawable: Drawable?, isVideo: Boolean): Binder {
            binding.thumbnail.setImageDrawable(drawable)
            binding.thumbnail.setVideoIndicatorStyle(if (isVideo) ItemThumbnailView.VideoIndicator.LIST else null)
            return this
        }

        fun actionsVisible(isVisible: Boolean): Binder {
            binding.simpleActions.visibility = if (isVisible) VISIBLE else GONE
            return this
        }

        fun actions(): SimpleItemActionsView.Binder {
            return binding.simpleActions.binder
        }
    }

    override fun prepareVisualAscent(): Boolean {
        return VisualMargin.removeTopMargin(binding.centeringVisualMargin)
    }

    override fun prepareVisualDescent(): Boolean {
        return VisualMargin.removeTopMargin(binding.bottomMargin)
    }
}
package com.pocket.ui.view.item

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.R
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout

/**
 * An Item tile with a full width image, meta view, and Save/Share actions.
 *
 * https://www.figma.com/file/K70GjbldhXypBlshbeciA0/Android-MVP
 */
class DiscoverTileView : VisualMarginConstraintLayout {

    val binder = Binder()

    private val itemTile: ItemTileView
    private val actions: SimpleItemActionsView
    private val divider: View

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_discover_tile, this, true)

        itemTile = findViewById(R.id.rec_item)
        actions = findViewById(R.id.simple_actions)
        divider = findViewById(R.id.rec_bottom_divider)

        itemTile.isCheckable = false
        itemTile.isClickable = false
        itemTile.findViewById<View>(R.id.bottom_margin).visibility = View.GONE
        setBackgroundResource(R.drawable.cl_pkt_touchable_area)
    
        engageable.uiEntityType = UiEntityable.Type.CARD
        engageable.uiEntityComponentDetail = "discover_tile"
    }

    inner class Binder {
        fun item(): ItemTileView.Binder {
            return itemTile.Binder()
        }

        fun actions(): SimpleItemActionsView.Binder {
            return actions.binder
        }

        fun clear() : Binder  {
            itemTile.bind().clear()
            actions.binder.clear()
            divider(true)
            useLeftTileMargin(false)
            useRightTileMargin(false)
            onClick(null)
            return this
        }
        fun divider(isVisible: Boolean) : Binder {
            divider.visibility = if (isVisible) View.VISIBLE else View.GONE
            return this
        }
        fun useLeftTileMargin(leftTile: Boolean) : Binder {
            val params: MarginLayoutParams = divider.layoutParams as MarginLayoutParams
            params.rightMargin = if (leftTile) 0 else resources.getDimensionPixelSize(R.dimen.pkt_side_grid)
            divider.layoutParams = params
            itemTile.bind().marginEnd(if (leftTile) R.dimen.pkt_space_sm else R.dimen.pkt_side_grid)
            actions.binder.marginEnd(if (leftTile) R.dimen.pkt_space_sm else R.dimen.pkt_side_grid)
            return this
        }
        fun useRightTileMargin(rightTile: Boolean) : Binder {
            val params: MarginLayoutParams = divider.layoutParams as MarginLayoutParams
            params.leftMargin = if (rightTile) 0 else resources.getDimensionPixelSize(R.dimen.pkt_side_grid)
            divider.layoutParams = params
            itemTile.bind().marginStart(if (rightTile) R.dimen.pkt_space_sm else R.dimen.pkt_side_grid)
            actions.binder.marginStart(if (rightTile) R.dimen.pkt_space_sm else R.dimen.pkt_side_grid)
            return this
        }
        fun onClick(listener: OnClickListener?) : Binder {
            setOnClickListener(listener)
            return this
        }
    }
}
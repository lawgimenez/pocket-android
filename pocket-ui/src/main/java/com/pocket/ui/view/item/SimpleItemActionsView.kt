package com.pocket.ui.view.item

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DimenRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.pocket.ui.R
import com.pocket.ui.view.button.IconButton

/**
 * A View which contains a Pocket Save button and Share button.
 */
class SimpleItemActionsView : ConstraintLayout {

    val binder = Binder()

    private val save: SaveButton

    /** an invisible, unclickable View which mimics the reverse of the clickable SaveButton, so views to the right of it don't shift spacing, regardless of text label composition */
    private val saveSpacer: SaveButton

    private val share: LabelledItemActionButton

    private val overflow: IconButton

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_simple_item_actions, this, true)

        save = findViewById(R.id.save)
        saveSpacer = findViewById(R.id.save_space)
        share = findViewById(R.id.share)
        share.binder.description(R.string.ic_share).icon(R.drawable.ic_pkt_android_share_solid)
        overflow = findViewById(R.id.overflow)
        val pad = resources.getDimensionPixelSize(R.dimen.pkt_side_grid)
        overflow.setPadding(pad, 0, pad, 0)
    }

    inner class Binder {
        fun clear() : Binder {
            visible(true)
            labels(true)
            setSaved(false)
            setSaveListener(null)
            setShareListener(null)
            setOverflowListener(null)
            return this
        }

        fun visible(visible: Boolean) : Binder {
            visibility = if (visible) View.VISIBLE else View.GONE
            return this
        }

        fun labels(visible: Boolean) : Binder {
            save.bind().label(visible)
            saveSpacer.bind().label(visible)
            share.binder.label(visible)
            return this
        }

        fun setSaved(isSaved: Boolean) : Binder {
            save.bind().setSaved(isSaved)
            saveSpacer.bind().setSaved(!isSaved)
            return this
        }

        fun setSaveListener(listener: SaveButton.Binder.OnSaveButtonClickListener?) : Binder {
            save.bind().setOnSaveButtonClickListener { view, saved ->
                saveSpacer.bind().setSaved(!saved)
                listener?.onSaveButtonClicked(view, saved)
                saved
            }
            return this
        }

        fun setShareListener(listener: OnClickListener?) : Binder {
            share.setOnClickListener(listener)
            share.visibility  = if (listener != null) View.VISIBLE else View.GONE
            return this
        }

        fun setOverflowListener(listener: OnClickListener?) : Binder {
            overflow.setOnClickListener(listener)
            overflow.visibility = if (listener == null) View.GONE else View.VISIBLE
            return this
        }

        fun marginStart(@DimenRes margin: Int): Binder {
            val marginPx = resources.getDimensionPixelSize(margin)
            val paramsSave = save.layoutParams as MarginLayoutParams
            val paramsSpace = saveSpacer.layoutParams as MarginLayoutParams
            paramsSave.leftMargin = marginPx
            paramsSpace.leftMargin = marginPx
            save.layoutParams = paramsSave
            saveSpacer.layoutParams = paramsSpace
            return this
        }

        fun marginEnd(@DimenRes margin: Int): Binder {
            overflow.setPadding(resources.getDimensionPixelSize(R.dimen.pkt_side_grid), 0, resources.getDimensionPixelSize(margin), 0)
            return this
        }
    }
}
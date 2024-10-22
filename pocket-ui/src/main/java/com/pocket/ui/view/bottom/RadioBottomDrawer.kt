package com.pocket.ui.view.bottom

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.pocket.ui.R
import com.pocket.ui.util.DimenUtil
import com.pocket.ui.view.button.BoxButton
import com.pocket.ui.view.menu.RadioOptionRowView

open class RadioBottomDrawer : SimpleBottomDrawer {

    interface OnOptionSelectedListener {
        fun onOptionSelected(index: Int)
    }

    private var optionSelectedListener: OnOptionSelectedListener? = null

    private val optionViews = mutableListOf<RadioOptionRowView>()
    private var submitButton: BoxButton = BoxButton(context)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onLazyInflated() {
        super.onLazyInflated()
        submitButton.minimumHeight = DimenUtil.dpToPxInt(context, 52f)
        submitButton.isEnabled = false
        val sidePad = resources.getDimensionPixelSize(R.dimen.pkt_side_grid)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.leftMargin = sidePad
        params.rightMargin = sidePad
        submitButton.layoutParams = params
        submitButton.uiEntityIdentifier = "submit"
    }

    private fun uncheckAll() {
        for (view in optionViews)
            view.isChecked = false
    }

    fun setOptions(@StringRes options: List<Int>) {

        optionViews.clear()
        bind().clear()

        for (option in options) {
            val view = RadioOptionRowView(context)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.setLabel(option)
            view.setOnClickListener {
                uncheckAll()
                view.isChecked = true
                submitButton.isEnabled = true
                optionSelectedListener?.onOptionSelected(list.indexOfChild(view))
            }
            optionViews.add(view)
            bind().addView(view)
        }

        bind().addView(submitButton)
    }

    fun submitButton(@StringRes label: Int, listener: OnClickListener?) {
        if (label == 0) submitButton.text = null else submitButton.setTextAndUpdateEnUsLabel(label)
        submitButton.setOnClickListener(listener)
    }

    fun onOptionSelected(listener: OnOptionSelectedListener?) {
        optionSelectedListener = listener
    }

    fun selectedIndex() : Int {
        for(view in optionViews) {
            if (view.isChecked) return optionViews.indexOf(view)
        }
        return -1;
    }
}
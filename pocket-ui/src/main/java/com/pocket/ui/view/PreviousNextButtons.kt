package com.pocket.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.pocket.analytics.api.UiEntityable
import com.pocket.ui.databinding.ViewPreviousNextButtonsBinding
import com.pocket.ui.view.themed.ThemedConstraintLayout

class PreviousNextButtons
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ThemedConstraintLayout(context, attrs, defStyleAttr) {

    private val views =
        ViewPreviousNextButtonsBinding.inflate(LayoutInflater.from(context), this).apply {
            previousItem.setUiEntityType(UiEntityable.Type.BUTTON)
            nextItem.setUiEntityType(UiEntityable.Type.BUTTON)
        }

    fun showPreviousButton() {
        views.previousItem.visibility = VISIBLE
    }
    fun showNextButton() {
        views.nextItem.visibility = VISIBLE
    }

    fun hidePreviousButton() {
        views.previousItem.visibility = INVISIBLE
    }
    fun hideNextButton() {
        views.nextItem.visibility = INVISIBLE
    }

    fun onPreviousClick(onClick: OnClickListener) = views.previousItem.setOnClickListener(onClick)
    fun onNextClick(onClick: OnClickListener) = views.nextItem.setOnClickListener(onClick)
}

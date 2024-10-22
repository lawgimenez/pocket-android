package com.pocket.ui.view.edittext

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.pocket.ui.R
import com.pocket.ui.databinding.ViewTextFinderBinding
import com.pocket.ui.view.themed.ThemedConstraintLayout

class TextFinderView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : ThemedConstraintLayout(
    context,
    attrs,
    defStyleAttr
), TextFinderLayout {

    private val binding = ViewTextFinderBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).also {
        setBackgroundResource(R.drawable.cl_pkt_bg)
        isClickable = true
    }

    override fun root(): View {
        return binding.root
    }

    override fun cancel(): View {
        return binding.cancel
    }

    override fun input(): EditText {
        return binding.input
    }

    override fun count(): TextView {
        return binding.count
    }

    override fun back(): View {
        return binding.back
    }

    override fun forward(): View {
        return binding.forward
    }

    companion object {

        @JvmStatic
        @BindingAdapter("count")
        fun setCountText(view: TextFinderView, text: String) {
            view.binding.count.text = text
        }
    }
}
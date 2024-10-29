package com.pocket.util.android.view

import android.graphics.Paint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.pocket.app.App
import com.pocket.sdk.api.value.MarkdownString
import com.pocket.sdk.util.MarkdownHandler
import com.pocket.ui.view.themed.ThemedTextView

@BindingAdapter("visibility")
fun setVisibility(view: View, visible: Boolean) {
    view.visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

@BindingAdapter("textId")
fun setText(view: TextView, textId: Int) {
    view.text = view.context.getString(textId)
}

@BindingAdapter("textUnderline")
fun setTextUnderline(view: TextView, enabled: Boolean) {
    view.paintFlags = if (enabled) {
        view.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    } else {
        view.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
    }
}

@BindingAdapter("textMarkdown")
fun setTextMarkdown(view: ThemedTextView, markdown: String) {
    with(MarkdownHandler(view.context) { App.viewUrl(view.context, it) }) {
        view.setMarkdownString(MarkdownString(markdown))
    }

}

@BindingAdapter("drawableId")
fun setDrawable(view: ImageView, drawableId: Int) {
    view.setImageDrawable(ContextCompat.getDrawable(view.context, drawableId))
}

@BindingAdapter("textColorId")
fun setTextColor(view: TextView, textColorId: Int) {
    view.setTextColor(ContextCompat.getColorStateList(view.context, textColorId))
}

@BindingAdapter("tintColorId")
fun setTintColor(view: ImageView, tintColorId: Int) {
    ImageViewCompat.setImageTintList(view, ContextCompat.getColorStateList(view.context, tintColorId))
}
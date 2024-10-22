package com.pocket.util.android

import android.view.View
import android.widget.TextView

@JvmOverloads
fun TextView.setTextOrHide(text: CharSequence?, blankVisibility: Int = View.GONE): Int {
    this.text = text
    return hideIfBlank(text, blankVisibility)
}

@JvmOverloads
fun TextView.hideIfBlank(text: CharSequence? = this.text, blankVisibility: Int = View.GONE): Int {
    visibility = if (text.isNullOrBlank()) blankVisibility else View.VISIBLE
    return visibility
}

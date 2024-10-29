package com.pocket.ui.view

import androidx.databinding.BindingAdapter

@BindingAdapter("title")
fun setAppBarTitle(appBar: AppBar, text: String) {
    appBar.bind().title(text)
}
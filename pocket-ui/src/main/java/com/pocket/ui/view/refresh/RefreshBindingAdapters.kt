package com.pocket.ui.view.refresh

import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

@BindingAdapter("isRefreshing")
fun setRefreshing(view: SwipeRefreshLayout, isRefreshing: Boolean) {
    view.isRefreshing = isRefreshing
}
package com.pocket.util.android

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

fun Fragment.showKeyboard(view: View) {
    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Fragment.hideKeyboard() {
    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        ?.hideSoftInputFromWindow(view?.windowToken, 0)
}
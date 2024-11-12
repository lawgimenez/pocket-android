package com.pocket.util

import android.content.Context
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.internal.managers.ViewComponentManager.FragmentContextWrapper

/**
 * Used to get a fragment activity from a view if the view is within an activity that uses
 * dagger/hilt.
 */
fun Context.asFragmentActivity(): FragmentActivity? =
    if (this is FragmentContextWrapper) {
        this.baseContext as? FragmentActivity
    } else {
        this as? FragmentActivity
    }
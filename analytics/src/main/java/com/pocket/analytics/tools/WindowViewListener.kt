package com.pocket.analytics.tools

import android.view.View

/**
 * Tracks Views added to windows, either through new Activity creation, dialogs, PopupMenus, or Toast notifications.
 */
object WindowViewListener {
    fun onViewAdded(block: (View) -> Unit) {
        try {
            val windowManagerGlobalClass = Class.forName("android.view.WindowManagerGlobal")
            val windowManagerGlobalInstance = windowManagerGlobalClass.getDeclaredMethod("getInstance").invoke(null)
            val mViews = windowManagerGlobalClass.getDeclaredField("mViews").apply { isAccessible = true }
            mViews[windowManagerGlobalInstance] = object : ArrayList<View>(mViews[windowManagerGlobalInstance] as ArrayList<View>) {
                override fun add(element: View): Boolean {
                    block(element)
                    return super.add(element)
                }
            }
        } catch (ignored: Throwable) {}
    }
}
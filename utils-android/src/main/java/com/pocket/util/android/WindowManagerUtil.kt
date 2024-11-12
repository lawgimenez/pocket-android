package com.pocket.util.android

import android.os.Build
import android.view.WindowManager

/**
 * Methods for getting screen width and height depending on API level
 */
object WindowManagerUtil {
    @JvmStatic
    fun getScreenWidth(windowManager: WindowManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            windowManager.defaultDisplay.width
        }
    }

    @JvmStatic
    fun getScreenHeight(windowManager: WindowManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            windowManager.defaultDisplay.height
        }
    }
}

fun WindowManager.getScreenWidth(): Int = WindowManagerUtil.getScreenWidth(this)
fun WindowManager.getScreenHeight(): Int = WindowManagerUtil.getScreenHeight(this)
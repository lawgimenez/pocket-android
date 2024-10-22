package com.pocket.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

interface DrawableLoader {
    fun getDrawable(@DrawableRes drawableRes: Int): Drawable?

    companion object {
        operator fun invoke(context: Context): DrawableLoader = object : DrawableLoader {
            override fun getDrawable(drawableRes: Int): Drawable? =
                ContextCompat.getDrawable(context, drawableRes)
        }
    }
}
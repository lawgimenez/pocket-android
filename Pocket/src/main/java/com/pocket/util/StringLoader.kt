package com.pocket.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * An interface for getting strings without relying on android context
 */
interface StringLoader {
    fun getString(@StringRes resourceId: Int): String
    fun getQuantityString(@PluralsRes id: Int, quantity: Int, formatArgs: Any): String

    companion object {
        operator fun invoke(context: Context): StringLoader = object : StringLoader {
            override fun getString(resourceId: Int): String = context.getString(resourceId)
            override fun getQuantityString(id: Int, quantity: Int, formatArgs: Any): String =
                context.resources.getQuantityString(id, quantity, formatArgs)
        }
    }
}
package com.pocket.ui.view.themed

import android.content.Context
import android.view.View
import io.reactivex.Observable

/**
 * Something that can provide what the current theme of a view, screen or app is.
 * See [AppThemeUtil] for more details.
 */
interface Themed {
    /**
     * @return The current theme state as attributes that are on.
     * Use whatever custom attributes you setup for your themes.
     */
    fun getThemeState(view: View): IntArray?

    fun getThemeColors(context: Context): ThemeColors
    fun getThemeColorsChanges(context: Context): Observable<ThemeColors>
}

enum class ThemeColors {
    LIGHT,
    DARK,
}

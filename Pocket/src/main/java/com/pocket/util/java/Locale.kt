package com.pocket.util.java

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable replacement for [LocaleUtils]
 */
@Singleton
class Locale @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun isGerman(): Boolean = LocaleUtils.isGerman(context)

    override fun toString(): String =
        context.resources.configuration.locale.let { locale -> "${locale.language}-${locale.country}" }
}
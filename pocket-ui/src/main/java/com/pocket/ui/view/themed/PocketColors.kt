package com.pocket.ui.view.themed

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PocketColors(
    val background: Color,
    val grey1: Color,
    val grey2: Color,
    val grey3: Color,
    val grey4: Color,
    val grey5: Color,
    val grey6: Color,
    val grey7: Color,
    val teal1: Color,
    val teal2: Color,
    val onTeal: Color,
    val onBackground: Color = grey1,
    val cardBackground: Color = background,
)

val LocalPocketColors = staticCompositionLocalOf { LightColors }

@Suppress("MagicNumber")
val LightColors = PocketColors(
    background = Color(0xFFFFFFFF),
    grey1 = Color(0xFF1A1A1A),
    grey2 = Color(0xFF333333),
    grey3 = Color(0xFF404040),
    grey4 = Color(0xFF737373),
    grey5 = Color(0xFF8C8C8C),
    grey6 = Color(0xFFD9D9D9),
    grey7 = Color(0xFFECECEC),
    teal1 = Color(0xFF004D48),
    teal2 = Color(0xFF008078),
    onTeal = Color(0xFFFFFFFF),
)

@Suppress("MagicNumber")
val DarkColors = PocketColors(
    background = Color(0XFF1A1A1A),
    grey1 = Color(0xFFF2F2F2),
    grey2 = Color(0xFFCCCCCC),
    grey3 = Color(0xFFCCCCCC),
    grey4 = Color(0xFF999999),
    grey5 = Color(0xFF737373),
    grey6 = Color(0xFF404040),
    grey7 = Color(0xFF333333),
    teal1 = Color(0xFF004D48),
    teal2 = Color(0xFF008078),
    onTeal = Color(0xFFFFFFFF),
    cardBackground = Color(0xFF222222),
)

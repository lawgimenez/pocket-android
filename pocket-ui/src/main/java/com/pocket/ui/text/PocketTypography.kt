package com.pocket.ui.text

import android.content.res.AssetManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class PocketTypography(
    val h1: TextStyle = TextStyle(
        fontSize = 48.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Medium,
    ),
    val h2: TextStyle = TextStyle(
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Medium,
    ),
    val h3: TextStyle = TextStyle(
        fontSize = 33.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
    ),
    val h4: TextStyle = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Medium,
    ),
    val h5: TextStyle = TextStyle(
        fontSize = 23.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    ),
    val h6: TextStyle = TextStyle(
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    val h7: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    val p1: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    val p2: TextStyle = TextStyle(
        fontSize = 19.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
    ),
    val p3: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    val p4: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
) {
    fun withDefaultFontFamily(fontFamily: FontFamily) = copy(
        h1 = h1.copy(fontFamily = fontFamily),
        h2 = h2.copy(fontFamily = fontFamily),
        h3 = h3.copy(fontFamily = fontFamily),
        h4 = h4.copy(fontFamily = fontFamily),
        h5 = h5.copy(fontFamily = fontFamily),
        h6 = h6.copy(fontFamily = fontFamily),
        h7 = h7.copy(fontFamily = fontFamily),
        p1 = p1.copy(fontFamily = fontFamily),
        p2 = p2.copy(fontFamily = fontFamily),
        p3 = p3.copy(fontFamily = fontFamily),
        p4 = p4.copy(fontFamily = fontFamily),
    )
}

val LocalPocketTypography = staticCompositionLocalOf { PocketTypography() }

fun Graphik(assets: AssetManager) = FontFamily(
    Font(Fonts.Font.GRAPHIK_LCG_REGULAR.filename, assets, FontWeight.Normal),
    Font(Fonts.Font.GRAPHIK_LCG_REGULAR_ITALIC.filename, assets, FontWeight.Normal, FontStyle.Italic),
    Font(Fonts.Font.GRAPHIK_LCG_MEDIUM.filename, assets, FontWeight.Medium),
    Font(Fonts.Font.GRAPHIK_LCG_MEDIUM_ITALIC.filename, assets, FontWeight.Medium, FontStyle.Italic),
    Font(Fonts.Font.GRAPHIK_LCG_BOLD.filename, assets, FontWeight.Bold),
)

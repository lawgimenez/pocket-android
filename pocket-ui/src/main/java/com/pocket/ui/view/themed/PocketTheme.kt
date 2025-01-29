package com.pocket.ui.view.themed

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pocket.ui.PocketDimensions
import com.pocket.ui.text.Graphik
import com.pocket.ui.text.LocalPocketTypography
import com.pocket.ui.text.PocketTypography

@Composable
fun PocketTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themeColors = themeColors(context)
    val pocketColors = when (themeColors) {
        ThemeColors.LIGHT -> LightColors
        ThemeColors.DARK -> DarkColors
    }

    val pocketTypography = PocketTypography().withDefaultFontFamily(Graphik(context.assets))

    MaterialTheme(
        colorScheme = when (themeColors) {
            ThemeColors.LIGHT -> lightColorScheme()
            ThemeColors.DARK -> darkColorScheme()
        }
            .copy(
                primary = pocketColors.teal2,
                background = pocketColors.background,
                surface = pocketColors.background,
                surfaceContainerHigh = pocketColors.background,
                surfaceVariant = pocketColors.grey7,
                onBackground = pocketColors.onBackground,
                onSurface = pocketColors.onBackground,
                onSurfaceVariant = Color.Black,
            ),
    ) {
        CompositionLocalProvider(
            LocalPocketColors provides pocketColors,
            LocalPocketTypography provides pocketTypography,
            LocalContentColor provides pocketColors.onBackground,
            LocalTextStyle provides pocketTypography.p1,
            content = content
        )
    }
}

object PocketTheme {
    val colors: PocketColors
        @Composable
        get() = LocalPocketColors.current
    val typography: PocketTypography
        @Composable
        get() = LocalPocketTypography.current
    val dimensions: PocketDimensions
        get() = PocketDimensions
}

@Composable
private fun themeColors(context: Context): ThemeColors {
    val themed = AppThemeUtil.findThemed(context)
    val themeColors by themed?.getThemeColorsChanges(context)
        ?.subscribeAsState(initial = themed.getThemeColors(context))
        ?: (if (isSystemInDarkTheme()) ThemeColors.DARK else ThemeColors.LIGHT).let { remember { mutableStateOf(it) } }
    return themeColors
}

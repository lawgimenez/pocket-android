package com.pocket.ui.view.themed

import android.content.Context
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.platform.LocalContext
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
                surfaceVariant = pocketColors.grey7,
                onBackground = pocketColors.onBackground,
                onSurface = pocketColors.onBackground,
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
}

@Composable
private fun themeColors(context: Context): ThemeColors {
    val themed = AppThemeUtil.findThemed(context)
    val themeColors by themed?.getThemeColorsChanges(context)
        ?.subscribeAsState(initial = themed.getThemeColors(context))
        ?: remember { mutableStateOf(ThemeColors.LIGHT) }
    return themeColors
}

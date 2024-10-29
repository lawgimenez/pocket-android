@file:Suppress("TopLevelPropertyNaming")

package com.pocket.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val LargeScreenSmallestWidth = 590

@Composable
fun sideGrid(): Dp {
    val configuration = LocalConfiguration.current
    return if (configuration.smallestScreenWidthDp >= LargeScreenSmallestWidth) {
        40.dp
    } else {
        20.dp
    }
}

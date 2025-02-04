package com.pocket.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp

object PocketDimensions {
    private const val LargeScreenSmallestWidth = 590

    val spaceSmall: Dp @Composable get() = dimensionResource(R.dimen.pkt_space_sm)
    val spaceMedium: Dp @Composable get() = dimensionResource(R.dimen.pkt_space_md)
    val spaceLarge: Dp @Composable get() = dimensionResource(R.dimen.pkt_space_lg)

    val sideGrid: Dp
        @Composable get() {
            val configuration = LocalConfiguration.current
            return if (configuration.smallestScreenWidthDp >= LargeScreenSmallestWidth) {
                spaceLarge
            } else {
                spaceMedium
            }
        }
}

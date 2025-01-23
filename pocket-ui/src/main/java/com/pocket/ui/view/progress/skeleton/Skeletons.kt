package com.pocket.ui.view.progress.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.pocket.ui.view.themed.PocketTheme

@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    color: Color = PocketTheme.colors.grey6,
) {
    Box(modifier.background(color, CircleShape))
}

@Composable
fun TextSkeleton(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = PocketTheme.colors.grey6,
) = with(LocalDensity.current) {
    Skeleton(
        modifier
            .padding(vertical = (style.lineHeight.toDp() - style.fontSize.toDp()) / 2)
            .height(style.fontSize.toDp()),
        color,
    )
}

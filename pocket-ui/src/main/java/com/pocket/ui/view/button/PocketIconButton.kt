package com.pocket.ui.view.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pocket.ui.view.themed.PocketTheme

@Composable
fun PocketIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides PocketTheme.colors.grey3,
    ) {
        // TODO: IconButton doesn't show content description on long press.
        IconButton(
            onClick,
            modifier.size(50.dp),
            content = content,
        )
    }
}

@Preview
@Composable
fun UpIconButtonPreview() {
    PocketIconButton(onClick = { }) {
        UpIcon()
    }
}

package com.pocket.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pocket.ui.R
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.button.UpIcon
import com.pocket.ui.view.themed.PocketTheme

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    actions: @Composable() (RowScope.() -> Unit) = {},
) {
    val navIconBuiltInSpace = 13.dp

    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(PocketTheme.dimensions.sideGrid - navIconBuiltInSpace))
            navigationIcon()
            Spacer(Modifier.width(dimensionResource(R.dimen.pkt_space_md) - navIconBuiltInSpace))
            TitleContainer(content = title)
            Spacer(Modifier.width(dimensionResource(R.dimen.pkt_space_md)))
            Row(content = actions)
        }
        ThinDivider()
    }
}

@Composable
private fun RowScope.TitleContainer(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides PocketTheme.typography.h7,
    ) {
        Box(Modifier.weight(1f)) {
            content()
        }
    }
}

@Preview
@Composable
fun AppBarPreview() {
    AppBar(
        Modifier.width(480.dp),
        {
            PocketIconButton(onClick = {}) {
                UpIcon()
            }
        },
        { Text("Title") },
    )
}

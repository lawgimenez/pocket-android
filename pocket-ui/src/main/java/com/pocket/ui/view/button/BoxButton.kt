package com.pocket.ui.view.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pocket.ui.view.themed.PocketTheme

@Composable
fun BoxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Text(
            text,
            style = PocketTheme.typography.h7,
            color = PocketTheme.colors.onTeal,
        )
    }
}

@Preview
@Composable
private fun BoxButtonPreview() {
    BoxButton(
        text = "Preview",
        onClick = {},
    )
}

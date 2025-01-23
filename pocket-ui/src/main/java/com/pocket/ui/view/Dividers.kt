package com.pocket.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pocket.ui.R
import com.pocket.ui.view.themed.PocketTheme

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.pkt_thin_divider_height))
            .background(PocketTheme.colors.grey6)
    )
}

@Preview()
@Composable
fun DividersPreview() {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = dimensionResource(R.dimen.pkt_space_sm))
    ) {
        ThinDivider()
        // Spacer()
        // ThickDivider()
        // etc.
    }
}

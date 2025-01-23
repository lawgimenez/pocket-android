package com.pocket.ui.view.button

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.pocket.ui.R

@Composable
fun UpIcon(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_pkt_back_arrow_line),
        stringResource(R.string.ic_up),
        modifier,
    )
}

@Composable
fun OverflowMenuIcon(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_pkt_android_overflow_solid),
        stringResource(R.string.ic_overflow),
        modifier,
    )
}

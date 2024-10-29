package com.pocket.app.home.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.ideashower.readitlater.R
import com.pocket.ui.view.button.BoxButton
import com.pocket.ui.view.themed.PocketTheme

class SignInBanner
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    enum class Layout {
        Compact, Expanded
    }

    var onSignInClick: (() -> Unit)? = null

    @Composable override fun Content() {
        PocketTheme {
            val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
            SignInBanner(
                onSignInClick = { onSignInClick?.invoke() },
                layout = when (windowSizeClass.windowWidthSizeClass) {
                    WindowWidthSizeClass.COMPACT -> Layout.Compact
                    else -> Layout.Expanded
                }
            )
        }
    }
}

@Composable private fun SignInBanner(
    modifier: Modifier = Modifier,
    onSignInClick: () -> Unit,
    layout: SignInBanner.Layout = SignInBanner.Layout.Compact,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = PocketTheme.colors.cardBackground,
        shadowElevation = 2.dp,
    ) {
        SignInBanner(
            Modifier.padding(16.dp),
            layout,
            title = {
                Text(
                    stringResource(R.string.home_sign_in_banner),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp,
                )
            },
            button = { modifier ->
                BoxButton(
                    stringResource(R.string.ac_continue),
                    onClick = onSignInClick,
                    modifier = modifier,
                )
            }
        )
    }
}

@Composable private fun SignInBanner(
    modifier: Modifier = Modifier,
    layout: SignInBanner.Layout = SignInBanner.Layout.Compact,
    title: @Composable () -> Unit,
    button: @Composable (Modifier) -> Unit,
) {
    when (layout) {
        SignInBanner.Layout.Compact -> SignInBannerCompact(modifier, title, button)
        SignInBanner.Layout.Expanded -> SignInBannerExpanded(modifier, title, button)
    }
}

@Composable private fun SignInBannerCompact(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    button: @Composable (Modifier) -> Unit,
) {
    Column(modifier) {
        title()
        Spacer(Modifier.height(dimensionResource(com.pocket.ui.R.dimen.pkt_space_sm)))
        button(Modifier.fillMaxWidth())
    }
}

@Composable private fun SignInBannerExpanded(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    button: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        title()
        Spacer(
            Modifier.widthIn(min = dimensionResource(com.pocket.ui.R.dimen.pkt_space_sm))
                .weight(1f)
        )
        button(Modifier.width(200.dp))
    }
}

@Preview
@Composable
private fun SignInBannerPreview() {
    PocketTheme {
        SignInBanner(Modifier.padding(16.dp), { })
    }
}

@Preview(widthDp = 750)
@Composable
private fun SignInBannerExpandedPreview() {
    PocketTheme {
        SignInBanner(
            Modifier.padding(16.dp),
            { },
            SignInBanner.Layout.Expanded,
        )
    }
}

package com.pocket.app.settings.appicon

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ideashower.readitlater.R
import com.pocket.sdk.util.AbsPocketFragment
import com.pocket.ui.view.AppBar
import com.pocket.ui.view.button.PocketIconButton
import com.pocket.ui.view.button.UpIcon
import com.pocket.ui.view.themed.PocketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppIconSettingsFragment : AbsPocketFragment() {
    private val viewModel by viewModels<AppIconSettingsViewModel>()

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PocketTheme {
            AppIconSettingsScreen(
                onUpClick = { findNavController().navigateUp() },
            )
        }
    }

    override fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedImpl(view, savedInstanceState)
        viewModel.onViewShown()
    }
}

data class AppIconUiState(
    val drawable: Drawable?,
    @StringRes val label: Int,
    val selected: Boolean,
)

@Composable
private fun AppIconSettingsScreen(
    viewModel: AppIconSettingsViewModel = viewModel(),
    onUpClick: () -> Unit,
) {
    AppIconSettingsScreen(
        automaticIcon = viewModel.automaticIcon,
        allIcons = viewModel.allIcons,
        onUpClick = onUpClick,
        onAutomaticIconClick = viewModel::onAutomaticIconClick,
        onIconClick = viewModel::onIconClick,
    )
}

@Composable
private fun AppIconSettingsScreen(
    automaticIcon: AppIconUiState,
    allIcons: List<AppIconUiState>,
    onUpClick: () -> Unit,
    onAutomaticIconClick: () -> Unit,
    onIconClick: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                navigationIcon = {
                    PocketIconButton(onClick = onUpClick) {
                        UpIcon()
                    }
                },
                title = { Text(stringResource(R.string.setting_app_icon_label)) },
            )
        }
    ) { contentPadding ->
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp),
            Modifier
                .consumeWindowInsets(contentPadding)
                .padding(horizontal = dimensionResource(com.pocket.ui.R.dimen.pkt_side_grid)),
            contentPadding = contentPadding,
        ) {
            header { AppIconSettingsHeader(stringResource(R.string.setting_app_icon_header_automatic)) }
            item(contentType = AppIconSettingsContentType.Icon) {
                AppIcon(
                    icon = rememberDrawablePainter(automaticIcon.drawable),
                    label = stringResource(automaticIcon.label),
                    selected = automaticIcon.selected,
                    onClick = onAutomaticIconClick
                )
            }
            header { AppIconSettingsHeader(stringResource(R.string.setting_app_icon_header_all)) }
            itemsIndexed(
                allIcons,
                contentType = { _, _ -> AppIconSettingsContentType.Icon }
            ) { index, icon ->
                AppIcon(
                    icon = rememberDrawablePainter(icon.drawable),
                    label = stringResource(icon.label),
                    selected = icon.selected,
                    onClick = { onIconClick(index) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(dimensionResource(com.pocket.ui.R.dimen.pkt_space_md)))
            }
        }
    }
}

@Composable
private fun AppIconSettingsHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier.padding(
            vertical = dimensionResource(com.pocket.ui.R.dimen.pkt_space_md)
        ),
        color = PocketTheme.colors.grey3,
        style = PocketTheme.typography.h7,
    )
}

@Composable
private fun AppIcon(
    icon: Painter,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onClick = onClick,
            )
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val borderWidth = 2.dp
        val borderSpacing = 2.dp
        Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .padding(dimensionResource(com.pocket.ui.R.dimen.pkt_space_sm) - borderWidth - borderSpacing)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = borderWidth,
                            color = colorResource(com.pocket.ui.R.color.pkt_themed_teal_2),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(borderWidth + borderSpacing)
            ,
        )
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = dimensionResource(id = com.pocket.ui.R.dimen.pkt_space_sm)),
            textAlign = TextAlign.Center,
        )
    }
}

private enum class AppIconSettingsContentType {
    Header,
    Icon,
}

private fun LazyGridScope.header(
    key: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit,
) {
    item(
        key,
        span = { GridItemSpan(maxLineSpan) },
        contentType = AppIconSettingsContentType.Header,
        content = content
    )
}

@Preview
@Composable
private fun AppIconSettingsScreenPreview() {
    val context = LocalContext.current
    PocketTheme {
        AppIconSettingsScreen(
            automaticIcon = AppIconUiState(
                AppCompatResources.getDrawable(context, R.drawable.ic_launcher_foreground),
                R.string.setting_app_icon_classic,
                false,
            ),
            allIcons = listOf(
                AppIconUiState(
                    AppCompatResources.getDrawable(context, R.drawable.ic_launcher_foreground),
                    R.string.setting_app_icon_classic,
                    true,
                ),
                AppIconUiState(
                    AppCompatResources.getDrawable(context, R.drawable.ic_launcher_pride_background),
                    R.string.setting_app_icon_pride_flag,
                    false,
                ),
            ),
            onUpClick = {},
            onAutomaticIconClick = {},
            onIconClick = {},
        )
    }
}

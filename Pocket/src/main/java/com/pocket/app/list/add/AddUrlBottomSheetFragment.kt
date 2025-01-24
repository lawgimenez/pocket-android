package com.pocket.app.list.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideashower.readitlater.R
import com.pocket.sdk.util.AbsPocketBottomSheetDialogFragment
import com.pocket.ui.view.button.BoxButton
import com.pocket.ui.view.themed.PocketTheme
import com.pocket.util.android.repeatOnResumed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddUrlBottomSheetFragment : AbsPocketBottomSheetDialogFragment() {
    companion object {
        fun newInstance() = AddUrlBottomSheetFragment()
    }

    private val viewModel by viewModels<AddUrlBottomSheetViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        PocketTheme {
            AddUrlBottomSheet()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onViewShown()
        repeatOnResumed {
            viewModel.navigationEvents.collect { event ->
                when (event) {
                    AddUrlBottomSheetViewModel.NavigationEvent.Close -> dismiss()
                }
            }
        }
    }
}

@Composable
fun AddUrlBottomSheet(
    viewModel: AddUrlBottomSheetViewModel = viewModel(),
) {
    AddUrlBottomSheet(
        viewModel.textFieldValue,
        viewModel::onTextFieldValueChange,
        { viewModel.onSaveButtonClick() },
        viewModel.textFieldIsError,
    )
}

@Composable
fun AddUrlBottomSheet(
    textFieldValue: String,
    onTextFieldValueChange: (String) -> Unit,
    onSaveButtonClick: () -> Unit,
    isError: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.padding(
                horizontal = PocketTheme.dimensions.sideGrid,
                vertical = dimensionResource(com.pocket.ui.R.dimen.pkt_space_md)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(dimensionResource(com.pocket.ui.R.dimen.pkt_space_md)))
            Text(
                stringResource(R.string.add_url_title),
                style = PocketTheme.typography.h5,
            )
            Spacer(Modifier.height(dimensionResource(com.pocket.ui.R.dimen.pkt_space_lg)))
            AddUrlTextField(textFieldValue, onTextFieldValueChange, isError)
            Spacer(Modifier.height(dimensionResource(com.pocket.ui.R.dimen.pkt_space_md)))
            BoxButton(
                text = stringResource(R.string.mu_read_later),
                onClick = onSaveButtonClick,
                Modifier.fillMaxWidth()
                    .height(48.dp),
            )
        }
    }
}

@Composable
private fun AddUrlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.add_url_hint)) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.add_url_error)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        singleLine = true,
    )
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
private fun AddUrlBottomSheetPreview() {
    PocketTheme {
        Box(
            Modifier
                .background(Color.Gray)
                .width(360.dp)
                .padding(top = 20.dp),
            propagateMinConstraints = true,
        ) {
            var textFieldValue by remember { mutableStateOf("") }
            AddUrlBottomSheet(
                textFieldValue,
                { textFieldValue = it },
                { },
                false,
            )
        }
    }
}
@Preview
@Composable
private fun AddUrlBottomSheetErrorPreview() {
    PocketTheme {
        Box(
            Modifier
                .background(Color.Gray)
                .width(360.dp)
                .padding(top = 20.dp),
            propagateMinConstraints = true,
        ) {
            AddUrlBottomSheet(
                "qwerty",
                { },
                { },
                true,
            )
        }
    }
}

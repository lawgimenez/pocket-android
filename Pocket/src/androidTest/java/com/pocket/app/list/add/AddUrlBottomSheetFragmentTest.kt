package com.pocket.app.list.add

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher.Companion.keyIsDefined
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.ideashower.readitlater.R
import org.junit.Rule
import kotlin.test.Test

class AddUrlBottomSheetFragmentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun initialState_displaysTitle() {
        with(composeTestRule) {
            setContent {
                AddUrlBottomSheet(
                    textFieldValue = "",
                    onTextFieldValueChange = {},
                    onSaveButtonClick = {},
                    isError = false,
                )
            }
            onNodeWithText(activity.getString(R.string.add_url_title))
                .assertIsDisplayed()
        }
    }

    @Test
    fun initialState_displaysHint() {
        with(composeTestRule) {
            setContent {
                AddUrlBottomSheet(
                    textFieldValue = "",
                    onTextFieldValueChange = {},
                    onSaveButtonClick = {},
                    isError = false,
                )
            }
            onNodeWithText(activity.getString(R.string.add_url_hint))
                .assertIsDisplayed()
        }
    }

    @Test
    fun initialState_displaysButton() {
        with(composeTestRule) {
            setContent {
                AddUrlBottomSheet(
                    textFieldValue = "",
                    onTextFieldValueChange = {},
                    onSaveButtonClick = {},
                    isError = false,
                )
            }
            onNodeWithText(activity.getString(com.pocket.ui.R.string.ac_save_to_pocket))
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun textFieldNotEmpty_hidesHint() {
        with(composeTestRule) {
            setContent {
                var textFieldValue by remember { mutableStateOf("") }
                AddUrlBottomSheet(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = { textFieldValue = it },
                    onSaveButtonClick = {},
                    isError = false,
                )
            }
            onNode(hasSetTextAction())
                .performTextInput("input")
            onNodeWithText(activity.getString(R.string.add_url_hint))
                .assertDoesNotExist()
        }
    }

    @Test
    fun isError_displaysError() {
        with(composeTestRule) {
            setContent {
                AddUrlBottomSheet(
                    textFieldValue = "wrong",
                    onTextFieldValueChange = {},
                    onSaveButtonClick = {},
                    isError = true,
                )
            }
            onNode(hasSetTextAction())
                .assert(keyIsDefined(SemanticsProperties.Error))
                .assertTextContains(activity.getString(R.string.add_url_error))
        }
    }
}

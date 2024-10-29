package com.pocket.app.list.add

import app.cash.turbine.test
import com.pocket.analytics.FakeTracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.analytics.assertTracked
import com.pocket.repository.FakeItemRepository
import com.pocket.repository.FakeUserRepository
import com.pocket.test.MainDispatcherRule
import com.pocket.usecase.Save
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class AddUrlBottomSheetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    val itemRepository = FakeItemRepository()
    val tracker = FakeTracker()
    val subject = AddUrlBottomSheetViewModel(
        Save(itemRepository, FakeUserRepository()),
        tracker,
    )

    @Test
    fun `starts with empty text field`() {
        assertEquals("", subject.textFieldValue)
    }

    @Test
    fun `starts without showing error`() {
        assertFalse(subject.textFieldIsError)
    }

    @Test
    fun `tracks impression`() {
        subject.onViewShown()

        tracker.assertTracked(SavesEvents.addUrlBottomSheetShown())
    }

    @Test
    fun `when trying to save an invalid url then shows error`() {
        subject.onTextFieldValueChange("this is not a valid URL")
        subject.onSaveButtonClick()

        assertTrue(subject.textFieldIsError)
    }

    @Test
    fun `when trying to save an invalid url then sends an analytics event`() {
        subject.onTextFieldValueChange("this is not a valid URL")
        subject.onSaveButtonClick()

        tracker.assertTracked(SavesEvents.addUrlBottomSheetSaveFailed())
    }
}

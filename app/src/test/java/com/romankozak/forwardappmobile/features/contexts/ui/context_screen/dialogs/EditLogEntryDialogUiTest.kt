package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EditLogEntryDialogUiTest {
    @get:Rule val ui = createComposeRule()
    private val entry = ContextLog(id = "entry", timestamp = 1_577_960_437_123L, type = "COMMENT", description = "Original", details = "Details text", workspaceId = "workspace")

    @Test fun `editing text without changing date preserves exact original timestamp`() {
        var saved: Triple<String, String?, Long>? = null
        ui.setContent { MaterialTheme { EditLogEntryDialog(entry, {}, { description, details, timestamp -> saved = Triple(description, details, timestamp) }) } }
        ui.onNodeWithText("Description").performTextReplacement("Updated")
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals(Triple("Updated", "Details text", entry.timestamp), saved) }
    }

    @Test fun `past date confirmation remains editable and survives dialog state restoration before Save`() {
        val restoration = StateRestorationTester(ui)
        var saved: Long? = null
        restoration.setContent { MaterialTheme { EditLogEntryDialog(entry, {}, { _, _, timestamp -> saved = timestamp }) } }
        ui.onNode(hasText("Дата й час:", substring = true)).performClick()
        ui.onNodeWithText("Дата й час запису").assertIsDisplayed()
        ui.onNodeWithText("Підтвердити").assertIsEnabled().performClick()
        ui.runOnIdle { assertNull(saved) }
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Save").performClick()
        ui.runOnIdle { assertEquals(entry.timestamp / 60_000 * 60_000, saved) }
    }

    @Test fun `cancelling date selection and properties never saves anything`() {
        var saves = 0
        var dismisses = 0
        ui.setContent { MaterialTheme { EditLogEntryDialog(entry, { dismisses++ }, { _, _, _ -> saves++ }) } }
        ui.onNode(hasText("Дата й час:", substring = true)).performClick()
        ui.onNodeWithText("Скасувати").performClick()
        ui.onNodeWithText("Cancel").performClick()
        ui.runOnIdle { assertEquals(0, saves); assertEquals(1, dismisses) }
    }
}

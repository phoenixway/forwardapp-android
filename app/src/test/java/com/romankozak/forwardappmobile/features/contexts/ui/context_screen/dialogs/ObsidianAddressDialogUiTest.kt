package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ObsidianAddressDialogUiTest {
    @get:Rule val ui = createComposeRule()
    private var added: Pair<String, String?>? = null
    private fun show() = ui.setContent {
        MaterialTheme { AddObsidianLinkDialog({}, { target, vault -> added = target to vault }, { _, _ -> }) }
    }
    @Test fun `copied URL provides its vault and disables creating another note`() {
        show()
        val url = "obsidian://open?vault=Actual&file=Folder%2FNote.md"
        ui.onNodeWithText("Obsidian URL, ім’я або шлях").performTextInput(url)
        ui.onNodeWithText("Vault (optional)").assertDoesNotExist()
        ui.onNodeWithText("Створити та додати").assertIsNotEnabled()
        ui.onNodeWithText("Add", ignoreCase = true).performClick()
        ui.runOnIdle { assertEquals(url to "Actual", added) }
    }
    @Test fun `relative path supports explicit vault`() {
        show()
        ui.onNodeWithText("Obsidian URL, ім’я або шлях").performTextInput("Folder/Note.md")
        ui.onNodeWithText("Vault (optional)").performTextInput("Vault")
        ui.onNodeWithText("Add", ignoreCase = true).performClick()
        ui.runOnIdle { assertEquals("Folder/Note.md" to "Vault", added) }
    }
    @Test fun `malformed URL disables submission without creating anything`() {
        show()
        ui.onNodeWithText("Obsidian URL, ім’я або шлях").performTextInput("obsidian://open?vault=V")
        ui.onNodeWithText("Add", ignoreCase = true).assertIsNotEnabled()
        ui.onNodeWithText("Створити та додати").assertIsNotEnabled()
        ui.runOnIdle { assertNull(added) }
    }
}

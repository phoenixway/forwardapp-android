package com.romankozak.forwardappmobile.features.semanticeditor

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SemanticEditorTabsUiTest {
    @get:Rule val ui = createComposeRule()
    private fun model(type: SemanticEntityClass = SemanticEntityClass.ORIENTATION) =
        SemanticEntityEditorModel("subject", type, "Лікування", "Лікування", null, 1)

    @Composable
    private fun Editor(model: SemanticEntityEditorModel, onName: (String) -> Unit = {}, onPlanning: () -> Unit = {},
                       onSemantic: () -> Unit = {}, onWorkspace: () -> Unit = {},
                       onCreate: () -> Unit = {}, creationBusy: Boolean = false) {
        SemanticEditorContent(model, PaddingValues(), onName, onPlanning, onSemantic, {}, {}, {}, {},
            onWorkspace, {}, {}, { _, _ -> }, onCreateWorkspace = onCreate,
            workspaceCreationBusy = creationBusy, tags = { Text("Теги тест") })
    }

    @Test fun `main has identity tags and Orientation planning without relation clutter`() {
        var planning = 0
        ui.setContent { MaterialTheme { Editor(model(), onPlanning = { planning++ }) } }
        ui.onNodeWithText("Назва").assertIsDisplayed()
        ui.onNodeWithText("Теги тест").assertIsDisplayed()
        ui.onNodeWithText("Planning").performScrollTo().assertIsDisplayed().performClick()
        ui.onNodeWithText("Додати семантичну сутність").assertDoesNotExist()
        ui.runOnIdle { assertEquals(1, planning) }
    }

    @Test fun `Aspect taxonomy belongs to semantics and never fabricates planning`() {
        ui.setContent { MaterialTheme { Editor(model(SemanticEntityClass.ASPECT)) } }
        ui.onNodeWithText("Planning").assertDoesNotExist()
        ui.onNodeWithText("Семантика").performClick()
        ui.onNodeWithText("Батьківський аспект").assertIsDisplayed()
        ui.onNodeWithText("Кореневий аспект").assertIsDisplayed()
        ui.onNodeWithText("Назва").assertDoesNotExist()
    }

    @Test fun `semantic add preserves callback and selected tab`() {
        var added = 0
        ui.setContent { MaterialTheme { Editor(model(), onSemantic = { added++ }) } }
        ui.onNodeWithText("Семантика").performClick()
        ui.onNodeWithText("Додати семантичну сутність").performClick()
        ui.onNodeWithText("Семантика").assertIsSelected()
        ui.runOnIdle { assertEquals(1, added) }
    }

    @Test fun `Workspace add remains on separate tab with same handler`() {
        var added = 0
        ui.setContent { MaterialTheme { Editor(model(), onWorkspace = { added++ }) } }
        ui.onNodeWithText("Workspaces").performClick()
        ui.onNodeWithText("Пов’язати").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(1, added) }
    }

    @Test fun `name draft remains intact after switching tabs`() {
        ui.setContent { MaterialTheme {
            var current by remember { mutableStateOf(model()) }
            Editor(current, onName = { current = current.copy(name = it) })
        } }
        ui.onNode(hasSetTextAction()).performTextReplacement("Новий зміст")
        ui.onNodeWithText("Семантика").performClick()
        ui.onNodeWithText("Основне").performClick()
        ui.onNode(hasSetTextAction()).assertTextContains("Новий зміст")
    }

    @Test fun `selected tab and page draft survive saved state restoration`() {
        val restoration = StateRestorationTester(ui)
        restoration.setContent { MaterialTheme {
            SemanticEditorTabs(model()) { tab ->
                var input by rememberSaveable { mutableStateOf("") }
                TextField(input, { input = it }, label = { Text(tab.name) })
            }
        } }
        ui.onNode(hasSetTextAction()).performTextReplacement("Чернетка")
        ui.onNodeWithText("Workspaces").performClick()
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Workspaces").assertIsSelected()
        ui.onNodeWithText("Основне").performClick()
        ui.onNode(hasSetTextAction()).assertTextContains("Чернетка")
    }

    @Test fun `mutation errors stay visible on every tab`() {
        ui.setContent { MaterialTheme { Editor(model().copy(errorMessage = "Не вдалося зберегти")) } }
        listOf("Основне", "Семантика", "Workspaces").forEach {
            ui.onNodeWithText(it).performClick()
            ui.onNodeWithText("Не вдалося зберегти").assertIsDisplayed()
        }
    }

    @Test fun `busy model still disables semantic mutation after switching tab`() {
        ui.setContent { MaterialTheme { Editor(model().copy(isRelationMutationInFlight = true)) } }
        ui.onNodeWithText("Семантика").performClick()
        ui.onNodeWithText("Додати семантичну сутність").assertIsNotEnabled()
    }

    @Test fun `touch tabs replace visible page in both directions repeatedly`() {
        ui.setContent { MaterialTheme { Editor(model(SemanticEntityClass.ASPECT)) } }
        repeat(2) {
            ui.onNodeWithText("Семантика").performTouchInput { click() }
            ui.onNodeWithText("Кореневий аспект").assertIsDisplayed()
            ui.onNodeWithText("Теги тест").assertIsNotDisplayed()
            ui.onNodeWithText("Workspaces").performTouchInput { click() }
            ui.onNodeWithText("Місце для роботи").assertIsDisplayed()
            ui.onNodeWithText("Кореневий аспект").assertIsNotDisplayed()
            ui.onNodeWithText("Основне").performTouchInput { click() }
            ui.onNodeWithText("Теги тест").assertIsDisplayed()
        }
    }

    @Test fun `swiping page updates selected tab and visible content together`() {
        ui.setContent { MaterialTheme {
            SemanticEditorTabs(model(), Modifier.testTag("editor")) { tab -> Text("Page ${tab.name}") }
        } }
        ui.onNodeWithTag("editor").performTouchInput { swipeLeft() }
        ui.onNodeWithText("Семантика").assertIsSelected()
        ui.onNodeWithText("Page SEMANTICS").assertIsDisplayed()
        ui.onNodeWithTag("editor").performTouchInput { swipeRight() }
        ui.onNodeWithText("Основне").assertIsSelected()
        ui.onNodeWithText("Page MAIN").assertIsDisplayed()
    }

    @Test fun `create Workspace is directly available on Workspace tab and preserves handler`() {
        var creates = 0
        var links = 0
        ui.setContent { MaterialTheme { Editor(model(), onCreate = { creates++ }, onWorkspace = { links++ }) } }
        ui.onNodeWithText(CREATE_EMBODYING_WORKSPACE_LABEL).assertDoesNotExist()
        ui.onNodeWithText("Workspaces").performClick()
        ui.onNodeWithText(CREATE_EMBODYING_WORKSPACE_LABEL).performScrollTo().performClick()
        ui.runOnIdle { assertEquals(1, creates); assertEquals(0, links) }
        ui.onNodeWithText("Пов’язати").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(1, links) }
    }

    @Test fun `existing embodiment hides redundant create action but keeps binding navigation`() {
        val bound = model().copy(workspaceBindings = listOf(
            WorkspaceBindingItem("binding", "workspace", "Кабінет", WorkspaceBindingType.EMBODIES, true)))
        ui.setContent { MaterialTheme { Editor(bound) } }
        ui.onNodeWithText("Workspaces").performClick()
        ui.onNodeWithText(CREATE_EMBODYING_WORKSPACE_LABEL).assertDoesNotExist()
        ui.onNodeWithText("Кабінет").assertIsDisplayed()
        ui.onNodeWithText("Пов’язати").assertIsEnabled()
    }

    @Test fun `create Workspace respects mutation and creation busy states`() {
        var busy by mutableStateOf(false)
        var saving by mutableStateOf(false)
        ui.setContent { MaterialTheme { Editor(model().copy(isSaving = saving), creationBusy = busy) } }
        ui.onNodeWithText("Workspaces").performClick()
        ui.onNodeWithText(CREATE_EMBODYING_WORKSPACE_LABEL).assertIsEnabled()
        ui.runOnIdle { busy = true }
        ui.onNodeWithText(CREATE_EMBODYING_WORKSPACE_LABEL).assertIsNotEnabled()
        ui.runOnIdle { busy = false; saving = true }
        ui.onNodeWithText(CREATE_EMBODYING_WORKSPACE_LABEL).assertIsNotEnabled()
    }

    @Test fun `overflow retains destructive action without duplicating workspace creation`() {
        var deleted = 0
        ui.setContent { MaterialTheme {
            EmbodyingWorkspaceAction(model(), EmbodyingWorkspaceState(), onDelete = { deleted++ })
        } }
        ui.onNodeWithContentDescription("Дії сутності").performClick()
        ui.onNodeWithText("$CREATE_EMBODYING_WORKSPACE_LABEL…").assertDoesNotExist()
        ui.onNodeWithText("Видалити сутність…").performClick()
        ui.runOnIdle { assertEquals(1, deleted) }
    }
}

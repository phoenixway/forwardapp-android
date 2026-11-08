package com.romankozak.forwardappmobile.features.workspace

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.BigPictureNodeMenu
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.WorkspaceActionsSheetGrid
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.MenuItem
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NavigationClipboardUiTest {
    @get:Rule val ui = createComposeRule()

    @Test fun `leaf card menu pastes under exact occurrence without opening entity`() {
        var destination: H1StructuralParent? = null
        var opened = false
        val node = H1TypedNode.Occurrence("leaf-link")
        ui.setContent { MaterialTheme {
            BigPictureNodeMenu(node, true, {}, {}, hasChildren = false, onOpen = { opened = true },
                onPasteChildren = { destination = it })
        } }
        ui.onNodeWithContentDescription("Дії розміщення").performClick()
        ui.onNodeWithText("Вставити нащадки").performClick()
        ui.runOnIdle { assertThat(destination).isEqualTo(H1StructuralParent.Node(node)); assertThat(opened).isFalse() }
    }

    @Test fun `container has same paste action using typed container destination`() {
        var destination: H1StructuralParent? = null
        val node = H1TypedNode.Container("same-raw-id")
        ui.setContent { MaterialTheme { BigPictureNodeMenu(node, true, {}, {}, onPasteChildren = { destination = it }) } }
        ui.onNodeWithContentDescription("Дії розміщення").performClick()
        ui.onNodeWithText("Вставити нащадки").performClick()
        ui.runOnIdle { assertThat(destination).isEqualTo(H1StructuralParent.Node(node)) }
    }

    @Test fun `empty clipboard omits paste and copy remains distinct from cut`() {
        var copies = 0
        var cuts = 0
        ui.setContent { MaterialTheme { BigPictureNodeMenu(H1TypedNode.Occurrence("p"), true, { cuts++ }, {}, onCopy = { copies++ }) } }
        ui.onNodeWithContentDescription("Дії розміщення").performClick()
        ui.onNodeWithText("Вставити нащадки").assertDoesNotExist()
        ui.onNodeWithText("Копіювати").performClick()
        ui.runOnIdle { assertThat(copies).isEqualTo(1); assertThat(cuts).isEqualTo(0) }
    }

    @Test fun `busy card cannot dispatch paste`() {
        ui.setContent { MaterialTheme { BigPictureNodeMenu(H1TypedNode.Occurrence("p"), false, {}, {},
            onOpen = {}, onPasteChildren = { error("Must not dispatch") }) } }
        ui.onNodeWithContentDescription("Дії розміщення").performClick()
        ui.onNodeWithText("Вставити нащадки").assertIsNotEnabled()
    }

    private fun row(id: String, parent: String, kind: PlacementKind) = WorkspaceOccurrence(id,
        listOf("Корінь", parent, "Workspace"), kind, NavTarget.ContextHierarchy(placementIdToReveal = id))
    private fun state() = WorkspaceOccurrencesState(loading = false, workspaceAvailable = true,
        occurrences = listOf(row("primary", "A", PlacementKind.PRIMARY), row("link", "B", PlacementKind.LINK)))

    @Test fun `Workspace copy has no occurrence chooser and uses independent handler`() {
        var copies = 0
        ui.setContent { MaterialTheme { WorkspaceNavigationClipboardSection("w", state(), true, { copies++; true }, { error("No cut") }) } }
        ui.onNodeWithText("Копіювати").performClick()
        ui.onNodeWithText("Яку появу перемістити?").assertDoesNotExist()
        ui.runOnIdle { assertThat(copies).isEqualTo(1) }
    }

    @Test fun `Workspace cut requires exact path selection and cancellation writes nothing`() {
        var selected: String? = null
        ui.setContent { MaterialTheme { WorkspaceNavigationClipboardSection("w", state(), true, { true }, { selected = it; true }) } }
        ui.onNodeWithText("Вирізати").performClick()
        ui.onNodeWithText("Корінь › A › Workspace").assertIsDisplayed()
        ui.onNodeWithText("Корінь › B › Workspace").assertIsDisplayed()
        ui.onNodeWithText("Закрити").performClick()
        ui.runOnIdle { assertThat(selected).isNull() }
        ui.onNodeWithText("Вирізати").performClick()
        ui.onNodeWithText("Корінь › B › Workspace").performClick()
        ui.runOnIdle { assertThat(selected).isEqualTo("link") }
    }

    @Test fun `unplaced Workspace can copy but cannot cut`() {
        ui.setContent { MaterialTheme { WorkspaceNavigationClipboardSection("w", state().copy(occurrences = emptyList()), true,
            { true }, { error("Must not cut") }) } }
        ui.onNodeWithText("Копіювати").assertIsEnabled()
        ui.onNodeWithText("Вирізати").assertIsNotEnabled()
    }

    @Test fun `unavailable or saving Workspace cannot prepare clipboard`() {
        ui.setContent { MaterialTheme { WorkspaceNavigationClipboardSection("w", state(), false,
            { error("Busy") }, { error("Busy") }) } }
        ui.onNodeWithText("Копіювати").assertIsNotEnabled()
        ui.onNodeWithText("Вирізати").assertIsNotEnabled()
    }

    @Test fun `Workspace action sheet contains grouped clipboard and preserves existing action handlers`() {
        var copied = 0
        var closed = 0
        var original = 0
        ui.setContent { MaterialTheme {
            WorkspaceActionsSheetGrid(listOf(
                MenuItem("Existing action", Icons.Default.ContentCopy, { original++ }),
                MenuItem("Hidden action", Icons.Default.ContentCopy, {}, isVisible = false),
            )) {
                WorkspaceNavigationClipboardSection("w", state(), true, { copied++; true }, { true },
                    onPrepared = { closed++ })
            }
        } }
        ui.onNodeWithText("Дії Workspace").assertIsDisplayed()
        ui.onNodeWithText("Navigation").assertIsDisplayed()
        ui.onNodeWithText("Копіювати").performScrollTo().performClick()
        ui.runOnIdle { assertThat(copied).isEqualTo(1); assertThat(closed).isEqualTo(1) }
        ui.onNodeWithText("Existing action").performScrollTo().performClick()
        ui.runOnIdle { assertThat(original).isEqualTo(1) }
        ui.onNodeWithText("Hidden action").assertDoesNotExist()
    }

    @Test fun `failed copy and cancelled cut do not dismiss action sheet`() {
        var closed = 0
        var cut = 0
        ui.setContent { MaterialTheme {
            WorkspaceNavigationClipboardSection("w", state(), true, { false }, { cut++; true }, onPrepared = { closed++ })
        } }
        ui.onNodeWithText("Копіювати").performClick()
        ui.onNodeWithText("Workspace недоступний").assertIsDisplayed()
        ui.onNodeWithText("Вирізати").performClick()
        ui.onNodeWithText("Закрити").performClick()
        ui.runOnIdle { assertThat(closed).isEqualTo(0); assertThat(cut).isEqualTo(0) }
    }
}

package com.romankozak.forwardappmobile.features.missions.presentation

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.workspace.EmbodyingWorkspaceRequest
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MissionWorkspaceActionUiTest {
    @get:Rule val ui = createComposeRule()
    private val create = MissionSemanticWorkspaceAction.Create(EmbodyingWorkspaceRequest("source", ManagedSubjectType.ORIENTATION, 1, "Intent", null))

    @Test fun `create wording names Orientation rather than converting Mission into Workspace`() {
        var calls = 0
        ui.setContent { MaterialTheme { MissionWorkspaceActionItem(create) { calls++ } } }
        ui.onNodeWithText("Створити робочий простір для орієнтира…").performClick()
        ui.runOnIdle { assertEquals(1, calls) }
        ui.onNodeWithText("Відкрити робочий простір").assertDoesNotExist()
    }

    @Test fun `existing Workspace action includes canonical title and subtitle is clickable`() {
        var calls = 0
        ui.setContent { MaterialTheme { MissionWorkspaceActionItem(MissionSemanticWorkspaceAction.Open("w", "Operational")) { calls++ } } }
        ui.onNodeWithText("Відкрити робочий простір").assertExists()
        ui.onNodeWithText("Operational").performClick()
        ui.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun `unsupported or unavailable participation advertises neither action`() {
        ui.setContent { MaterialTheme { MissionWorkspaceActionItem(null) { error("Must not dispatch") } } }
        ui.onNodeWithText("Відкрити робочий простір").assertDoesNotExist()
        ui.onNodeWithText("Створити робочий простір для орієнтира…").assertDoesNotExist()
    }

    @Test fun `Workspace badge is explicit clickable and absent without an embodiment`() {
        var calls = 0
        ui.setContent { MaterialTheme {
            MissionWorkspaceBadge(MissionSemanticWorkspaceAction.Open("workspace", "Operational")) { calls++ }
            MissionWorkspaceBadge(null) { error("No Workspace") }
        } }
        ui.onNodeWithText("Workspace · Operational").performClick()
        ui.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun `compact Mission title opens default action while checkbox alone toggles completion`() {
        var opens = 0
        var toggles = 0
        ui.setContent { MaterialTheme {
            CompactReadonlyMissionRow(
                workspace = MissionSemanticWorkspaceAction.Open("workspace", "Operational"),
                onOpenMission = { opens++ }, orientationSource = null, relatedOrientations = emptyList(),
                onOpenRelated = {}, onOpenSource = {},
                mission = TacticalMission(id = 42, title = "Mission", description = null, deadline = 0, projectId = null),
                streamTitle = null, showStream = false, isDragging = false,
                onToggle = { toggles++ }, onMoreClick = {},
            )
        } }
        ui.onNodeWithText("Mission").performClick()
        ui.onNodeWithText("Workspace · Operational").performClick()
        ui.runOnIdle { assertEquals(2, opens); assertEquals(0, toggles) }
        ui.onNode(isToggleable()).performClick()
        ui.runOnIdle { assertEquals(2, opens); assertEquals(1, toggles) }
    }

    @Test fun `extracted Mission menu preserves all original actions beside the Workspace action`() {
        val events = mutableListOf<String>()
        ui.setContent { MaterialTheme {
            MissionActionSheet(TacticalMission(id = 42, title = "Mission", description = null, deadline = 0, projectId = null),
                emptyList(), "general", emptyList(), null,
                onDismiss = { events += "dismiss" }, onEdit = { events += "edit" }, onToggleCompleted = { events += "complete" },
                onAddToToday = { events += "today" }, onAddToArc = { events += "arc" }, onAssignMissionStream = {}, onOpenStreamPicker = {},
                onAssignActivitySlot = {}, onOpenSlotPicker = {}, onPostpone = { events += "postpone" }, onContinue = { events += "extend" },
                onCopyMission = { events += "copy" }, onCutMission = { events += "cut" }, onDeleteMission = { events += "delete" },
                semanticWorkspaceAction = { MissionWorkspaceActionItem(create) { events += "workspace" } })
        } }
        listOf("Mark done", "Add to today", "Add to arc", "Postpone", "Extend", "Move stream", "Add slot", "Edit", "Copy", "Cut", "Delete", "Cancel")
            .forEach { ui.onAllNodesWithText(it).onLast().assertExists() }
        listOf("Add to today", "Edit", "Створити робочий простір для орієнтира…").forEach { label ->
            ui.onAllNodesWithText(label).onLast().performSemanticsAction(SemanticsActions.OnClick) { it() }
        }
        ui.runOnIdle { assertEquals(listOf("today", "edit", "workspace"), events) }
    }
}

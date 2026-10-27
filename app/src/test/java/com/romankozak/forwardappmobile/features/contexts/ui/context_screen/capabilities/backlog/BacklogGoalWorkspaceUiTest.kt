package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BacklogGoalWorkspaceUiTest {
    @get:Rule val ui = createComposeRule()

    @Composable private fun Sheet(canCreate: Boolean, onCreate: () -> Unit, onOpen: () -> Unit, onDismiss: () -> Unit) {
        BacklogItemActionsBottomSheet(onDismiss = onDismiss, itemTitle = "Goal", isCompleted = false,
            onRemindersClick = {}, onAddToDayPlan = {}, missionStreams = emptyList(), selectedMissionStreamId = "general",
            currentTacticalPriorityStreamId = null, onMissionStreamSelected = {}, isTacticalPriority = false,
            onToggleTacticalPriority = {}, onStartTracking = {}, onToggleCompleted = {}, onCopyContent = {},
            onDelete = {}, onDeleteEverywhere = {}, onCreateWorkspace = onCreate, canCreateWorkspace = canCreate,
            onOpenSemanticEditor = onOpen)
    }

    @Test fun `Goal menu retains existing actions and exposes distinct canonical commands`() {
        val events = mutableListOf<String>()
        ui.setContent { MaterialTheme { Sheet(true, { events += "create" }, { events += "semantic" }, { events += "dismiss" }) } }
        ui.onNodeWithText("Додати в план дня").assertExists()
        ui.onNodeWithText("Create workspace").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.onNodeWithText("Open in semantic editor").performSemanticsAction(SemanticsActions.OnClick) { it() }
        ui.runOnIdle { assertEquals(listOf("create", "dismiss", "semantic", "dismiss"), events) }
    }

    @Test fun `existing embodiment disables creation but keeps explicit semantic editor access`() {
        ui.setContent { MaterialTheme { Sheet(false, {}, {}, {}) } }
        ui.onNodeWithText("Create workspace").assertIsNotEnabled()
        ui.onNodeWithText("Open in semantic editor").assertIsEnabled()
    }

    @Test fun `Workspace badge handles its own click rather than opening the Goal card`() {
        var workspace = 0
        var goal = 0
        ui.setContent { MaterialTheme {
            Box(Modifier.clickable { goal++ }) { BacklogGoalWorkspaceBadge("Operational") { workspace++ } }
        } }
        ui.onNodeWithText("Workspace · Operational").performClick()
        ui.runOnIdle { assertEquals(1, workspace); assertEquals(0, goal) }
    }
}

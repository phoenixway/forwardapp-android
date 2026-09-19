package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyProjectMenuAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogStateManagerWorkspaceMenuTest {
    @Test
    fun workspacePresentationMenuDoesNotNeedLegacyContextBacking() {
        val stateManager = DialogStateManager()

        stateManager.onMenuRequested(
            projectId = "workspace-only",
            projectName = "Workspace only",
        )

        val menu = stateManager.dialogState.value as DialogState.ProjectMenu
        assertEquals("workspace-only", menu.projectId)
        assertTrue(menu.availability.open)
        assertTrue(menu.availability.addSubproject)
        assertTrue(menu.availability.addToDayPlan)
    }

    @Test
    fun unsupportedLegacyActionsStayExplicitlyDisabled() {
        val availability = HierarchyProjectMenuAvailability()

        assertFalse(availability.edit)
        assertFalse(availability.move)
        assertFalse(availability.delete)
        assertFalse(availability.copyContextLink)
        assertFalse(availability.addNoteDocument)
    }
}

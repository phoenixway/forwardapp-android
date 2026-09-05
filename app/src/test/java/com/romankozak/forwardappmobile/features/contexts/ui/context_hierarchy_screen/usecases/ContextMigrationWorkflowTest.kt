package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationConfidence
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationOutcome
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationPreview
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextMigrationChoice
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMigrationWorkflowTest {
    @Test
    fun `system Context cannot start the migration workflow`() {
        assertTrue(!canStartContextMigrationFromUi(context("sys_inbox")))
    }
    @Test
    fun `classifier recommendations never become executable selection without explicit choice`() {
        listOf(
            ContextClassificationOutcome.ASPECT_AND_WORKSPACE,
            ContextClassificationOutcome.ORIENTATION_AND_WORKSPACE,
            ContextClassificationOutcome.WORKSPACE_ONLY,
        ).forEach { outcome ->
            val state = workflow(outcome)
            assertNull(state.selectedChoice)
            assertTrue(runCatching { state.materializeTarget() }.isFailure)
        }
    }

    @Test
    fun `aspect recommendation materializes only after explicit confirmation choice`() {
        val state = workflow(ContextClassificationOutcome.ASPECT_AND_WORKSPACE).copy(selectedChoice = ContextMigrationChoice.NEW_ASPECT)
        assertEquals(ContextMigrationTarget.NewAspectWithExistingWorkspace(), state.materializeTarget())
    }

    @Test
    fun `new Orientation requires explicit target choice and explicit kind`() {
        val state = workflow(ContextClassificationOutcome.ORIENTATION_AND_WORKSPACE)

        assertNull(state.selectedChoice)
        assertTrue(runCatching { state.materializeTarget() }.isFailure)

        val explicitlySelected =
            state.copy(selectedChoice = ContextMigrationChoice.NEW_ORIENTATION)

        assertTrue(runCatching { explicitlySelected.materializeTarget() }.isFailure)
        assertEquals(
            ContextMigrationTarget.NewOrientationWithExistingWorkspace(OrientationKind.GOAL),
            explicitlySelected
                .copy(selectedOrientationKind = OrientationKind.GOAL)
                .materializeTarget(),
        )
    }

    @Test
    fun `existing selections and Workspace-only materialize exact canonical targets`() {
        val base = workflow(ContextClassificationOutcome.REVIEW_REQUIRED)
        assertEquals(
            ContextMigrationTarget.ExistingAspectWithExistingWorkspace("aspect"),
            base.copy(selectedChoice = ContextMigrationChoice.EXISTING_ASPECT, selectedExistingAspectId = "aspect").materializeTarget(),
        )
        assertEquals(
            ContextMigrationTarget.ExistingOrientationWithExistingWorkspace("orientation"),
            base.copy(selectedChoice = ContextMigrationChoice.EXISTING_ORIENTATION, selectedExistingOrientationId = "orientation").materializeTarget(),
        )
        assertEquals(
            ContextMigrationTarget.WorkspaceOnly,
            base.copy(selectedChoice = ContextMigrationChoice.WORKSPACE_ONLY).materializeTarget(),
        )
    }

    @Test
    fun `workspace with relations never becomes a synthetic migration target`() {
        val state = workflow(ContextClassificationOutcome.WORKSPACE_WITH_RELATIONS)
        assertNull(state.selectedChoice)
        assertTrue(runCatching { state.materializeTarget() }.isFailure)
    }

    @Test
    fun `missing existing candidate selection fails instead of choosing one`() {
        val state = workflow(ContextClassificationOutcome.REVIEW_REQUIRED)
            .copy(selectedChoice = ContextMigrationChoice.EXISTING_ASPECT)
        assertTrue(runCatching { state.materializeTarget() }.isFailure)
    }

    private fun workflow(outcome: ContextClassificationOutcome) =
        DialogState.ContextMigration(
            context = context("context"),
            preview = ContextClassificationPreview("context", "context", outcome, ContextClassificationConfidence.HIGH, emptyList()),
            aspectCandidates = emptyList(),
            orientationCandidates = emptyList(),
        )

    private fun context(id: String) =
        Context(
            id = id,
            name = "Context",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
}

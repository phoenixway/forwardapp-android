package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationConfidence
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationOutcome
import com.romankozak.forwardappmobile.data.orientation.ContextClassificationPreview
import com.romankozak.forwardappmobile.data.orientation.CanonicalContextMigrationRepository
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationCandidateReader
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationResult
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationTarget
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextMigrationChoice
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state.DialogStateManager
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMigrationWorkflowTest {
    @Test
    fun `system Context cannot start the migration workflow`() {
        assertTrue(!canStartContextMigrationFromUi(SystemContexts.INBOX.raw))
    }

    @Test
    fun `migration start resolves raw Context internally by project id`() =
        runTest {
            val rawContext = context("ordinary", "Ordinary")
            val contextRepository = mockk<ContextRepository>()
            coEvery { contextRepository.getContextById(rawContext.id) } returns rawContext
            val migrationRepository = mockk<CanonicalContextMigrationRepository>(relaxed = true)
            val candidateReader = mockk<ContextMigrationCandidateReader>()
            coEvery { candidateReader.existingAspectCandidates() } returns emptyList()
            coEvery { candidateReader.existingOrientationCandidates() } returns emptyList()
            val dialogStateManager = DialogStateManager()
            val coordinator =
                ContextMigrationCoordinator(
                    candidateReader = candidateReader,
                    migrationRepository = migrationRepository,
                    contextRepository = contextRepository,
                    dialogStateManager = dialogStateManager,
                )

            assertTrue(coordinator.start(rawContext.id))
            coVerify(exactly = 1) { contextRepository.getContextById(rawContext.id) }
            val state = dialogStateManager.currentContextMigration()
            assertEquals(rawContext.id, state?.projectId)
            assertEquals(rawContext.name, state?.projectName)
        }

    @Test
    fun `missing raw Context fails migration start closed`() =
        runTest {
            val contextRepository = mockk<ContextRepository>()
            coEvery { contextRepository.getContextById("missing") } returns null
            val coordinator =
                ContextMigrationCoordinator(
                    candidateReader = mockk<ContextMigrationCandidateReader>(relaxed = true),
                    migrationRepository = mockk(relaxed = true),
                    contextRepository = contextRepository,
                    dialogStateManager = DialogStateManager(),
                )

            assertTrue(!coordinator.start("missing"))
        }

    @Test
    fun `migration execute uses dialog project id`() =
        runTest {
            val rawContext = context("ordinary", "Ordinary")
            val contextRepository = mockk<ContextRepository>()
            coEvery { contextRepository.getContextById(rawContext.id) } returns rawContext
            val migrationRepository = mockk<CanonicalContextMigrationRepository>()
            val candidateReader = mockk<ContextMigrationCandidateReader>()
            coEvery { candidateReader.existingAspectCandidates() } returns emptyList()
            coEvery { candidateReader.existingOrientationCandidates() } returns emptyList()
            coEvery {
                migrationRepository.migrateContext(rawContext.id, ContextMigrationTarget.WorkspaceOnly, any())
            } returns
                ContextMigrationResult(
                    contextId = rawContext.id,
                    subjectId = null,
                    workspaceId = rawContext.id,
                    mappingId = null,
                    changed = true,
                )
            val coordinator =
                ContextMigrationCoordinator(
                    candidateReader = candidateReader,
                    migrationRepository = migrationRepository,
                    contextRepository = contextRepository,
                    dialogStateManager = DialogStateManager(),
                )

            assertTrue(coordinator.start(rawContext.id))
            coordinator.selectChoice(ContextMigrationChoice.WORKSPACE_ONLY)
            coordinator.requestConfirmation()

            assertTrue(coordinator.execute().isSuccess)
            coVerify(exactly = 1) {
                migrationRepository.migrateContext(rawContext.id, ContextMigrationTarget.WorkspaceOnly, any())
            }
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
            projectId = "context",
            projectName = "Context",
            preview = ContextClassificationPreview("context", "context", outcome, ContextClassificationConfidence.HIGH, emptyList()),
            aspectCandidates = emptyList(),
            orientationCandidates = emptyList(),
        )

    private fun context(id: String, name: String) =
        com.romankozak.forwardappmobile.core.data.models.entities.Context(
            id = id,
            name = name,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
}

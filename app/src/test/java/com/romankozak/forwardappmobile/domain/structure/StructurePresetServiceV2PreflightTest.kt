package com.romankozak.forwardappmobile.domain.structure

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureWithItems
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class StructurePresetServiceV2PreflightTest {
    private val structures = mockk<ContextStructureRepository>(relaxed = true)
    private val contexts = mockk<ContextRepository>(relaxed = true)
    private val workspaces = mockk<CanonicalWorkspaceRepository>(relaxed = true)
    private val attachments = mockk<AttachmentsRepository>(relaxed = true)
    private val notes = mockk<NoteDocumentRepository>(relaxed = true)
    private val checklists = mockk<ChecklistRepository>(relaxed = true)

    private fun service() =
        StructurePresetService(
            contextStructureRepository = structures,
            attachmentRepository = attachments,
            noteDocumentRepository = notes,
            checklistRepository = checklists,
            contextRepository = contexts,
            canonicalWorkspaceRepository = workspaces,
        )

    @Test
    fun `preset role stays on live ordinary Context owner`() = runTest {
        val contextId = "legacy-owner"
        val presetCode = "development"
        val context = mockk<Context>()

        every { context.id } returns contextId
        every { context.isDeleted } returns false
        every { context.roleCode } returns "old-role"
        coEvery { contexts.getContextById(contextId) } returns context
        coEvery { structures.presetRequiresChildWorkspace(presetCode) } returns false
        coEvery { structures.activeStructureRequiresChildWorkspace(contextId) } returns false
        coEvery { structures.getStructureWithItems(contextId) } returns
            ContextStructureWithItems(
                structure = ContextConfiguration.default(contextId),
                items = emptyList(),
            )

        service().applyPresetToContext(contextId, presetCode)

        coVerify(exactly = 1) {
            contexts.updateContextRole(
                contextId = contextId,
                roleCode = presetCode,
            )
        }
        coVerify(exactly = 0) {
            workspaces.updateRole(
                id = any(),
                roleCode = any(),
                now = any(),
            )
        }
    }

    @Test
    fun `preset role writes canonical Workspace when Context shell is absent`() = runTest {
        val contextId = "standalone-owner"
        val presetCode = "development"

        coEvery { contexts.getContextById(contextId) } returns null
        coEvery { structures.presetRequiresChildWorkspace(presetCode) } returns false
        coEvery { structures.activeStructureRequiresChildWorkspace(contextId) } returns false
        coEvery { structures.getStructureWithItems(contextId) } returns
            ContextStructureWithItems(
                structure = ContextConfiguration.default(contextId),
                items = emptyList(),
            )

        service().applyPresetToContext(contextId, presetCode)

        coVerify(exactly = 0) {
            contexts.updateContextRole(any(), any())
        }
        coVerify(exactly = 1) {
            workspaces.updateRole(
                id = contextId,
                roleCode = presetCode,
                now = any(),
            )
        }
    }

    @Test
    fun `System preset role writes canonical Workspace even while Context shell exists`() = runTest {
        val contextId = SystemContexts.PERSONAL_MANAGEMENT.raw
        val presetCode = "management"
        val context = mockk<Context>()

        every { context.id } returns contextId
        every { context.isDeleted } returns false
        every { context.roleCode } returns presetCode
        coEvery { contexts.getContextById(contextId) } returns context
        coEvery { structures.presetRequiresChildWorkspace(presetCode) } returns false
        coEvery { structures.activeStructureRequiresChildWorkspace(contextId) } returns false
        coEvery { structures.getStructureWithItems(contextId) } returns
            ContextStructureWithItems(
                structure = ContextConfiguration.default(contextId),
                items = emptyList(),
            )

        service().applyPresetToContext(contextId, presetCode)

        coVerify(exactly = 0) {
            contexts.updateContextRole(any(), any())
        }
        coVerify(exactly = 1) {
            workspaces.updateRole(
                id = contextId,
                roleCode = presetCode,
                now = any(),
            )
        }
    }

    @Test
    fun `V2 target-only structural preset rejects before capability or role writes`() = runTest {
        coEvery { structures.presetRequiresChildWorkspace("child_preset") } returns true
        coEvery { structures.activeStructureRequiresChildWorkspace("parent") } returns false

        val failure = runCatching {
            service().applyPresetToContext("parent", "child_preset")
        }.exceptionOrNull()

        assertTrue(failure is PresetParentOccurrenceRequiredException)
        coVerify(exactly = 0) { structures.applyPresetToContext(any(), any()) }
        coVerify(exactly = 0) { contexts.getContextById(any()) }
        coVerify(exactly = 0) { structures.getStructureWithItems(any()) }
        coVerify(exactly = 0) {
            workspaces.ensureChildWorkspaceByRoleAtOccurrence(any(), any(), any(), any())
        }
    }

    @Test
    fun `V2 target-only preset rejects existing structural item before replacing structure`() = runTest {
        coEvery { structures.presetRequiresChildWorkspace("attachment_only") } returns false
        coEvery { structures.activeStructureRequiresChildWorkspace("parent") } returns true

        val failure = runCatching {
            service().applyPresetToContext("parent", "attachment_only")
        }.exceptionOrNull()

        assertTrue(failure is PresetParentOccurrenceRequiredException)
        coVerify(exactly = 0) { structures.applyPresetToContext(any(), any()) }
        coVerify(exactly = 0) { contexts.getContextById(any()) }
    }

    @Test
    fun `V2 target-only structure synchronization rejects before materialization`() = runTest {
        coEvery { structures.activeStructureRequiresChildWorkspace("parent") } returns true

        val failure = runCatching {
            service().applyContextStructure("parent")
        }.exceptionOrNull()

        assertTrue(failure is PresetParentOccurrenceRequiredException)
        coVerify(exactly = 0) { structures.getStructureWithItems(any()) }
    }
    @Test
    fun `occurrence-aware structure materializes SUBCONTEXT under exact parent occurrence`() = runTest {
        val placementId = PlacementId("parent-placement")
        val configuration = ContextConfiguration.default("parent")
        val item =
            ContextStructureItem(
                id = "child-item",
                contextStructureId = configuration.id,
                entityType = "SUBCONTEXT",
                roleCode = "child-role",
                containerType = null,
                title = "Child",
                mandatory = true,
            )

        coEvery { structures.activeStructureRequiresChildWorkspace("parent") } returns true
        coEvery { structures.getStructureWithItems("parent") } returns
            ContextStructureWithItems(
                structure = configuration,
                items = listOf(item),
            )

        service().applyContextStructure("parent", placementId)

        coVerify(exactly = 1) {
            workspaces.requirePresetParentOccurrence(
                parentWorkspaceId = "parent",
                parentPlacementId = placementId,
            )
        }
        coVerify(exactly = 1) {
            workspaces.ensureChildWorkspaceByRoleAtOccurrence(
                parentWorkspaceId = "parent",
                parentPlacementId = placementId,
                roleCode = "child-role",
                title = "Child",
            )
        }
    }

    @Test
    fun `V2 UI preflight rejects prospective SUBCONTEXT without structure writes`() = runTest {
        val failure = runCatching {
            service().requireTargetOnlyStructureSupported(
                contextId = "parent",
                prospectiveSubcontext = true,
            )
        }.exceptionOrNull()

        assertTrue(failure is PresetParentOccurrenceRequiredException)
        coVerify(exactly = 0) { structures.ensureStructure(any()) }
        coVerify(exactly = 0) { structures.addOrUpdateItem(any(), any()) }
        coVerify(exactly = 0) { structures.setItemEnabled(any(), any()) }
        coVerify(exactly = 0) { structures.getStructureWithItems(any()) }
    }

    @Test
    fun `V2 UI preflight rejects existing active SUBCONTEXT without structure writes`() = runTest {
        coEvery { structures.activeStructureRequiresChildWorkspace("parent") } returns true

        val failure = runCatching {
            service().requireTargetOnlyStructureSupported("parent")
        }.exceptionOrNull()

        assertTrue(failure is PresetParentOccurrenceRequiredException)
        coVerify(exactly = 0) { structures.ensureStructure(any()) }
        coVerify(exactly = 0) { structures.addOrUpdateItem(any(), any()) }
        coVerify(exactly = 0) { structures.setItemEnabled(any(), any()) }
    }

}

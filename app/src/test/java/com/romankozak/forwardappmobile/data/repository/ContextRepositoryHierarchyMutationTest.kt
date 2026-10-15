package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import javax.inject.Provider
import kotlinx.coroutines.test.runTest
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextRepositoryHierarchyMutationTest {
    @Test
    fun `direction auto-links derive children from canonical H1 not Context parentId`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val hierarchy = mockk<CanonicalHierarchyPlacementRepository>()
        val projector = mockk<SystemWorkspacePresentationContextProjector>()
        val directionAccess = mockk<SystemContextCanonicalInboxDirectionAccess>()
        val parent = placement("parent-placement", "parent", null)
        val child = placement("child-placement", "child", parent.id)
        coEvery {
            hierarchy.getPrimaryAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "parent"), any())
        } returns parent
        coEvery { hierarchy.getLiveChildren(parent.id, any()) } returns listOf(child)
        coEvery { projector.projectPresentationUniverse() } returns
            listOf(
                ContextPresentation(
                    id = "child",
                    name = "Canonical child",
                    description = null,
                    parentId = null,
                    roleCode = null,
                    order = 0L,
                    tags = emptyList(),
                ),
            )
        coEvery { directionAccess.directionAutoLinkEnabled("parent") } returns true

        val repository =
            repository(
                contextDao = contextDao,
                workspaceWriteThrough = mockk(relaxed = true),
                hierarchyPlacementRepository = hierarchy,
                presentationProjector = projector,
                directionAccess = directionAccess,
            )

        repository.ensureDirectionFrontLinksForExistingChildren("parent")

        coVerify(exactly = 0) { contextDao.getActiveContextsByParentId(any()) }
    }

    @Test
    fun `settings update changes only settings fields and leaves tags separate`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = scalarWriteThrough()
        val persisted = scalarContext(id = "context", isCompleted = true, isAttachmentsExpanded = true)
        coEvery { contextDao.getContextById(persisted.id) } returns persisted
        val written = slot<Context>()
        coEvery { contextDao.update(capture(written)) } returns Unit

        repository(
            contextDao,
            writeThrough,
            systemWorkspaceTagAuthority = passthroughTagAuthority(),
        ).updateContextSettings(
            contextId = persisted.id,
            update =
                ContextSettingsUpdate(
                    name = "Settings name",
                    description = "Settings description",
                    relatedLinks = emptyList(),
                    isContextManagementEnabled = true,
                    effort = 3f,
                    cost = 4f,
                    risk = 5f,
                    weightEffort = 6f,
                    weightCost = 7f,
                    weightRisk = 8f,
                    rawScore = 9f,
                    displayScore = 10,
                    scoringStatus = "updated-scoring",
                ),
        )

        val actual = written.captured
        assertEquals("Settings name", actual.name)
        assertEquals("Settings description", actual.description)
        assertEquals(emptyList<Any>(), actual.relatedLinks)
        assertEquals(persisted.showCheckboxes, actual.showCheckboxes)
        assertEquals(true, actual.isContextManagementEnabled)
        assertEquals(9f, actual.rawScore)
        assertEquals(10, actual.displayScore)
        assertEquals("updated-scoring", actual.scoringStatus)
        assertEquals(persisted.parentId, actual.parentId)
        assertEquals(persisted.order, actual.order)
        assertEquals(persisted.tags, actual.tags)
        assertEquals(persisted.roleCode, actual.roleCode)
        assertEquals(persisted.contextStatus, actual.contextStatus)
        assertEquals(persisted.isCompleted, actual.isCompleted)
    }

    @Test
    fun `completion derives from fresh persisted Context`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = scalarWriteThrough()
        val persisted = scalarContext(id = "context", isCompleted = false, isAttachmentsExpanded = true)
        coEvery { contextDao.getContextById(persisted.id) } returns persisted
        val written = slot<Context>()
        coEvery { contextDao.update(capture(written)) } returns Unit

        repository(
            contextDao,
            writeThrough,
            systemWorkspaceTagAuthority = passthroughTagAuthority(),
        ).updateContextCompleted(persisted.id, true)

        assertScalarMutation(
            actual = written.captured,
            persisted = persisted,
            expectedCompleted = true,
            expectedAttachmentsExpanded = persisted.isAttachmentsExpanded,
            expectedRoleCode = persisted.roleCode,
        )
        coVerify(exactly = 1) { contextDao.getContextById(persisted.id) }
    }

    @Test
    fun `completion is a no op when current value already matches`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val persisted = scalarContext(id = "context", isCompleted = true)
        coEvery { contextDao.getContextById(persisted.id) } returns persisted

        repository(contextDao, mockk(relaxed = true)).updateContextCompleted(persisted.id, true)

        coVerify(exactly = 0) { contextDao.update(any<Context>()) }
    }

    @Test
    fun `ordinary role update derives from fresh persisted Context`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = scalarWriteThrough()
        val persisted = scalarContext(id = "context", isCompleted = true, isAttachmentsExpanded = true)
        coEvery { contextDao.getContextById(persisted.id) } returns persisted
        val written = slot<Context>()
        coEvery { contextDao.update(capture(written)) } returns Unit

        repository(
            contextDao,
            writeThrough,
            systemWorkspaceTagAuthority = passthroughTagAuthority(),
        ).updateContextRole(persisted.id, "new-role")

        assertScalarMutation(
            actual = written.captured,
            persisted = persisted,
            expectedCompleted = persisted.isCompleted,
            expectedAttachmentsExpanded = persisted.isAttachmentsExpanded,
            expectedRoleCode = "new-role",
        )
        coVerify(exactly = 1) { contextDao.getContextById(persisted.id) }
    }

    @Test
    fun `ordinary role update is a no op when current value already matches`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val persisted = scalarContext(id = "context")
        coEvery { contextDao.getContextById(persisted.id) } returns persisted

        repository(contextDao, mockk(relaxed = true)).updateContextRole(persisted.id, persisted.roleCode)

        coVerify(exactly = 0) { contextDao.update(any<Context>()) }
    }

    @Test
    fun `System role update delegates by id without Context lookup`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)

        repository(
            contextDao,
            mockk(relaxed = true),
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
        ).updateContextRole(SystemContexts.PERSONAL_MANAGEMENT.raw, "new-role")

        coVerify(exactly = 1) {
            canonicalWorkspaceRepository.updateRole(
                id = SystemContexts.PERSONAL_MANAGEMENT.raw,
                roleCode = "new-role",
                now = any(),
            )
        }
        coVerify(exactly = 0) { contextDao.getContextById(any()) }
    }

    private fun repository(
        contextDao: ContextDao,
        workspaceWriteThrough: ContextWorkspaceWriteThrough,
        canonicalWorkspaceRepository: CanonicalWorkspaceRepository = mockk(relaxed = true),
        systemWorkspaceTagAuthority: SystemWorkspaceTagAuthority = passthroughTagAuthority(),
        hierarchyPlacementRepository: CanonicalHierarchyPlacementRepository = mockk(relaxed = true),
        presentationProjector: SystemWorkspacePresentationContextProjector = mockk(relaxed = true),
        directionAccess: SystemContextCanonicalInboxDirectionAccess = mockk(relaxed = true),
    ): ContextRepository =
        ContextRepository(
            contextDao = contextDao,
            legacyNoteRepository = mockk(relaxed = true),
            activityRepository = mockk(relaxed = true),
            recentItemsRepository = mockk(relaxed = true),
            reminderRepository = mockk(relaxed = true),
            contextLogRepository = mockk(relaxed = true),
            searchRepository = mockk(relaxed = true),
            noteDocumentRepository = mockk(relaxed = true),
            musicNoteRepository = mockk(relaxed = true),
            checklistRepository = mockk(relaxed = true),
            attachmentRepository = mockk(relaxed = true),
            goalRepository = mockk(relaxed = true),
            contextTimeTrackingRepository = mockk(relaxed = true),
            listItemRepository = mockk(relaxed = true),
            backlogPlacementCommands = mockk(relaxed = true),
            contextStructureDao = mockk(relaxed = true),
            structurePresetDao = mockk(relaxed = true),
            directionRepository = mockk(relaxed = true),
            aiEventRepository = mockk(relaxed = true),
            contextMarkerHandlerProvider = mockk(relaxed = true),
            tagAssociationHandler = mockk(relaxed = true),
            workspaceWriteThrough = workspaceWriteThrough,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            canonicalWorkspaceTagRepository = mockk(relaxed = true),
            systemWorkspaceTagAuthority = systemWorkspaceTagAuthority,
            systemContextCanonicalInboxDirectionAccess = directionAccess,
            canonicalKeyProblemsRepository = mockk(relaxed = true),
            canonicalInboxRepository = mockk(relaxed = true),
            canonicalConnectionsRepository = mockk(relaxed = true),
            canonicalBacklogRepository = mockk(relaxed = true),
            backlogPresentationLifecycle = mockk(relaxed = true),
            hierarchyPlacementLifecycleCoordinator = mockk(relaxed = true),
            hierarchyPlacementRepository = hierarchyPlacementRepository,
            systemWorkspacePresentationContextProjector = presentationProjector,
        )

    private fun placement(
        id: String,
        workspaceId: String,
        parentId: PlacementId?,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = HierarchyId.GENERAL,
        target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspaceId),
        parentPlacementId = parentId,
        placementKind = PlacementKind.PRIMARY,
        siblingOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun context(
        id: String,
        parentId: String?,
    ) = Context(
        id = id,
        name = id,
        description = null,
        parentId = parentId,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun scalarWriteThrough(): ContextWorkspaceWriteThrough =
        mockk<ContextWorkspaceWriteThrough>().also { writeThrough ->
            coEvery { writeThrough.mutate<Context>(any(), any()) } coAnswers {
                secondArg<suspend () -> Context>().invoke()
            }
        }

    private fun passthroughTagAuthority(): SystemWorkspaceTagAuthority =
        mockk<SystemWorkspaceTagAuthority>().also { authority ->
            coEvery {
                authority.reconcileBeforeContextWrite(any(), any(), any())
            } coAnswers {
                firstArg<Context>()
            }
        }

    private fun scalarContext(
        id: String,
        isCompleted: Boolean = false,
        isAttachmentsExpanded: Boolean = false,
    ) = Context(
        id = id,
        name = "Fresh persisted name",
        description = "Fresh persisted description",
        parentId = "fresh-parent",
        createdAt = 1L,
        updatedAt = 10L,
        syncedAt = 9L,
        version = 41L,
        tags = listOf("fresh", "persisted"),
        order = 37L,
        isCompleted = isCompleted,
        isAttachmentsExpanded = isAttachmentsExpanded,
        contextStatus = "persisted-status",
        contextStatusText = "Persisted status text",
        roleCode = "persisted-role",
    )

    private fun assertScalarMutation(
        actual: Context,
        persisted: Context,
        expectedCompleted: Boolean,
        expectedAttachmentsExpanded: Boolean,
        expectedRoleCode: String?,
    ) {
        assertEquals(expectedCompleted, actual.isCompleted)
        assertEquals(expectedAttachmentsExpanded, actual.isAttachmentsExpanded)
        assertEquals(expectedRoleCode, actual.roleCode)
        assertEquals(persisted.name, actual.name)
        assertEquals(persisted.description, actual.description)
        assertEquals(persisted.parentId, actual.parentId)
        assertEquals(persisted.order, actual.order)
        assertEquals(persisted.tags, actual.tags)
        assertEquals(persisted.contextStatus, actual.contextStatus)
        assertTrue(actual.updatedAt != persisted.updatedAt)
        assertEquals(null, actual.syncedAt)
        assertEquals(persisted.version + 1L, actual.version)
    }
}

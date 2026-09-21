package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import javax.inject.Provider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextRepositoryHierarchyMutationTest {
    @Test
    fun `shared state update changes only its explicitly owned fields`() = runTest {
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
        ).updateContextSharedState(
            contextId = persisted.id,
            update =
                ContextSharedStateUpdate(
                    name = "Updated name",
                    description = "Updated description",
                    contextStatus = "updated-status",
                    defaultViewModeName = "DASHBOARD",
                    isCompleted = true,
                ),
        )

        val actual = written.captured
        assertEquals("Updated name", actual.name)
        assertEquals("Updated description", actual.description)
        assertEquals("updated-status", actual.contextStatus)
        assertEquals("DASHBOARD", actual.defaultViewModeName)
        assertTrue(actual.isCompleted)
        assertEquals(persisted.parentId, actual.parentId)
        assertEquals(persisted.order, actual.order)
        assertEquals(persisted.tags, actual.tags)
        assertEquals(persisted.roleCode, actual.roleCode)
        assertEquals(persisted.isAttachmentsExpanded, actual.isAttachmentsExpanded)
        assertEquals(persisted.rawScore, actual.rawScore)
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
                    showCheckboxes = true,
                    isContextManagementEnabled = true,
                    valueImportance = 1f,
                    valueImpact = 2f,
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
        assertTrue(actual.showCheckboxes)
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
    fun `attachments toggle uses current persisted value`() = runTest {
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
        ).toggleContextAttachmentsExpanded(persisted.id)

        assertScalarMutation(
            actual = written.captured,
            persisted = persisted,
            expectedCompleted = persisted.isCompleted,
            expectedAttachmentsExpanded = false,
            expectedRoleCode = persisted.roleCode,
        )
        coVerify(exactly = 1) { contextDao.getContextById(persisted.id) }
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

    @Test
    fun `ordinary move reads current Context and preserves its order and non topology state`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = mockk<ContextWorkspaceWriteThrough>()
        coEvery { writeThrough.mutate<Context>(any(), any()) } coAnswers {
            secondArg<suspend () -> Context>().invoke()
        }
        val persisted =
            Context(
                id = "context",
                name = "Fresh persisted name",
                description = "Fresh persisted description",
                parentId = "old-parent",
                createdAt = 1L,
                updatedAt = 10L,
                syncedAt = 9L,
                version = 41L,
                tags = listOf("fresh", "persisted"),
                order = 37L,
                roleCode = "management",
            )
        coEvery { contextDao.getContextById(persisted.id) } returns persisted
        val written = slot<Context>()
        coEvery { contextDao.update(capture(written)) } returns Unit

        repository(contextDao, writeThrough).moveContextById(persisted.id, "new-parent")

        val actual = written.captured
        assertEquals("new-parent", actual.parentId)
        assertEquals(persisted.order, actual.order)
        assertEquals(persisted.name, actual.name)
        assertEquals(persisted.description, actual.description)
        assertEquals(persisted.tags, actual.tags)
        assertEquals(persisted.roleCode, actual.roleCode)
        assertTrue(actual.updatedAt != persisted.updatedAt)
        assertEquals(null, actual.syncedAt)
        assertEquals(persisted.version + 1L, actual.version)
        coVerify(exactly = 1) { contextDao.getContextById(persisted.id) }
    }

    @Test
    fun `ordinary move is a no op when current parent already matches`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = mockk<ContextWorkspaceWriteThrough>(relaxed = true)
        val directionRepository = mockk<DirectionRepository>(relaxed = true)
        val persisted = context(id = "context", parentId = "parent")
        coEvery { contextDao.getContextById(persisted.id) } returns persisted

        repository(contextDao, writeThrough, directionRepository = directionRepository)
            .moveContextById(persisted.id, persisted.parentId)

        coVerify(exactly = 0) { contextDao.update(any<Context>()) }
        coVerify(exactly = 0) {
            directionRepository.addDirectionLinkedAtFront(any(), any(), any())
        }
    }

    @Test
    fun `System move delegates by stable id without reading Context`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = mockk<ContextWorkspaceWriteThrough>(relaxed = true)
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)

        repository(
            contextDao,
            writeThrough,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
        ).moveContextById(
            contextId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            newParentId = "parent",
            allowSystemMoves = true,
        )

        coVerify(exactly = 1) {
            canonicalWorkspaceRepository.movePreservingOrder(
                id = SystemContexts.PERSONAL_MANAGEMENT.raw,
                newParentWorkspaceId = "parent",
                now = any(),
            )
        }
        coVerify(exactly = 0) { contextDao.getContextById(any()) }
    }

    @Test
    fun `System move remains fail closed unless explicitly allowed`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = mockk<ContextWorkspaceWriteThrough>(relaxed = true)
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)

        repository(
            contextDao,
            writeThrough,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
        ).moveContextById(
            contextId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            newParentId = "parent",
        )

        coVerify(exactly = 0) { canonicalWorkspaceRepository.movePreservingOrder(any(), any(), any()) }
        coVerify(exactly = 0) { contextDao.getContextById(any()) }
    }

    @Test
    fun `ordinary hierarchy update preserves fresh persisted non topology state`() = runTest {
        val contextDao = mockk<ContextDao>(relaxed = true)
        val writeThrough = mockk<ContextWorkspaceWriteThrough>()
        coEvery { writeThrough.mutate<Int>(any(), any()) } coAnswers {
            secondArg<suspend () -> Int>().invoke()
        }

        val persisted =
            Context(
                id = "context",
                name = "Fresh persisted name",
                description = "Fresh persisted description",
                parentId = "old-parent",
                createdAt = 1L,
                updatedAt = 10L,
                syncedAt = 9L,
                isDeleted = false,
                version = 41L,
                tags = listOf("fresh", "persisted"),
                isExpanded = false,
                order = 3L,
                isAttachmentsExpanded = true,
                defaultViewModeName = "DASHBOARD",
                isCompleted = true,
                isContextManagementEnabled = true,
                contextStatus = "persisted-status",
                contextStatusText = "Persisted status text",
                contextLogLevel = "persisted-log-level",
                totalTimeSpentMinutes = 321L,
                valueImportance = 1.25f,
                valueImpact = 2.5f,
                effort = 3.75f,
                cost = 4.5f,
                risk = 5.25f,
                weightEffort = 0.75f,
                weightCost = 1.25f,
                weightRisk = 1.5f,
                rawScore = 6.5f,
                displayScore = 73,
                scoringStatus = "persisted-scoring-status",
                showCheckboxes = true,
                roleCode = "management",
            )

        coEvery {
            contextDao.getContextsByIds(listOf(persisted.id))
        } returns listOf(persisted)

        val written = mutableListOf<Context>()
        coEvery {
            contextDao.update(any<List<Context>>())
        } coAnswers {
            val rows = firstArg<List<Context>>()
            written += rows
            rows.size
        }

        val repository = repository(contextDao, writeThrough)

        val result =
            repository.applyHierarchyUpdates(
                listOf(
                    ContextHierarchyUpdate(
                        id = persisted.id,
                        parentId = "new-parent",
                        order = 7L,
                    ),
                ),
            )

        assertEquals(1, result)
        assertEquals(1, written.size)

        val actual = written.single()

        assertTrue(actual.updatedAt != null)
        assertTrue(actual.updatedAt != persisted.updatedAt)

        assertEquals(
            persisted.copy(
                parentId = "new-parent",
                order = 7L,
                updatedAt = actual.updatedAt,
                syncedAt = null,
                version = persisted.version + 1L,
            ),
            actual,
        )

        coVerify(exactly = 1) {
            contextDao.getContextsByIds(listOf(persisted.id))
        }
    }

    private fun repository(
        contextDao: ContextDao,
        writeThrough: ContextWorkspaceWriteThrough,
        directionRepository: DirectionRepository = mockk(relaxed = true),
        canonicalWorkspaceRepository: CanonicalWorkspaceRepository = mockk(relaxed = true),
        systemWorkspaceTagAuthority: SystemWorkspaceTagAuthority = mockk(relaxed = true),
    ): ContextRepository {
        val markerProvider = mockk<Provider<ContextMarkerHandler>>()
        every { markerProvider.get() } returns mockk(relaxed = true)

        return ContextRepository(
            contextDao = contextDao,
            contextTagRefDao = mockk(relaxed = true),
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
            directionRepository = directionRepository,
            aiEventRepository = mockk(relaxed = true),
            contextMarkerHandlerProvider = markerProvider,
            tagAssociationHandler = mockk(relaxed = true),
            workspaceWriteThrough = writeThrough,
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            canonicalWorkspaceTagRepository = mockk(relaxed = true),
            systemWorkspaceTagAuthority = systemWorkspaceTagAuthority,
            systemContextCanonicalInboxDirectionAccess = mockk(relaxed = true),
            canonicalKeyProblemsRepository = mockk(relaxed = true),
            canonicalInboxRepository = mockk(relaxed = true),
            canonicalConnectionsRepository = mockk(relaxed = true),
            canonicalBacklogRepository = mockk(relaxed = true),
            backlogPresentationLifecycle = mockk(relaxed = true),
            hierarchyPlacementLifecycleCoordinator = mockk(relaxed = true),
        )
    }

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

package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.data.repository.ContextHierarchyUpdate
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextParentLinkDao
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DropPosition
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.sync.SyncRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextActionsUseCaseTest {
    private val contextRepository = mockk<ContextRepository>(relaxed = true)
    private val contextParentLinkDao = mockk<ContextParentLinkDao>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true)

    private val useCase =
        ContextActionsUseCase(
            contextRepository = contextRepository,
            contextParentLinkDao = contextParentLinkDao,
            syncRepository = syncRepository,
            settingsRepository = settingsRepository,
            mainBeaconRepository = mainBeaconRepository,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

    @Test
    fun getMoveProjectRouteTreatsOrphanedContextAsRoot() = runTest {
        val orphanParent = "missing-parent"
        val project =
            context(
                id = "orphan",
                parentId = orphanParent,
            )

        val route = requireNotNull(useCase.getMoveProjectRoute(project.id, allProjects = listOf(project)))

        assertEquals("root", route.currentParentId)
    }

    @Test
    fun getMoveProjectRouteFailsClosedWhenRawContextIsMissing() {
        assertNull(useCase.getMoveProjectRoute("missing", allProjects = emptyList()))
    }

    @Test
    fun deleteForwardsRootAndDescendantsAsIdsWithoutContextCommandPayload() = runTest {
        val root = context(id = "root")
        val child = context(id = "child", parentId = root.id)
        val grandchild = context(id = "grandchild", parentId = child.id)

        useCase.onDeleteProjectConfirmed(
            projectId = root.id,
            childMap = mapOf(root.id to listOf(child), child.id to listOf(grandchild)),
        )

        coVerify(exactly = 1) {
            contextRepository.deleteContextsByIds(listOf(root.id, child.id, grandchild.id))
        }
    }

    @Test
    fun deleteTraversalIsCycleSafeAndDoesNotRepeatRootId() = runTest {
        val root = context(id = "root")
        val child = context(id = "child", parentId = root.id)

        useCase.onDeleteProjectConfirmed(
            projectId = root.id,
            childMap = mapOf(root.id to listOf(child), child.id to listOf(root)),
        )

        coVerify(exactly = 1) {
            contextRepository.deleteContextsByIds(listOf(root.id, child.id))
        }
    }

    @Test
    fun sameParentReorderSendsOnlyNormalizedHierarchyUpdates() = runTest {
        val parentId = "parent"
        val parent = context(id = parentId)
        val first = context(id = "first", parentId = parentId, order = 0)
        val second = context(id = "second", parentId = parentId, order = 1)
        val third = context(id = "third", parentId = parentId, order = 2)

        useCase.onProjectReorder(
            fromId = third.id,
            toId = first.id,
            position = DropPosition.BEFORE,
            isSearchActive = false,
            allProjects = listOf(parent, first, second, third),
        )

        coVerify(exactly = 1) {
            contextRepository.applyHierarchyUpdates(
                listOf(
                    ContextHierarchyUpdate(third.id, parentId, 0),
                    ContextHierarchyUpdate(first.id, parentId, 1),
                    ContextHierarchyUpdate(second.id, parentId, 2),
                ),
            )
        }
    }

    @Test
    fun crossParentReorderSendsSourceAndTargetTopologyUpdates() = runTest {
        val sourceParentId = "source-parent"
        val targetParentId = "target-parent"
        val sourceParent = context(id = sourceParentId)
        val targetParent = context(id = targetParentId)
        val sourceSibling = context(id = "source-sibling", parentId = sourceParentId, order = 0)
        val moved = context(id = "moved", parentId = sourceParentId, order = 1)
        val target = context(id = "target", parentId = targetParentId, order = 0)

        useCase.onProjectReorder(
            fromId = moved.id,
            toId = target.id,
            position = DropPosition.AFTER,
            isSearchActive = false,
            allProjects = listOf(sourceParent, targetParent, sourceSibling, moved, target),
        )

        coVerify(exactly = 1) {
            contextRepository.applyHierarchyUpdates(
                listOf(
                    ContextHierarchyUpdate(sourceSibling.id, sourceParentId, 0),
                    ContextHierarchyUpdate(target.id, targetParentId, 0),
                    ContextHierarchyUpdate(moved.id, targetParentId, 1),
                ),
            )
        }
    }

    @Test
    fun reorderContextSiblingsSendsDirectChildrenAsHierarchyUpdates() = runTest {
        val parentId = "parent"
        val first = context(id = "first", parentId = parentId, order = 0)
        val second = context(id = "second", parentId = parentId, order = 1)
        coEvery { mainBeaconRepository.getBeaconById(parentId) } returns null
        coEvery { contextParentLinkDao.getActiveLinks() } returns emptyList()

        useCase.reorderContextSiblings(
            parentContextId = parentId,
            orderedContextIds = listOf(second.id, first.id),
            allProjects = listOf(first, second),
        )

        coVerify(exactly = 1) {
            contextRepository.applyHierarchyUpdates(
                listOf(
                    ContextHierarchyUpdate(second.id, parentId, 0),
                    ContextHierarchyUpdate(first.id, parentId, 1),
                ),
            )
        }
    }

    @Test
    fun reorderContextSiblingsKeepsAdditionalParentLinksOnTheirDaoPath() = runTest {
        val parentId = "additional-parent"
        val linkedChild = context(id = "linked-child", parentId = "primary-parent")
        coEvery { mainBeaconRepository.getBeaconById(parentId) } returns null
        coEvery { contextParentLinkDao.getActiveLinks() } returns
            listOf(ContextParentLink(parentContextId = parentId, childContextId = linkedChild.id))

        useCase.reorderContextSiblings(
            parentContextId = parentId,
            orderedContextIds = listOf(linkedChild.id),
            allProjects = listOf(linkedChild),
        )

        coVerify(exactly = 1) {
            contextParentLinkDao.updateOrder(
                parentContextId = parentId,
                childContextId = linkedChild.id,
                order = 0,
                updatedAt = any(),
            )
        }
        coVerify(exactly = 0) { contextRepository.applyHierarchyUpdates(any()) }
    }

    @Test
    fun personalManagementCanBeMovedUnderAnotherContext() = runTest {
        val targetParent = context(id = "target-parent")
        val personalManagement = context(id = SystemContexts.PERSONAL_MANAGEMENT.raw)

        useCase.onListChooserResult(
            newParentId = targetParent.id,
            projectBeingMovedId = personalManagement.id,
            allProjects = listOf(personalManagement, targetParent),
        )

        coVerify(exactly = 1) {
            contextRepository.moveContextById(
                contextId = personalManagement.id,
                newParentId = targetParent.id,
                allowSystemMoves = true,
            )
        }
    }

    private fun context(
        id: String,
        parentId: String? = null,
        name: String = id,
        order: Long = 0,
    ): Context =
        Context(
            id = id,
            name = name,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            order = order,
        )
}

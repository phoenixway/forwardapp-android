package com.romankozak.forwardappmobile.desktop.features.contexts

import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceExplorerIntent
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.CreateContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteBacklogItemUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DeleteContextUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveBacklogUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.ObserveContextTreeUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemContentUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateBacklogItemDoneUseCase
import com.romankozak.forwardappmobile.shared.domain.contexts.UpdateContextUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopWorkspaceStoreTest {
    @Test
    fun intentsDriveContextAndBacklogCrudState() = runTest {
        val repository = InMemoryDesktopWorkspaceRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val store =
            DesktopWorkspaceStore(
                observeContextTree = ObserveContextTreeUseCase(repository),
                observeBacklog = ObserveBacklogUseCase(repository),
                createContext = CreateContextUseCase(repository),
                updateContext = UpdateContextUseCase(repository),
                deleteContext = DeleteContextUseCase(repository),
                createBacklogItem = CreateBacklogItemUseCase(repository),
                deleteBacklogItem = DeleteBacklogItemUseCase(repository),
                updateBacklogItemContent = UpdateBacklogItemContentUseCase(repository),
                updateBacklogItemDone = UpdateBacklogItemDoneUseCase(repository),
                scope = scope,
            )

        advanceUntilIdle()
        assertEquals("Core", store.state.value.selectedContextName)

        store.dispatch(WorkspaceExplorerIntent.ContextSelected("core"))
        store.dispatch(WorkspaceExplorerIntent.StartCreatingContext)
        store.dispatch(WorkspaceExplorerIntent.ContextDraftNameChanged("Projects"))
        store.dispatch(WorkspaceExplorerIntent.ContextDraftDescriptionChanged("Created in store test"))
        store.dispatch(WorkspaceExplorerIntent.ContextDraftStatusChanged(SharedContextStatus.InProgress))
        store.dispatch(WorkspaceExplorerIntent.ContextDraftViewChanged(SharedContextView.Backlog))
        store.dispatch(WorkspaceExplorerIntent.SaveContext)

        advanceUntilIdle()

        val createdContext = repository.contexts.first { context -> context.name == "Projects" }
        assertEquals(createdContext.id, store.state.value.selectedContextId)
        assertEquals("Projects", store.state.value.selectedContextName)

        store.dispatch(WorkspaceExplorerIntent.StartCreatingBacklogItem)
        store.dispatch(WorkspaceExplorerIntent.BacklogDraftTitleChanged("First desktop task"))
        store.dispatch(WorkspaceExplorerIntent.BacklogDraftDetailsChanged("Created through intent API"))
        store.dispatch(WorkspaceExplorerIntent.BacklogDraftPriorityChanged(SharedBacklogPriority.High))
        store.dispatch(WorkspaceExplorerIntent.SaveBacklogItem)

        advanceUntilIdle()

        val createdBacklog = repository.backlogItems.first { item -> item.contextId == createdContext.id }
        assertEquals(1, store.state.value.backlogItems.size)
        assertEquals("First desktop task", createdBacklog.title)

        store.dispatch(WorkspaceExplorerIntent.StartEditingBacklogItem(createdBacklog.id))
        store.dispatch(WorkspaceExplorerIntent.BacklogDraftTitleChanged("Updated desktop task"))
        store.dispatch(WorkspaceExplorerIntent.ToggleBacklogItemDone(createdBacklog.id, true))

        advanceUntilIdle()

        val updatedBacklog = repository.backlogItems.first { item -> item.id == createdBacklog.id }
        assertTrue(updatedBacklog.isDone)

        store.dispatch(WorkspaceExplorerIntent.StartEditingBacklogItem(createdBacklog.id))
        store.dispatch(WorkspaceExplorerIntent.BacklogDraftTitleChanged("Updated desktop task"))
        store.dispatch(WorkspaceExplorerIntent.BacklogDraftPriorityChanged(SharedBacklogPriority.Critical))
        store.dispatch(WorkspaceExplorerIntent.SaveBacklogItem)

        advanceUntilIdle()

        val savedBacklog = repository.backlogItems.first { item -> item.id == createdBacklog.id }
        assertEquals("Updated desktop task", savedBacklog.title)
        assertEquals(SharedBacklogPriority.Critical, savedBacklog.priority)

        store.dispatch(WorkspaceExplorerIntent.DeleteBacklogItem(createdBacklog.id))
        advanceUntilIdle()
        assertTrue(repository.backlogItems.none { item -> item.id == createdBacklog.id })

        store.dispatch(WorkspaceExplorerIntent.StartEditingContext)
        store.dispatch(WorkspaceExplorerIntent.ContextDraftNameChanged("Projects Updated"))
        store.dispatch(WorkspaceExplorerIntent.SaveContext)

        advanceUntilIdle()

        assertEquals("Projects Updated", repository.contexts.first { it.id == createdContext.id }.name)

        store.dispatch(WorkspaceExplorerIntent.DeleteContext)
        advanceUntilIdle()

        assertFalse(repository.contexts.any { it.id == createdContext.id })
        assertEquals("Core", store.state.value.selectedContextName)
    }

    @Test
    fun deletingParentContextRemovesSubtreeAndFallsBackToSafeSelection() = runTest {
        val repository = InMemoryDesktopWorkspaceRepository.withHierarchy()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val store =
            DesktopWorkspaceStore(
                observeContextTree = ObserveContextTreeUseCase(repository),
                observeBacklog = ObserveBacklogUseCase(repository),
                createContext = CreateContextUseCase(repository),
                updateContext = UpdateContextUseCase(repository),
                deleteContext = DeleteContextUseCase(repository),
                createBacklogItem = CreateBacklogItemUseCase(repository),
                deleteBacklogItem = DeleteBacklogItemUseCase(repository),
                updateBacklogItemContent = UpdateBacklogItemContentUseCase(repository),
                updateBacklogItemDone = UpdateBacklogItemDoneUseCase(repository),
                scope = scope,
            )

        advanceUntilIdle()

        store.dispatch(WorkspaceExplorerIntent.ContextSelected("project"))
        advanceUntilIdle()
        assertEquals("Project", store.state.value.selectedContextName)
        assertEquals(1, store.state.value.backlogItems.size)

        store.dispatch(WorkspaceExplorerIntent.DeleteContext)
        advanceUntilIdle()

        assertFalse(repository.contexts.any { it.id == "project" || it.id == "child" })
        assertTrue(repository.backlogItems.none { it.contextId == "project" || it.contextId == "child" })
        assertEquals("Core", store.state.value.selectedContextName)
        assertEquals("core", store.state.value.selectedContextId)
        assertTrue(store.state.value.backlogItems.isEmpty())
    }

    private class InMemoryDesktopWorkspaceRepository : DesktopWorkspaceRepository {
        val contexts =
            mutableListOf(
                SharedContextSummary(
                    id = "core",
                    name = "Core",
                    description = "Root context",
                    parentId = null,
                    status = SharedContextStatus.InProgress,
                    defaultView = SharedContextView.Dashboard,
                    score = 100,
                    isCompleted = false,
                ),
            )
        val backlogItems = mutableListOf<SharedBacklogItem>()

        override suspend fun getContexts(): List<SharedContextSummary> = contexts.toList()

        override suspend fun createContext(
            parentId: String?,
            name: String,
            description: String?,
            status: SharedContextStatus,
            defaultView: SharedContextView,
            enabledCapabilityIds: List<String>,
            experimentalCapabilityIds: List<String>,
        ): SharedContextSummary {
            val created =
                SharedContextSummary(
                    id = "context-${contexts.size + 1}",
                    name = name,
                    description = description,
                    parentId = parentId,
                    status = status,
                    defaultView = defaultView,
                    score = 0,
                    isCompleted = status == SharedContextStatus.Completed,
                    enabledCapabilityIds = enabledCapabilityIds,
                    experimentalCapabilityIds = experimentalCapabilityIds,
                )
            contexts += created
            return created
        }

        override suspend fun updateContext(
            contextId: String,
            name: String,
            description: String?,
            status: SharedContextStatus,
            defaultView: SharedContextView,
            enabledCapabilityIds: List<String>,
            experimentalCapabilityIds: List<String>,
        ): SharedContextSummary? {
            val index = contexts.indexOfFirst { context -> context.id == contextId }
            if (index == -1) return null
            val updated =
                contexts[index].copy(
                    name = name,
                    description = description,
                    status = status,
                    defaultView = defaultView,
                    isCompleted = status == SharedContextStatus.Completed,
                    enabledCapabilityIds = enabledCapabilityIds,
                    experimentalCapabilityIds = experimentalCapabilityIds,
                )
            contexts[index] = updated
            return updated
        }

        override suspend fun deleteContext(contextId: String): Boolean {
            val idsToDelete = buildIdsToDelete(contextId)
            if (idsToDelete.isEmpty()) return false
            contexts.removeAll { context -> context.id in idsToDelete }
            backlogItems.removeAll { item -> item.contextId in idsToDelete }
            return true
        }

        override suspend fun getBacklogItems(contextId: String): List<SharedBacklogItem> =
            backlogItems.filter { item -> item.contextId == contextId }

        override suspend fun createBacklogItem(
            contextId: String,
            title: String,
            details: String?,
            priority: SharedBacklogPriority,
        ): SharedBacklogItem {
            val created =
                SharedBacklogItem(
                    id = "item-${backlogItems.size + 1}",
                    contextId = contextId,
                    title = title,
                    details = details,
                    kind = SharedBacklogItemKind.Task,
                    priority = priority,
                    isDone = false,
                )
            backlogItems += created
            return created
        }

        override suspend fun updateBacklogItemDone(
            itemId: String,
            isDone: Boolean,
        ): SharedBacklogItem? {
            val index = backlogItems.indexOfFirst { item -> item.id == itemId }
            if (index == -1) return null
            val updated = backlogItems[index].copy(isDone = isDone)
            backlogItems[index] = updated
            return updated
        }

        override suspend fun updateBacklogItemContent(
            itemId: String,
            title: String,
            details: String?,
            priority: SharedBacklogPriority,
        ): SharedBacklogItem? {
            val index = backlogItems.indexOfFirst { item -> item.id == itemId }
            if (index == -1) return null
            val updated =
                backlogItems[index].copy(
                    title = title,
                    details = details,
                    priority = priority,
                )
            backlogItems[index] = updated
            return updated
        }

        override suspend fun deleteBacklogItem(itemId: String): Boolean =
            backlogItems.removeIf { item -> item.id == itemId }

        private fun buildIdsToDelete(rootId: String): Set<String> {
            if (contexts.none { context -> context.id == rootId }) return emptySet()
            val childrenByParent = contexts.groupBy { context -> context.parentId }
            val result = linkedSetOf<String>()

            fun visit(id: String) {
                if (!result.add(id)) return
                childrenByParent[id].orEmpty().forEach { child -> visit(child.id) }
            }

            visit(rootId)
            return result
        }

        companion object {
            fun withHierarchy(): InMemoryDesktopWorkspaceRepository =
                InMemoryDesktopWorkspaceRepository().apply {
                    contexts +=
                        SharedContextSummary(
                            id = "project",
                            name = "Project",
                            description = "Parent branch",
                            parentId = "core",
                            status = SharedContextStatus.InProgress,
                            defaultView = SharedContextView.Backlog,
                            score = 10,
                            isCompleted = false,
                        )
                    contexts +=
                        SharedContextSummary(
                            id = "child",
                            name = "Child",
                            description = "Nested branch",
                            parentId = "project",
                            status = SharedContextStatus.Planning,
                            defaultView = SharedContextView.Backlog,
                            score = 4,
                            isCompleted = false,
                        )
                    backlogItems +=
                        SharedBacklogItem(
                            id = "project-item",
                            contextId = "project",
                            title = "Project backlog",
                            details = "Parent branch item",
                            kind = SharedBacklogItemKind.Task,
                            priority = SharedBacklogPriority.Medium,
                            isDone = false,
                        )
                    backlogItems +=
                        SharedBacklogItem(
                            id = "child-item",
                            contextId = "child",
                            title = "Child backlog",
                            details = "Nested branch item",
                            kind = SharedBacklogItemKind.Task,
                            priority = SharedBacklogPriority.High,
                            isDone = false,
                        )
                }
        }
    }
}

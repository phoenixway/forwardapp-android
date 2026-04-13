package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import kotlinx.serialization.json.Json
import java.util.UUID

class FileBasedDesktopWorkspaceRepository(
    private val fileStore: DesktopWorkspaceFileStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : DesktopWorkspaceRepository {
    private fun readSnapshot(): DesktopWorkspaceSnapshot =
        json.decodeFromString<DesktopWorkspaceSnapshot>(fileStore.readSnapshot())

    override suspend fun getContexts(): List<SharedContextSummary> = readSnapshot().contexts

    override suspend fun createContext(
        parentId: String?,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
    ): SharedContextSummary? {
        val snapshot = readSnapshot()
        val newContext =
            SharedContextSummary(
                id = "context-${UUID.randomUUID()}",
                name = name,
                description = description,
                parentId = parentId,
                status = status,
                defaultView = defaultView,
                score = 0,
                isCompleted = status == SharedContextStatus.Completed,
            )
        val updatedSnapshot = snapshot.copy(contexts = snapshot.contexts + newContext)
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return newContext
    }

    override suspend fun updateContext(
        contextId: String,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
    ): SharedContextSummary? {
        val snapshot = readSnapshot()
        val updatedContext =
            snapshot.contexts.firstOrNull { context -> context.id == contextId }?.copy(
                name = name,
                description = description,
                status = status,
                defaultView = defaultView,
                isCompleted = status == SharedContextStatus.Completed,
            ) ?: return null
        val updatedSnapshot =
            snapshot.copy(
                contexts =
                    snapshot.contexts.map { context ->
                        if (context.id == contextId) {
                            context.copy(
                                name = name,
                                description = description,
                                status = status,
                                defaultView = defaultView,
                                isCompleted = status == SharedContextStatus.Completed,
                            )
                        } else {
                            context
                        }
                    },
            )
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return updatedContext
    }

    override suspend fun deleteContext(contextId: String): Boolean {
        val snapshot = readSnapshot()
        val contextIdsToDelete = collectContextIdsToDelete(contextId, snapshot.contexts)
        if (contextIdsToDelete.isEmpty()) {
            return false
        }
        val updatedSnapshot =
            snapshot.copy(
                contexts = snapshot.contexts.filterNot { context -> context.id in contextIdsToDelete },
                backlogItems = snapshot.backlogItems.filterNot { item -> item.contextId in contextIdsToDelete },
            )
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return true
    }

    override suspend fun getBacklogItems(contextId: String): List<SharedBacklogItem> =
        readSnapshot().backlogItems.filter { item -> item.contextId == contextId }

    override suspend fun createBacklogItem(
        contextId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem? {
        val snapshot = readSnapshot()
        val newItem =
            SharedBacklogItem(
                id = "desktop-${UUID.randomUUID()}",
                contextId = contextId,
                title = title,
                details = details,
                kind = SharedBacklogItemKind.Task,
                priority = priority,
                isDone = false,
            )
        val updatedSnapshot = snapshot.copy(backlogItems = snapshot.backlogItems + newItem)
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return newItem
    }

    override suspend fun updateBacklogItemDone(
        itemId: String,
        isDone: Boolean,
    ): SharedBacklogItem? {
        val snapshot = readSnapshot()
        val updatedItem =
            snapshot.backlogItems.firstOrNull { item -> item.id == itemId }?.copy(isDone = isDone)
                ?: return null
        val updatedSnapshot =
            snapshot.copy(
                backlogItems =
                    snapshot.backlogItems.map { item ->
                        if (item.id == itemId) {
                            item.copy(isDone = isDone)
                        } else {
                            item
                        }
                    },
            )
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return updatedItem
    }

    override suspend fun updateBacklogItemContent(
        itemId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem? {
        val snapshot = readSnapshot()
        val updatedItem =
            snapshot.backlogItems.firstOrNull { item -> item.id == itemId }?.copy(
                title = title,
                details = details,
                priority = priority,
            ) ?: return null
        val updatedSnapshot =
            snapshot.copy(
                backlogItems =
                    snapshot.backlogItems.map { item ->
                        if (item.id == itemId) {
                            item.copy(
                                title = title,
                                details = details,
                                priority = priority,
                            )
                        } else {
                            item
                        }
                    },
            )
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return updatedItem
    }

    override suspend fun deleteBacklogItem(itemId: String): Boolean {
        val snapshot = readSnapshot()
        val updatedItems = snapshot.backlogItems.filterNot { item -> item.id == itemId }
        if (updatedItems.size == snapshot.backlogItems.size) {
            return false
        }
        val updatedSnapshot = snapshot.copy(backlogItems = updatedItems)
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return true
    }

    private fun collectContextIdsToDelete(
        rootContextId: String,
        contexts: List<SharedContextSummary>,
    ): Set<String> {
        val childrenByParentId = contexts.groupBy { context -> context.parentId }
        val result = linkedSetOf<String>()

        fun visit(contextId: String) {
            if (!result.add(contextId)) {
                return
            }
            childrenByParentId[contextId].orEmpty().forEach { child ->
                visit(child.id)
            }
        }

        if (contexts.none { context -> context.id == rootContextId }) {
            return emptySet()
        }
        visit(rootContextId)
        return result
    }
}

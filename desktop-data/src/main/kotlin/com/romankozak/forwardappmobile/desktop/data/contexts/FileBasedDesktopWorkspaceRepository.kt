package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import java.time.Clock
import kotlinx.serialization.json.Json
import java.util.UUID

class FileBasedDesktopWorkspaceRepository(
    private val fileStore: DesktopWorkspaceFileStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val clock: Clock = Clock.systemUTC(),
) : DesktopWorkspaceRepository {
    private fun readSnapshot(): DesktopWorkspaceSnapshot =
        json.decodeFromString<DesktopWorkspaceSnapshot>(fileStore.readSnapshot())

    override suspend fun getContexts(): List<SharedContextSummary> =
        readSnapshot().contexts.filterNot { context -> context.isDeleted }

    override suspend fun createContext(
        parentId: String?,
        name: String,
        description: String?,
        status: SharedContextStatus,
        defaultView: SharedContextView,
        enabledCapabilityIds: List<String>,
        experimentalCapabilityIds: List<String>,
    ): SharedContextSummary? {
        val snapshot = readSnapshot()
        val now = clock.millis()
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
                enabledCapabilityIds = enabledCapabilityIds.withDefaultCapability(defaultView),
                experimentalCapabilityIds = experimentalCapabilityIds.normalizedCapabilityIds(),
                sync = SharedSyncMetadata(createdAt = now, updatedAt = now, version = 1),
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
        enabledCapabilityIds: List<String>,
        experimentalCapabilityIds: List<String>,
    ): SharedContextSummary? {
        val snapshot = readSnapshot()
        val now = clock.millis()
        val updatedContext =
            snapshot.contexts.firstOrNull { context -> context.id == contextId }?.copy(
                name = name,
                description = description,
                status = status,
                defaultView = defaultView,
                isCompleted = status == SharedContextStatus.Completed,
                enabledCapabilityIds = enabledCapabilityIds.withDefaultCapability(defaultView),
                experimentalCapabilityIds = experimentalCapabilityIds.normalizedCapabilityIds(),
                sync = snapshot.contexts.first { context -> context.id == contextId }.sync.nextVersion(now),
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
                                enabledCapabilityIds = enabledCapabilityIds.withDefaultCapability(defaultView),
                                experimentalCapabilityIds = experimentalCapabilityIds.normalizedCapabilityIds(),
                                sync = context.sync.nextVersion(now),
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
        val now = clock.millis()
        val contextIdsToDelete = collectContextIdsToDelete(contextId, snapshot.contexts.filterNot { context -> context.isDeleted })
        if (contextIdsToDelete.isEmpty()) {
            return false
        }
        val updatedSnapshot =
            snapshot.copy(
                contexts =
                    snapshot.contexts.map { context ->
                        if (context.id in contextIdsToDelete) {
                            context.copy(isDeleted = true, sync = context.sync.nextVersion(now))
                        } else {
                            context
                        }
                    },
                backlogItems =
                    snapshot.backlogItems.map { item ->
                        if (item.contextId in contextIdsToDelete) {
                            item.copy(isDeleted = true, sync = item.sync.nextVersion(now))
                        } else {
                            item
                        }
                    },
            )
        fileStore.writeSnapshot(json.encodeToString(DesktopWorkspaceSnapshot.serializer(), updatedSnapshot))
        return true
    }

    override suspend fun getBacklogItems(contextId: String): List<SharedBacklogItem> =
        readSnapshot().backlogItems.filter { item -> item.contextId == contextId && !item.isDeleted }

    override suspend fun getDayPlans(): List<SharedDayPlan> = readSnapshot().dayPlans

    override suspend fun getDayTasks(): List<SharedDayTask> =
        readSnapshot().dayTasks.filterNot { task -> task.isDeleted }

    override suspend fun createBacklogItem(
        contextId: String,
        title: String,
        details: String?,
        priority: SharedBacklogPriority,
    ): SharedBacklogItem? {
        val snapshot = readSnapshot()
        val now = clock.millis()
        val newItem =
            SharedBacklogItem(
                id = "desktop-${UUID.randomUUID()}",
                contextId = contextId,
                title = title,
                details = details,
                kind = SharedBacklogItemKind.Task,
                priority = priority,
                isDone = false,
                sync = SharedSyncMetadata(createdAt = now, updatedAt = now, version = 1),
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
        val now = clock.millis()
        val updatedItem =
            snapshot.backlogItems.firstOrNull { item -> item.id == itemId }?.let { item ->
                item.copy(isDone = isDone, sync = item.sync.nextVersion(now))
            }
                ?: return null
        val updatedSnapshot =
            snapshot.copy(
                backlogItems =
                    snapshot.backlogItems.map { item ->
                        if (item.id == itemId) {
                            item.copy(isDone = isDone, sync = item.sync.nextVersion(now))
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
        val now = clock.millis()
        val updatedItem =
            snapshot.backlogItems.firstOrNull { item -> item.id == itemId }?.let { item ->
                item.copy(
                    title = title,
                    details = details,
                    priority = priority,
                    sync = item.sync.nextVersion(now),
                )
            } ?: return null
        val updatedSnapshot =
            snapshot.copy(
                backlogItems =
                    snapshot.backlogItems.map { item ->
                        if (item.id == itemId) {
                            item.copy(
                                title = title,
                                details = details,
                                priority = priority,
                                sync = item.sync.nextVersion(now),
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
        val now = clock.millis()
        if (snapshot.backlogItems.none { item -> item.id == itemId && !item.isDeleted }) {
            return false
        }
        val updatedSnapshot =
            snapshot.copy(
                backlogItems =
                    snapshot.backlogItems.map { item ->
                        if (item.id == itemId) {
                            item.copy(isDeleted = true, sync = item.sync.nextVersion(now))
                        } else {
                            item
                        }
                    },
            )
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

private fun SharedSyncMetadata.nextVersion(now: Long): SharedSyncMetadata =
    copy(
        createdAt = createdAt.takeIf { it > 0L } ?: now,
        updatedAt = now,
        version = version + 1L,
    )

private fun List<String>.withDefaultCapability(defaultView: SharedContextView): List<String> =
    (
        this +
            "dashboard" +
            SharedContextCapabilityCatalog.capabilityIdFor(defaultView) +
            if (defaultView == SharedContextView.Connections) listOf("connections") else emptyList()
    ).normalizedCapabilityIds()

private fun List<String>.normalizedCapabilityIds(): List<String> =
    SharedContextCapabilityCatalog.normalizeCapabilityIds(this)

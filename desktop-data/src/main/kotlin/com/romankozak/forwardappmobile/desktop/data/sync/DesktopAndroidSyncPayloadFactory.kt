package com.romankozak.forwardappmobile.desktop.data.sync

import com.romankozak.forwardappmobile.desktop.data.contexts.DesktopWorkspaceFileStore
import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DesktopAndroidSyncPayloadFactory(
    private val fileStore: DesktopWorkspaceFileStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun createDeltaBackupJsonString(since: Long?): String? {
        val snapshot = json.decodeFromString(DesktopWorkspaceSnapshot.serializer(), fileStore.readSnapshot())
        val changedContexts = snapshot.contexts.filter { context -> context.sync.changedAfter(since) }
        val changedBacklogItems = snapshot.backlogItems.filter { item -> item.sync.changedAfter(since) }
        if (changedContexts.isEmpty() && changedBacklogItems.isEmpty()) {
            return null
        }
        val exportedAt = System.currentTimeMillis()
        val bundle =
            buildJsonObject {
                put("snapshotVersion", 2)
                put("exportedAt", exportedAt)
                put("contexts", JsonArray(changedContexts.mapIndexed { index, context -> context.toSnapshotJson(index, exportedAt) }))
                put("goals", JsonArray(changedBacklogItems.map { item -> item.toGoalSnapshotJson(exportedAt) }))
                put("backlogItems", JsonArray(changedBacklogItems.mapIndexed { index, item -> item.toBacklogItemSnapshotJson(index, exportedAt) }))
                put("backlogOrders", JsonArray(changedBacklogItems.mapIndexed { index, item -> item.toBacklogOrderSnapshotJson(index, exportedAt) }))
            }
        val backup =
            buildJsonObject {
                put("backupSchemaVersion", 2)
                put("exportedAt", exportedAt)
                put("database", JsonNull)
                put("settings", JsonNull)
                put("snapshotBundle", bundle)
            }
        return json.encodeToString(JsonObject.serializer(), backup)
    }
}

private fun SharedSyncMetadata.changedAfter(since: Long?): Boolean =
    updatedAt > 0L && (since == null || updatedAt > since)

private fun SharedContextSummary.toSnapshotJson(
    index: Int,
    exportedAt: Long,
): JsonObject {
    val metadata = sync.withFallback(exportedAt)
    return buildJsonObject {
        put("id", id)
        put("name", name)
        put("parentId", parentId?.let(::JsonPrimitive) ?: JsonNull)
        put("description", description?.let(::JsonPrimitive) ?: JsonNull)
        put("createdAt", metadata.createdAt)
        put("updatedAt", metadata.updatedAt)
        put("isExpanded", true)
        put("isDeleted", isDeleted)
        put("version", metadata.version)
        put("tags", JsonArray(emptyList()))
        put("relatedLinks", JsonArray(emptyList()))
        put("order", index)
        put("isAttachmentsExpanded", false)
        put("defaultViewModeName", defaultView.toAndroidViewModeName())
        put("isCompleted", isCompleted)
        put("isContextManagementEnabled", true)
        put("contextStatus", status.toAndroidContextStatus())
        put("contextStatusText", JsonNull)
        put("contextLogLevel", "NORMAL")
        put("totalTimeSpentMinutes", 0)
        put("valueImportance", 0)
        put("valueImpact", 0)
        put("effort", 0)
        put("cost", 0)
        put("risk", 0)
        put("weightEffort", 1.0f)
        put("weightCost", 1.0f)
        put("weightRisk", 1.0f)
        put("rawScore", 0.0)
        put("displayScore", score.toDouble())
        put("scoringStatus", "NOT_ASSESSED")
        put("showCheckboxes", false)
        put("roleCode", JsonNull)
    }
}

private fun SharedBacklogItem.toGoalSnapshotJson(exportedAt: Long): JsonObject {
    val metadata = sync.withFallback(exportedAt)
    return buildJsonObject {
        put("id", sourceEntityId ?: goalId)
        put("text", title)
        put("description", details?.let(::JsonPrimitive) ?: JsonNull)
        put("isCompleted", isDone)
        put("goalStatus", if (isDone) "DONE" else "ACTIVE")
        put("createdAt", metadata.createdAt)
        put("updatedAt", metadata.updatedAt)
        put("version", metadata.version)
        put("isDeleted", isDeleted)
        put("tags", JsonArray(emptyList()))
        put("scoringStatus", "NOT_ASSESSED")
        put("valueImportance", 0)
        put("valueImpact", 0)
        put("effort", 0)
        put("cost", 0)
        put("risk", 0)
        put("weightEffort", 1.0f)
        put("weightCost", 1.0f)
        put("weightRisk", 1.0f)
        put("rawScore", 0.0)
        put("displayScore", 0.0)
        put("relativeSize", 0)
        put("parentValueImportance", JsonNull)
        put("impactOnParentGoal", JsonNull)
        put("timeCost", JsonNull)
        put("financialCost", JsonNull)
    }
}

private fun SharedBacklogItem.toBacklogItemSnapshotJson(
    index: Int,
    exportedAt: Long,
): JsonObject {
    val metadata = sync.withFallback(exportedAt)
    return buildJsonObject {
        put("id", id)
        put("contextId", contextId)
        put("itemType", "GOAL")
        put("entityId", sourceEntityId ?: goalId)
        put("order", index.toLong())
        put("updatedAt", metadata.updatedAt)
        put("version", metadata.version)
        put("isDeleted", isDeleted)
    }
}

private fun SharedBacklogItem.toBacklogOrderSnapshotJson(
    index: Int,
    exportedAt: Long,
): JsonObject {
    val metadata = sync.withFallback(exportedAt)
    return buildJsonObject {
        put("id", "desktop-order-$id")
        put("listId", contextId)
        put("itemId", id)
        put("order", index.toLong())
        put("orderVersion", metadata.version)
        put("updatedAt", metadata.updatedAt)
        put("isDeleted", isDeleted)
    }
}

private val SharedBacklogItem.goalId: String
    get() = "desktop-goal-$id"

private fun SharedSyncMetadata.withFallback(exportedAt: Long): SharedSyncMetadata =
    SharedSyncMetadata(
        createdAt = createdAt.takeIf { it > 0L } ?: exportedAt,
        updatedAt = updatedAt.takeIf { it > 0L } ?: exportedAt,
        version = version.takeIf { it > 0L } ?: 1L,
    )

private fun SharedContextStatus.toAndroidContextStatus(): String =
    when (this) {
        SharedContextStatus.NoPlan -> "NO_PLAN"
        SharedContextStatus.Planning -> "PLANNING"
        SharedContextStatus.InProgress -> "IN_PROGRESS"
        SharedContextStatus.Completed -> "COMPLETED"
        SharedContextStatus.OnHold -> "ON_HOLD"
        SharedContextStatus.Paused -> "PAUSED"
    }

private fun SharedContextView.toAndroidViewModeName(): String =
    when (this) {
        SharedContextView.Backlog -> "BACKLOG"
        SharedContextView.Inbox -> "INBOX"
        SharedContextView.Connections -> "CONNECTIONS"
        SharedContextView.Dashboard -> "DASHBOARD"
        SharedContextView.Direction -> "DIRECTION"
        SharedContextView.Log -> "LOG"
        SharedContextView.JournalLog -> "JOURNAL_LOG"
        SharedContextView.Artifact -> "ARTIFACT"
        SharedContextView.KeyProblems -> "KEY_PROBLEMS"
    }

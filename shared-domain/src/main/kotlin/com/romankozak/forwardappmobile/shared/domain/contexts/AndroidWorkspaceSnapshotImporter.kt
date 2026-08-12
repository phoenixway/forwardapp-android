package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.ResolvedWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

class AndroidWorkspaceSnapshotImporter(
    private val json: Json,
) {
    fun parse(snapshotText: String): ResolvedWorkspaceSnapshot? {
        val root = runCatching { json.parseToJsonElement(snapshotText).jsonObject }.getOrNull() ?: return null

        val wrappedSnapshotBundle = root.fieldObject("snapshotBundle", "e")
        if (wrappedSnapshotBundle != null) {
            return resolveSnapshotBundle(wrappedSnapshotBundle, WorkspaceSnapshotFormat.AndroidSnapshotBundleV2)
        }

        val wrappedLegacyDatabase = root.fieldObject("database", "c")
        if (wrappedLegacyDatabase != null) {
            return resolveLegacyDatabase(wrappedLegacyDatabase, WorkspaceSnapshotFormat.AndroidLegacyDatabase)
        }

        if (root.looksLikeSnapshotBundle()) {
            return resolveSnapshotBundle(root, WorkspaceSnapshotFormat.AndroidSnapshotBundleV2)
        }

        if (root.looksLikeLegacyDatabase()) {
            return resolveLegacyDatabase(root, WorkspaceSnapshotFormat.AndroidLegacyDatabase)
        }

        return null
    }

    private fun resolveSnapshotBundle(
        source: JsonObject,
        format: WorkspaceSnapshotFormat,
    ): ResolvedWorkspaceSnapshot {
        val contexts = parseSnapshotContexts(source)
        val contextIds = contexts.mapTo(linkedSetOf()) { it.id }
        val itemResolvers = buildSnapshotItemResolvers(source)
        val backlogOrders = parseBacklogOrders(source.fieldArray("backlogOrders"))
        val backlogItems =
            source.fieldArray("backlogItems")
                .mapNotNull { item ->
                    item.toSnapshotBacklogItem(itemResolvers = itemResolvers, validContextIds = contextIds)
                }.sortedWith(compareBy({ backlogOrders[it.contextId to it.id] ?: Long.MAX_VALUE }, { it.title.lowercase() }))
        val dayPlans = parseDayPlans(source)
        val dayPlanIds = dayPlans.mapTo(hashSetOf()) { plan -> plan.id }
        val dayTasks = parseDayTasks(source = source, validDayPlanIds = dayPlanIds, validContextIds = contextIds)

        return ResolvedWorkspaceSnapshot(
            snapshot =
                DesktopWorkspaceSnapshot(
                    contexts = contexts,
                    backlogItems = backlogItems,
                    dayPlans = dayPlans,
                    dayTasks = dayTasks,
                ),
            format = format,
        )
    }

    private fun resolveLegacyDatabase(
        source: JsonObject,
        format: WorkspaceSnapshotFormat,
    ): ResolvedWorkspaceSnapshot {
        val contexts = parseLegacyContexts(source)
        val contextIds = contexts.mapTo(linkedSetOf()) { it.id }
        val itemResolvers = buildLegacyItemResolvers(source)
        val backlogOrders = parseBacklogOrders(source.fieldArray("backlogOrders", "order"))
        val backlogItems =
            source.fieldArray("listItems", "backlogItems_legacy_c")
                .mapNotNull { item ->
                    item.toLegacyBacklogItem(itemResolvers = itemResolvers, validContextIds = contextIds)
                }.sortedWith(compareBy({ backlogOrders[it.contextId to it.id] ?: Long.MAX_VALUE }, { it.title.lowercase() }))
        val dayPlans = parseDayPlans(source)
        val dayPlanIds = dayPlans.mapTo(hashSetOf()) { plan -> plan.id }
        val dayTasks = parseDayTasks(source = source, validDayPlanIds = dayPlanIds, validContextIds = contextIds)

        return ResolvedWorkspaceSnapshot(
            snapshot =
                DesktopWorkspaceSnapshot(
                    contexts = contexts,
                    backlogItems = backlogItems,
                    dayPlans = dayPlans,
                    dayTasks = dayTasks,
                ),
            format = format,
        )
    }

    private fun parseSnapshotContexts(source: JsonObject): List<SharedContextSummary> {
        val rawContexts =
            source.fieldArray("contexts")
                .mapNotNull { entry ->
                    ParsedContext(
                        id = entry.string("id") ?: return@mapNotNull null,
                        name = entry.string("name").orEmpty(),
                        description = entry.string("description"),
                        parentId = entry.string("parentId"),
                        status = entry.string("contextStatus").toSharedContextStatus(),
                        defaultView = entry.string("defaultViewModeName").toSharedContextView(),
                        score = (entry.double("displayScore") ?: entry.double("rawScore") ?: 0.0).roundToInt(),
                        isCompleted = entry.boolean("isCompleted") ?: false,
                        isDeleted = entry.boolean("isDeleted") ?: false,
                        sortOrder = entry.long("order") ?: Long.MAX_VALUE,
                        sync = entry.syncMetadata(),
                    )
                }.sortedWith(compareBy<ParsedContext>({ it.sortOrder }, { it.name.lowercase() }))

        return normalizeContexts(rawContexts)
    }

    private fun parseLegacyContexts(source: JsonObject): List<SharedContextSummary> {
        val rawContexts =
            source.fieldArray("projects", "goalLists")
                .mapNotNull { entry ->
                    ParsedContext(
                        id = entry.string("id") ?: return@mapNotNull null,
                        name = entry.string("name").orEmpty(),
                        description = entry.string("description"),
                        parentId = entry.string("parentId"),
                        status = entry.string("contextStatus").toSharedContextStatus(),
                        defaultView = entry.string("defaultViewModeName").toSharedContextView(),
                        score = (entry.double("displayScore") ?: entry.double("rawScore") ?: 0.0).roundToInt(),
                        isCompleted = entry.boolean("isCompleted") ?: false,
                        isDeleted = entry.boolean("isDeleted") ?: false,
                        sortOrder = entry.long("order") ?: Long.MAX_VALUE,
                        sync = entry.syncMetadata(),
                    )
                }.sortedWith(compareBy<ParsedContext>({ it.sortOrder }, { it.name.lowercase() }))

        return normalizeContexts(rawContexts)
    }

    private fun normalizeContexts(rawContexts: List<ParsedContext>): List<SharedContextSummary> {
        val validIds = rawContexts.filterNot { context -> context.isDeleted }.mapTo(hashSetOf()) { it.id }
        return rawContexts.map { context ->
            SharedContextSummary(
                id = context.id,
                name = context.name.ifBlank { "Untitled Context" },
                description = context.description,
                parentId = context.parentId.takeIf { parentId -> parentId in validIds },
                status = context.status,
                defaultView = context.defaultView,
                score = context.score,
                isCompleted = context.isCompleted,
                isDeleted = context.isDeleted,
                sync = context.sync,
            )
        }
    }

    private fun buildSnapshotItemResolvers(source: JsonObject): AndroidItemResolvers =
        AndroidItemResolvers(
            goals =
                source.fieldArray("goals")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(
                            title = item.string("text"),
                            details = item.string("description"),
                            isDone = item.boolean("isCompleted"),
                            sync = item.syncMetadata(),
                        )
                    },
            notes =
                source.fieldArray("notes")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("title"), details = item.string("content"))
                    },
            documents =
                source.fieldArray("documents")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name"), details = item.string("content"))
                    },
            checklists =
                source.fieldArray("checklists")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name"))
                    },
            scripts =
                source.fieldArray("scripts")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name"), details = item.string("content"))
                    },
            links =
                source.fieldArray("linkItemEntities")
                    .associateByKey(idField = "id") { item ->
                        val linkData = item.fieldObject("linkData")
                        NamedItem(
                            title = linkData?.string("displayName") ?: linkData?.string("target"),
                            details = linkData?.string("target"),
                        )
                    },
            inbox =
                source.fieldArray("inbox")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("text"))
                    },
            contexts =
                source.fieldArray("contexts")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name"), details = item.string("description"))
                    },
        )

    private fun buildLegacyItemResolvers(source: JsonObject): AndroidItemResolvers =
        AndroidItemResolvers(
            goals =
                source.fieldArray("goals")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(
                            title = item.string("text"),
                            details = item.string("description"),
                            isDone = item.boolean("completed") ?: item.boolean("isCompleted"),
                        )
                    },
            notes =
                source.fieldArray("legacyNotes", "notes")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("title"), details = item.string("content"))
                    },
            documents =
                source.fieldArray("documents", "customLists")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name"), details = item.string("content"))
                    },
            checklists =
                source.fieldArray("checklists", "g")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name", "title"))
                    },
            scripts =
                source.fieldArray("scripts")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(
                            title = item.string("name"),
                            details = item.string("description") ?: item.string("content"),
                        )
                    },
            links =
                source.fieldArray("linkItemEntities", "k")
                    .associateByKey(idField = "id") { item ->
                        val linkData = item.fieldObject("linkData")
                        NamedItem(
                            title = linkData?.string("displayName") ?: linkData?.string("target"),
                            details = linkData?.string("target"),
                        )
                    },
            inbox =
                source.fieldArray("inboxRecords", "l")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("text"))
                    },
            contexts =
                source.fieldArray("projects", "goalLists")
                    .associateByKey(idField = "id") { item ->
                        NamedItem(title = item.string("name"), details = item.string("description"))
                    },
        )

    private fun parseBacklogOrders(entries: List<JsonObject>): Map<Pair<String, String>, Long> =
        entries
            .filterNot { entry -> entry.boolean("isDeleted") == true }
            .associate { entry ->
                val listId = entry.string("listId", "contextId", "projectId").orEmpty()
                val itemId = entry.string("itemId").orEmpty()
                val order = entry.long("order") ?: Long.MAX_VALUE
                (listId to itemId) to order
            }

    private fun parseDayPlans(source: JsonObject): List<SharedDayPlan> =
        source.fieldArray("dayPlans")
            .mapNotNull { entry ->
                if (entry.boolean("isDeleted") == true) {
                    null
                } else {
                    SharedDayPlan(
                        id = entry.string("id") ?: return@mapNotNull null,
                        date = entry.long("date") ?: return@mapNotNull null,
                        name = entry.string("name"),
                        status = entry.string("status").orEmpty(),
                        sync = entry.syncMetadata(),
                    )
                }
            }.sortedByDescending { plan -> plan.date }

    private fun parseDayTasks(
        source: JsonObject,
        validDayPlanIds: Set<String>,
        validContextIds: Set<String>,
    ): List<SharedDayTask> =
        source.fieldArray("dayTasks")
            .mapNotNull { entry ->
                if (entry.boolean("isDeleted") == true) {
                    null
                } else {
                    val dayPlanId = entry.string("dayPlanId") ?: return@mapNotNull null
                    if (dayPlanId !in validDayPlanIds) return@mapNotNull null
                    val projectId = entry.string("projectId")?.takeIf { contextId -> contextId in validContextIds }
                    val linkedProjectIds =
                        entry.stringArray("linkedProjectIds")
                            .filter { contextId -> contextId in validContextIds }
                            .distinct()
                    SharedDayTask(
                        id = entry.string("id") ?: return@mapNotNull null,
                        dayPlanId = dayPlanId,
                        title = entry.string("title").orEmpty(),
                        description = entry.string("description"),
                        projectId = projectId,
                        linkedProjectIds = linkedProjectIds,
                        isDone = entry.boolean("completed") ?: false,
                        priority = entry.string("priority").orEmpty(),
                        order = entry.long("order") ?: Long.MAX_VALUE,
                        scheduledTime = entry.long("scheduledTime"),
                        dueTime = entry.long("dueTime"),
                        sync = entry.syncMetadata(),
                    )
                }
            }.sortedWith(compareBy({ task -> task.dayPlanId }, { task -> task.order }, { task -> task.title.lowercase() }))

    private fun JsonObject.toSnapshotBacklogItem(
        itemResolvers: AndroidItemResolvers,
        validContextIds: Set<String>,
    ): SharedBacklogItem? {
        val itemId = string("id") ?: return null
        val contextId = string("contextId") ?: return null
        val isDeleted = boolean("isDeleted") ?: false
        if (!isDeleted && contextId !in validContextIds) {
            return null
        }
        val itemType = string("itemType").orEmpty()
        val entityId = string("entityId").orEmpty()
        val resolved = itemResolvers.resolve(itemType = itemType, entityId = entityId)
        return SharedBacklogItem(
            id = itemId,
            contextId = contextId,
            title = resolved.title.orEmpty(),
            details = resolved.details,
            kind = itemType.toSharedBacklogItemKind(),
            priority = itemType.toSharedBacklogPriority(),
            isDone = resolved.isDone ?: false,
            sourceEntityId = entityId.takeIf { it.isNotBlank() },
            isDeleted = isDeleted,
            sync = syncMetadata().mergeWith(resolved.sync),
        )
    }

    private fun JsonObject.toLegacyBacklogItem(
        itemResolvers: AndroidItemResolvers,
        validContextIds: Set<String>,
    ): SharedBacklogItem? {
        val itemId = string("id") ?: return null
        val contextId = string("contextId", "listId", "projectId") ?: return null
        val isDeleted = boolean("isDeleted") ?: false
        if (!isDeleted && contextId !in validContextIds) {
            return null
        }
        val itemType = string("itemType").orEmpty()
        val entityId = string("entityId").orEmpty()
        val resolved = itemResolvers.resolve(itemType = itemType, entityId = entityId)
        return SharedBacklogItem(
            id = itemId,
            contextId = contextId,
            title = resolved.title.orEmpty(),
            details = resolved.details,
            kind = itemType.toSharedBacklogItemKind(),
            priority = itemType.toSharedBacklogPriority(),
            isDone = resolved.isDone ?: false,
            sourceEntityId = entityId.takeIf { it.isNotBlank() },
            isDeleted = isDeleted,
            sync = syncMetadata().mergeWith(resolved.sync),
        )
    }

    private fun AndroidItemResolvers.resolve(
        itemType: String,
        entityId: String,
    ): NamedItem {
        val normalizedType = itemType.uppercase()
        val namedItem =
            when (normalizedType) {
                "GOAL" -> goals[entityId]
                "NOTE" -> notes[entityId]
                "NOTE_DOCUMENT" -> documents[entityId]
                "CHECKLIST" -> checklists[entityId]
                "SCRIPT" -> scripts[entityId]
                "LINK_ITEM", "LINK" -> links[entityId]
                "INBOX" -> inbox[entityId]
                "SUBLIST", "CONTEXT" -> contexts[entityId]
                "MUSIC_NOTE" -> documents[entityId]
                else -> null
            }

        if (namedItem != null) {
            return namedItem.withFallback(type = normalizedType, entityId = entityId)
        }

        val fallbackTitle = buildString {
            append(normalizedType.ifBlank { "ITEM" })
            if (entityId.isNotBlank()) {
                append(": ")
                append(entityId)
            }
        }
        return NamedItem(title = fallbackTitle, details = null, isDone = false)
    }
}

private data class ParsedContext(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val status: SharedContextStatus,
    val defaultView: SharedContextView,
    val score: Int,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val sortOrder: Long,
    val sync: SharedSyncMetadata,
)

private data class AndroidItemResolvers(
    val goals: Map<String, NamedItem>,
    val notes: Map<String, NamedItem>,
    val documents: Map<String, NamedItem>,
    val checklists: Map<String, NamedItem>,
    val scripts: Map<String, NamedItem>,
    val links: Map<String, NamedItem>,
    val inbox: Map<String, NamedItem>,
    val contexts: Map<String, NamedItem>,
)

private data class NamedItem(
    val title: String?,
    val details: String? = null,
    val isDone: Boolean? = null,
    val sync: SharedSyncMetadata = SharedSyncMetadata(),
) {
    fun withFallback(
        type: String,
        entityId: String,
    ): NamedItem =
        copy(
            title =
                title?.takeIf { value -> value.isNotBlank() }
                    ?: "$type: ${entityId.ifBlank { "unknown" }}",
            isDone = isDone ?: false,
        )
}

private fun JsonObject.syncMetadata(): SharedSyncMetadata {
    val createdAt = long("createdAt") ?: 0L
    val updatedAt = long("updatedAt") ?: createdAt
    return SharedSyncMetadata(
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = long("version", "orderVersion") ?: 0L,
    )
}

private fun SharedSyncMetadata.mergeWith(other: SharedSyncMetadata): SharedSyncMetadata =
    if (other.updatedAt > updatedAt || (other.updatedAt == updatedAt && other.version > version)) {
        other
    } else {
        this
    }

private fun JsonObject.looksLikeSnapshotBundle(): Boolean = containsKey("contexts") && containsKey("backlogItems")

private fun JsonObject.looksLikeLegacyDatabase(): Boolean =
    containsKey("projects") ||
        containsKey("goalLists") ||
        containsKey("listItems") ||
        containsKey("backlogItems_legacy_c")

private fun JsonObject.fieldArray(vararg keys: String): List<JsonObject> =
    keys
        .asSequence()
        .mapNotNull { key -> this[key] as? JsonArray }
        .firstOrNull()
        ?.mapNotNull { element -> element as? JsonObject }
        .orEmpty()

private fun JsonObject.fieldObject(vararg keys: String): JsonObject? =
    keys
        .asSequence()
        .mapNotNull { key -> this[key] as? JsonObject }
        .firstOrNull()

private fun JsonObject.string(vararg keys: String): String? =
    keys
        .asSequence()
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.contentOrNull }
        .firstOrNull()

private fun JsonObject.stringArray(vararg keys: String): List<String> =
    keys
        .asSequence()
        .mapNotNull { key -> this[key] as? JsonArray }
        .firstOrNull()
        ?.mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull }
        .orEmpty()

private fun JsonObject.boolean(vararg keys: String): Boolean? =
    keys
        .asSequence()
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.booleanOrNull }
        .firstOrNull()

private fun JsonObject.long(vararg keys: String): Long? =
    keys
        .asSequence()
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.longOrNull }
        .firstOrNull()

private fun JsonObject.double(vararg keys: String): Double? =
    keys
        .asSequence()
        .mapNotNull { key ->
            val primitive = this[key] as? JsonPrimitive ?: return@mapNotNull null
            primitive.doubleOrNull ?: primitive.longOrNull?.toDouble() ?: primitive.intOrNull?.toDouble()
        }.firstOrNull()

private fun List<JsonObject>.associateByKey(
    idField: String,
    valueSelector: (JsonObject) -> NamedItem,
): Map<String, NamedItem> =
    buildMap {
        for (entry in this@associateByKey) {
            if (entry.boolean("isDeleted") == true) {
                continue
            }
            val id = entry.string(idField) ?: continue
            put(id, valueSelector(entry))
        }
    }

private fun String?.toSharedContextStatus(): SharedContextStatus =
    when (this?.uppercase()) {
        "PLANNING" -> SharedContextStatus.Planning
        "IN_PROGRESS", "ACTIVE" -> SharedContextStatus.InProgress
        "COMPLETED", "DONE" -> SharedContextStatus.Completed
        "ON_HOLD" -> SharedContextStatus.OnHold
        "PAUSED" -> SharedContextStatus.Paused
        else -> SharedContextStatus.NoPlan
    }

private fun String?.toSharedContextView(): SharedContextView =
    when (this?.uppercase()) {
        "INBOX" -> SharedContextView.Inbox
        "CONNECTIONS" -> SharedContextView.Connections
        "DASHBOARD" -> SharedContextView.Dashboard
        "DIRECTION" -> SharedContextView.Direction
        "LOG" -> SharedContextView.Log
        "JOURNAL_LOG" -> SharedContextView.JournalLog
        "ARTIFACT" -> SharedContextView.Artifact
        "KEY_PROBLEMS" -> SharedContextView.KeyProblems
        else -> SharedContextView.Backlog
    }

private fun String.toSharedBacklogItemKind(): SharedBacklogItemKind =
    when (uppercase()) {
        "GOAL" -> SharedBacklogItemKind.Goal
        "NOTE", "NOTE_DOCUMENT", "MUSIC_NOTE" -> SharedBacklogItemKind.Note
        "CHECKLIST" -> SharedBacklogItemKind.Checklist
        "LINK_ITEM", "LINK", "SUBLIST", "CONTEXT" -> SharedBacklogItemKind.Link
        "SCRIPT" -> SharedBacklogItemKind.Task
        else -> SharedBacklogItemKind.Task
    }

private fun String.toSharedBacklogPriority(): SharedBacklogPriority =
    when (uppercase()) {
        "GOAL", "SUBLIST", "CONTEXT" -> SharedBacklogPriority.High
        "SCRIPT", "CHECKLIST" -> SharedBacklogPriority.Medium
        else -> SharedBacklogPriority.Low
    }

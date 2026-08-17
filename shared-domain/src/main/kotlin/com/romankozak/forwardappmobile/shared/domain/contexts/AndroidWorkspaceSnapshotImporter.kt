package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.ResolvedWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItemKind
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextCapabilityCatalog
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayFocusItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedRecurringTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
            val snapshotBundle = resolveSnapshotBundle(wrappedSnapshotBundle, WorkspaceSnapshotFormat.AndroidSnapshotBundleV2)
            val legacyDatabase =
                root.fieldObject("database", "c")
                    ?.let { database -> resolveLegacyDatabase(database, WorkspaceSnapshotFormat.AndroidLegacyDatabase) }
            return if (legacyDatabase == null) {
                snapshotBundle
            } else {
                snapshotBundle.withMergedDayMaterial(legacyDatabase)
            }
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
        val dayFocusItems = parseDayFocusItems(source = source, validDayPlanIds = dayPlanIds)
        val dayTasks = parseDayTasks(source = source, validDayPlanIds = dayPlanIds, validContextIds = contextIds)
        val recurringTasks = parseRecurringTasks(source)

        return ResolvedWorkspaceSnapshot(
            snapshot =
                DesktopWorkspaceSnapshot(
                    contexts = contexts,
                    backlogItems = backlogItems,
                    dayPlans = dayPlans,
                    dayFocusItems = dayFocusItems,
                    dayTasks = dayTasks,
                    recurringTasks = recurringTasks,
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
        val dayFocusItems = parseDayFocusItems(source = source, validDayPlanIds = dayPlanIds)
        val dayTasks = parseDayTasks(source = source, validDayPlanIds = dayPlanIds, validContextIds = contextIds)
        val recurringTasks = parseRecurringTasks(source)

        return ResolvedWorkspaceSnapshot(
            snapshot =
                DesktopWorkspaceSnapshot(
                    contexts = contexts,
                    backlogItems = backlogItems,
                    dayPlans = dayPlans,
                    dayFocusItems = dayFocusItems,
                    dayTasks = dayTasks,
                    recurringTasks = recurringTasks,
                ),
            format = format,
        )
    }

    private fun parseSnapshotContexts(source: JsonObject): List<SharedContextSummary> {
        val capabilityConfigs = parseContextCapabilityConfigs(source)
        val rawContexts =
            source.fieldArray("contexts")
                .mapNotNull { entry ->
                    val contextId = entry.string("id") ?: return@mapNotNull null
                    ParsedContext(
                        id = contextId,
                        name = entry.string("name").orEmpty(),
                        description = entry.string("description"),
                        parentId = entry.string("parentId"),
                        status = entry.string("contextStatus").toSharedContextStatus(),
                        defaultView = entry.string("defaultViewModeName").toSharedContextView(),
                        capabilityConfig = capabilityConfigs[contextId],
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
        val capabilityConfigs = parseContextCapabilityConfigs(source)
        val rawContexts =
            source.fieldArray("projects", "goalLists")
                .mapNotNull { entry ->
                    val contextId = entry.string("id") ?: return@mapNotNull null
                    ParsedContext(
                        id = contextId,
                        name = entry.string("name").orEmpty(),
                        description = entry.string("description"),
                        parentId = entry.string("parentId"),
                        status = entry.string("contextStatus").toSharedContextStatus(),
                        defaultView = entry.string("defaultViewModeName").toSharedContextView(),
                        capabilityConfig = capabilityConfigs[contextId],
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
                enabledCapabilityIds = context.capabilityConfig.enabledWithFallback(context.defaultView),
                experimentalCapabilityIds = context.capabilityConfig?.experimentalCapabilityIds.orEmpty(),
                score = context.score,
                isCompleted = context.isCompleted,
                isDeleted = context.isDeleted,
                sync = context.sync,
            )
        }
    }

    private fun parseContextCapabilityConfigs(source: JsonObject): Map<String, ParsedCapabilityConfig> =
        source.fieldArray("contextConfigurations", "contextStructures", "projectStructures")
            .mapNotNull { entry ->
                if (entry.boolean("isDeleted") == true) {
                    return@mapNotNull null
                }
                val contextId = entry.string("contextId", "projectId") ?: return@mapNotNull null
                contextId to
                    ParsedCapabilityConfig(
                        enabledCapabilityIds = entry.enabledLegacyCapabilityIds(),
                        experimentalCapabilityIds = entry.capabilityIdArray("experimentalCapabilityIds"),
                    )
            }.toMap()

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

    private fun parseDayFocusItems(
        source: JsonObject,
        validDayPlanIds: Set<String>,
    ): List<SharedDayFocusItem> =
        source.fieldArray("dayFocusItems")
            .mapNotNull { entry ->
                val dayPlanId = entry.string("dayPlanId") ?: return@mapNotNull null
                val isDeleted = entry.boolean("isDeleted") ?: false
                if (!isDeleted && dayPlanId !in validDayPlanIds) return@mapNotNull null
                SharedDayFocusItem(
                    id = entry.string("id") ?: return@mapNotNull null,
                    dayPlanId = dayPlanId,
                    title = entry.string("title").orEmpty(),
                    notes = entry.string("notes"),
                    type = entry.string("type").orEmpty().ifBlank { "FOCUS" },
                    isEveryday = entry.boolean("isEveryday") ?: false,
                    recurringKey = entry.string("recurringKey"),
                    budgetPercent = entry.long("budgetPercent")?.toInt(),
                    order = entry.long("order") ?: Long.MAX_VALUE,
                    isDeleted = isDeleted,
                    sync = entry.syncMetadata(),
                )
            }.sortedWith(compareBy({ item -> item.dayPlanId }, { item -> item.order }, { item -> item.title.lowercase() }))

    private fun parseDayTasks(
        source: JsonObject,
        validDayPlanIds: Set<String>,
        validContextIds: Set<String>,
    ): List<SharedDayTask> =
        source.fieldArray("dayTasks")
            .mapNotNull { entry ->
                val dayPlanId = entry.string("dayPlanId") ?: return@mapNotNull null
                val isDeleted = entry.boolean("isDeleted") ?: false
                if (!isDeleted && dayPlanId !in validDayPlanIds) return@mapNotNull null
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
                    recurringTaskId = entry.string("recurringTaskId"),
                    taskType = entry.string("taskType"),
                    isDone = entry.boolean("completed") ?: false,
                    priority = entry.string("priority").orEmpty(),
                    order = entry.long("order") ?: Long.MAX_VALUE,
                    scheduledTime = entry.long("scheduledTime"),
                    estimatedDurationMinutes = entry.long("estimatedDurationMinutes"),
                    dueTime = entry.long("dueTime"),
                    points = entry.long("points")?.toInt() ?: 0,
                    isDeleted = isDeleted,
                    sync = entry.syncMetadata(),
                )
            }.sortedWith(compareBy({ task -> task.dayPlanId }, { task -> task.order }, { task -> task.title.lowercase() }))

    private fun parseRecurringTasks(source: JsonObject): List<SharedRecurringTask> =
        source.fieldArray("recurringTasks")
            .mapNotNull { entry ->
                val recurrenceRule = entry.fieldObject("recurrenceRule")
                SharedRecurringTask(
                    id = entry.string("id") ?: return@mapNotNull null,
                    title = entry.string("title").orEmpty(),
                    description = entry.string("description"),
                    goalId = entry.string("goalId"),
                    linkedProjectIds = entry.stringArray("linkedProjectIds").distinct(),
                    duration = entry.long("duration")?.toInt(),
                    priority = entry.string("priority").orEmpty().ifBlank { "MEDIUM" },
                    points = entry.long("points")?.toInt() ?: 0,
                    frequency = recurrenceRule?.string("frequency").orEmpty().ifBlank { "DAILY" },
                    daysOfWeek = recurrenceRule?.stringArray("daysOfWeek").orEmpty(),
                    startDate = entry.long("startDate") ?: return@mapNotNull null,
                    endDate = entry.long("endDate"),
                )
            }

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
    val capabilityConfig: ParsedCapabilityConfig?,
    val score: Int,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val sortOrder: Long,
    val sync: SharedSyncMetadata,
)

private data class ParsedCapabilityConfig(
    val enabledCapabilityIds: List<String>,
    val experimentalCapabilityIds: List<String>,
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

private fun ResolvedWorkspaceSnapshot.withMergedDayMaterial(
    legacyDatabase: ResolvedWorkspaceSnapshot,
): ResolvedWorkspaceSnapshot =
    copy(
        snapshot =
            snapshot.copy(
                dayPlans =
                    mergeById(
                        localItems = snapshot.dayPlans,
                        incomingItems = legacyDatabase.snapshot.dayPlans,
                        idSelector = SharedDayPlan::id,
                        syncSelector = SharedDayPlan::sync,
                    ),
                dayFocusItems =
                    mergeById(
                        localItems = snapshot.dayFocusItems,
                        incomingItems = legacyDatabase.snapshot.dayFocusItems,
                        idSelector = SharedDayFocusItem::id,
                        syncSelector = SharedDayFocusItem::sync,
                    ),
                dayTasks =
                    mergeById(
                        localItems = snapshot.dayTasks,
                        incomingItems = legacyDatabase.snapshot.dayTasks,
                        idSelector = SharedDayTask::id,
                        syncSelector = SharedDayTask::sync,
                    ),
                recurringTasks =
                    mergeStaticById(
                        localItems = snapshot.recurringTasks,
                        incomingItems = legacyDatabase.snapshot.recurringTasks,
                        idSelector = SharedRecurringTask::id,
                    ),
            ),
    )

private fun <T> mergeById(
    localItems: List<T>,
    incomingItems: List<T>,
    idSelector: (T) -> String,
    syncSelector: (T) -> SharedSyncMetadata,
): List<T> {
    val incomingById = incomingItems.associateBy(idSelector)
    val localById = localItems.associateBy(idSelector)
    val ids =
        linkedSetOf<String>().apply {
            addAll(localItems.map(idSelector))
            addAll(incomingItems.map(idSelector))
        }
    return ids.mapNotNull { id ->
        val local = localById[id]
        val incoming = incomingById[id]
        when {
            local == null -> incoming
            incoming == null -> local
            syncSelector(incoming).isNewerThan(syncSelector(local)) -> incoming
            else -> local
        }
    }
}

private fun <T> mergeStaticById(
    localItems: List<T>,
    incomingItems: List<T>,
    idSelector: (T) -> String,
): List<T> =
    (localItems + incomingItems)
        .associateBy(idSelector)
        .values
        .toList()

private fun SharedSyncMetadata.isNewerThan(other: SharedSyncMetadata): Boolean =
    updatedAt > other.updatedAt || (updatedAt == other.updatedAt && version > other.version)

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

private fun JsonObject.capabilityIdArray(vararg keys: String): List<String> =
    keys
        .asSequence()
        .mapNotNull { key -> this[key] as? JsonArray }
        .firstOrNull()
        ?.mapNotNull(JsonElement::capabilityRaw)
        ?.normalizeCapabilityIds()
        .orEmpty()

private fun JsonElement.capabilityRaw(): String? =
    when (this) {
        is JsonPrimitive -> contentOrNull
        is JsonObject -> string("raw")
        else -> null
    }

private fun JsonObject.boolean(vararg keys: String): Boolean? =
    keys
        .asSequence()
        .mapNotNull { key -> (this[key] as? JsonPrimitive)?.booleanOrNull }
        .firstOrNull()

private fun JsonObject.enabledLegacyCapabilityIds(): List<String> =
    buildList {
        addIfTrue("inbox", boolean("enableInbox"))
        addIfTrue("log", boolean("enableLog"))
        addIfTrue("artifact", boolean("enableArtifact"))
        addIfTrue("dashboard", boolean("enableDashboard"))
        addIfTrue("backlog", boolean("enableBacklog"))
        if (boolean("enableAttachments") == true) {
            add("connections")
        }
    }.normalizeCapabilityIds()

private fun MutableList<String>.addIfTrue(
    capabilityId: String,
    enabled: Boolean?,
) {
    if (enabled == true) {
        add(capabilityId)
    }
}

private fun ParsedCapabilityConfig?.enabledWithFallback(defaultView: SharedContextView): List<String> =
    (
        this?.enabledCapabilityIds.orEmpty() +
            this?.experimentalCapabilityIds.orEmpty() +
            SharedContextCapabilityCatalog.capabilityIdFor(defaultView) +
            "dashboard"
    ).normalizeCapabilityIds()

private fun List<String>.normalizeCapabilityIds(): List<String> =
    SharedContextCapabilityCatalog.normalizeCapabilityIds(this)

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

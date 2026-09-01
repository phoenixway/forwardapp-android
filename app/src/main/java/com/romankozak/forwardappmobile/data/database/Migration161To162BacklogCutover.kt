package com.romankozak.forwardappmobile.data.database

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationBindings
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationIssueSeverity
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationPlanner
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogOwnerWorkspaceState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogTargetState
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyBacklogItemSource
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyBacklogOrderSource
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import java.util.UUID

/**
 * BACKLOG Stage 5 Android authority cutover.
 *
 * Schema 161 still owns Context-backed explicit BACKLOG placement through
 * list_items plus the BacklogOrder compatibility mirror.
 *
 * The 161 -> 162 transaction freezes the schema-161 evidence, feeds it through
 * the same shared BacklogMigrationPlanner contract used by Stage-4 dry-run,
 * fails closed before mutation when accounting is incomplete, and then
 * materializes canonical workspace_backlog_entries.
 *
 * list_items and backlog_orders are deliberately retained as historical and
 * pre-Stage-7 transport evidence. They are no longer Android runtime authority.
 */
val MIGRATION_161_162 =
    object : Migration(161, 162) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            val legacyItems = loadLegacyItems161(db)
            val legacyOrders = loadLegacyOrders161(db)
            val workspaces = loadWorkspaces161(db)
            val capabilities = loadCapabilities161(db)
            val mappings = loadMappings161(db)
            val existingEntries = loadExistingBacklogEntries161(db)

            val provenContextBacked =
                workspaces.filter { workspace ->
                    workspace.provenance == CONTEXT_BACKED_PROVENANCE &&
                        !workspace.sourceContextId.isNullOrBlank() &&
                        workspace.id == workspace.sourceContextId
                }

            val workspaceIdByContextId =
                provenContextBacked.associate { workspace ->
                    requireNotNull(workspace.sourceContextId) to workspace.id
                }

            val ownerWorkspaceStateById =
                provenContextBacked.associate { workspace ->
                    workspace.id to BacklogOwnerWorkspaceState(workspace.isDeleted)
                }

            val diagnostics = mutableListOf<String>()

            val expectedCapabilityIds =
                expectedBacklogCapabilityIds161(
                    workspaceIds =
                        provenContextBacked
                            .asSequence()
                            .filterNot { it.isDeleted }
                            .map { it.id }
                            .toList(),
                    capabilities = capabilities,
                    diagnostics = diagnostics,
                )

            val contextBackedWorkspaceIds =
                provenContextBacked.mapTo(hashSetOf()) { it.id }

            existingEntries
                .filter { it.workspaceId in contextBackedWorkspaceIds }
                .forEach { entry ->
                    diagnostics +=
                        "CONTEXT_BACKED_CANONICAL_DESTINATION_PRESENT: " +
                            "Workspace ${entry.workspaceId} already contains canonical BACKLOG entry ${entry.id}"
                }

            val orientationIdByGoalId =
                mappings
                    .asSequence()
                    .filter { mapping ->
                        mapping.sourceType == GOAL_SOURCE_TYPE &&
                            !mapping.isDeleted &&
                            mapping.state == CUT_OVER_MAPPING_STATE
                    }
                    .associate { mapping -> mapping.sourceId to mapping.subjectId }

            val targetStateByRef =
                buildTargetStates161(
                    db = db,
                    workspaces = workspaces,
                )

            val bindings =
                BacklogMigrationBindings(
                    workspaceIdByContextId = workspaceIdByContextId,
                    ownerWorkspaceStateById = ownerWorkspaceStateById,
                    capabilityInstanceIdByWorkspaceId = expectedCapabilityIds,
                    orientationIdByGoalId = orientationIdByGoalId,
                    targetStateByRef = targetStateByRef,
                    parentWorkspaceIdByWorkspaceId =
                        workspaces.associate { workspace ->
                            workspace.id to workspace.parentWorkspaceId
                        },
                    existingCanonicalIds =
                        existingEntries.mapTo(hashSetOf()) { it.id },
                )

            val plan =
                BacklogMigrationPlanner.plan(
                    items = legacyItems,
                    orders = legacyOrders,
                    bindings = bindings,
                )

            plan.issues
                .filter { it.severity == BacklogMigrationIssueSeverity.ERROR }
                .forEach { issue ->
                    diagnostics +=
                        "${issue.code}: item=${issue.itemId} order=${issue.orderId}: ${issue.detail}"
                }

            if (!plan.isFullyAccounted) {
                diagnostics +=
                    "SOURCE_ACCOUNTING: items=${plan.itemAccounting.size}/${plan.itemSourceCount}, " +
                        "orders=${plan.orderAccounting.size}/${plan.orderSourceCount}, " +
                        "entries=${plan.entries.size}"
            }

            check(diagnostics.isEmpty() && plan.isFullyAccounted) {
                "BACKLOG cutover blocked:\n${diagnostics.distinct().joinToString("\n")}"
            }

            ensureMissingBacklogCapabilities161(
                db = db,
                workspaces = workspaces.associateBy { it.id },
                capabilities = capabilities,
                expectedIds = expectedCapabilityIds,
                now = now,
            )

            plan.entries.forEach { entry ->
                db.execSQL(
                    """
                    INSERT INTO workspace_backlog_entries(
                        id,
                        workspaceId,
                        capabilityInstanceId,
                        targetKind,
                        targetId,
                        entryOrder,
                        createdAt,
                        updatedAt,
                        syncedAt,
                        isDeleted,
                        version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        entry.id,
                        entry.workspaceId,
                        entry.capabilityInstanceId,
                        entry.target.kind.name,
                        entry.target.id,
                        entry.order,
                        entry.createdAt,
                        entry.updatedAt,
                        boolInt161(entry.isDeleted),
                        entry.version,
                    ),
                )
            }

            check(
                scalarLong161(
                    db,
                    "SELECT COUNT(*) FROM workspace_backlog_entries " +
                        "WHERE workspaceId IN (" +
                        provenContextBacked.joinToString(",") { "'${sqlLiteral161(it.id)}'" } +
                        ")",
                ) == plan.entries.size.toLong(),
            ) {
                "BACKLOG cutover blocked: canonical entry accounting mismatch after insert"
            }

            plan.entries.forEach { entry ->
                val row =
                    db.query(
                        """
                        SELECT
                            workspaceId,
                            capabilityInstanceId,
                            targetKind,
                            targetId,
                            entryOrder,
                            isDeleted,
                            version
                        FROM workspace_backlog_entries
                        WHERE id = ?
                        LIMIT 1
                        """.trimIndent(),
                        arrayOf(entry.id),
                    ).use { cursor ->
                        check(cursor.moveToFirst()) {
                            "BACKLOG cutover blocked: missing canonical entry ${entry.id}"
                        }
                        CanonicalBacklogCheck161(
                            workspaceId = cursor.getString(0),
                            capabilityInstanceId = cursor.getString(1),
                            targetKind = cursor.getString(2),
                            targetId = cursor.getString(3),
                            order = cursor.getLong(4),
                            isDeleted = cursor.getInt(5) != 0,
                            version = cursor.getLong(6),
                        )
                    }

                check(
                    row.workspaceId == entry.workspaceId &&
                        row.capabilityInstanceId == entry.capabilityInstanceId &&
                        row.targetKind == entry.target.kind.name &&
                        row.targetId == entry.target.id &&
                        row.order == entry.order &&
                        row.isDeleted == entry.isDeleted &&
                        row.version == entry.version
                ) {
                    "BACKLOG cutover blocked: canonical entry ${entry.id} differs from frozen plan"
                }
            }
        }
    }

private data class BacklogWorkspace161(
    val id: String,
    val provenance: String,
    val sourceContextId: String?,
    val isDeleted: Boolean,
    val parentWorkspaceId: String?,
    val createdAt: Long,
)

private data class BacklogCapability161(
    val id: String,
    val workspaceId: String,
    val capabilityType: String,
    val instanceKey: String,
)

private data class BacklogMapping161(
    val sourceType: String,
    val sourceId: String,
    val subjectId: String,
    val state: String,
    val isDeleted: Boolean,
)

private data class ExistingBacklogEntry161(
    val id: String,
    val workspaceId: String,
)

private data class CanonicalBacklogCheck161(
    val workspaceId: String,
    val capabilityInstanceId: String,
    val targetKind: String,
    val targetId: String,
    val order: Long,
    val isDeleted: Boolean,
    val version: Long,
)

private fun loadLegacyItems161(db: SupportSQLiteDatabase): List<LegacyBacklogItemSource> =
    db.query("SELECT * FROM list_items").use { cursor ->
        val id = cursor.requireColumn161("id")
        val contextId = cursor.requireColumn161("context_id", "contextId")
        val itemType = cursor.requireColumn161("itemType", "item_type")
        val entityId = cursor.requireColumn161("entityId", "entity_id")
        val associationOwner = cursor.optionalColumn161("association_owner_context_id", "associationOwnerContextId")
        val associationTag = cursor.optionalColumn161("association_tag", "associationTag")
        val order = cursor.requireColumn161("item_order", "order")
        val updatedAt = cursor.optionalColumn161("updatedAt", "updated_at")
        val syncedAt = cursor.optionalColumn161("synced_at", "syncedAt")
        val isDeleted = cursor.requireColumn161("is_deleted", "isDeleted")
        val version = cursor.requireColumn161("version")

        buildList {
            while (cursor.moveToNext()) {
                add(
                    LegacyBacklogItemSource(
                        id = cursor.getString(id),
                        contextId = cursor.getString(contextId),
                        itemType = cursor.getString(itemType),
                        entityId = cursor.getString(entityId),
                        associationOwnerContextId = cursor.stringOrNull161(associationOwner),
                        associationTag = cursor.stringOrNull161(associationTag),
                        order = cursor.getLong(order),
                        updatedAt = cursor.longOrNull161(updatedAt),
                        syncedAt = cursor.longOrNull161(syncedAt),
                        isDeleted = cursor.getInt(isDeleted) != 0,
                        version = cursor.getLong(version),
                    ),
                )
            }
        }
    }

private fun loadLegacyOrders161(db: SupportSQLiteDatabase): List<LegacyBacklogOrderSource> =
    db.query("SELECT * FROM backlog_orders").use { cursor ->
        val id = cursor.requireColumn161("id")
        val listId = cursor.requireColumn161("listId", "list_id")
        val itemId = cursor.requireColumn161("itemId", "item_id")
        val order = cursor.requireColumn161("order", "item_order")
        val orderVersion = cursor.requireColumn161("orderVersion", "order_version")
        val updatedAt = cursor.optionalColumn161("updatedAt", "updated_at")
        val syncedAt = cursor.optionalColumn161("syncedAt", "synced_at")
        val isDeleted = cursor.requireColumn161("isDeleted", "is_deleted")

        buildList {
            while (cursor.moveToNext()) {
                add(
                    LegacyBacklogOrderSource(
                        id = cursor.getString(id),
                        listId = cursor.getString(listId),
                        itemId = cursor.getString(itemId),
                        order = cursor.getLong(order),
                        orderVersion = cursor.getLong(orderVersion),
                        updatedAt = cursor.longOrNull161(updatedAt),
                        syncedAt = cursor.longOrNull161(syncedAt),
                        isDeleted = cursor.getInt(isDeleted) != 0,
                    ),
                )
            }
        }
    }

private fun loadWorkspaces161(db: SupportSQLiteDatabase): List<BacklogWorkspace161> =
    db.query("SELECT * FROM workspaces").use { cursor ->
        val id = cursor.requireColumn161("id")
        val provenance = cursor.requireColumn161("provenance")
        val sourceContextId = cursor.optionalColumn161("sourceContextId", "source_context_id")
        val isDeleted = cursor.requireColumn161("isDeleted", "is_deleted")
        val parentWorkspaceId = cursor.optionalColumn161("parentWorkspaceId", "parent_workspace_id")
        val createdAt = cursor.requireColumn161("createdAt", "created_at")

        buildList {
            while (cursor.moveToNext()) {
                add(
                    BacklogWorkspace161(
                        id = cursor.getString(id),
                        provenance = cursor.getString(provenance),
                        sourceContextId = cursor.stringOrNull161(sourceContextId),
                        isDeleted = cursor.getInt(isDeleted) != 0,
                        parentWorkspaceId = cursor.stringOrNull161(parentWorkspaceId),
                        createdAt = cursor.getLong(createdAt),
                    ),
                )
            }
        }
    }

private fun loadCapabilities161(db: SupportSQLiteDatabase): List<BacklogCapability161> =
    db.query("SELECT * FROM workspace_capability_instances").use { cursor ->
        val id = cursor.requireColumn161("id")
        val workspaceId = cursor.requireColumn161("workspaceId")
        val capabilityType = cursor.requireColumn161("capabilityType")
        val instanceKey = cursor.requireColumn161("instanceKey")

        buildList {
            while (cursor.moveToNext()) {
                add(
                    BacklogCapability161(
                        id = cursor.getString(id),
                        workspaceId = cursor.getString(workspaceId),
                        capabilityType = cursor.getString(capabilityType),
                        instanceKey = cursor.getString(instanceKey),
                    ),
                )
            }
        }
    }

private fun loadMappings161(db: SupportSQLiteDatabase): List<BacklogMapping161> =
    db.query("SELECT * FROM legacy_subject_mappings").use { cursor ->
        val sourceType = cursor.requireColumn161("sourceType")
        val sourceId = cursor.requireColumn161("sourceId")
        val subjectId = cursor.requireColumn161("subjectId")
        val state = cursor.requireColumn161("state")
        val isDeleted = cursor.requireColumn161("isDeleted", "is_deleted")

        buildList {
            while (cursor.moveToNext()) {
                add(
                    BacklogMapping161(
                        sourceType = cursor.getString(sourceType),
                        sourceId = cursor.getString(sourceId),
                        subjectId = cursor.getString(subjectId),
                        state = cursor.getString(state),
                        isDeleted = cursor.getInt(isDeleted) != 0,
                    ),
                )
            }
        }
    }

private fun loadExistingBacklogEntries161(db: SupportSQLiteDatabase): List<ExistingBacklogEntry161> =
    db.query("SELECT id, workspaceId FROM workspace_backlog_entries").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    ExistingBacklogEntry161(
                        id = cursor.getString(0),
                        workspaceId = cursor.getString(1),
                    ),
                )
            }
        }
    }

private fun expectedBacklogCapabilityIds161(
    workspaceIds: List<String>,
    capabilities: List<BacklogCapability161>,
    diagnostics: MutableList<String>,
): Map<String, String> {
    val existingByLogical =
        capabilities
            .filter {
                it.capabilityType == BACKLOG_CAPABILITY_TYPE &&
                    it.instanceKey == DEFAULT_INSTANCE_KEY
            }
            .associateBy { it.workspaceId }

    val allById = capabilities.associateBy { it.id }
    val result = linkedMapOf<String, String>()

    workspaceIds
        .distinct()
        .sorted()
        .forEach { workspaceId ->
            val existing = existingByLogical[workspaceId]
            if (existing != null) {
                if (existing.id.isBlank()) {
                    diagnostics +=
                        "INVALID_EXISTING_CAPABILITY_ID: Workspace $workspaceId has blank BACKLOG capability id"
                } else {
                    result[workspaceId] = existing.id
                }
                return@forEach
            }

            val expectedId = stableBacklogCapabilityId161(workspaceId)
            val collision = allById[expectedId]
            if (collision != null) {
                diagnostics +=
                    "CAPABILITY_ID_COLLISION: deterministic BACKLOG id $expectedId for Workspace " +
                        "$workspaceId is occupied by ${collision.workspaceId}:${collision.capabilityType}:${collision.instanceKey}"
            } else {
                result[workspaceId] = expectedId
            }
        }

    return result
}

private fun buildTargetStates161(
    db: SupportSQLiteDatabase,
    workspaces: List<BacklogWorkspace161>,
): Map<WorkspaceBacklogTargetRef, BacklogTargetState> {
    val result = linkedMapOf<WorkspaceBacklogTargetRef, BacklogTargetState>()

    workspaces.forEach { workspace ->
        result[
            WorkspaceBacklogTargetRef(
                WorkspaceBacklogTargetKind.WORKSPACE,
                workspace.id,
            ),
        ] = BacklogTargetState(workspace.isDeleted)
    }

    val orientationIds =
        db.query("SELECT subjectId FROM orientations").use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    db.query("SELECT * FROM managed_subjects").use { cursor ->
        val id = cursor.requireColumn161("id")
        val subjectType = cursor.requireColumn161("subjectType")
        val isDeleted = cursor.requireColumn161("isDeleted", "is_deleted")
        while (cursor.moveToNext()) {
            val subjectId = cursor.getString(id)
            if (cursor.getString(subjectType) == ORIENTATION_SUBJECT_TYPE && subjectId in orientationIds) {
                result[
                    WorkspaceBacklogTargetRef(
                        WorkspaceBacklogTargetKind.ORIENTATION,
                        subjectId,
                    ),
                ] = BacklogTargetState(cursor.getInt(isDeleted) != 0)
            }
        }
    }

    loadTypedTargets161(
        db,
        tableCandidates = listOf("link_items"),
        kind = WorkspaceBacklogTargetKind.LINK_ITEM,
        result = result,
    )
    loadTypedTargets161(
        db,
        tableCandidates = listOf("legacy_notes", "notes"),
        kind = WorkspaceBacklogTargetKind.LEGACY_NOTE,
        result = result,
    )

    val documents = loadRawTargets161(db, listOf("note_documents"))
    documents.forEach { target ->
        result[
            WorkspaceBacklogTargetRef(
                WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
                target.first,
            ),
        ] = BacklogTargetState(target.second)
        result[
            WorkspaceBacklogTargetRef(
                WorkspaceBacklogTargetKind.JOURNAL_DOCUMENT,
                target.first,
            ),
        ] = BacklogTargetState(target.second)
    }

    loadTypedTargets161(
        db,
        tableCandidates = listOf("checklists"),
        kind = WorkspaceBacklogTargetKind.CHECKLIST,
        result = result,
    )
    loadTypedTargets161(
        db,
        tableCandidates = listOf("music_notes"),
        kind = WorkspaceBacklogTargetKind.MUSIC_NOTE,
        result = result,
    )

    return result
}

private fun loadTypedTargets161(
    db: SupportSQLiteDatabase,
    tableCandidates: List<String>,
    kind: WorkspaceBacklogTargetKind,
    result: MutableMap<WorkspaceBacklogTargetRef, BacklogTargetState>,
) {
    loadRawTargets161(db, tableCandidates).forEach { (id, isDeleted) ->
        result[WorkspaceBacklogTargetRef(kind, id)] = BacklogTargetState(isDeleted)
    }
}

private fun loadRawTargets161(
    db: SupportSQLiteDatabase,
    tableCandidates: List<String>,
): List<Pair<String, Boolean>> {
    val table = tableCandidates.firstOrNull { tableExists161(db, it) } ?: return emptyList()
    return db.query("SELECT * FROM `$table`").use { cursor ->
        val id = cursor.requireColumn161("id")
        val deleted = cursor.optionalColumn161("isDeleted", "is_deleted")
        buildList {
            while (cursor.moveToNext()) {
                add(
                    cursor.getString(id) to
                        if (deleted >= 0) cursor.getInt(deleted) != 0 else false,
                )
            }
        }
    }
}

private fun ensureMissingBacklogCapabilities161(
    db: SupportSQLiteDatabase,
    workspaces: Map<String, BacklogWorkspace161>,
    capabilities: List<BacklogCapability161>,
    expectedIds: Map<String, String>,
    now: Long,
) {
    val existingLogical =
        capabilities
            .filter {
                it.capabilityType == BACKLOG_CAPABILITY_TYPE &&
                    it.instanceKey == DEFAULT_INSTANCE_KEY
            }
            .associateBy { it.workspaceId }

    val fallbackOrder =
        scalarLong161(
            db,
            "SELECT COALESCE(MAX(capabilityOrder), -1) + 1 FROM workspace_capability_instances",
        )

    expectedIds.forEach { (workspaceId, expectedId) ->
        if (existingLogical[workspaceId] != null) return@forEach
        val workspace =
            requireNotNull(workspaces[workspaceId]) {
                "BACKLOG cutover blocked: missing Workspace $workspaceId during capability materialization"
            }

        db.execSQL(
            """
            INSERT INTO workspace_capability_instances(
                id,
                workspaceId,
                capabilityType,
                instanceKey,
                capabilityOrder,
                state,
                configurationVersion,
                configuration,
                createdAt,
                updatedAt,
                syncedAt,
                isDeleted,
                version
            ) VALUES (?, ?, ?, ?, ?, ?, 1, '{}', ?, ?, NULL, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(
                expectedId,
                workspaceId,
                BACKLOG_CAPABILITY_TYPE,
                DEFAULT_INSTANCE_KEY,
                fallbackOrder,
                DISABLED_CAPABILITY_STATE,
                workspace.createdAt,
                now,
            ),
        )
    }
}

private fun stableBacklogCapabilityId161(workspaceId: String): String =
    LegacySubjectUuid
        .uuidV5(
            UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
            "WORKSPACE:CAPABILITY:$workspaceId:$BACKLOG_CAPABILITY_TYPE:$DEFAULT_INSTANCE_KEY",
        ).toString()

private fun tableExists161(
    db: SupportSQLiteDatabase,
    table: String,
): Boolean =
    db.query(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
        arrayOf(table),
    ).use { it.moveToFirst() }

private fun scalarLong161(
    db: SupportSQLiteDatabase,
    sql: String,
): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected scalar result for: $sql" }
        cursor.getLong(0)
    }

private fun Cursor.requireColumn161(vararg names: String): Int =
    names
        .asSequence()
        .map(::getColumnIndex)
        .firstOrNull { it >= 0 }
        ?: error(
            "Missing required column ${names.joinToString("/")} in " +
                columnNames.joinToString(","),
        )

private fun Cursor.optionalColumn161(vararg names: String): Int =
    names
        .asSequence()
        .map(::getColumnIndex)
        .firstOrNull { it >= 0 }
        ?: -1

private fun Cursor.stringOrNull161(index: Int): String? =
    if (index < 0 || isNull(index)) null else getString(index)

private fun Cursor.longOrNull161(index: Int): Long? =
    if (index < 0 || isNull(index)) null else getLong(index)

private fun boolInt161(value: Boolean): Int = if (value) 1 else 0

private fun sqlLiteral161(value: String): String = value.replace("'", "''")

private const val CONTEXT_BACKED_PROVENANCE = "CONTEXT_BACKED"
private const val BACKLOG_CAPABILITY_TYPE = "BACKLOG"
private const val DEFAULT_INSTANCE_KEY = "default"
private const val GOAL_SOURCE_TYPE = "GOAL"
private const val CUT_OVER_MAPPING_STATE = "CUT_OVER"
private const val ORIENTATION_SUBJECT_TYPE = "ORIENTATION"
private const val DISABLED_CAPABILITY_STATE = "DISABLED"

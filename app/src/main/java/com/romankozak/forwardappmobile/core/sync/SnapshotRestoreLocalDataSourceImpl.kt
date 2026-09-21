package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalOrientationPayload
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementGroupScopeSyncStore
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementLinkedAppearanceSyncStore
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementSyncStore
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.sync.datasource.SnapshotRestoreLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

fun interface CanonicalSnapshotTransactionWriter {
    suspend fun apply(bundle: SnapshotBundle)
}

@Singleton
class MergeCanonicalSnapshotTransactionWriter
    @Inject
    constructor(
        private val mergeLocalDataSource: MergeLocalDataSourceImpl,
    ) : CanonicalSnapshotTransactionWriter {
        override suspend fun apply(bundle: SnapshotBundle) {
            mergeLocalDataSource.applyCanonicalSnapshotBundle(bundle)
        }
    }

/** Atomic Room replacement. File decoding and restore-only canonicalization occur before this boundary. */
@Singleton
class SnapshotRestoreLocalDataSourceImpl
    @Inject
    constructor(
        private val database: AppDatabase,
        private val writer: CanonicalSnapshotTransactionWriter,
        private val dayManagementRuntimeRepository: DayManagementRuntimeRepository,
    ) : SnapshotRestoreLocalDataSource {
        override suspend fun replaceWith(bundle: SnapshotBundle) {
            validateBeforeClear(bundle)

            val hierarchyStore = CanonicalHierarchyPlacementSyncStore(database)
            val groupScopeStore =
                CanonicalHierarchyPlacementGroupScopeSyncStore(database)
            val linkedAppearanceStore =
                CanonicalHierarchyPlacementLinkedAppearanceSyncStore(database)

            val hierarchySnapshots =
                bundle.hierarchyPlacements ?: hierarchyStore.loadAll()
            val decodedHierarchy =
                hierarchyStore.decodeAndValidateForRestore(
                    bundle = bundle,
                    snapshots = hierarchySnapshots,
                )

            val groupScopeSnapshots =
                bundle.hierarchyPlacementGroupScopes ?: groupScopeStore.loadAll()
            val decodedGroupScopes =
                groupScopeStore.decodeAndValidateForRestore(
                    bundle = bundle,
                    snapshots = groupScopeSnapshots,
                    placements = decodedHierarchy,
                    requireComplete = bundle.hierarchyPlacementGroupScopes != null,
                )

            val linkedAppearanceSnapshots =
                bundle.hierarchyPlacementLinkedAppearances ?: linkedAppearanceStore.loadAll()
            val decodedLinkedAppearances =
                linkedAppearanceStore.decodeAndValidateForRestore(
                    snapshots = linkedAppearanceSnapshots,
                    placements = decodedHierarchy,
                )

            // H1 restore is explicit: peer merge correctly resets syncedAt, while
            // full backup/restore must preserve it exactly.
            val roomBundle =
                bundle.copy(
                    dayManagementRuntimeState = null,
                    hierarchyPlacements = null,
                    hierarchyPlacementGroupScopes = null,
                    hierarchyPlacementLinkedAppearances = null,
                )

            database.withTransaction {
                TransactionAwareRoomClearer(database).clearAllApplicationTables()
                writer.apply(roomBundle)
                hierarchyStore.restoreExactDecoded(decodedHierarchy)
                groupScopeStore.restoreExactDecoded(
                    scopes = decodedGroupScopes,
                    requireComplete = bundle.hierarchyPlacementGroupScopes != null,
                )
                linkedAppearanceStore.restoreExactDecoded(decodedLinkedAppearances)
                assertRestoreInvariants(bundle)
            }
            bundle.dayManagementRuntimeState?.let { dayManagementRuntimeRepository.importSnapshot(it) }
        }

        private fun validateBeforeClear(bundle: SnapshotBundle) {
            requireValidCanonicalDayThemePayload(bundle)
            requireValidCanonicalOrientationPayload(bundle)
            require(bundle.workspaces != null) {
                "Restore replacement requires a canonical Workspace payload"
            }
            require(bundle.workspaceBacklogEntries != null) {
                "Restore replacement requires canonical BACKLOG presence"
            }
            require(bundle.workspaceInboxRecords != null) {
                "Restore replacement requires canonical INBOX presence"
            }
            require(
                bundle.contexts.none { context ->
                    !context.isDeleted && !SystemContexts.isSystem(ContextId(context.id))
                },
            ) { "Restore replacement refuses live ordinary Context authority" }
            require(bundle.contexts.none { SystemContexts.isSystem(ContextId(it.id)) }) {
                "Restore replacement refuses reserved System Context shells"
            }
        }

        private suspend fun assertRestoreInvariants(source: SnapshotBundle) {
            val contexts = database.contextDao().getAllRaw()
            check(
                contexts.none { context ->
                    !context.isDeleted && !SystemContexts.isSystem(ContextId(context.id))
                },
            ) { "Restore produced a live ordinary Context" }
            check(contexts.none { SystemContexts.isSystem(ContextId(it.id)) }) {
                "Restore produced a reserved System Context shell"
            }
            check(source.workspaceBacklogEntries != null && source.workspaceInboxRecords != null)

            val sql = database.openHelper.writableDatabase
            sql.query("PRAGMA foreign_key_check").use { cursor ->
                check(cursor.count == 0) { "Restore foreign_key_check failed with ${cursor.count} rows" }
            }
            check(
                scalarCount(
                    """
                    SELECT COUNT(*)
                    FROM workspace_backlog_entries entry
                    LEFT JOIN workspaces workspace ON workspace.id = entry.workspaceId
                    LEFT JOIN workspace_capability_instances capability
                      ON capability.id = entry.capabilityInstanceId
                    WHERE workspace.id IS NULL
                       OR capability.id IS NULL
                       OR capability.workspaceId != entry.workspaceId
                       OR capability.capabilityType != 'BACKLOG'
                    """.trimIndent(),
                ) == 0L,
            ) { "Restore produced BACKLOG rows without canonical Workspace/capability ownership" }
            check(
                scalarCount(
                    """
                    SELECT COUNT(*)
                    FROM workspace_inbox_records record
                    LEFT JOIN workspaces workspace ON workspace.id = record.workspaceId
                    LEFT JOIN workspace_capability_instances capability
                      ON capability.id = record.capabilityInstanceId
                    WHERE workspace.id IS NULL
                       OR capability.id IS NULL
                       OR capability.workspaceId != record.workspaceId
                       OR capability.capabilityType != 'INBOX'
                    """.trimIndent(),
                ) == 0L,
            ) { "Restore produced INBOX rows without canonical Workspace/capability ownership" }
        }

        private fun scalarCount(query: String): Long =
            database.openHelper.writableDatabase.query(query).use { cursor ->
                check(cursor.moveToFirst())
                cursor.getLong(0)
            }
    }

/**
 * Room 2.8 clearAllTables() opens its own transaction. Restore instead clears
 * every application table inside the already-open replacement transaction.
 */
internal class TransactionAwareRoomClearer(
    private val database: AppDatabase,
) {
    fun clearAllApplicationTables() {
        val sql = database.openHelper.writableDatabase
        check(sql.inTransaction()) { "Transaction-aware clear requires an active Room transaction" }
        sql.execSQL("PRAGMA defer_foreign_keys = TRUE")

        val tables =
            sql.query(
                """
                SELECT name, sql
                FROM sqlite_master
                WHERE type = 'table'
                  AND name NOT LIKE 'sqlite_%'
                  AND name NOT IN ('android_metadata', 'room_master_table')
                ORDER BY name
                """.trimIndent(),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            SchemaTable(
                                name = cursor.getString(0),
                                isVirtual = cursor.getString(1)?.startsWith("CREATE VIRTUAL TABLE", true) == true,
                            ),
                        )
                    }
                }
            }
        val shadowNames =
            tables.filter { it.isVirtual }.flatMapTo(hashSetOf()) { table ->
                FTS_SHADOW_SUFFIXES.map { suffix -> table.name + suffix }
            }
        val applicationTables = tables.filterNot { it.name in shadowNames }
        check(applicationTables.isNotEmpty()) { "Room schema census found no application tables" }

        applicationTables.sortedBy { it.isVirtual }.forEach { table ->
            sql.execSQL("DELETE FROM ${quoteIdentifier(table.name)}")
        }
        applicationTables.forEach { table ->
            sql.query("SELECT COUNT(*) FROM ${quoteIdentifier(table.name)}").use { cursor ->
                check(cursor.moveToFirst() && cursor.getLong(0) == 0L) {
                    "Atomic restore clear did not empty ${table.name}"
                }
            }
        }
    }

    private fun quoteIdentifier(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    private data class SchemaTable(
        val name: String,
        val isVirtual: Boolean,
    )

    private companion object {
        // SQLite FTS shadow storage is owned by the virtual table itself and
        // must not participate independently in application-table clearing or
        // empty verification.
        //
        // FTS3/FTS4:
        //   _content, _segments, _segdir, _docsize, _stat
        //
        // FTS5:
        //   _data, _idx, _content, _docsize, _config
        val FTS_SHADOW_SUFFIXES =
            listOf(
                "_content",
                "_segments",
                "_segdir",
                "_docsize",
                "_stat",
                "_data",
                "_idx",
                "_config",
            )
    }
}

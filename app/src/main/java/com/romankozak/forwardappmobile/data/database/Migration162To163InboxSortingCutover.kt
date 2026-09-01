package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingLegacyPlanner
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingMigrationBindings
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyInboxSortingSource

/**
 * INBOX_SORTING policy authority cutover.
 *
 * The migration feeds every legacy settings row through the shared frozen
 * planner before mutating capability configuration. The physical legacy table
 * is retained temporarily for guarded old-full-backup evidence, but is emptied
 * and must never regain runtime or live-transport authority.
 */
val MIGRATION_162_163 =
    object : Migration(162, 163) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val sources = loadInboxSortingSources162(db)
            val workspaces = loadInboxSortingWorkspaces162(db)
            val capabilities = loadInboxSortingCapabilities162(db)
            val diagnostics = mutableListOf<String>()

            sources.filter { it.updatedAt < 0L }.forEach { source ->
                diagnostics += "INVALID_SOURCE_TIMESTAMP: Context ${source.contextId} has ${source.updatedAt}"
            }

            val provenWorkspaceGroups =
                workspaces
                    .filter {
                        !it.isDeleted &&
                            it.provenance == CONTEXT_BACKED_PROVENANCE_163 &&
                            !it.sourceContextId.isNullOrBlank() &&
                            it.id == it.sourceContextId
                    }
                    .groupBy { requireNotNull(it.sourceContextId) }
            provenWorkspaceGroups.filterValues { it.size > 1 }.forEach { (contextId, matches) ->
                diagnostics +=
                    "AMBIGUOUS_OWNER_WORKSPACE: Context $contextId resolves to " +
                        matches.joinToString { it.id }
            }
            val workspaceIdByContextId =
                provenWorkspaceGroups
                    .filterValues { it.size == 1 }
                    .mapValues { (_, matches) -> matches.single().id }

            val capabilityGroups = capabilities.groupBy { it.workspaceId }
            capabilityGroups.filterValues { it.size > 1 }.forEach { (workspaceId, matches) ->
                diagnostics +=
                    "MULTIPLE_CAPABILITY_INSTANCES: Workspace $workspaceId has " +
                        matches.joinToString { it.id }
            }
            val capabilityIdByWorkspaceId =
                capabilityGroups
                    .filterValues { it.size == 1 }
                    .mapValues { (_, matches) -> matches.single().id }

            val plan =
                InboxSortingLegacyPlanner.plan(
                    sources = sources,
                    bindings =
                        InboxSortingMigrationBindings(
                            workspaceIdByContextId = workspaceIdByContextId,
                            capabilityInstanceIdByWorkspaceId = capabilityIdByWorkspaceId,
                        ),
                )
            plan.issues.forEach { issue ->
                diagnostics +=
                    "${issue.code}: context=${issue.contextId} line=${issue.lineNumber}: ${issue.detail}"
            }
            if (!plan.isFullyAccounted) {
                diagnostics += "SOURCE_ACCOUNTING: ${plan.updates.size}/${plan.sourceCount}"
            }

            val legacyUpdateByCapabilityId = plan.updates.associateBy { it.capabilityInstanceId }
            val desiredUpdates =
                capabilities.mapNotNull { capability ->
                    val legacy = legacyUpdateByCapabilityId[capability.id]
                    val desiredVersion: Int
                    val desiredConfiguration: String
                    val desiredUpdatedAt: Long
                    if (legacy != null) {
                        desiredVersion = legacy.configurationVersion
                        desiredConfiguration = legacy.configuration
                        desiredUpdatedAt = maxOf(capability.updatedAt, legacy.sourceUpdatedAt)
                    } else if (
                        capability.configurationVersion == InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION &&
                        capability.configuration == LEGACY_EMPTY_CONFIGURATION_163
                    ) {
                        desiredVersion = InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION
                        desiredConfiguration = InboxSortingCapabilityConfigurationCodec.encodeDefault()
                        desiredUpdatedAt = maxOf(capability.updatedAt, now)
                    } else {
                        val valid =
                            runCatching {
                                InboxSortingCapabilityConfigurationCodec.validate(
                                    capability.configurationVersion,
                                    capability.configuration,
                                )
                            }.exceptionOrNull()
                        if (valid != null) {
                            diagnostics +=
                                "INVALID_CANONICAL_CONFIGURATION: capability=${capability.id}: ${valid.message}"
                        }
                        return@mapNotNull null
                    }

                    if (
                        capability.configurationVersion == desiredVersion &&
                        capability.configuration == desiredConfiguration
                    ) {
                        null
                    } else {
                        InboxSortingCapabilityUpdate163(
                            id = capability.id,
                            configurationVersion = desiredVersion,
                            configuration = desiredConfiguration,
                            updatedAt = desiredUpdatedAt,
                        )
                    }
                }

            check(diagnostics.isEmpty() && plan.isFullyAccounted) {
                "INBOX_SORTING cutover blocked:\n${diagnostics.distinct().joinToString("\n")}"
            }

            desiredUpdates.forEach { update ->
                db.execSQL(
                    """
                    UPDATE workspace_capability_instances
                    SET configurationVersion = ?,
                        configuration = ?,
                        updatedAt = ?,
                        syncedAt = NULL,
                        version = version + 1
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf<Any?>(
                        update.configurationVersion,
                        update.configuration,
                        update.updatedAt,
                        update.id,
                    ),
                )
            }

            loadInboxSortingCapabilities162(db).forEach { capability ->
                InboxSortingCapabilityConfigurationCodec.validate(
                    capability.configurationVersion,
                    capability.configuration,
                )
            }

            db.execSQL("DELETE FROM context_inbox_sorting")
            check(scalarLong163(db, "SELECT COUNT(*) FROM context_inbox_sorting") == 0L) {
                "INBOX_SORTING cutover blocked: legacy policy rows remain after materialization"
            }
        }
    }

private data class InboxSortingWorkspace162(
    val id: String,
    val sourceContextId: String?,
    val provenance: String,
    val isDeleted: Boolean,
)

private data class InboxSortingCapability162(
    val id: String,
    val workspaceId: String,
    val configurationVersion: Int,
    val configuration: String,
    val updatedAt: Long,
)

private data class InboxSortingCapabilityUpdate163(
    val id: String,
    val configurationVersion: Int,
    val configuration: String,
    val updatedAt: Long,
)

private fun loadInboxSortingSources162(db: SupportSQLiteDatabase): List<LegacyInboxSortingSource> =
    db.query(
        "SELECT context_id, rules_text, updated_at FROM context_inbox_sorting ORDER BY context_id",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    LegacyInboxSortingSource(
                        contextId = cursor.getString(0),
                        rulesText = cursor.getString(1),
                        updatedAt = cursor.getLong(2),
                    ),
                )
            }
        }
    }

private fun loadInboxSortingWorkspaces162(db: SupportSQLiteDatabase): List<InboxSortingWorkspace162> =
    db.query("SELECT id, sourceContextId, provenance, isDeleted FROM workspaces").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    InboxSortingWorkspace162(
                        id = cursor.getString(0),
                        sourceContextId = cursor.getString(1),
                        provenance = cursor.getString(2),
                        isDeleted = cursor.getInt(3) != 0,
                    ),
                )
            }
        }
    }

private fun loadInboxSortingCapabilities162(db: SupportSQLiteDatabase): List<InboxSortingCapability162> =
    db.query(
        """
        SELECT id, workspaceId, configurationVersion, configuration, updatedAt
        FROM workspace_capability_instances
        WHERE capabilityType = 'INBOX_SORTING' AND instanceKey = 'default'
        ORDER BY workspaceId, id
        """.trimIndent(),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    InboxSortingCapability162(
                        id = cursor.getString(0),
                        workspaceId = cursor.getString(1),
                        configurationVersion = cursor.getInt(2),
                        configuration = cursor.getString(3),
                        updatedAt = cursor.getLong(4),
                    ),
                )
            }
        }
    }

private fun scalarLong163(db: SupportSQLiteDatabase, sql: String): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected scalar result for: $sql" }
        cursor.getLong(0)
    }

private const val CONTEXT_BACKED_PROVENANCE_163 = "CONTEXT_BACKED"
private const val LEGACY_EMPTY_CONFIGURATION_163 = "{}"

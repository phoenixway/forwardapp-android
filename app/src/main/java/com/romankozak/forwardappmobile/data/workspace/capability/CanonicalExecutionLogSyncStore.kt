package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.sync.datasource.CanonicalExecutionLogSyncVersion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical transport/invariant boundary for Workspace-owned EXECUTION_LOG rows.
 *
 * Content stays in context_execution_logs. This store owns only the canonical
 * partition contract, transport mapping, conflict resolution, and sync ack.
 */
@Singleton
class CanonicalExecutionLogSyncStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val contextManagementDao: ContextManagementDao,
        private val workspaceDao: WorkspaceDao,
    ) {
        suspend fun loadAll(): List<CanonicalExecutionLogSnapshot> =
            contextManagementDao.getCanonicalExecutionLogs().map { it.toCanonicalSnapshot() }

        suspend fun loadUnsynced(): List<CanonicalExecutionLogSnapshot> =
            contextManagementDao.getUnsyncedCanonicalExecutionLogs().map { it.toCanonicalSnapshot() }

        suspend fun loadChangedSince(timestamp: Long): List<CanonicalExecutionLogSnapshot> =
            contextManagementDao.getCanonicalExecutionLogsChangedSince(timestamp)
                .map { it.toCanonicalSnapshot() }

        /**
         * Snapshot ingress callers must apply canonical Workspace payload first.
         * The surrounding Room transaction remains owned by the caller.
         */
        suspend fun mergeIncoming(incoming: List<CanonicalExecutionLogSnapshot>?) {
            if (incoming == null || incoming.isEmpty()) return

            require(incoming.map { it.id }.toSet().size == incoming.size) {
                "Canonical EXECUTION_LOG payload contains duplicate ids"
            }

            val localById = contextManagementDao.getAllLogs().associateBy { it.id }

            incoming.forEach { candidate ->
                require(candidate.id.isNotBlank()) {
                    "Canonical EXECUTION_LOG id must not be blank"
                }
                require(candidate.workspaceId.isNotBlank()) {
                    "Canonical EXECUTION_LOG workspaceId must not be blank"
                }

                val workspace =
                    requireNotNull(workspaceDao.getById(candidate.workspaceId)) {
                        "Canonical EXECUTION_LOG ${candidate.id} references missing Workspace ${candidate.workspaceId}"
                    }

                require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                    "Canonical EXECUTION_LOG ${candidate.id} must reference a canonical-only Workspace"
                }
                require(!workspace.isDeleted || candidate.isDeleted) {
                    "Live canonical EXECUTION_LOG ${candidate.id} cannot belong to a deleted Workspace"
                }

                localById[candidate.id]?.let { local ->
                    require(local.contextId == null && local.workspaceId != null) {
                        "EXECUTION_LOG id collision between legacy Context and canonical Workspace streams: ${candidate.id}"
                    }
                    require(local.workspaceId == candidate.workspaceId) {
                        "Canonical EXECUTION_LOG ownership cannot move between Workspaces: ${candidate.id}"
                    }
                }
            }

            val winners =
                incoming.filter { candidate ->
                    val local = localById[candidate.id] ?: return@filter true
                    incomingWins(candidate, local)
                }

            if (winners.isNotEmpty()) {
                contextManagementDao.insertLogs(winners.map { it.toEntity() })
            }
        }

        suspend fun markSynced(versions: List<CanonicalExecutionLogSyncVersion>) {
            if (versions.isEmpty()) return

            val syncedAt = System.currentTimeMillis()
            database.withTransaction {
                versions.forEach { sent ->
                    contextManagementDao.markCanonicalExecutionLogSyncedIfVersionMatches(
                        id = sent.id,
                        expectedVersion = sent.version,
                        syncedAt = syncedAt,
                    )
                }
            }
        }

        private fun incomingWins(
            incoming: CanonicalExecutionLogSnapshot,
            local: ContextLog,
        ): Boolean {
            val localUpdatedAt = local.updatedAt ?: Long.MIN_VALUE
            return when {
                incoming.version != local.version -> incoming.version > local.version
                incoming.updatedAt != localUpdatedAt -> incoming.updatedAt > localUpdatedAt
                incoming.isDeleted != local.isDeleted -> incoming.isDeleted
                else -> false
            }
        }
    }

private fun ContextLog.toCanonicalSnapshot(): CanonicalExecutionLogSnapshot {
    require(contextId == null && workspaceId != null) {
        "Canonical EXECUTION_LOG persistence row must have contextId=null and workspaceId!=null"
    }

    return CanonicalExecutionLogSnapshot(
        id = id,
        workspaceId = requireNotNull(workspaceId),
        timestamp = timestamp,
        type = type,
        description = description,
        details = details,
        updatedAt =
            requireNotNull(updatedAt) {
                "Canonical EXECUTION_LOG persistence row must have updatedAt"
            },
        version = version,
        isDeleted = isDeleted,
    )
}

private fun CanonicalExecutionLogSnapshot.toEntity(): ContextLog =
    ContextLog(
        id = id,
        contextId = null,
        timestamp = timestamp,
        type = type,
        description = description,
        details = details,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
    )

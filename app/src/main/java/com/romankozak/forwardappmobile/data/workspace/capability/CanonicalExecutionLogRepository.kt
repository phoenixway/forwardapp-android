package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed EXECUTION_LOG command boundary.
 *
 * Shared capability-instance lifecycle is delegated to the kernel. This
 * repository remains the owner of execution-log content and its invariants.
 */
@Singleton
class CanonicalExecutionLogRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextManagementDao: ContextManagementDao,
        private val instanceStore: CanonicalCapabilityInstanceStore,
    ) {
        suspend fun enable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): String = instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.disable(SPEC, workspaceId, now)

        suspend fun archive(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.restore(SPEC, workspaceId, now)

        suspend fun delete(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.delete(SPEC, workspaceId, now)

        suspend fun getLiveLogs(workspaceId: String): List<ContextLog> {
            requireCanonicalWorkspace(workspaceId)
            return contextManagementDao.getLiveCanonicalExecutionLogsForWorkspace(workspaceId)
        }

        suspend fun createLog(
            workspaceId: String,
            type: String,
            description: String,
            details: String? = null,
            timestamp: Long = System.currentTimeMillis(),
            now: Long = timestamp,
        ): String =
            database.withTransaction {
                requireAuthoringEnabled(workspaceId)

                val id = UUID.randomUUID().toString()
                require(contextManagementDao.getLogById(id) == null) {
                    "Generated EXECUTION_LOG id already exists"
                }

                contextManagementDao.insertLog(
                    ContextLog(
                        id = id,
                        contextId = null,
                        timestamp = timestamp,
                        type = type,
                        description = description,
                        details = details,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        workspaceId = workspaceId,
                    ),
                )
                id
            }

        suspend fun updateLog(
            workspaceId: String,
            logId: String,
            type: String,
            description: String,
            details: String? = null,
            timestamp: Long? = null,
            now: Long = System.currentTimeMillis(),
        ) =
            database.withTransaction {
                requireAuthoringEnabled(workspaceId)
                val current = requireCanonicalOwnedLog(workspaceId, logId)
                require(!current.isDeleted) {
                    "Deleted EXECUTION_LOG row cannot be updated"
                }

                contextManagementDao.insertLog(
                    current.bumpSync(now).copy(
                        timestamp = timestamp ?: current.timestamp,
                        type = type,
                        description = description,
                        details = details,
                    ),
                )
            }

        suspend fun deleteLog(
            workspaceId: String,
            logId: String,
            now: Long = System.currentTimeMillis(),
        ) =
            database.withTransaction {
                requireAuthoringEnabled(workspaceId)
                val current = requireCanonicalOwnedLog(workspaceId, logId)
                if (current.isDeleted) return@withTransaction

                contextManagementDao.insertLog(current.softDelete(now))
            }

        private suspend fun requireAuthoringEnabled(workspaceId: String) {
            instanceStore.requireActiveInstance(SPEC, workspaceId)
        }

        private suspend fun requireCanonicalOwnedLog(
            workspaceId: String,
            logId: String,
        ): ContextLog {
            val log =
                requireNotNull(contextManagementDao.getLogById(logId)) {
                    "EXECUTION_LOG row does not exist"
                }

            require(log.contextId == null && log.workspaceId != null) {
                "Canonical EXECUTION_LOG commands cannot mutate a legacy Context row"
            }
            require(log.workspaceId == workspaceId) {
                "EXECUTION_LOG row belongs to another Workspace"
            }
            return log
        }

        private suspend fun requireCanonicalWorkspace(workspaceId: String) {
            val workspace =
                requireNotNull(workspaceDao.getById(workspaceId)) {
                    "Workspace does not exist"
                }

            require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "EXECUTION_LOG canonical commands require CANONICAL_ONLY Workspace ownership"
            }
        }

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.EXECUTION_LOG,
                    configurationCodec = ExecutionLogCapabilityConfigurationCodec,
                )
        }
    }

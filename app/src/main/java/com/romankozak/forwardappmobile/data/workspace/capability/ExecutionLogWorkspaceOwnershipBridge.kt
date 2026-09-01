package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import javax.inject.Inject
import javax.inject.Singleton

data class ExecutionLogWorkspaceOwnershipRepairReport(
    val assignedLogs: Int,
    val unresolvedContexts: Int,
)

/**
 * Transitional EXECUTION_LOG ownership boundary.
 *
 * A Workspace owner is assigned only when Workspace provenance proves that
 * the Workspace is the Context-backed projection of the same Context.
 */
@Singleton
class ExecutionLogWorkspaceOwnershipBridge
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val contextManagementDao: ContextManagementDao,
    ) {
        suspend fun resolveContextBackedWorkspaceId(contextId: String): String? =
            workspaceDao.getContextBackedForContextId(contextId)?.id

        suspend fun repairUnresolved(): ExecutionLogWorkspaceOwnershipRepairReport =
            database.withTransaction {
                var canonicalizedLogs = 0
                val unresolvedContextIds = linkedSetOf<String>()

                contextManagementDao.getLegacyContextLogs().forEach { log ->
                    val contextId = log.contextId ?: return@forEach
                    val workspace = workspaceDao.getContextBackedForContextId(contextId)

                    if (workspace == null) {
                        unresolvedContextIds += contextId
                        return@forEach
                    }

                    if (log.workspaceId != null && log.workspaceId != workspace.id) {
                        unresolvedContextIds += contextId
                        return@forEach
                    }

                    if (workspace.isDeleted && !log.isDeleted) {
                        unresolvedContextIds += contextId
                        return@forEach
                    }

                    contextManagementDao.insertLog(
                        log.copy(
                            contextId = null,
                            workspaceId = workspace.id,
                            updatedAt = log.updatedAt ?: log.timestamp,
                            syncedAt = null,
                        ),
                    )
                    canonicalizedLogs += 1
                }

                ExecutionLogWorkspaceOwnershipRepairReport(
                    assignedLogs = canonicalizedLogs,
                    unresolvedContexts = unresolvedContextIds.size,
                )
            }

    }

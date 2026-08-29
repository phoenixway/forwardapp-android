package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
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
        private val workspaceDao: WorkspaceDao,
        private val contextManagementDao: ContextManagementDao,
    ) {
        suspend fun resolveContextBackedWorkspaceId(contextId: String): String? =
            workspaceDao.getContextBackedForContextId(contextId)?.id

        suspend fun repairUnresolved(): ExecutionLogWorkspaceOwnershipRepairReport {
            var assignedLogs = 0
            var unresolvedContexts = 0

            contextManagementDao.getContextIdsWithoutWorkspaceOwner().forEach { contextId ->
                val workspaceId = resolveContextBackedWorkspaceId(contextId)
                if (workspaceId == null) {
                    unresolvedContexts += 1
                } else {
                    assignedLogs +=
                        contextManagementDao.assignWorkspaceOwnerForContext(
                            contextId = contextId,
                            workspaceId = workspaceId,
                        )
                }
            }

            return ExecutionLogWorkspaceOwnershipRepairReport(
                assignedLogs = assignedLogs,
                unresolvedContexts = unresolvedContexts,
            )
        }
    }

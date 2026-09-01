package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLogEntryTypeValues
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextLogRepository
    @Inject
    constructor(
        private val contextManagementDao: ContextManagementDao,
        private val workspaceOwnershipBridge: ExecutionLogWorkspaceOwnershipBridge,
        private val canonicalExecutionLogRepository: CanonicalExecutionLogRepository,
    ) {

        fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> = contextManagementDao.getLogsForContextStream(contextId)

        suspend fun addToggleContextManagementLog(
            contextId: String,
            isEnabled: Boolean,
        ) {
            val status = if (isEnabled) "активовано" else "деактивовано"
            addSystemContextLogEntry(
                contextId = contextId,
                type = ContextLogEntryTypeValues.AUTOMATIC,
                description = "Управління контекстом було $status.",
            )
        }

        suspend fun addUpdateContextStatusLog(
            contextId: String,
            newStatus: String,
            statusText: String?,
        ) {
            val logDescription =
                "Статус змінено на '$newStatus'." +
                    (statusText?.let { "\nКоментар: $it" } ?: "")
            addSystemContextLogEntry(
                contextId = contextId,
                type = ContextLogEntryTypeValues.STATUS_CHANGE,
                description = logDescription,
            )
        }

        suspend fun addContextComment(
            contextId: String,
            comment: String,
        ) {
            addContextLogEntry(
                contextId = contextId,
                type = ContextLogEntryTypeValues.COMMENT,
                description = comment,
            )
        }

        suspend fun addContextLogEntry(
            contextId: String,
            type: String,
            description: String,
            details: String? = null,
        ) {
            val workspaceId = requireContextBackedWorkspace(contextId)
            canonicalExecutionLogRepository.createLog(
                workspaceId = workspaceId,
                type = type,
                description = description,
                details = details,
            )
        }

        suspend fun addSystemContextLogEntry(
            contextId: String,
            type: String,
            description: String,
            details: String? = null,
        ) {
            val workspaceId = requireContextBackedWorkspace(contextId)
            canonicalExecutionLogRepository.createSystemLog(
                workspaceId = workspaceId,
                type = type,
                description = description,
                details = details,
            )
        }

        private suspend fun requireContextBackedWorkspace(contextId: String): String =
            requireNotNull(workspaceOwnershipBridge.resolveContextBackedWorkspaceId(contextId)) {
                "EXECUTION_LOG Context $contextId has no provenance-backed Workspace owner"
            }

        suspend fun updateContextExecutionLog(log: ContextLog) {
            val workspaceId =
                requireNotNull(log.workspaceId) {
                    "EXECUTION_LOG row has no Workspace owner"
                }
            require(log.contextId == null) {
                "Legacy Context EXECUTION_LOG row must be canonicalized before mutation"
            }
            canonicalExecutionLogRepository.updateLog(
                workspaceId = workspaceId,
                logId = log.id,
                type = log.type,
                description = log.description,
                details = log.details,
                timestamp = log.timestamp,
            )
        }

        suspend fun deleteContextExecutionLog(log: ContextLog) {
            val workspaceId =
                requireNotNull(log.workspaceId) {
                    "EXECUTION_LOG row has no Workspace owner"
                }
            require(log.contextId == null) {
                "Legacy Context EXECUTION_LOG row must be canonicalized before mutation"
            }
            canonicalExecutionLogRepository.deleteLog(
                workspaceId = workspaceId,
                logId = log.id,
            )
        }

        suspend fun tombstoneOwnedContentForWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int =
            canonicalExecutionLogRepository.tombstoneOwnedContentForWorkspaces(
                workspaceIds = workspaceIds,
                now = now,
            )
    }

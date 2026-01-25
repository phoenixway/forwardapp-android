package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLogEntryTypeValues
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextLogRepository
    @Inject
    constructor(
        private val contextManagementDao: ContextManagementDao,
    ) {
        fun getContextLogsStream(contextId: String): Flow<List<ContextLog>> = contextManagementDao.getLogsForContextStream(contextId)

        suspend fun addToggleContextManagementLog(
            contextId: String,
            isEnabled: Boolean,
        ) {
            val status = if (isEnabled) "активовано" else "деактивовано"
            addContextLogEntry(
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
            addContextLogEntry(
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
            val now = System.currentTimeMillis()
            val logEntry =
                ContextLog(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    timestamp = now,
                    type = type,
                    description = description,
                    details = details,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            contextManagementDao.insertLog(logEntry)
        }

        suspend fun updateContextExecutionLog(log: ContextLog) {
            contextManagementDao.updateLog(log.bumpSync())
        }

        suspend fun deleteContextExecutionLog(log: ContextLog) {
            contextManagementDao.insertLog(log.softDelete())
        }
    }

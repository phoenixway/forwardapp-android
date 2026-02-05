package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLogEntryTypeValues
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ActivityManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class LogActivityHandler
    @Inject
    constructor(
        private val contextLogRepository: ContextLogRepository,
        private val activityManager: ActivityManager,
        private val stateManager: ContextStateManager,
        private val scope: CoroutineScope,
    ) {
        fun onEditLogEntry(log: ContextLog) = stateManager.updateState { it.copy(logEntryToEdit = log) }

        fun onDismissEditLogEntryDialog() = stateManager.updateState { it.copy(logEntryToEdit = null) }

        fun onDeleteLogEntry(log: ContextLog) = scope.launch { contextLogRepository.deleteContextExecutionLog(log) }

        fun onUpdateLogEntry(
            log: ContextLog,
            description: String,
            details: String?,
        ) = scope.launch {
            contextLogRepository.updateContextExecutionLog(log.copy(description = description, details = details))
            onDismissEditLogEntryDialog()
        }

        fun onStartTrackingCurrentProject(id: String) = scope.launch { activityManager.startActivity(id) }

        fun stopOngoingActivity() = scope.launch { activityManager.stopActivity() }

        fun setReminderForOngoingActivity(
            activity: ActivityRecord,
            time: Long,
        ) { /* логіка */ }

        // New methods
        fun addProjectComment(
            text: String,
            contextId: String,
        ) {
            if (text.isBlank()) return
            scope.launch {
                contextLogRepository.addContextComment(contextId, text)
            }
        }

        fun addMilestone(
            text: String,
            contextId: String,
        ) {
            if (text.isBlank()) return
            scope.launch {
                contextLogRepository.addContextLogEntry(
                    contextId = contextId,
                    type = ContextLogEntryTypeValues.MILESTONE,
                    description = text,
                )
            }
        }
    }

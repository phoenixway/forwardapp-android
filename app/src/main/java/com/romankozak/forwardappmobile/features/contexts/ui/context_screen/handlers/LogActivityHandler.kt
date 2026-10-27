package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLogEntryTypeValues
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ActivityManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LogActivityHandler
    constructor(
        private val contextLogRepository: ContextLogRepository,
        private val activityManager: ActivityManager,
        private val stateManager: ContextStateManager,
        private val scope: CoroutineScope,
        private val canonicalWorkspaceRoute: Boolean,
        private val onError: (String) -> Unit,
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
            @Suppress("UNUSED_PARAMETER") activity: ActivityRecord,
            @Suppress("UNUSED_PARAMETER") time: Long,
        ) { /* логіка */ }

        // New methods
        fun addProjectComment(
            text: String,
            contextId: String,
        ) {
            submitLog(text, contextId, ContextLogEntryTypeValues.COMMENT, InputMode.AddProjectLog)
        }

        fun addMilestone(
            text: String,
            contextId: String,
        ) {
            submitLog(text, contextId, ContextLogEntryTypeValues.MILESTONE, InputMode.AddMilestone)
        }

        private fun submitLog(text: String, ownerId: String, type: String, inputMode: InputMode) {
            if (text.isBlank()) return
            scope.launch {
                try {
                    if (canonicalWorkspaceRoute) {
                        contextLogRepository.addWorkspaceLogEntry(ownerId, type, text)
                    } else {
                        contextLogRepository.addContextLogEntry(ownerId, type, text)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    android.util.Log.e("WorkspaceLog", "Failed to append $type", error)
                    stateManager.updateState { current ->
                        val sameOwner = if (canonicalWorkspaceRoute) current.presentation?.id == ownerId else current.context?.id == ownerId
                        if (sameOwner && current.inputMode == inputMode && current.inputValue.text.isBlank()) {
                            current.copy(inputValue = TextFieldValue(text))
                        } else current
                    }
                    onError("Не вдалося додати запис до логу.")
                }
            }
        }
    }

package com.romankozak.forwardappmobile.ui.screens.activitytracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.domain.reminders.cancelForActivityRecord
import com.romankozak.forwardappmobile.domain.reminders.scheduleForActivityRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ActivityTrackerViewModel
    @Inject
    constructor(
        private val repository: ActivityRepository,
        private val alarmScheduler: AlarmScheduler,
        private val savedStateHandle: SavedStateHandle, 
    ) : ViewModel() {
        private val _inputText = MutableStateFlow("")
        val inputText = _inputText.asStateFlow()

        private val _editingRecord = MutableStateFlow<ActivityRecord?>(null)
        val editingRecord = _editingRecord.asStateFlow()

        private val _recordToDelete = MutableStateFlow<ActivityRecord?>(null)
        val recordToDelete = _recordToDelete.asStateFlow()

        private val _isEditingLastTimedRecord = MutableStateFlow(false)
        val isEditingLastTimedRecord = _isEditingLastTimedRecord.asStateFlow()

        private val _recordForReminder = MutableStateFlow<ActivityRecord?>(null)
        val recordForReminder = _recordForReminder.asStateFlow()

        val activityLog: StateFlow<List<ActivityRecord>> =
            repository
                .getLogStream()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val lastOngoingActivity: StateFlow<ActivityRecord?> =
            activityLog
                .map { log ->
                    log.firstOrNull { it.isOngoing }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun onInputTextChanged(text: String) {
            _inputText.value = text
        }

        private fun clearInput() {
            _inputText.value = ""
        }

        init {
            savedStateHandle.get<String>("recordIdToEdit")?.let { recordId ->
                viewModelScope.launch {
                    activityLog
                        .mapNotNull { log -> log.find { it.id == recordId } }
                        .first()
                        .let { record ->
                            onEditRequest(record)
                            savedStateHandle.remove<String>("recordIdToEdit")
                        }
                }
            }
        }

        fun onTimelessRecordClick() =
            viewModelScope.launch {
                if (_inputText.value.isBlank()) return@launch
                repository.addTimelessRecord(_inputText.value)
                clearInput()
            }

        fun onToggleStartStop() =
            viewModelScope.launch {
                val text = _inputText.value
                val ongoingActivity = lastOngoingActivity.value
                val now = System.currentTimeMillis()

                if (text.isNotBlank()) {
                    repository.startActivity(text, now)
                } else if (ongoingActivity != null) {
                    repository.endLastActivity(now)
                }

                clearInput()
            }

        fun onEditRequest(record: ActivityRecord) {
            if (!record.isTimeless) {
                val timedRecords = activityLog.value.filter { !it.isTimeless }
                val lastTimedRecord = timedRecords.lastOrNull()
                _isEditingLastTimedRecord.value = (record.id == lastTimedRecord?.id)
            } else {
                _isEditingLastTimedRecord.value = false
            }
            _editingRecord.value = record
        }

        fun onEditDialogDismiss() {
            _editingRecord.value = null
            _isEditingLastTimedRecord.value = false
        }

        fun onRecordUpdated(
            newText: String,
            newStartTime: Long?,
            newEndTime: Long?,
        ) = viewModelScope.launch {
            val recordToUpdate = _editingRecord.value
            if (recordToUpdate != null && newText.isNotBlank()) {
                val isTimeValid = if (newStartTime != null && newEndTime != null) newEndTime >= newStartTime else true

                if (isTimeValid) {
                    if (newStartTime != null && newEndTime == null) {
                        lastOngoingActivity.value?.let {
                            if (it.id != recordToUpdate.id) {
                                repository.endLastActivity(System.currentTimeMillis())
                            }
                        }
                    }
                    val updatedRecord = recordToUpdate.copy(text = newText, startTime = newStartTime, endTime = newEndTime)
                    repository.updateRecord(updatedRecord)
                }
            }
            onEditDialogDismiss()
        }

        fun onRestartActivity(record: ActivityRecord) =
            viewModelScope.launch {
                val ongoingActivity = lastOngoingActivity.value
                val now = System.currentTimeMillis()

                if (ongoingActivity != null) {
                    repository.endLastActivity(now)
                }

                repository.startActivity(record.text, now)
                clearInput()
            }

        fun onDeleteRequest(record: ActivityRecord) {
            _recordToDelete.value = record
        }

        fun onDeleteConfirm() =
            viewModelScope.launch {
                _recordToDelete.value?.let {
                    repository.deleteRecord(it)
                }
                onDeleteDismiss()
            }

        fun onDeleteDismiss() {
            _recordToDelete.value = null
        }

        fun onClearLogConfirm() =
            viewModelScope.launch {
                repository.clearLog()
            }

        fun onSetReminder(record: ActivityRecord) {
            _recordForReminder.value = record
        }

        fun onReminderDialogDismiss() {
            _recordForReminder.value = null
        }

        fun onSetReminder(timestamp: Long) =
            viewModelScope.launch {
                val record = _recordForReminder.value ?: return@launch

                val updatedRecord = record.copy(reminderTime = timestamp)
                repository.updateRecord(updatedRecord)

                alarmScheduler.scheduleForActivityRecord(updatedRecord)
                onReminderDialogDismiss()
            }

        fun onClearReminder() =
            viewModelScope.launch {
                val record = _recordForReminder.value
                if (record != null) {
                    val updatedRecord = record.copy(reminderTime = null)
                    repository.updateRecord(updatedRecord)
                    alarmScheduler.cancelForActivityRecord(record)
                }
                onReminderDialogDismiss()
            }
    }

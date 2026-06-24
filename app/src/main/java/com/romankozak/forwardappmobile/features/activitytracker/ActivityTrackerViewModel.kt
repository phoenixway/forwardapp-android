package com.romankozak.forwardappmobile.features.activitytracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.data.repository.ActivityInputOutcome
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.domain.reminders.cancelForActivityRecord
import com.romankozak.forwardappmobile.domain.reminders.scheduleForActivityRecord
import com.romankozak.forwardappmobile.domain.userawareness.StateSlashCommandParser
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

internal fun toDateHeader(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@HiltViewModel
class ActivityTrackerViewModel
    @Inject
    constructor(
        private val repository: ActivityRepository,
        private val alarmScheduler: AlarmScheduler,
        private val savedStateHandle: SavedStateHandle,
        private val slashCommandParser: StateSlashCommandParser,
    ) : ViewModel() {
        private companion object {
            const val INITIAL_RECENT_RECORDS = 320
            const val OLDER_RECORDS_PAGE_SIZE = 220
        }

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
        private val _snackbarEvents = MutableSharedFlow<String>()
        val snackbarEvents = _snackbarEvents.asSharedFlow()
        private val _loadedOlderRecords = MutableStateFlow<List<ActivityRecord>>(emptyList())
        private val _isLoadingOlderRecords = MutableStateFlow(false)
        val isLoadingOlderRecords = _isLoadingOlderRecords.asStateFlow()
        private val _hasMoreOlderRecords = MutableStateFlow(true)
        val hasMoreOlderRecords = _hasMoreOlderRecords.asStateFlow()

        private val recentActivityLog: StateFlow<List<ActivityRecord>> =
            repository
                .getRecentLogStream(INITIAL_RECENT_RECORDS)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val activityLog: StateFlow<List<ActivityRecord>> =
            combine(recentActivityLog, _loadedOlderRecords) { recentRecords, olderRecords ->
                (olderRecords + recentRecords)
                    .distinctBy { it.id }
                    .sortedBy { it.startTime ?: it.createdAt }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val groupedActivityLog: StateFlow<Map<String, List<ActivityRecord>>> =
            activityLog.map { log ->
                log.groupBy { toDateHeader(it.startTime ?: it.createdAt) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        val lastOngoingActivity: StateFlow<ActivityRecord?> =
            repository
                .findLastOngoingActivityFlow()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun onInputTextChanged(text: String) {
            _inputText.value = text
        }

        fun loadOlderRecords() {
            if (_isLoadingOlderRecords.value || !_hasMoreOlderRecords.value) return

            viewModelScope.launch {
                _isLoadingOlderRecords.value = true
                try {
                    val oldestLoadedRecord =
                        listOfNotNull(
                            _loadedOlderRecords.value.firstOrNull(),
                            recentActivityLog.value.firstOrNull(),
                        ).minByOrNull { it.startTime ?: it.createdAt }

                    val beforeCreatedAt = oldestLoadedRecord?.createdAt ?: run {
                        _hasMoreOlderRecords.value = false
                        return@launch
                    }

                    val olderRecords =
                        repository.getOlderLogRecords(
                            beforeCreatedAt = beforeCreatedAt,
                            limit = OLDER_RECORDS_PAGE_SIZE,
                        )

                    if (olderRecords.isEmpty()) {
                        _hasMoreOlderRecords.value = false
                    } else {
                        _loadedOlderRecords.update { loaded ->
                            (olderRecords + loaded)
                                .distinctBy { it.id }
                                .sortedBy { it.startTime ?: it.createdAt }
                        }
                        if (olderRecords.size < OLDER_RECORDS_PAGE_SIZE) {
                            _hasMoreOlderRecords.value = false
                        }
                    }
                } finally {
                    _isLoadingOlderRecords.value = false
                }
            }
        }

        private fun clearInput() {
            _inputText.value = ""
        }

        init {
            viewModelScope.launch {
                recentActivityLog.collect { recentRecords ->
                    if (recentRecords.isEmpty()) {
                        _loadedOlderRecords.value = emptyList()
                        _hasMoreOlderRecords.value = false
                    } else if (_loadedOlderRecords.value.isEmpty()) {
                        _hasMoreOlderRecords.value = recentRecords.size >= INITIAL_RECENT_RECORDS
                    }
                }
            }

            savedStateHandle.get<String>("recordIdToEdit")?.let { recordId ->
                viewModelScope.launch {
                    repository.getActivityRecordById(recordId)?.let { record ->
                        onEditRequest(record)
                        savedStateHandle.remove<String>("recordIdToEdit")
                    }
                }
            }
        }

        fun onTimelessRecordClick() =
            viewModelScope.launch {
                if (_inputText.value.isBlank()) return@launch
                val result = repository.addTimelessRecord(_inputText.value)
                if (result.outcome == ActivityInputOutcome.STATE_CHANGED_ONLY) {
                    result.appliedStateChange?.let { change ->
                        _snackbarEvents.emit(stateChangeMessage(change.type, change.crisisLevel))
                    }
                }
                clearInput()
            }

        fun onToggleStartStop() =
            viewModelScope.launch {
                val text = _inputText.value
                val ongoingActivity = lastOngoingActivity.value
                val now = System.currentTimeMillis()

                if (text.isNotBlank()) {
                    val parsed = slashCommandParser.parse(text)
                    if (parsed.detectedChange != null && parsed.cleanedText.isBlank()) {
                        val result = repository.addTimelessRecord(text, now)
                        if (result.outcome == ActivityInputOutcome.STATE_CHANGED_ONLY) {
                            _snackbarEvents.emit(
                                stateChangeMessage(
                                    parsed.detectedChange.type,
                                    parsed.detectedChange.crisisLevel,
                                ),
                            )
                        }
                    } else {
                        repository.startActivity(text, now)
                    }
                } else if (ongoingActivity != null) {
                    repository.endLastActivity(now)
                }

                clearInput()
            }

        private fun stateChangeMessage(
            type: UserAwarenessStateType,
            crisisLevel: Int?,
        ): String =
            when (type) {
                UserAwarenessStateType.NORMAL -> "Стан: NORMAL"
                UserAwarenessStateType.CRISIS -> "Стан: CRISIS L${crisisLevel ?: 1}"
                UserAwarenessStateType.EXHAUSTION -> "Стан: EXHAUSTION"
                UserAwarenessStateType.UNPRODUCTIVE -> "Стан: LOW DRIVE"
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
            recordKind: String,
            newStartTime: Long?,
            newEndTime: Long?,
            xpGained: Int?,
            antyXp: Int?,
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
                    val updatedRecord =
                        recordToUpdate.copy(
                            text = newText,
                            recordKind = recordKind,
                            startTime = newStartTime,
                            endTime = newEndTime,
                            xpGained = xpGained,
                            antyXp = antyXp,
                        )
                    repository.updateRecord(updatedRecord)
                    _loadedOlderRecords.update { records ->
                        records.map { existing ->
                            if (existing.id == updatedRecord.id) updatedRecord else existing
                        }
                    }
                }
            }
            onEditDialogDismiss()
        }

        fun onAddCompletedAction(
            text: String,
            xpGained: Int?,
            antyXp: Int?,
        ) = viewModelScope.launch {
            repository.addCompletedActivity(text, xpGained, antyXp)
        }

        fun onAddTodaySummary(text: String) =
            viewModelScope.launch {
                repository.upsertTodaySummary(text)
                clearInput()
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
                    _loadedOlderRecords.update { records -> records.filterNot { record -> record.id == it.id } }
                }
                onDeleteDismiss()
            }

        fun onDeleteDismiss() {
            _recordToDelete.value = null
        }

        fun onClearLogConfirm() =
            viewModelScope.launch {
                repository.clearLog()
                _loadedOlderRecords.value = emptyList()
                _hasMoreOlderRecords.value = false
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

                alarmScheduler.cancelForActivityRecord(record)

                val updatedRecord = record.copy(reminderTime = timestamp)
                repository.updateRecord(updatedRecord)
                _loadedOlderRecords.update { records ->
                    records.map { existing ->
                        if (existing.id == updatedRecord.id) updatedRecord else existing
                    }
                }

                alarmScheduler.scheduleForActivityRecord(updatedRecord)
                onReminderDialogDismiss()
            }

        fun onClearReminder() =
            viewModelScope.launch {
                val record = _recordForReminder.value
                if (record != null) {
                    val updatedRecord = record.copy(reminderTime = null)
                    repository.updateRecord(updatedRecord)
                    _loadedOlderRecords.update { records ->
                        records.map { existing ->
                            if (existing.id == updatedRecord.id) updatedRecord else existing
                        }
                    }
                    alarmScheduler.cancelForActivityRecord(record)
                }
                onReminderDialogDismiss()
            }
    }

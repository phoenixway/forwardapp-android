// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/activitytracker/ActivityTrackerViewModel.kt

package com.romankozak.forwardappmobile.ui.screens.activitytracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityTrackerViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    // ✨ НОВИЙ СТАН: Зберігає запис, що редагується, та контролює видимість діалогу
    private val _editingRecord = MutableStateFlow<ActivityRecord?>(null)
    val editingRecord = _editingRecord.asStateFlow()

    val activityLog: StateFlow<List<ActivityRecord>> = repository.getLogStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastOngoingActivity: StateFlow<ActivityRecord?> = activityLog.map { log ->
        log.firstOrNull { it.isOngoing }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    private fun clearInput() {
        _inputText.value = ""
    }

    fun onTimelessRecordClick() = viewModelScope.launch {
        if (_inputText.value.isBlank()) return@launch
        repository.addTimelessRecord(_inputText.value)
        clearInput()
    }

    fun onToggleStartStop() = viewModelScope.launch {
        val text = _inputText.value
        val ongoingActivity = lastOngoingActivity.value
        val now = System.currentTimeMillis()

        if (ongoingActivity != null) {
            repository.endLastActivity(now)
        }

        if (text.isNotBlank()) {
            repository.startActivity(text, now)
        }

        clearInput()
    }

    // ✨ ОНОВЛЕНО: Тепер ця функція відкриває діалог для редагування
    fun onEditRequest(record: ActivityRecord) {
        _editingRecord.value = record
    }

    // ✨ НОВА ФУНКЦІЯ: Закриває діалог редагування
    fun onEditDialogDismiss() {
        _editingRecord.value = null
    }

    // ✨ НОВА ФУНКЦІЯ: Оновлює запис з новим текстом
    fun onRecordUpdated(newText: String) = viewModelScope.launch {
        val recordToUpdate = _editingRecord.value
        if (recordToUpdate != null && newText.isNotBlank()) {
            // Створюємо копію запису з новим текстом
            val updatedRecord = recordToUpdate.copy(text = newText)
            repository.updateRecord(updatedRecord)
        }
        onEditDialogDismiss() // Закриваємо діалог після збереження
    }

    fun onRestartActivity(record: ActivityRecord) = viewModelScope.launch {
        val ongoingActivity = lastOngoingActivity.value
        val now = System.currentTimeMillis()

        if (ongoingActivity != null) {
            repository.endLastActivity(now)
        }

        repository.startActivity(record.text, now)
        clearInput()
    }

    fun onClearLogConfirm() = viewModelScope.launch {
        repository.clearLog()
    }
}
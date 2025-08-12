// Створіть новий файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/activitytracker/ActivityTrackerViewModel.kt

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
        repository.addTimelessRecord(_inputText.value)
        clearInput()
    }

    fun onToggleStartStop() = viewModelScope.launch {
        val text = _inputText.value
        val ongoingActivity = lastOngoingActivity.value
        val now = System.currentTimeMillis()

        // Якщо є активна задача, її потрібно завершити
        if (ongoingActivity != null) {
            repository.endLastActivity(now)
        }

        // Якщо в полі є текст, починаємо нову задачу
        if (text.isNotBlank()) {
            repository.startActivity(text, now)
        }

        clearInput()
    }

    fun onClearLogConfirm() = viewModelScope.launch {
        repository.clearLog()
    }
}
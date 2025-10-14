
package com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    private val entityId: String = savedStateHandle.get<String>("entityId") ?: ""
    private val entityType: String = savedStateHandle.get<String>("entityType") ?: ""

    fun loadReminders(entityId: String, entityType: String) {
        savedStateHandle["entityId"] = entityId
        savedStateHandle["entityType"] = entityType
        viewModelScope.launch {
            projectRepository.reminderDao.getRemindersForEntity(entityId).collect { reminders ->
                _reminders.value = reminders
            }
        }
    }

    fun addReminder(reminderTime: Long) {
        val entityId: String = savedStateHandle.get<String>("entityId") ?: ""
        val entityType: String = savedStateHandle.get<String>("entityType") ?: ""
        if (entityId.isEmpty() || entityType.isEmpty()) return

        viewModelScope.launch {
            val reminder = Reminder(
                entityId = entityId,
                entityType = entityType,
                reminderTime = reminderTime,
                status = "SCHEDULED",
                creationTime = System.currentTimeMillis()
            )
            projectRepository.reminderDao.insert(reminder)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            projectRepository.reminderDao.update(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            projectRepository.reminderDao.delete(reminder)
        }
    }

    private val _showTimePickerDialog = MutableStateFlow(false)
    val showTimePickerDialog: StateFlow<Boolean> = _showTimePickerDialog

    fun onAddTimePickerDismiss() {
        _showTimePickerDialog.value = false
    }

    private val _editingReminder = MutableStateFlow<Reminder?>(null)
    val editingReminder: StateFlow<Reminder?> = _editingReminder

    fun onEditReminder(reminder: Reminder) {
        _editingReminder.value = reminder
    }

    fun onEditReminderDismiss() {
        _editingReminder.value = null
    }

    fun onAddTimePickerShow() {
        _showTimePickerDialog.value = true
    }
}

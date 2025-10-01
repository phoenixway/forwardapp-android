package com.romankozak.forwardappmobile.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    projectRepository: ProjectRepository
) : ViewModel() {

    val reminders: StateFlow<List<Goal>> = projectRepository.getAllGoalsFlow()
        .map { goals -> goals.filter { it.reminderTime != null } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}




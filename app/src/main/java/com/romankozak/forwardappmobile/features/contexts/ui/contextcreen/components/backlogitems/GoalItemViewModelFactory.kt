package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.backlogitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.common.ParsedTextData

class GoalItemViewModelFactory(
    private val goal: Goal,
    private val parsedData: ParsedTextData,
    private val reminder: Reminder?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoalItemViewModel(goal, parsedData, reminder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

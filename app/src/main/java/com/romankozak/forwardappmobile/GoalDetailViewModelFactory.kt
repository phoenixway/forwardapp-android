package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// --- ЗМІНЕНО: Додано highlightedGoalId ---
class GoalDetailViewModelFactory(
    private val goalDao: GoalDao,
    private val listId: String,
    private val highlightedGoalId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Передаємо ID в ViewModel
            return GoalDetailViewModel(goalDao, listId, highlightedGoalId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
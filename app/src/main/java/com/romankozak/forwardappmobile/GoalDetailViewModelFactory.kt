package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GoalDetailViewModelFactory(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao, // --- ДОДАНО ---
    private val listId: String,
    private val highlightedGoalId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Передаємо DAO в ViewModel
            return GoalDetailViewModel(goalDao, goalListDao, listId, highlightedGoalId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
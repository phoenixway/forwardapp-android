package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GoalDetailViewModelFactory(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    private val settingsRepo: SettingsRepository,
    private val listId: String,
    private val highlightedGoalId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // ВИПРАВЛЕНО: Аргументи передаються у правильному порядку
            return GoalDetailViewModel(goalDao, goalListDao, settingsRepo, listId, highlightedGoalId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
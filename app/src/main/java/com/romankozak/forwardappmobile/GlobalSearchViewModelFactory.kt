package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GlobalSearchViewModelFactory(
    private val goalDao: GoalDao,
    private val query: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GlobalSearchViewModel(goalDao, query) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
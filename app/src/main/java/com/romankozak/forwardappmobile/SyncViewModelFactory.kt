package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// --- ВИПРАВЛЕНО: Тепер фабрика приймає SyncRepository ---
class SyncViewModelFactory(
    private val syncRepo: SyncRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyncViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SyncViewModel(syncRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
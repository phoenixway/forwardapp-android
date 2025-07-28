package com.romankozak.forwardappmobile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncViewModel(private val syncRepo: SyncRepository) : ViewModel() {
    private val _report = MutableStateFlow<SyncReport?>(null)
    val report = _report.asStateFlow()

    private val _approvedChangeIds = MutableStateFlow<Set<String>>(emptySet())
    val approvedChangeIds = _approvedChangeIds.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var originalJsonString: String? = null

    fun createReport(syncDataViewModel: SyncDataViewModel) {
        val jsonString = syncDataViewModel.jsonString
        if (jsonString == null) {
            _error.value = "Немає даних для синхронізації."
            return
        }

        viewModelScope.launch {
            originalJsonString = jsonString
            try {
                val syncReport = syncRepo.createSyncReport(jsonString)
                _report.value = syncReport
                _approvedChangeIds.value = syncReport.changes.map { it.id }.toSet()
                _error.value = null
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Error creating sync report", e)
                _error.value = e.message ?: "Сталася невідома помилка."
                _report.value = null
            } finally {
                // Очищуємо дані, щоб не використовувати їх повторно
                syncDataViewModel.jsonString = null
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun toggleApproval(changeId: String) {
        val currentIds = _approvedChangeIds.value.toMutableSet()
        if (changeId in currentIds) {
            currentIds.remove(changeId)
        } else {
            currentIds.add(changeId)
        }
        _approvedChangeIds.value = currentIds
    }

    fun applyChanges(onComplete: () -> Unit) {
        viewModelScope.launch {
            val reportToApply = report.value
            val jsonToApply = originalJsonString

            if (reportToApply != null && jsonToApply != null) {
                val approved = reportToApply.changes.filter { it.id in _approvedChangeIds.value }
                syncRepo.applyChanges(approved, jsonToApply)
            }
            onComplete()
        }
    }
}

// --- КЛАС-ДУБЛІКАТ ВИДАЛЕНО ЗВІДСИ ---
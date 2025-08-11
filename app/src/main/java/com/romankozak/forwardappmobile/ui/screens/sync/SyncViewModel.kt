package com.romankozak.forwardappmobile.ui.screens.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.sync.ChangeType
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.data.sync.SyncReport
import com.romankozak.forwardappmobile.data.sync.SyncRepository
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

    // НОВА ПРАВИЛЬНА ВЕРСІЯ для SyncViewModel.kt
    fun toggleApproval(changeId: String, changeType: String) {
        val compoundId = changeId + changeType // Створюємо такий самий комбінований ключ
        val currentIds = _approvedChangeIds.value.toMutableSet()

        if (compoundId in currentIds) {
            currentIds.remove(compoundId)
        } else {
            currentIds.add(compoundId)
        }
        _approvedChangeIds.value = currentIds
    }

    fun applyChanges(onComplete: () -> Unit) {
        viewModelScope.launch {
            val reportToApply = report.value
            val jsonToApply = originalJsonString

            if (reportToApply != null && jsonToApply != null) {
                val approved = reportToApply.changes.filter { it.id in _approvedChangeIds.value }
                syncRepo.applyChanges(approved)
            }
            onComplete()
        }
    }

    // У файлі SyncViewModel.kt

    fun selectAllChanges() {
        report.value?.changes?.let { allChanges ->
            _approvedChangeIds.value = allChanges.map { it.id + it.type.name }.toSet()
        }
    }

    fun deselectAllChanges() {
        _approvedChangeIds.value = emptySet()
    }

    fun selectRecommendedChanges() {
        report.value?.changes?.let { allChanges ->
            _approvedChangeIds.value = allChanges
                .filter { it.type != ChangeType.Delete } // Обираємо все, крім видалень
                .map { it.id + it.type.name }
                .toSet()
        }
    }
}
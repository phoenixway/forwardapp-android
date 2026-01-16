package com.romankozak.forwardappmobile.features.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ChangeType
import com.romankozak.forwardappmobile.data.repository.SyncReport
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel
    @Inject
    constructor(
        private val syncRepo: SyncRepository,
    ) : ViewModel() {
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
                    _approvedChangeIds.value = syncReport.changes.map { it.id + it.type.name }.toSet()
                    _error.value = null
                } catch (e: Exception) {
                    Log.e("SyncViewModel", "Error creating sync report", e)
                    _error.value = e.message ?: "Сталася невідома помилка."
                    _report.value = null
                } finally {
                    syncDataViewModel.jsonString = null
                }
            }
        }

        fun clearError() {
            _error.value = null
        }

        fun toggleApproval(
            changeId: String,
            changeType: String,
        ) {
            val compoundId = changeId + changeType
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
                if (reportToApply != null) {
                    val approved = reportToApply.changes.filter { (it.id + it.type.name) in _approvedChangeIds.value }
                    syncRepo.applyChanges(approved)
                }
                onComplete()
            }
        }

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
                _approvedChangeIds.value =
                    allChanges
                        .filter { it.type != ChangeType.Delete }
                        .map { it.id + it.type.name }
                        .toSet()
            }
        }
    }

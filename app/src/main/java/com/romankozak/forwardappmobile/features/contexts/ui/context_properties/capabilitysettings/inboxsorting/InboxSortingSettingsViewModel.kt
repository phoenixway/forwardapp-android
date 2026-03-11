package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.inboxsorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextInboxSortingRepository
import com.romankozak.forwardappmobile.data.repository.InboxSortingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxSortingSettingsUiState(
    val contextId: String? = null,
    val rulesText: String = "",
    val isSaving: Boolean = false,
    val isApplying: Boolean = false,
    val lastMessage: String? = null,
)

@HiltViewModel
class InboxSortingSettingsViewModel
    @Inject
    constructor(
        private val repository: ContextInboxSortingRepository,
        private val sortingService: InboxSortingService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(InboxSortingSettingsUiState())
        val uiState: StateFlow<InboxSortingSettingsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null

        fun bind(contextId: String) {
            val currentContextId = _uiState.value.contextId
            if (currentContextId == contextId && observeJob?.isActive == true) return

            observeJob?.cancel()
            _uiState.update { it.copy(contextId = contextId, lastMessage = null) }
            observeJob =
                viewModelScope.launch {
                    repository.observe(contextId).collectLatest { settings ->
                        _uiState.update { state ->
                            if (state.isSaving) state else state.copy(rulesText = settings.rulesText)
                        }
                    }
                }
        }

        fun onRulesTextChanged(value: String) {
            _uiState.update { it.copy(rulesText = value, lastMessage = null) }
        }

        fun saveRules() {
            val contextId = _uiState.value.contextId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, lastMessage = null) }
                repository.updateRulesText(contextId = contextId, rulesText = _uiState.value.rulesText)
                _uiState.update { it.copy(isSaving = false, lastMessage = "Правила збережено") }
            }
        }

        fun applySort(target: InboxSortingService.SortTarget) {
            val contextId = _uiState.value.contextId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isApplying = true, lastMessage = null) }
                val affected =
                    sortingService.applySorting(
                        contextId = contextId,
                        rulesText = _uiState.value.rulesText,
                        target = target,
                    )
                val targetLabel =
                    when (target) {
                        InboxSortingService.SortTarget.BACKLOG -> "беклог"
                        InboxSortingService.SortTarget.INBOX_RECORDS -> "inbox записи"
                        InboxSortingService.SortTarget.ATTACHMENTS -> "зв'язки"
                    }
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        lastMessage = "Впорядковано $affected елементів ($targetLabel)",
                    )
                }
            }
        }
    }

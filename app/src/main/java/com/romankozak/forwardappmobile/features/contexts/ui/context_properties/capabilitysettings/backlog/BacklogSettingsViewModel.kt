package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.backlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogConfigurationAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BacklogSettingsUiState(
    val contextId: String? = null,
    val removeAfterAutocopyEntriesWithTags: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
class BacklogSettingsViewModel
    @Inject
    constructor(
        private val contextStructureRepository: ContextStructureRepository,
        private val systemBacklogConfigurationAccess: SystemContextCanonicalBacklogConfigurationAccess,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BacklogSettingsUiState())
        val uiState: StateFlow<BacklogSettingsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null

        fun bind(contextId: String) {
            val currentContextId = _uiState.value.contextId
            if (currentContextId == contextId && observeJob?.isActive == true) return

            observeJob?.cancel()
            _uiState.update { it.copy(contextId = contextId) }
            observeJob =
                viewModelScope.launch {
                    if (systemBacklogConfigurationAccess.handles(contextId)) {
                        systemBacklogConfigurationAccess.observeState(contextId).collectLatest { canonical ->
                            _uiState.update { state ->
                                if (state.isSaving) {
                                    state
                                } else {
                                    state.copy(
                                        removeAfterAutocopyEntriesWithTags =
                                            canonical?.removeEntryAfterTagAutocopy == true,
                                    )
                                }
                            }
                        }
                        return@launch
                    }
                    contextStructureRepository.observeStructureOnly(contextId).collectLatest { structure ->
                        if (structure == null) {
                            contextStructureRepository.ensureStructure(contextId)
                            return@collectLatest
                        }
                        _uiState.update { state ->
                            if (state.isSaving) {
                                state
                            } else {
                                state.copy(
                                    removeAfterAutocopyEntriesWithTags =
                                        structure.removeBacklogEntryAfterTagAutocopy == true,
                                )
                            }
                        }
                    }
                }
        }

        fun onRemoveAfterAutocopyEntriesWithTagsChanged(enabled: Boolean) {
            val contextId = _uiState.value.contextId ?: return
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isSaving = true,
                        removeAfterAutocopyEntriesWithTags = enabled,
                    )
                }
                try {
                    if (systemBacklogConfigurationAccess.handles(contextId)) {
                        systemBacklogConfigurationAccess.setRemoveEntryAfterTagAutocopy(contextId, enabled)
                    } else {
                        val structure = contextStructureRepository.ensureStructure(contextId)
                        contextStructureRepository.updateStructure(
                            structure.copy(
                                removeBacklogEntryAfterTagAutocopy = enabled,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    reloadCanonicalWinner(contextId)
                } finally {
                    _uiState.update { state ->
                        if (state.contextId == contextId) state.copy(isSaving = false) else state
                    }
                }
            }
        }

        private suspend fun reloadCanonicalWinner(contextId: String) {
            if (!systemBacklogConfigurationAccess.handles(contextId)) return
            val canonical = systemBacklogConfigurationAccess.getState(contextId)
            _uiState.update { state ->
                if (state.contextId == contextId) {
                    state.copy(removeAfterAutocopyEntriesWithTags = canonical?.removeEntryAfterTagAutocopy == true)
                } else {
                    state
                }
            }
        }
    }

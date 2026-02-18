package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.direction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DirectionSettingsUiState(
    val contextId: String? = null,
    val autoAddChildContextToDirectionFront: Boolean = true,
    val isSaving: Boolean = false,
)

@HiltViewModel
class DirectionSettingsViewModel
    @Inject
    constructor(
        private val contextStructureRepository: ContextStructureRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DirectionSettingsUiState())
        val uiState: StateFlow<DirectionSettingsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null

        fun bind(contextId: String) {
            val currentContextId = _uiState.value.contextId
            if (currentContextId == contextId && observeJob?.isActive == true) return

            observeJob?.cancel()
            _uiState.update { it.copy(contextId = contextId) }
            observeJob =
                viewModelScope.launch {
                    contextStructureRepository.observeStructureOnly(contextId).collectLatest { structure ->
                        if (structure == null) {
                            contextStructureRepository.ensureStructure(contextId)
                            return@collectLatest
                        }
                        _uiState.update { state ->
                            if (state.isSaving) {
                                state
                            } else {
                                state.copy(autoAddChildContextToDirectionFront = structure.enableAutoLinkSubprojects ?: true)
                            }
                        }
                    }
                }
        }

        fun onAutoAddChildContextToDirectionFrontChanged(enabled: Boolean) {
            val contextId = _uiState.value.contextId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, autoAddChildContextToDirectionFront = enabled) }
                val structure = contextStructureRepository.ensureStructure(contextId)
                contextStructureRepository.updateStructure(
                    structure.copy(
                        enableAutoLinkSubprojects = enabled,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

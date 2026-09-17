package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.direction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectionSettingsUiState(
    val contextId: String? = null,
    val autoAddChildContextToDirectionFront: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class DirectionSettingsViewModel
    @Inject
    constructor(
        private val contextStructureRepository: ContextStructureRepository,
        private val contextRepository: ContextRepository,
        private val systemCapabilityAccess: SystemContextCanonicalInboxDirectionAccess,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DirectionSettingsUiState())
        val uiState: StateFlow<DirectionSettingsUiState> = _uiState.asStateFlow()

        private var observeJob: Job? = null

        fun bind(contextId: String) {
            val currentContextId = _uiState.value.contextId
            if (currentContextId == contextId && observeJob?.isActive == true) return

            observeJob?.cancel()
            _uiState.update { it.copy(contextId = contextId, errorMessage = null) }
            observeJob =
                viewModelScope.launch {
                    if (systemCapabilityAccess.handles(contextId)) {
                        systemCapabilityAccess.observeState(contextId).collectLatest { canonical ->
                            _uiState.update { state ->
                                if (state.isSaving) state else state.copy(
                                    autoAddChildContextToDirectionFront =
                                        canonical?.direction?.configuration?.autoLinkChildWorkspaces == true,
                                )
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
                                    autoAddChildContextToDirectionFront =
                                        structure.enableAutoLinkSubprojects ?: true,
                                )
                            }
                        }
                    }
                }
        }

        fun onAutoAddChildContextToDirectionFrontChanged(enabled: Boolean) {
            val contextId = _uiState.value.contextId ?: return
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isSaving = true,
                        autoAddChildContextToDirectionFront = enabled,
                        errorMessage = null,
                    )
                }
                try {
                    val handled =
                        systemCapabilityAccess.updateDirectionConfiguration(
                            contextId = contextId,
                            configuration = DirectionCapabilityConfigurationV1(enabled),
                        )
                    if (!handled) {
                        val structure = contextStructureRepository.ensureStructure(contextId)
                        contextStructureRepository.updateStructure(
                            structure.copy(
                                enableAutoLinkSubprojects = enabled,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                    if (enabled) {
                        contextRepository.ensureDirectionFrontLinksForExistingChildren(contextId)
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val restored = loadCurrentValue(contextId)
                    _uiState.update { state ->
                        if (state.contextId != contextId) state else state.copy(
                            autoAddChildContextToDirectionFront = restored,
                            errorMessage = error.message ?: "Не вдалося зберегти налаштування Direction",
                        )
                    }
                } finally {
                    _uiState.update { state ->
                        if (state.contextId != contextId) state else state.copy(isSaving = false)
                    }
                }
            }
        }

        private suspend fun loadCurrentValue(contextId: String): Boolean =
            if (systemCapabilityAccess.handles(contextId)) {
                systemCapabilityAccess.getState(contextId)
                    ?.direction
                    ?.configuration
                    ?.autoLinkChildWorkspaces == true
            } else {
                contextStructureRepository.getStructureByContext(contextId)
                    ?.enableAutoLinkSubprojects ?: true
            }
    }

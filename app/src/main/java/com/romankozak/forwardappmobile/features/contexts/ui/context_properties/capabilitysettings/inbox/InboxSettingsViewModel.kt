package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxOwnerVisibility
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

data class InboxSettingsUiState(
    val contextId: String? = null,
    val removeAfterAutocopyEntriesWithTags: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class InboxSettingsViewModel
    @Inject
    constructor(
        private val contextStructureRepository: ContextStructureRepository,
        private val systemCapabilityAccess: SystemContextCanonicalInboxDirectionAccess,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(InboxSettingsUiState())
        val uiState: StateFlow<InboxSettingsUiState> = _uiState.asStateFlow()

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
                                    removeAfterAutocopyEntriesWithTags =
                                        canonical?.inbox?.configuration?.ownerVisibility ==
                                            InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED,
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
                                    removeAfterAutocopyEntriesWithTags =
                                        structure.removeInboxEntryAfterTagAutocopy == true,
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
                        errorMessage = null,
                    )
                }
                try {
                    val handled =
                        systemCapabilityAccess.updateInboxConfiguration(
                            contextId = contextId,
                            configuration =
                                InboxCapabilityConfigurationV1(
                                    ownerVisibility =
                                        if (enabled) {
                                            InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED
                                        } else {
                                            InboxOwnerVisibility.KEEP_VISIBLE
                                        },
                                ),
                        )
                    if (!handled) {
                        val structure = contextStructureRepository.ensureStructure(contextId)
                        contextStructureRepository.updateStructure(
                            structure.copy(
                                removeInboxEntryAfterTagAutocopy = enabled,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val restored = loadCurrentValue(contextId)
                    _uiState.update { state ->
                        if (state.contextId != contextId) state else state.copy(
                            removeAfterAutocopyEntriesWithTags = restored,
                            errorMessage = error.message ?: "Не вдалося зберегти налаштування Inbox",
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
                    ?.inbox
                    ?.configuration
                    ?.ownerVisibility == InboxOwnerVisibility.HIDE_WHEN_ASSOCIATED
            } else {
                contextStructureRepository.getStructureByContext(contextId)
                    ?.removeInboxEntryAfterTagAutocopy == true
            }
    }

package com.romankozak.forwardappmobile.features.contexts.ui.context_configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class StructurePresetsUiState(
    val presets: List<ContextRoleProfile> = emptyList(),
    val selectedPreset: ContextRoleProfile? = null,
)

@HiltViewModel
class StructurePresetsViewModel
    @Inject
    constructor(
        private val structurePresetDao: StructurePresetDao,
        private val contextStructureRepository: ContextStructureRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StructurePresetsUiState())
        val uiState: StateFlow<StructurePresetsUiState> = _uiState.asStateFlow()

        init {
            observePresets()
        }

        private fun observePresets() {
            viewModelScope.launch {
                contextStructureRepository.ensureReservedBaseRolePresets()
                structurePresetDao.getAll().collectLatest { presets ->
                    val selected = _uiState.value.selectedPreset
                    val newSelected = selected ?: presets.firstOrNull()
                    _uiState.update { it.copy(presets = presets, selectedPreset = newSelected) }
                }
            }
        }

        fun selectPreset(preset: ContextRoleProfile) {
            _uiState.update { it.copy(selectedPreset = preset) }
        }

        fun addPreset(
            code: String,
            label: String,
            description: String?,
        ) {
            viewModelScope.launch {
                val preset =
                    ContextRoleProfile(
                        id = UUID.randomUUID().toString(),
                        code = code,
                        label = label,
                        description = description,
                    )
                structurePresetDao.insertPreset(preset)
            }
        }

        fun removePreset(preset: ContextRoleProfile) {
            if (ContextRoleRegistry.isReservedBaseRole(preset.code)) return
            viewModelScope.launch {
                structurePresetDao.deleteById(preset.id)
                if (_uiState.value.selectedPreset?.id == preset.id) {
                    _uiState.update { it.copy(selectedPreset = null) }
                }
            }
        }
    }

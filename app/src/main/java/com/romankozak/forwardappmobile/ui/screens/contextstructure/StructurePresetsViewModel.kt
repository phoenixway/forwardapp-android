package com.romankozak.forwardappmobile.ui.screens.contextstructure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.data.database.models.StructurePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class StructurePresetsUiState(
    val presets: List<StructurePreset> = emptyList(),
    val selectedPreset: StructurePreset? = null,
)

@HiltViewModel
class StructurePresetsViewModel @Inject constructor(
    private val structurePresetDao: StructurePresetDao,
    private val structurePresetItemDao: StructurePresetItemDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StructurePresetsUiState())
    val uiState: StateFlow<StructurePresetsUiState> = _uiState.asStateFlow()

    init {
        observePresets()
    }

    private fun observePresets() {
        viewModelScope.launch {
            structurePresetDao.getAll().collectLatest { presets ->
                val selected = _uiState.value.selectedPreset
                val newSelected = selected ?: presets.firstOrNull()
                _uiState.update { it.copy(presets = presets, selectedPreset = newSelected) }
            }
        }
    }

    fun selectPreset(preset: StructurePreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun addPreset(code: String, label: String, description: String?) {
        viewModelScope.launch {
            val preset = StructurePreset(
                id = UUID.randomUUID().toString(),
                code = code,
                label = label,
                description = description,
            )
            structurePresetDao.insertPreset(preset)
        }
    }
}

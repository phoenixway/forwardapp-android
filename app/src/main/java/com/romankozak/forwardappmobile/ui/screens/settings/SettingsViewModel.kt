package com.romankozak.forwardappmobile.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val ollamaUrl: String = "",
    val fastModel: String = "",
    val smartModel: String = "",
    val modelsState: ModelsState = ModelsState.Loading
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val ollamaService: OllamaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepo.ollamaUrlFlow,
                settingsRepo.ollamaFastModelFlow,
                settingsRepo.ollamaSmartModelFlow
            ) { url, fast, smart ->
                SettingsUiState(ollamaUrl = url, fastModel = fast, smartModel = smart)
            }.collect { state ->
                _uiState.update { it.copy(
                    ollamaUrl = state.ollamaUrl,
                    fastModel = state.fastModel,
                    smartModel = state.smartModel
                )}
                // Автоматично завантажуємо моделі, якщо URL є
                if (state.ollamaUrl.isNotBlank()) {
                    fetchAvailableModels()
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(ollamaUrl = newUrl) }
    }

    fun onFastModelSelected(modelName: String) {
        _uiState.update { it.copy(fastModel = modelName) }
    }

    fun onSmartModelSelected(modelName: String) {
        _uiState.update { it.copy(smartModel = modelName) }
    }

    fun fetchAvailableModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(modelsState = ModelsState.Loading) }
            val result = ollamaService.getAvailableModels(_uiState.value.ollamaUrl)
            result.onSuccess { models ->
                _uiState.update { it.copy(modelsState = ModelsState.Success(models)) }
            }.onFailure { error ->
                _uiState.update { it.copy(modelsState = ModelsState.Error("Error: ${error.message}")) }
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value
            settingsRepo.saveOllamaUrl(currentState.ollamaUrl)
            settingsRepo.saveOllamaModels(currentState.fastModel, currentState.smartModel)
        }
    }
}
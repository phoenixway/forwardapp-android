package com.romankozak.forwardappmobile.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner.NerManager
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner.NerState
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Ollama
    val ollamaUrl: String = "",
    val fastModel: String = "",
    val smartModel: String = "",
    val modelsState: ModelsState = ModelsState.Loading,
    // NER Model
    val nerModelUri: String = "",
    val nerTokenizerUri: String = "",
    val nerLabelsUri: String = "",
    // --- ДОДАНО: Властивість для стану NER-моделі ---
    val nerState: NerState = NerState.NotInitialized
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val ollamaService: OllamaService,
    // --- ДОДАНО: Впровадження NerManager ---
    private val nerManager: NerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // --- ДОДАНО: Відстеження стану NER-моделі ---
        viewModelScope.launch {
            nerManager.nerState.collect { state ->
                _uiState.update { it.copy(nerState = state) }
            }
        }

        viewModelScope.launch {
            val settingsFlows = listOf(
                settingsRepo.ollamaUrlFlow,
                settingsRepo.ollamaFastModelFlow,
                settingsRepo.ollamaSmartModelFlow,
                settingsRepo.nerModelUriFlow,
                settingsRepo.nerTokenizerUriFlow,
                settingsRepo.nerLabelsUriFlow
            )

            combine(settingsFlows) { values ->
                // Оновлюємо _uiState, але не перезаписуємо nerState,
                // оскільки він надходить з окремого потоку.
                _uiState.update {
                    it.copy(
                        ollamaUrl = values[0],
                        fastModel = values[1],
                        smartModel = values[2],
                        nerModelUri = values[3],
                        nerTokenizerUri = values[4],
                        nerLabelsUri = values[5]
                    )
                }
            }.collect {
                // Автоматично завантажуємо моделі Ollama, якщо URL є
                if (_uiState.value.ollamaUrl.isNotBlank()) {
                    fetchAvailableModels()
                }
            }
        }
    }

    // --- Ollama handlers ---
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

    // --- NER Handlers ---
    fun onNerModelFileSelected(uri: String) {
        _uiState.update { it.copy(nerModelUri = uri) }
    }

    fun onNerTokenizerFileSelected(uri: String) {
        _uiState.update { it.copy(nerTokenizerUri = uri) }
    }

    fun onNerLabelsFileSelected(uri: String) {
        _uiState.update { it.copy(nerLabelsUri = uri) }
    }

    // --- ДОДАНО: Метод для перезавантаження моделі ---
    fun reloadNerModel() {
        nerManager.reinitialize()
    }

    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value
            // Save Ollama settings
            settingsRepo.saveOllamaUrl(currentState.ollamaUrl)
            settingsRepo.saveOllamaModels(currentState.fastModel, currentState.smartModel)
            // Save NER settings
            settingsRepo.saveNerUris(
                modelUri = currentState.nerModelUri,
                tokenizerUri = currentState.nerTokenizerUri,
                labelsUri = currentState.nerLabelsUri
            )
        }
    }
}
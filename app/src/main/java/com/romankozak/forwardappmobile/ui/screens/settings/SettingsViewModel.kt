

package com.romankozak.forwardappmobile.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val ollamaUrl: String = "",
    val fastModel: String = "",
    val smartModel: String = "",
    val modelsState: ModelsState = ModelsState.Loading,
    val nerModelUri: String = "",
    val nerTokenizerUri: String = "",
    val nerLabelsUri: String = "",
    val nerState: NerState = NerState.NotInitialized,
    val rolesFolderUri: String = "",
    val desktopAddress: String = "",
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepo: SettingsRepository,
        private val ollamaService: OllamaService,
        private val nerManager: NerManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                nerManager.nerState.collect { state ->
                    _uiState.update { it.copy(nerState = state) }
                }
            }

            viewModelScope.launch {
                
                val settingsFlows =
                    listOf(
                        settingsRepo.ollamaUrlFlow,
                        settingsRepo.ollamaFastModelFlow,
                        settingsRepo.ollamaSmartModelFlow,
                        settingsRepo.nerModelUriFlow,
                        settingsRepo.nerTokenizerUriFlow,
                        settingsRepo.nerLabelsUriFlow,
                        settingsRepo.rolesFolderUriFlow,
                        settingsRepo.desktopAddressFlow,
                    )
                combine(settingsFlows) { values ->
                    _uiState.update {
                        it.copy(
                            ollamaUrl = values[0],
                            fastModel = values[1],
                            smartModel = values[2],
                            nerModelUri = values[3],
                            nerTokenizerUri = values[4],
                            nerLabelsUri = values[5],
                            rolesFolderUri = values[6],
                            desktopAddress = values[7],
                        )
                    }
                }.collect {
                    
                    if (_uiState.value.ollamaUrl.isNotBlank()) {
                        fetchAvailableModels()
                    }
                }
            }
        }

        
        fun onDesktopAddressChanged(newAddress: String) {
            _uiState.update { it.copy(desktopAddress = newAddress) }
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
                result
                    .onSuccess { models ->
                        _uiState.update { it.copy(modelsState = ModelsState.Success(models)) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(modelsState = ModelsState.Error("Error: ${error.message}")) }
                    }
            }
        }

        fun onNerModelFileSelected(uri: String) {
            _uiState.update { it.copy(nerModelUri = uri) }
        }

        fun onNerTokenizerFileSelected(uri: String) {
            _uiState.update { it.copy(nerTokenizerUri = uri) }
        }

        fun onNerLabelsFileSelected(uri: String) {
            _uiState.update { it.copy(nerLabelsUri = uri) }
        }

        fun reloadNerModel() {
            viewModelScope.launch {
                val currentState = _uiState.value
                settingsRepo.saveNerUris(
                    modelUri = currentState.nerModelUri,
                    tokenizerUri = currentState.nerTokenizerUri,
                    labelsUri = currentState.nerLabelsUri,
                )
                nerManager.reinitialize()
            }
        }

        fun onRolesFolderSelected(
            uri: Uri,
            context: Context,
        ) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            _uiState.update { it.copy(rolesFolderUri = uri.toString()) }
        }

        fun saveSettings() {
            viewModelScope.launch {
                val currentState = _uiState.value
                settingsRepo.saveOllamaUrl(currentState.ollamaUrl)
                settingsRepo.saveOllamaModels(currentState.fastModel, currentState.smartModel)
                settingsRepo.saveNerUris(
                    modelUri = currentState.nerModelUri,
                    tokenizerUri = currentState.nerTokenizerUri,
                    labelsUri = currentState.nerLabelsUri,
                )
                settingsRepo.saveRolesFolderUri(currentState.rolesFolderUri)
                
                settingsRepo.saveDesktopAddress(currentState.desktopAddress)
                
            }
        }
    }

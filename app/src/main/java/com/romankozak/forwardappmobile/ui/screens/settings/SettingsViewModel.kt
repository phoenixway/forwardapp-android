package com.romankozak.forwardappmobile.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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

import com.romankozak.forwardappmobile.data.repository.ServerDiscoveryState

data class SettingsUiState(
    val fastModel: String = "",
    val smartModel: String = "",
    val modelsState: ModelsState = ModelsState.Loading,
    val nerModelUri: String = "",
    val nerTokenizerUri: String = "",
    val nerLabelsUri: String = "",
    val nerState: NerState = NerState.NotInitialized,
    val rolesFolderUri: String = "",
    val serverIpConfigurationMode: String = "auto",
    val manualServerIp: String = "",
    val wifiSyncPort: Int = 8080,
    val ollamaPort: Int = 11434,
    val fastApiPort: Int = 8000,
    val serverDiscoveryState: ServerDiscoveryState = ServerDiscoveryState.Loading,
    val themeSettings: com.romankozak.forwardappmobile.ui.theme.ThemeSettings = com.romankozak.forwardappmobile.ui.theme.ThemeSettings(),
    val attachmentsLibraryEnabled: Boolean = FeatureToggles.attachmentsLibraryEnabled,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
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
            val settingsFlows = listOf(
                settingsRepo.ollamaFastModelFlow,
                settingsRepo.ollamaSmartModelFlow,
                settingsRepo.nerModelUriFlow,
                settingsRepo.nerTokenizerUriFlow,
                settingsRepo.nerLabelsUriFlow,
                settingsRepo.rolesFolderUriFlow,
                settingsRepo.themeSettings,
                settingsRepo.serverIpConfigurationModeFlow,
                settingsRepo.manualServerIpFlow,
                settingsRepo.wifiSyncPortFlow,
                settingsRepo.ollamaPortFlow,
                settingsRepo.fastApiPortFlow,
                settingsRepo.attachmentsLibraryEnabledFlow,
            )
            combine(settingsFlows) { values ->
                _uiState.update {
                    it.copy(
                        fastModel = values[0] as String,
                        smartModel = values[1] as String,
                        nerModelUri = values[2] as String,
                        nerTokenizerUri = values[3] as String,
                        nerLabelsUri = values[4] as String,
                        rolesFolderUri = values[5] as String,
                        themeSettings = values[6] as com.romankozak.forwardappmobile.ui.theme.ThemeSettings,
                        serverIpConfigurationMode = values[7] as String,
                        manualServerIp = values[8] as String,
                        wifiSyncPort = values[9] as Int,
                        ollamaPort = values[10] as Int,
                        fastApiPort = values[11] as Int,
                        attachmentsLibraryEnabled = values[12] as Boolean,
                    )
                }
            }.collect { 
                refreshServerDiscovery()
            }
        }
    }

    fun refreshServerDiscovery() {
        viewModelScope.launch {
            settingsRepo.discoverServer().collect {
                _uiState.update { ui -> ui.copy(serverDiscoveryState = it) }
                if (it is ServerDiscoveryState.Found) {
                    fetchAvailableModels()
                }
            }
        }
    }

    fun onFastModelSelected(modelName: String) {
        _uiState.update { it.copy(fastModel = modelName) }
    }

    fun onSmartModelSelected(modelName: String) {
        _uiState.update { it.copy(smartModel = modelName) }
    }

    fun fetchAvailableModels() {

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

    fun onRolesFolderSelected(uri: Uri, context: Context) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        _uiState.update { it.copy(rolesFolderUri = uri.toString()) }
    }

    fun onLightThemeSelected(themeName: com.romankozak.forwardappmobile.ui.theme.ThemeName) {
        _uiState.update { it.copy(themeSettings = it.themeSettings.copy(lightThemeName = themeName)) }
    }

    fun onDarkThemeSelected(themeName: com.romankozak.forwardappmobile.ui.theme.ThemeName) {
        _uiState.update { it.copy(themeSettings = it.themeSettings.copy(darkThemeName = themeName)) }
    }

    fun onThemeModeSelected(themeMode: com.romankozak.forwardappmobile.ui.theme.ThemeMode) {
        _uiState.update { it.copy(themeSettings = it.themeSettings.copy(themeMode = themeMode)) }
    }

    fun onServerIpConfigurationModeChanged(isAuto: Boolean) {
        _uiState.update { it.copy(serverIpConfigurationMode = if (isAuto) "auto" else "manual") }
    }

    fun onManualServerIpChanged(address: String) {
        _uiState.update { it.copy(manualServerIp = address) }
    }

    fun onWifiSyncPortChanged(port: String) {
        _uiState.update { it.copy(wifiSyncPort = port.toIntOrNull() ?: 8080) }
    }

    fun onOllamaPortChanged(port: String) {
        _uiState.update { it.copy(ollamaPort = port.toIntOrNull() ?: 11434) }
    }

    fun onFastApiPortChanged(port: String) {
        _uiState.update { it.copy(fastApiPort = port.toIntOrNull() ?: 8000) }
    }

    fun onAttachmentsLibraryToggle(enabled: Boolean) {
        _uiState.update {
            it.copy(attachmentsLibraryEnabled = enabled)
        }
        FeatureToggles.attachmentsLibraryEnabled = enabled
    }

    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value
            Log.e("SettingsViewModel", "Saving server settings: mode=${currentState.serverIpConfigurationMode}, ip=${currentState.manualServerIp}, wifiPort=${currentState.wifiSyncPort}, ollamaPort=${currentState.ollamaPort}, fastApiPort=${currentState.fastApiPort}")
            settingsRepo.saveOllamaModels(currentState.fastModel, currentState.smartModel)
            settingsRepo.saveNerUris(
                modelUri = currentState.nerModelUri,
                tokenizerUri = currentState.nerTokenizerUri,
                labelsUri = currentState.nerLabelsUri,
            )
            settingsRepo.saveRolesFolderUri(currentState.rolesFolderUri)
            settingsRepo.saveServerAddressSettings(
                mode = currentState.serverIpConfigurationMode,
                manualIp = currentState.manualServerIp,
                wifiSyncPort = currentState.wifiSyncPort,
                ollamaPort = currentState.ollamaPort,
                fastApiPort = currentState.fastApiPort,
            )
            settingsRepo.saveThemeSettings(currentState.themeSettings)
            settingsRepo.saveAttachmentsLibraryEnabled(currentState.attachmentsLibraryEnabled)
        }
    }
}

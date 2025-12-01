package com.romankozak.forwardappmobile.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.repository.RolesRepository
import com.romankozak.forwardappmobile.data.repository.RingtoneSettings
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.reminders.RingtoneType
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.romankozak.forwardappmobile.data.repository.ServerDiscoveryState

data class SettingsUiState(
    val fastModel: String = "",
    val smartModel: String = "",
    val modelsState: ModelsState = ModelsState.Success(emptyList()),
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
    val featureToggles: Map<FeatureFlag, Boolean> = FeatureFlag.values().associateWith { FeatureToggles.isEnabled(it) },
    val attachmentsLibraryEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary),
    val scriptsLibraryEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary),
    val allowSystemProjectMoves: Boolean = FeatureToggles.isEnabled(FeatureFlag.AllowSystemProjectMoves),
    val planningModesEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.PlanningModes),
    val wifiSyncEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.WifiSync),
    val strategicManagementEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.StrategicManagement),
    val aiChatEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.AiChat),
    val aiInsightsEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.AiInsights),
    val aiLifeManagementEnabled: Boolean = FeatureToggles.isEnabled(FeatureFlag.AiLifeManagement),
    val ringtoneType: RingtoneType = RingtoneType.Energetic,
    val ringtoneUris: Map<RingtoneType, String> = mapOf(
        RingtoneType.Energetic to "",
        RingtoneType.Moderate to "",
        RingtoneType.Quiet to "",
    ),
    val ringtoneVolumes: Map<RingtoneType, Float> = mapOf(
        RingtoneType.Energetic to 1f,
        RingtoneType.Moderate to 0.8f,
        RingtoneType.Quiet to 0.5f,
    ),
    val reminderVibrationEnabled: Boolean = true,
    val wifiSyncServerEnabled: Boolean = false,
    val desktopSyncAddress: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val nerManager: NerManager,
    private val rolesRepo: RolesRepository,
    private val ollamaService: OllamaService,
) : ViewModel() {
    private val logTag = "AI_CHAT_OLLAMA"
    private var fetchModelsJob: Job? = null

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
                settingsRepo.featureTogglesFlow,
                settingsRepo.ringtoneTypeFlow,
                settingsRepo.ringtoneUrisFlow,
                settingsRepo.ringtoneVolumesFlow,
                settingsRepo.reminderVibrationEnabledFlow,
                settingsRepo.wifiSyncServerEnabledFlow,
                settingsRepo.desktopSyncAddressFlow,
            )
            combine(settingsFlows) { values ->
                val featureToggles = values[12] as Map<FeatureFlag, Boolean>
                val ringtoneType = values[13] as RingtoneType
                val ringtoneUris = values[14] as Map<RingtoneType, String>
                val ringtoneVolumes = values[15] as Map<RingtoneType, Float>
                val reminderVibrationEnabled = values[16] as Boolean
                val wifiSyncServerEnabled = values[17] as Boolean
                val desktopSyncAddress = values[18] as String
                val attachmentsEnabled = featureToggles[FeatureFlag.AttachmentsLibrary] ?: FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary)
                val scriptsEnabled = featureToggles[FeatureFlag.ScriptsLibrary] ?: FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)
                val allowSystemMoves = featureToggles[FeatureFlag.AllowSystemProjectMoves] ?: FeatureToggles.isEnabled(FeatureFlag.AllowSystemProjectMoves)
                val planningModesEnabled = featureToggles[FeatureFlag.PlanningModes] ?: FeatureToggles.isEnabled(FeatureFlag.PlanningModes)
                val wifiSyncEnabled = featureToggles[FeatureFlag.WifiSync] ?: FeatureToggles.isEnabled(FeatureFlag.WifiSync)
                val strategicEnabled = featureToggles[FeatureFlag.StrategicManagement] ?: FeatureToggles.isEnabled(FeatureFlag.StrategicManagement)
                val aiChatEnabled = featureToggles[FeatureFlag.AiChat] ?: FeatureToggles.isEnabled(FeatureFlag.AiChat)
                val aiInsightsEnabled = featureToggles[FeatureFlag.AiInsights] ?: FeatureToggles.isEnabled(FeatureFlag.AiInsights)
                val aiLifeEnabled = featureToggles[FeatureFlag.AiLifeManagement] ?: FeatureToggles.isEnabled(FeatureFlag.AiLifeManagement)
                FeatureToggles.updateAll(featureToggles)
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
                        featureToggles = featureToggles,
                        attachmentsLibraryEnabled = attachmentsEnabled,
                        scriptsLibraryEnabled = scriptsEnabled,
                        allowSystemProjectMoves = allowSystemMoves,
                        planningModesEnabled = planningModesEnabled,
                        wifiSyncEnabled = wifiSyncEnabled,
                        strategicManagementEnabled = strategicEnabled,
                        aiChatEnabled = aiChatEnabled,
                        aiInsightsEnabled = aiInsightsEnabled,
                        aiLifeManagementEnabled = aiLifeEnabled,
                        ringtoneType = ringtoneType,
                        ringtoneUris = ringtoneUris,
                        ringtoneVolumes = ringtoneVolumes,
                        reminderVibrationEnabled = reminderVibrationEnabled,
                        wifiSyncServerEnabled = wifiSyncServerEnabled,
                        desktopSyncAddress = desktopSyncAddress,
                    )
                }
            }.collect {
                refreshServerDiscovery()
            }
        }
    }

    fun refreshServerDiscovery() {
        viewModelScope.launch {
            settingsRepo
                .discoverServer()
                .collect {
                    _uiState.update { ui -> ui.copy(serverDiscoveryState = it) }
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
        fetchModelsJob?.cancel()
        fetchModelsJob = viewModelScope.launch {
            _uiState.update { it.copy(modelsState = ModelsState.Loading) }
            try {
                val manualBase = _uiState.value.manualServerIp.takeIf { ip -> ip.isNotBlank() }
                val resolvedDiscovery = withTimeoutOrNull(3_000) { settingsRepo.getOllamaUrl().firstOrNull { !it.isNullOrBlank() } }
                val baseUrl = manualBase ?: resolvedDiscovery
                if (baseUrl.isNullOrBlank()) {
                    _uiState.update { it.copy(modelsState = ModelsState.Error("Не знайдено адресу Ollama")) }
                    return@launch
                }
                Log.d(logTag, "[Settings] Fetch models from $baseUrl")
                val result = ollamaService.getAvailableModels(baseUrl)
                result.onSuccess { models ->
                    Log.d(logTag, "[Settings] Models loaded: ${models.size}")
                    _uiState.update { it.copy(modelsState = ModelsState.Success(models)) }
                }.onFailure { e ->
                    Log.e(logTag, "[Settings] Model fetch failed: ${e.message}", e)
                    _uiState.update { it.copy(modelsState = ModelsState.Error(e.message ?: "Помилка завантаження моделей")) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                Log.e(logTag, "[Settings] Model fetch unexpected error: ${e.message}", e)
                _uiState.update { it.copy(modelsState = ModelsState.Error(e.message ?: "Помилка завантаження моделей")) }
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

    private fun updateFeatureToggle(flag: FeatureFlag, enabled: Boolean) {
        _uiState.update { state ->
            val updated = state.featureToggles + (flag to enabled)
            state.copy(
                featureToggles = updated,
                attachmentsLibraryEnabled = updated[FeatureFlag.AttachmentsLibrary] ?: state.attachmentsLibraryEnabled,
                scriptsLibraryEnabled = updated[FeatureFlag.ScriptsLibrary] ?: state.scriptsLibraryEnabled,
                allowSystemProjectMoves = updated[FeatureFlag.AllowSystemProjectMoves] ?: state.allowSystemProjectMoves,
                planningModesEnabled = updated[FeatureFlag.PlanningModes] ?: state.planningModesEnabled,
                wifiSyncEnabled = updated[FeatureFlag.WifiSync] ?: state.wifiSyncEnabled,
                strategicManagementEnabled = updated[FeatureFlag.StrategicManagement] ?: state.strategicManagementEnabled,
                aiChatEnabled = updated[FeatureFlag.AiChat] ?: state.aiChatEnabled,
                aiInsightsEnabled = updated[FeatureFlag.AiInsights] ?: state.aiInsightsEnabled,
                aiLifeManagementEnabled = updated[FeatureFlag.AiLifeManagement] ?: state.aiLifeManagementEnabled,
            )
        }
        FeatureToggles.update(flag, enabled)
        viewModelScope.launch { settingsRepo.saveFeatureToggle(flag, enabled) }
    }

    fun onAttachmentsLibraryToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.AttachmentsLibrary, enabled)
    }

    fun onScriptsLibraryToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.ScriptsLibrary, enabled)
    }

    fun onAllowSystemProjectMovesToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.AllowSystemProjectMoves, enabled)
    }

    fun onPlanningModesToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.PlanningModes, enabled)
    }

    fun onWifiSyncToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.WifiSync, enabled)
        if (!enabled) {
            _uiState.update { it.copy(wifiSyncServerEnabled = false) }
            viewModelScope.launch { settingsRepo.saveWifiSyncServerEnabled(false) }
        }
    }

    fun onWifiSyncServerEnabledToggle(enabled: Boolean) {
        _uiState.update { it.copy(wifiSyncServerEnabled = enabled) }
    }

    fun onDesktopSyncAddressChanged(address: String) {
        _uiState.update { it.copy(desktopSyncAddress = address) }
    }

    fun onStrategicManagementToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.StrategicManagement, enabled)
    }

    fun onAiChatToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.AiChat, enabled)
    }

    fun onAiInsightsToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.AiInsights, enabled)
    }

    fun onAiLifeManagementToggle(enabled: Boolean) {
        updateFeatureToggle(FeatureFlag.AiLifeManagement, enabled)
    }

    fun onRingtoneTypeSelected(type: RingtoneType) {
        _uiState.update { it.copy(ringtoneType = type) }
    }

    fun onRingtoneUriSelected(type: RingtoneType, uri: String) {
        _uiState.update { it.copy(ringtoneUris = it.ringtoneUris + (type to uri)) }
    }

    fun onRingtoneVolumeChanged(type: RingtoneType, volume: Float) {
        _uiState.update { it.copy(ringtoneVolumes = it.ringtoneVolumes + (type to volume.coerceIn(0f, 1f))) }
    }

    fun onReminderVibrationToggle(enabled: Boolean) {
        _uiState.update { it.copy(reminderVibrationEnabled = enabled) }
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
            settingsRepo.saveWifiSyncServerEnabled(currentState.wifiSyncServerEnabled)
            settingsRepo.saveDesktopSyncAddress(currentState.desktopSyncAddress)
            settingsRepo.saveRingtoneType(currentState.ringtoneType)
            currentState.ringtoneUris.forEach { (type, uri) ->
                settingsRepo.saveRingtoneUri(type, uri)
            }
            currentState.ringtoneVolumes.forEach { (type, volume) ->
                settingsRepo.saveRingtoneVolume(type, volume)
            }
            settingsRepo.setReminderVibrationEnabled(currentState.reminderVibrationEnabled)
            settingsRepo.saveThemeSettings(currentState.themeSettings)
            FeatureFlag.values().forEach { flag ->
                val enabled = currentState.featureToggles[flag] ?: FeatureToggles.isEnabled(flag)
                settingsRepo.saveFeatureToggle(flag, enabled)
            }
        }
    }
}

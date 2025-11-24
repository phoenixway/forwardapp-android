package com.romankozak.forwardappmobile.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.settings.components.NerSettingsCard
import com.romankozak.forwardappmobile.ui.screens.settings.components.PermissionsSettingsCard
import com.romankozak.forwardappmobile.ui.screens.settings.components.RolesSettingsCard
import com.romankozak.forwardappmobile.ui.screens.settings.components.RingtoneSettingsCard
import com.romankozak.forwardappmobile.ui.screens.settings.components.ServerSettingsCard
import com.romankozak.forwardappmobile.ui.screens.settings.components.ThemeSettingsCard
import com.romankozak.forwardappmobile.ui.screens.settings.models.PlanningSettings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import com.romankozak.forwardappmobile.ui.screens.settings.components.AnimatedTextField
import com.romankozak.forwardappmobile.ui.screens.settings.components.SettingsCard
import com.romankozak.forwardappmobile.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    planningSettings: PlanningSettingsState,
    initialVaultName: String,
    reservedContextCount: Int,
    onManageContextsClick: () -> Unit,
    onBack: () -> Unit,
    onSave: (PlanningSettings) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var tempShowModes by remember(planningSettings.showModes) { mutableStateOf(planningSettings.showModes) }
    var tempDailyTag by remember(planningSettings.dailyTag) { mutableStateOf(planningSettings.dailyTag) }
    var tempMediumTag by remember(planningSettings.mediumTag) { mutableStateOf(planningSettings.mediumTag) }
    var tempLongTag by remember(planningSettings.longTag) { mutableStateOf(planningSettings.longTag) }
    var tempVaultName by remember(initialVaultName) { mutableStateOf(initialVaultName) }

    var initialViewModelState by remember { mutableStateOf<SettingsUiState?>(null) }
    LaunchedEffect(uiState) {
        if (initialViewModelState == null && uiState != SettingsUiState()) {
            initialViewModelState = uiState
        }
    }

    val isDirty by remember(uiState, tempShowModes, tempDailyTag, tempMediumTag, tempLongTag, tempVaultName) {
        derivedStateOf {
            val planningIsDirty = tempShowModes != planningSettings.showModes ||
                    tempDailyTag != planningSettings.dailyTag ||
                    tempMediumTag != planningSettings.mediumTag ||
                    tempLongTag != planningSettings.longTag ||
                    tempVaultName != initialVaultName

            val viewModelIsDirty = initialViewModelState?.let {
                uiState.serverIpConfigurationMode != it.serverIpConfigurationMode ||
                uiState.manualServerIp != it.manualServerIp ||
                uiState.wifiSyncPort != it.wifiSyncPort ||
                uiState.ollamaPort != it.ollamaPort ||
                uiState.fastApiPort != it.fastApiPort ||
                uiState.fastModel != it.fastModel ||
                uiState.smartModel != it.smartModel ||
                uiState.nerModelUri != it.nerModelUri ||
                uiState.nerTokenizerUri != it.nerTokenizerUri ||
                uiState.nerLabelsUri != it.nerLabelsUri ||
                uiState.rolesFolderUri != it.rolesFolderUri ||
                uiState.themeSettings != it.themeSettings ||
                uiState.ringtoneType != it.ringtoneType ||
                uiState.ringtoneUris != it.ringtoneUris ||
                uiState.ringtoneVolumes != it.ringtoneVolumes ||
                uiState.reminderVibrationEnabled != it.reminderVibrationEnabled
            } ?: false

            planningIsDirty || viewModelIsDirty
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp).imePadding(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Close") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = isDirty,
                    onClick = {
                        onSave(
                            PlanningSettings(
                                showModes = tempShowModes,
                                dailyTag = tempDailyTag,
                                mediumTag = tempMediumTag,
                                longTag = tempLongTag,
                                vaultName = tempVaultName
                            )
                        )
                        viewModel.saveSettings()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("Save") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).fillMaxSize().imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PermissionsSettingsCard()

            RingtoneSettingsCard(
                currentType = uiState.ringtoneType,
                ringtoneUris = uiState.ringtoneUris,
                ringtoneVolumes = uiState.ringtoneVolumes,
                vibrationEnabled = uiState.reminderVibrationEnabled,
                onTypeSelected = viewModel::onRingtoneTypeSelected,
                onRingtonePicked = viewModel::onRingtoneUriSelected,
                onVolumeChanged = viewModel::onRingtoneVolumeChanged,
                onVibrationToggle = viewModel::onReminderVibrationToggle,
            )

            ThemeSettingsCard(
                themeSettings = uiState.themeSettings,
                onThemeModeSelected = viewModel::onThemeModeSelected,
                onLightThemeSelected = viewModel::onLightThemeSelected,
                onDarkThemeSelected = viewModel::onDarkThemeSelected,
            )

            SettingsCard(
                title = "Experimental Features",
                icon = Icons.Default.Build,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Attachments library",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.attachmentsLibraryEnabled,
                            onCheckedChange = viewModel::onAttachmentsLibraryToggle
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_allow_system_project_moves),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.allowSystemProjectMoves,
                            onCheckedChange = viewModel::onAllowSystemProjectMovesToggle,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Planning modes",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.planningModesEnabled,
                            onCheckedChange = viewModel::onPlanningModesToggle,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Wi‑Fi sync",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.wifiSyncEnabled,
                            onCheckedChange = viewModel::onWifiSyncToggle,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Strategic management",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.strategicManagementEnabled,
                            onCheckedChange = viewModel::onStrategicManagementToggle,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Chat",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.aiChatEnabled,
                            onCheckedChange = viewModel::onAiChatToggle,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Insights",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.aiInsightsEnabled,
                            onCheckedChange = viewModel::onAiInsightsToggle,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Life Management",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = uiState.aiLifeManagementEnabled,
                            onCheckedChange = viewModel::onAiLifeManagementToggle,
                        )
                    }
                }
            }

            ServerSettingsCard(
                state = uiState,
                onIpConfigModeChange = viewModel::onServerIpConfigurationModeChanged,
                onIpChange = viewModel::onManualServerIpChanged,
                onOllamaPortChange = viewModel::onOllamaPortChanged,
                onWifiSyncPortChange = viewModel::onWifiSyncPortChanged,
                onFastApiPortChange = viewModel::onFastApiPortChanged,
                onFetchClick = viewModel::fetchAvailableModels,
                onFastModelSelect = viewModel::onFastModelSelected,
                onSmartModelSelect = viewModel::onSmartModelSelected,
                onRefreshDiscovery = viewModel::refreshServerDiscovery
            )

            RolesSettingsCard(
                state = uiState,
                onFolderSelected = viewModel::onRolesFolderSelected,
            )
            NerSettingsCard(
                state = uiState,
                onModelFileSelected = viewModel::onNerModelFileSelected,
                onTokenizerFileSelected = viewModel::onNerTokenizerFileSelected,
                onLabelsFileSelected = viewModel::onNerLabelsFileSelected,
                onReloadClick = viewModel::reloadNerModel,
            )

            SettingsCard(
                title = "Planning Modes",
                icon = Icons.Default.Tune,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Show planning scale modes", modifier = Modifier.weight(1f))
                    Switch(checked = tempShowModes, onCheckedChange = { tempShowModes = it })
                }
                AnimatedTextField(
                    value = tempDailyTag,
                    onValueChange = { tempDailyTag = it },
                    label = "Daily Mode Tag",
                    helper = "Tag used for daily planning mode",
                )
                AnimatedTextField(
                    value = tempMediumTag,
                    onValueChange = { tempMediumTag = it },
                    label = "Medium Mode Tag",
                    helper = "Tag used for medium planning mode",
                )
                AnimatedTextField(
                    value = tempLongTag,
                    onValueChange = { tempLongTag = it },
                    label = "Long Mode Tag",
                    helper = "Tag used for long planning mode",
                )
            }

            SettingsCard(
                title = "Integrations",
                icon = Icons.Default.Link,
            ) {
                Text("Specify the exact name of your Obsidian Vault for link integration.")
                AnimatedTextField(
                    value = tempVaultName,
                    onValueChange = { tempVaultName = it },
                    label = "Obsidian Vault Name",
                    helper = "Exact vault name for link integration",
                    singleLine = true,
                )
            }

            SettingsCard(
                title = "Contexts",
                icon = Icons.Default.Label,
            ) {
                OutlinedButton(
                    onClick = onManageContextsClick,
                    modifier = Modifier.fillMaxWidth(),
                    content = { Text("Manage Reserved Contexts ($reservedContextCount)") },
                    colors = ButtonDefaults.outlinedButtonColors(),
                )
            }

            SettingsCard(
                title = "Debug Options",
                icon = Icons.Default.BugReport,
            ) {
                Button(onClick = { throw RuntimeException("Test Crash from Settings") }) {
                    Text("Test Crash")
                }
            }
        }
    }
}

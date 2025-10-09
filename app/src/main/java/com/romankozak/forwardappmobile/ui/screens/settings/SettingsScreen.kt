package com.romankozak.forwardappmobile.ui.screens.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.repository.ServerDiscoveryState
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.ui.ModelsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState

fun getFileName(uri: Uri, context: Context): String {
    var fileName: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        fileName = uri.lastPathSegment
    }
    return fileName ?: "Unknown file"
}

fun getFolderName(uri: Uri, context: Context): String =
    try {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        context.contentResolver.query(docUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                uri.lastPathSegment ?: "Selected Folder"
            }
        } ?: uri.toString()
    } catch (e: Exception) {
        uri.lastPathSegment ?: "Selected Folder"
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    planningSettings: PlanningSettingsState,
    initialVaultName: String,
    reservedContextCount: Int,
    onManageContextsClick: () -> Unit,
    onBack: () -> Unit,
    onSave: (showModes: Boolean, dailyTag: String, mediumTag: String, longTag: String, vaultName: String) -> Unit,
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
                uiState.themeSettings != it.themeSettings
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
                ) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = isDirty,
                    onClick = {
                        onSave(tempShowModes, tempDailyTag, tempMediumTag, tempLongTag, tempVaultName)
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

            ThemeSettingsCard(
                themeSettings = uiState.themeSettings,
                onThemeModeSelected = viewModel::onThemeModeSelected,
                onLightThemeSelected = viewModel::onLightThemeSelected,
                onDarkThemeSelected = viewModel::onDarkThemeSelected,
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSettingsCard(
    state: SettingsUiState,
    onIpConfigModeChange: (String) -> Unit,
    onIpChange: (String) -> Unit,
    onOllamaPortChange: (String) -> Unit,
    onWifiSyncPortChange: (String) -> Unit,
    onFastApiPortChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onFastModelSelect: (String) -> Unit,
    onSmartModelSelect: (String) -> Unit,
    onRefreshDiscovery: () -> Unit
) {
    SettingsCard(title = "Remote Server Settings", icon = Icons.Default.Dns) {
        // Unified Address Mode
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Server IP Address", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                AnimatedVisibility(visible = state.serverIpConfigurationMode == "auto") {
                    IconButton(onClick = onRefreshDiscovery) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Discovery")
                    }
                }
            }
            Text("Used for AI Server, File Sync, etc.", style = MaterialTheme.typography.bodySmall)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    onClick = { 
                        onIpConfigModeChange("auto")
                    },
                    selected = state.serverIpConfigurationMode == "auto"
                ) {
                    Text("Auto")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    onClick = { 
                        onIpConfigModeChange("manual")
                    },
                    selected = state.serverIpConfigurationMode == "manual"
                ) {
                    Text("Manual")
                }
            }

            AnimatedVisibility(visible = state.serverIpConfigurationMode == "auto") {
                when (val discoveryState = state.serverDiscoveryState) {
                    is ServerDiscoveryState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Searching for server...")
                        }
                    }
                    is ServerDiscoveryState.Found -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Found", tint = Color.Green)
                            Text("Found at: ${discoveryState.address}")
                        }
                    }
                    is ServerDiscoveryState.NotFound -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = "Not Found", tint = Color.Yellow)
                            Text("Server not found on the network.")
                        }
                    }
                    is ServerDiscoveryState.Error -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Text("Error: ${discoveryState.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.serverIpConfigurationMode == "manual") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedTextField(
                        value = state.manualServerIp,
                        onValueChange = onIpChange,
                        label = "Manual Server IP",
                        helper = "e.g., 192.168.1.5",
                        singleLine = true
                    )
                    AnimatedTextField(
                        value = state.ollamaPort.toString(),
                        onValueChange = onOllamaPortChange,
                        label = "Ollama Port",
                        helper = "Port for Ollama service",
                        singleLine = true
                    )
                    AnimatedTextField(
                        value = state.wifiSyncPort.toString(),
                        onValueChange = onWifiSyncPortChange,
                        label = "WiFi Sync Port",
                        helper = "Port for WiFi sync service",
                        singleLine = true
                    )
                    AnimatedTextField(
                        value = state.fastApiPort.toString(),
                        onValueChange = onFastApiPortChange,
                        label = "FastAPI Port",
                        helper = "Port for FastAPI service",
                        singleLine = true
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Ollama Models
        Text("Ollama AI Models", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = onFetchClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.serverDiscoveryState is ServerDiscoveryState.Found && state.modelsState !is ModelsState.Loading,
        ) {
            if (state.modelsState is ModelsState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Fetch Available Models")
            }
        }

        when (val modelsState = state.modelsState) {
            is ModelsState.Error -> {
                Text(modelsState.message, color = MaterialTheme.colorScheme.error)
            }
            is ModelsState.Success -> {
                ModelSelector(
                    label = "Fast Model",
                    selectedValue = state.fastModel,
                    models = modelsState.models,
                    onModelSelected = onFastModelSelect,
                )
                ModelSelector(
                    label = "Smart Model",
                    selectedValue = state.smartModel,
                    models = modelsState.models,
                    onModelSelected = onSmartModelSelect,
                )
            }
            ModelsState.Loading -> { }
        }
    }
}


@Composable
private fun RolesSettingsCard(
    state: SettingsUiState,
    onFolderSelected: (Uri, Context) -> Unit,
) {
    val context = LocalContext.current

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
            onResult = { uri: Uri? ->
                uri?.let {
                    onFolderSelected(it, context)
                }
            },
        )

    SettingsCard(
        title = "Chat Roles",
        icon = Icons.Default.Face,
    ) {
        Text("Select a folder containing your role files (.md or .txt).")
        Spacer(modifier = Modifier.height(8.dp))
        FileSelector(
            label = "Roles Folder",
            selectedFileUri = state.rolesFolderUri,
            onSelectClick = { folderPickerLauncher.launch(null) },
            context = context,
            isFolder = true,
        )
    }
}

@Composable
private fun FileSelector(
    label: String,
    selectedFileUri: String,
    onSelectClick: () -> Unit,
    context: Context,
    isFolder: Boolean = false,
) {
    val displayName = remember(selectedFileUri) {
        if (selectedFileUri.isNotBlank()) {
            try {
                val uri = Uri.parse(selectedFileUri)
                if (isFolder) getFolderName(uri, context) else getFileName(uri, context)
            } catch (e: Exception) {
                "Invalid URI"
            }
        } else {
            "Not selected"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onSelectClick) {
            Text(if (isFolder) "Select Folder" else "Select File")
        }
    }
}

@Composable
private fun PermissionsSettingsCard() {
    val context = LocalContext.current
    var permissionUpdateTrigger by remember { mutableIntStateOf(0) }

    val hasNotificationPermission = remember(permissionUpdateTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val canScheduleExactAlarms = remember(permissionUpdateTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionUpdateTrigger++ },
    )

    SettingsCard(title = "Permissions", icon = Icons.Default.Security) {
        PermissionRow(
            icon = Icons.Default.Notifications,
            name = "Notifications",
            description = "Required to show reminders",
            isGranted = hasNotificationPermission,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PermissionRow(
            icon = Icons.Default.Alarm,
            name = "Exact Alarms",
            description = "Required for reminders to be on time",
            isGranted = canScheduleExactAlarms,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionRow(
    icon: ImageVector,
    name: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        headlineContent = { Text(name) },
        supportingContent = { Text(description) },
        trailingContent = {
            if (isGranted) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Granted", tint = Color(0xFF388E3C))
            } else {
                Button(onClick = onGrantClick, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Grant")
                }
            }
        },
    )
}

@Composable
private fun NerSettingsCard(
    state: SettingsUiState,
    onModelFileSelected: (String) -> Unit,
    onTokenizerFileSelected: (String) -> Unit,
    onLabelsFileSelected: (String) -> Unit,
    onReloadClick: () -> Unit,
) {
    val context = LocalContext.current
    val areAllFilesSelected = state.nerModelUri.isNotBlank() && state.nerTokenizerUri.isNotBlank() && state.nerLabelsUri.isNotBlank()

    val modelLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onModelFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for model file. The user might see errors later.", e)
            }
        }
    }

    val tokenizerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onTokenizerFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for tokenizer file.", e)
            }
        }
    }

    val labelsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onLabelsFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for labels file.", e)
            }
        }
    }

    SettingsCard(
        title = "Date/Time NER Model (ONNX)",
        icon = Icons.Default.Memory,
    ) {
        FileSelector(
            label = "Model File (.onnx)",
            selectedFileUri = state.nerModelUri,
            onSelectClick = { modelLauncher.launch(arrayOf("*/*", "application/octet-stream")) },
            context = context,
        )
        FileSelector(
            label = "Tokenizer File (.json)",
            selectedFileUri = state.nerTokenizerUri,
            onSelectClick = { tokenizerLauncher.launch(arrayOf("application/json", "text/plain")) },
            context = context,
        )
        FileSelector(
            label = "Labels File (.json)",
            selectedFileUri = state.nerLabelsUri,
            onSelectClick = { labelsLauncher.launch(arrayOf("application/json", "text/plain")) },
            context = context,
        )
        Spacer(modifier = Modifier.height(12.dp))
        NerStatusIndicator(
            nerState = state.nerState,
            areAllFilesSelected = areAllFilesSelected,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onReloadClick,
            enabled = areAllFilesSelected && state.nerState !is NerState.Downloading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Default.Sync, contentDescription = "Reload", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(if (state.nerState is NerState.Error) "Try Again" else "Reload Model")
        }
    }
}

@Composable
private fun NerStatusIndicator(nerState: NerState, areAllFilesSelected: Boolean) {
    val (icon, color, text) = when (nerState) {
        is NerState.Downloading -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.primary, "Loading: ${nerState.progress}%")
        is NerState.Error -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Error: ${nerState.message}")
        NerState.NotInitialized -> {
            val message = if (areAllFilesSelected) "Model not loaded. Press 'Reload Model'." else "Select all three model files"
            Triple(Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant, message)
        }
        NerState.Ready -> Triple(Icons.Default.CheckCircle, Color(0xFF388E3C), "Model loaded successfully")
    }

    val animatedColor by animateColorAsState(targetValue = color, label = "ner_status_color")

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = "Status", tint = animatedColor)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = animatedColor, modifier = Modifier.weight(1f))
        }
        if (nerState is NerState.Downloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { nerState.progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    label: String,
    selectedValue: String,
    models: List<String>,
    onModelSelected: (String) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String,
    singleLine: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
        label = "",
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(helper) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth().background(backgroundColor).onFocusChanged { isFocused = it.isFocused },
    )
}

@Composable
private fun ThemeSettingsCard(
    themeSettings: com.romankozak.forwardappmobile.ui.theme.ThemeSettings,
    onThemeModeSelected: (com.romankozak.forwardappmobile.ui.theme.ThemeMode) -> Unit,
    onLightThemeSelected: (com.romankozak.forwardappmobile.ui.theme.ThemeName) -> Unit,
    onDarkThemeSelected: (com.romankozak.forwardappmobile.ui.theme.ThemeName) -> Unit,
) {
    SettingsCard(
        title = "Theme",
        icon = Icons.Default.Palette,
    ) {
        ThemeModeSelector(
            selectedMode = themeSettings.themeMode,
            onModeSelected = onThemeModeSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        ThemePicker(
            label = "Light Theme",
            themes = com.romankozak.forwardappmobile.ui.theme.ThemeManager.themes.map { it.name },
            selectedTheme = themeSettings.lightThemeName,
            onThemeSelected = onLightThemeSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        ThemePicker(
            label = "Dark Theme",
            themes = com.romankozak.forwardappmobile.ui.theme.ThemeManager.themes.map { it.name },
            selectedTheme = themeSettings.darkThemeName,
            onThemeSelected = onDarkThemeSelected
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selectedMode: com.romankozak.forwardappmobile.ui.theme.ThemeMode,
    onModeSelected: (com.romankozak.forwardappmobile.ui.theme.ThemeMode) -> Unit
) {
    Column {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.romankozak.forwardappmobile.ui.theme.ThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(
    label: String,
    themes: List<com.romankozak.forwardappmobile.ui.theme.ThemeName>,
    selectedTheme: com.romankozak.forwardappmobile.ui.theme.ThemeName,
    onThemeSelected: (com.romankozak.forwardappmobile.ui.theme.ThemeName) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedTheme.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                themes.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.displayName) },
                        onClick = {
                            onThemeSelected(theme)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
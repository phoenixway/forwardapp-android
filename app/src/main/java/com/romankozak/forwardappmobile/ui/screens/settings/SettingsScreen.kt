package com.romankozak.forwardappmobile.ui.screens.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner.NerState
import com.romankozak.forwardappmobile.ui.ModelsState
import com.romankozak.forwardappmobile.ui.screens.backlogs.PlanningSettingsState

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    planningSettings: PlanningSettingsState,
    initialVaultName: String,
    reservedContextCount: Int,
    onManageContextsClick: () -> Unit,
    onBack: () -> Unit,
    onSave: (
        showModes: Boolean, dailyTag: String, mediumTag: String, longTag: String,
        vaultName: String
    ) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var tempShowModes by remember(planningSettings.showModes) { mutableStateOf(planningSettings.showModes) }
    var tempDailyTag by remember(planningSettings.dailyTag) { mutableStateOf(planningSettings.dailyTag) }
    var tempMediumTag by remember(planningSettings.mediumTag) { mutableStateOf(planningSettings.mediumTag) }
    var tempLongTag by remember(planningSettings.longTag) { mutableStateOf(planningSettings.longTag) }
    var tempVaultName by remember(initialVaultName) { mutableStateOf(initialVaultName) }

    // --- ПОКРАЩЕННЯ: Визначаємо, чи були внесені зміни, щоб активувати кнопку "Зберегти" ---
    val isDirty by remember {
        derivedStateOf {
            tempShowModes != planningSettings.showModes ||
                    tempDailyTag != planningSettings.dailyTag ||
                    tempMediumTag != planningSettings.mediumTag ||
                    tempLongTag != planningSettings.longTag ||
                    tempVaultName != initialVaultName ||
                    uiState.ollamaUrl != viewModel.uiState.value.ollamaUrl ||
                    uiState.fastModel != viewModel.uiState.value.fastModel ||
                    uiState.smartModel != viewModel.uiState.value.smartModel ||
                    uiState.nerModelUri != viewModel.uiState.value.nerModelUri ||
                    uiState.nerTokenizerUri != viewModel.uiState.value.nerTokenizerUri ||
                    uiState.nerLabelsUri != viewModel.uiState.value.nerLabelsUri
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .imePadding(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    // --- ПОКРАЩЕННЯ: Кнопка активна лише за наявності змін ---
                    enabled = isDirty,
                    onClick = {
                        onSave(
                            tempShowModes,
                            tempDailyTag,
                            tempMediumTag,
                            tempLongTag,
                            tempVaultName
                        )
                        viewModel.saveSettings()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PermissionsSettingsCard()

            OllamaSettingsCard(
                state = uiState,
                onUrlChange = viewModel::onUrlChanged,
                onFetchClick = viewModel::fetchAvailableModels,
                onFastModelSelect = viewModel::onFastModelSelected,
                onSmartModelSelect = viewModel::onSmartModelSelected
            )

            NerSettingsCard(
                state = uiState,
                onModelFileSelected = viewModel::onNerModelFileSelected,
                onTokenizerFileSelected = viewModel::onNerTokenizerFileSelected,
                onLabelsFileSelected = viewModel::onNerLabelsFileSelected,
                onReloadClick = viewModel::reloadNerModel
            )

            SettingsCard(
                title = "Planning Modes",
                icon = Icons.Default.Tune
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Show planning scale modes", modifier = Modifier.weight(1f))
                    Switch(checked = tempShowModes, onCheckedChange = { tempShowModes = it })
                }
                AnimatedTextField(
                    value = tempDailyTag,
                    onValueChange = { tempDailyTag = it },
                    label = "Daily Mode Tag",
                    helper = "Tag used for daily planning mode"
                )
                AnimatedTextField(
                    value = tempMediumTag,
                    onValueChange = { tempMediumTag = it },
                    label = "Medium Mode Tag",
                    helper = "Tag used for medium planning mode"
                )
                AnimatedTextField(
                    value = tempLongTag,
                    onValueChange = { tempLongTag = it },
                    label = "Long Mode Tag",
                    helper = "Tag used for long planning mode"
                )
            }

            SettingsCard(
                title = "Integrations",
                icon = Icons.Default.Link
            ) {
                Text("Specify the exact name of your Obsidian Vault for link integration.")
                AnimatedTextField(
                    value = tempVaultName,
                    onValueChange = { tempVaultName = it },
                    label = "Obsidian Vault Name",
                    helper = "Exact vault name for link integration",
                    singleLine = true
                )
            }

            SettingsCard(
                title = "Contexts",
                icon = Icons.Default.Label
            ) {
                OutlinedButton(
                    onClick = onManageContextsClick,
                    modifier = Modifier.fillMaxWidth(),
                    content = { Text("Manage Reserved Contexts ($reservedContextCount)") },
                    colors = ButtonDefaults.outlinedButtonColors()
                )
            }
        }
    }
}

// --- ПОКРАЩЕННЯ: Повністю перероблена секція дозволів для кращого вигляду ---
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
        onResult = { permissionUpdateTrigger++ }
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
            }
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
            }
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
    onGrantClick: () -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(name) },
        supportingContent = { Text(description) },
        trailingContent = {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF388E3C)
                )
            } else {
                Button(onClick = onGrantClick, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Grant")
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OllamaSettingsCard(
    state: SettingsUiState,
    onUrlChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onFastModelSelect: (String) -> Unit,
    onSmartModelSelect: (String) -> Unit
) {
    SettingsCard(
        title = "Ollama AI Integration",
        icon = Icons.Default.Dns
    ) {
        AnimatedTextField(
            value = state.ollamaUrl,
            onValueChange = onUrlChange,
            label = "Ollama Server URL",
            helper = "e.g., http://192.168.1.5:11434",
            singleLine = true
        )
        OutlinedButton(
            onClick = onFetchClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.ollamaUrl.isNotBlank() && state.modelsState !is ModelsState.Loading
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
                    onModelSelected = onFastModelSelect
                )
                ModelSelector(
                    label = "Smart Model",
                    selectedValue = state.smartModel,
                    models = modelsState.models,
                    onModelSelected = onSmartModelSelect
                )
            }
            ModelsState.Loading -> { /* Handled by button state */ }
        }
    }
}

@Composable
private fun NerSettingsCard(
    state: SettingsUiState,
    onModelFileSelected: (String) -> Unit,
    onTokenizerFileSelected: (String) -> Unit,
    onLabelsFileSelected: (String) -> Unit,
    onReloadClick: () -> Unit
) {
    val context = LocalContext.current

    val modelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onModelFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for model file", e)
                onModelFileSelected(it.toString())
            }
        }
    }

    val tokenizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onTokenizerFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for tokenizer file", e)
                onTokenizerFileSelected(it.toString())
            }
        }
    }

    val labelsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onLabelsFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for labels file", e)
                onLabelsFileSelected(it.toString())
            }
        }
    }

    SettingsCard(
        title = "Date/Time NER Model (ONNX)",
        icon = Icons.Default.Memory
    ) {
        FileSelector(
            label = "Model File (.onnx)",
            selectedFileUri = state.nerModelUri,
            onSelectClick = {
                modelLauncher.launch(arrayOf("*/*", "application/octet-stream"))
            },
            context = context
        )
        FileSelector(
            label = "Tokenizer File (.json)",
            selectedFileUri = state.nerTokenizerUri,
            onSelectClick = {
                tokenizerLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            context = context
        )
        FileSelector(
            label = "Labels File (.json)",
            selectedFileUri = state.nerLabelsUri,
            onSelectClick = {
                labelsLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            context = context
        )
        Spacer(modifier = Modifier.height(12.dp))
        NerStatusIndicator(
            nerState = state.nerState,
            onReloadClick = onReloadClick,
            areAllFilesSelected = state.nerModelUri.isNotBlank() &&
                    state.nerTokenizerUri.isNotBlank() &&
                    state.nerLabelsUri.isNotBlank()
        )
    }
}

@Composable
private fun NerStatusIndicator(
    nerState: NerState,
    onReloadClick: () -> Unit,
    areAllFilesSelected: Boolean
) {
    val (icon, color, text) = when (nerState) {
        is NerState.Downloading -> Triple(
            Icons.Default.Sync,
            MaterialTheme.colorScheme.primary,
            "Завантаження: ${nerState.progress}%"
        )
        is NerState.Error -> Triple(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Помилка: ${nerState.message}"
        )
        NerState.NotInitialized -> {
            val message = if (areAllFilesSelected) "Натисніть 'Зберегти' та перезапустіть" else "Оберіть усі три файли"
            Triple(Icons.Default.Error, MaterialTheme.colorScheme.onSurfaceVariant, message)
        }
        NerState.Ready -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF388E3C),
            "Модель успішно завантажена"
        )
    }

    val animatedColor by animateColorAsState(targetValue = color, label = "ner_status_color")

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = "Status", tint = animatedColor)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = animatedColor, modifier = Modifier.weight(1f))

            if (nerState is NerState.Error || (nerState is NerState.NotInitialized && areAllFilesSelected)) {
                OutlinedButton(onClick = onReloadClick) {
                    Text("Спробувати")
                }
            }
        }
        // --- ПОКРАЩЕННЯ: Додано індикатор прогресу ---
        if (nerState is NerState.Downloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { nerState.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FileSelector(
    label: String,
    selectedFileUri: String,
    onSelectClick: () -> Unit,
    context: Context
) {
    val fileName = remember(selectedFileUri) {
        if (selectedFileUri.isNotBlank()) getFileName(Uri.parse(selectedFileUri), context) else "Not selected"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onSelectClick) {
            Text("Select")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    label: String,
    selectedValue: String,
    models: List<String>,
    onModelSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        isExpanded = false
                    }
                )
            }
        }
    }
}


// --- ПОКРАЩЕННЯ: SettingsCard тепер може приймати іконку ---
@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
    singleLine: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else Color.Transparent, label = ""
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(helper) },
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
    )
}
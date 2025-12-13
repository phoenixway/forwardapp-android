package com.romankozak.forwardappmobile.ui.screens.projectstructure

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StructurePresetEditorScreen(
    navController: NavController,
    viewModel: StructurePresetEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.presetId == null) "Новий пресет" else "Редагувати пресет", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.onSave(); navController.popBackStack() }) {
                        Text("Зберегти")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.label,
                onValueChange = viewModel::onLabelChange,
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

                FeatureToggles(
                    enableInbox = uiState.enableInbox,
                    onInboxChange = viewModel::onEnableInboxChange,
                    enableLog = uiState.enableLog,
                    onLogChange = viewModel::onEnableLogChange,
                    enableArtifact = uiState.enableArtifact,
                    onArtifactChange = viewModel::onEnableArtifactChange,
                    enableAdvanced = uiState.enableAdvanced,
                    onAdvancedChange = viewModel::onEnableAdvancedChange,
                    enableDashboard = uiState.enableDashboard,
                    onDashboardChange = viewModel::onEnableDashboardChange,
                    enableBacklog = uiState.enableBacklog,
                    onBacklogChange = viewModel::onEnableBacklogChange,
                )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Елементи пресету", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.items, key = { it.id }) { item ->
                    PresetEditorItemRow(
                        item = item,
                        onRemove = { viewModel.removeItem(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPresetItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { entityType, roleCode, containerType, title, mandatory ->
                viewModel.addItem(
                    PresetEditorItem(
                        entityType = entityType,
                        roleCode = roleCode,
                        containerType = containerType,
                        title = title,
                        mandatory = mandatory,
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun FeatureToggles(
    enableInbox: Boolean,
    onInboxChange: (Boolean) -> Unit,
    enableLog: Boolean,
    onLogChange: (Boolean) -> Unit,
    enableArtifact: Boolean,
    onArtifactChange: (Boolean) -> Unit,
    enableAdvanced: Boolean,
    onAdvancedChange: (Boolean) -> Unit,
    enableDashboard: Boolean,
    onDashboardChange: (Boolean) -> Unit,
    enableBacklog: Boolean,
    onBacklogChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Фіче-флаги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FeatureToggleRow("Inbox", enableInbox, onInboxChange)
        FeatureToggleRow("Log", enableLog, onLogChange)
        FeatureToggleRow("Artifact", enableArtifact, onArtifactChange)
        FeatureToggleRow("Advanced mode", enableAdvanced, onAdvancedChange)
        FeatureToggleRow("Dashboard view", enableDashboard, onDashboardChange)
        FeatureToggleRow("Backlog view", enableBacklog, onBacklogChange)
    }
}

@Composable
private fun FeatureToggleRow(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        androidx.compose.material3.Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun PresetEditorItemRow(
    item: PresetEditorItem,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("type: ${item.entityType} ${item.containerType ?: ""}", style = MaterialTheme.typography.bodySmall)
            Text("role: ${item.roleCode}", style = MaterialTheme.typography.bodySmall)
            if (item.mandatory) {
                Text("Mandatory", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPresetItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (entityType: String, roleCode: String, containerType: String?, title: String, mandatory: Boolean) -> Unit,
) {
    val entityTypes = listOf("ATTACHMENT", "SUBPROJECT")
    val containerTypes = listOf("NOTE", "CHECKLIST", "URL", "PROJECT_LINK")
    var entityType by remember { mutableStateOf(entityTypes.first()) }
    var roleCode by remember { mutableStateOf("") }
    var containerType by remember { mutableStateOf(containerTypes.first()) }
    var title by remember { mutableStateOf("") }
    var mandatory by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && roleCode.isNotBlank()) {
                    onConfirm(entityType, roleCode, if (entityType == "ATTACHMENT") containerType else null, title, mandatory)
                }
            }) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
        title = { Text("Новий елемент пресету") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Entity type", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = Int.MAX_VALUE
                ) {
                    entityTypes.forEach { type ->
                        AssistChip(
                            onClick = { entityType = type },
                            label = { Text(type) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (entityType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (entityType == type) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (entityType == type) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        )
                    }
                }
                if (entityType == "ATTACHMENT") {
                    Text("Container type", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = Int.MAX_VALUE
                    ) {
                        containerTypes.forEach { type ->
                            AssistChip(
                                onClick = { containerType = type },
                                label = { Text(type) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (containerType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (containerType == type) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (containerType == type) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            )
                        }
                    }
                }
                androidx.compose.material3.OutlinedTextField(
                    value = roleCode,
                    onValueChange = { roleCode = it },
                    label = { Text("Role code") }
                )
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mandatory")
                    androidx.compose.material3.Switch(checked = mandatory, onCheckedChange = { mandatory = it })
                }
            }
        }
    )
}

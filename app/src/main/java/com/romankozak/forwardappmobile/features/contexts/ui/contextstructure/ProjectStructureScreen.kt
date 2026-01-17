package com.romankozak.forwardappmobile.features.contexts.ui.contextstructure

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStructureItem
import com.romankozak.forwardappmobile.features.contexts.data.models.StructurePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStructureScreen(
    navController: NavController,
    viewModel: ProjectStructureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var presetMenuExpanded by remember { mutableStateOf(false) }
    var addDialogVisible by remember { mutableStateOf(false) }
    var selectedPresetCode by remember { mutableStateOf(uiState.basePresetCode ?: uiState.presets.firstOrNull()?.code) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Структура проєкту", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { addDialogVisible = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add item")
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
        ) {
            PresetSelector(
                presets = uiState.presets,
                selectedPresetCode = selectedPresetCode,
                onPresetSelect = { selectedPresetCode = it },
                onApplyClick = {
                    selectedPresetCode?.let { code -> viewModel.applyPreset(code) }
                },
                expanded = presetMenuExpanded,
                onExpandChange = { presetMenuExpanded = it },
                basePresetCode = uiState.basePresetCode
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureFlagsSection(
                flags = uiState.featureFlags,
                onToggle = viewModel::onToggleFeatureFlag
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Елементи структури", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { viewModel.applyStructure() }) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Синхронізувати")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    StructureItemRow(
                        item = item,
                        onToggle = { enabled -> viewModel.toggleItem(item, enabled) }
                    )
                }
            }
        }
    }

    if (addDialogVisible) {
        AddStructureItemDialog(
            onDismiss = { addDialogVisible = false },
            onConfirm = { entityType, roleCode, containerType, title, mandatory ->
                viewModel.addItem(entityType, roleCode, containerType, title, mandatory)
                addDialogVisible = false
            }
        )
    }
}

@Composable
private fun PresetSelector(
    presets: List<StructurePreset>,
    selectedPresetCode: String?,
    onPresetSelect: (String) -> Unit,
    onApplyClick: () -> Unit,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    basePresetCode: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Пресет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { onExpandChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedPresetCode ?: basePresetCode ?: "Не вибрано")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) },
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = {
                            onPresetSelect(preset.code)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onApplyClick,
            enabled = selectedPresetCode != null,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Застосувати пресет")
        }
    }
}

@Composable
private fun FeatureFlagsSection(
    flags: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Фічі", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        flags.toList().forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(key, !value) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(key)
                Switch(checked = value, onCheckedChange = { onToggle(key, it) })
            }
        }
    }
}

@Composable
private fun StructureItemRow(
    item: ProjectStructureItem,
    onToggle: (Boolean) -> Unit,
) {
    val disabledToggle = item.mandatory
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("role: ${item.roleCode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val typeLabel = if (item.entityType.equals("ATTACHMENT", true)) "Вкладення" else "Підпроєкт"
                val icon = if (item.entityType.equals("ATTACHMENT", true)) Icons.Outlined.Link else Icons.Outlined.SubdirectoryArrowRight
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(typeLabel, style = MaterialTheme.typography.bodySmall)
                if (item.mandatory) {
                    Text("Mandatory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Switch(
            checked = item.isEnabled || item.mandatory,
            onCheckedChange = { onToggle(it) },
            enabled = !disabledToggle
        )
    }
}

@Composable
fun AddStructureItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (entityType: String, roleCode: String, containerType: String?, title: String, mandatory: Boolean) -> Unit,
) {
    var entityType by remember { mutableStateOf("ATTACHMENT") }
    var roleCode by remember { mutableStateOf("") }
    var containerType by remember { mutableStateOf("NOTE") }
    var title by remember { mutableStateOf("") }
    var mandatory by remember { mutableStateOf(false) }
    var showEntityMenu by remember { mutableStateOf(false) }
    var showContainerMenu by remember { mutableStateOf(false) }

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
        title = { Text("Нове семантичне поле") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { showEntityMenu = true }) { Text(if (entityType == "ATTACHMENT") "Вкладення" else "Підпроєкт") }
                    DropdownMenu(expanded = showEntityMenu, onDismissRequest = { showEntityMenu = false }) {
                        DropdownMenuItem(text = { Text("Вкладення") }, onClick = { entityType = "ATTACHMENT"; showEntityMenu = false })
                        DropdownMenuItem(text = { Text("Підпроєкт") }, onClick = { entityType = "SUBPROJECT"; showEntityMenu = false })
                    }
                }
                if (entityType == "ATTACHMENT") {
                    Box {
                        OutlinedButton(onClick = { showContainerMenu = true }) { Text(containerType) }
                        DropdownMenu(expanded = showContainerMenu, onDismissRequest = { showContainerMenu = false }) {
                            listOf("NOTE", "CHECKLIST", "URL", "PROJECT_LINK").forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = {
                                    containerType = type
                                    showContainerMenu = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = roleCode,
                    onValueChange = { roleCode = it },
                    label = { Text("Role code") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(checked = mandatory, onCheckedChange = { mandatory = it })
                    Text("Mandatory")
                }
            }
        }
    )
}

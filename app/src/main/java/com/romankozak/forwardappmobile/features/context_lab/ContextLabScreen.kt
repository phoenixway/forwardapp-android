package com.romankozak.forwardappmobile.features.context_lab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextRole
import com.romankozak.forwardappmobile.core.navigation.routes.EXPERIMENTAL_HIERARCHY_ROUTE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextLabScreen(
    viewModel: ContextLabViewModel,
    navController: NavController,
) {
    val contexts by viewModel.uiState.collectAsState()
    val activeId by viewModel.activeContextId.collectAsState()
    val allCaps = viewModel.allCapabilities
    val availableRoles = viewModel.availableRoles

    var newContextName by remember { mutableStateOf("") }
    var selectedRoleCode by remember { mutableStateOf(availableRoles.firstOrNull()?.code ?: "") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лабораторія Контекстів") },
                actions = {
                    IconButton(onClick = { navController.navigate(EXPERIMENTAL_HIERARCHY_ROUTE) }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Open Experimental Hierarchy",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
        ) {
            item {
                Text(
                    text = "Експериментальне керування поліморфними контекстами",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Створити новий контекст",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        OutlinedTextField(
                            value = newContextName,
                            onValueChange = { newContextName = it },
                            label = { Text("Ім'я контексту") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = availableRoles.find { it.code == selectedRoleCode }?.label ?: "",
                                onValueChange = {},
                                label = { Text("Роль") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Вибрати роль",
                                        )
                                    }
                                },
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                availableRoles.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role.label) },
                                        onClick = {
                                            selectedRoleCode = role.code
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (newContextName.isNotBlank() && selectedRoleCode.isNotBlank()) {
                                    viewModel.onCreateContext(newContextName, selectedRoleCode)
                                    newContextName = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newContextName.isNotBlank() && selectedRoleCode.isNotBlank(),
                        ) {
                            Text("Створити контекст")
                        }
                    }
                }
            }

            items(contexts) { context ->
                ContextItemCard(
                    context = context,
                    isActive = context.id == activeId,
                    allCapabilities = allCaps,
                    availableRoles = availableRoles,
                    onToggle = { capId -> viewModel.onToggleCapability(context.id, capId) },
                    onActivate = { viewModel.onActivateContext(context.id) },
                    onChangeRole = { newRoleCode -> viewModel.onChangeRole(context.id, newRoleCode) },
                )
            }
        }
    }
}

@Composable
fun ContextItemCard(
    context: com.romankozak.forwardappmobile.core.context.Context,
    isActive: Boolean,
    allCapabilities: Set<CapabilityDescriptor>,
    availableRoles: List<ContextRole>,
    onToggle: (CapabilityId) -> Unit,
    onActivate: () -> Unit,
    onChangeRole: (String) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = context.id.raw, style = MaterialTheme.typography.titleLarge)
                    // Випадаючий список для вибору ролі
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth(0.8f)) {
                        OutlinedTextField(
                            value = context.role.label,
                            onValueChange = {},
                            label = { Text("Роль") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Вибрати роль",
                                    )
                                }
                            },
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            availableRoles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.label) },
                                    onClick = {
                                        onChangeRole(role.code)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color.Green,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "Можливості (Capabilities):", style = MaterialTheme.typography.titleSmall)

            allCapabilities.forEach { cap ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = context.config.activeCapabilities.contains(cap.id),
                        onCheckedChange = { onToggle(cap.id) },
                    )
                    Text(text = cap.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive && context.config.activeViews.isNotEmpty(),
            ) {
                Text(if (isActive) "АКТИВНИЙ" else "АКТИВУВАТИ КОНТЕКСТ")
            }
            if (context.config.activeViews.isEmpty()) {
                Text(
                    text = "Цей контекст не має жодного екрану. Активуйте можливості з UI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

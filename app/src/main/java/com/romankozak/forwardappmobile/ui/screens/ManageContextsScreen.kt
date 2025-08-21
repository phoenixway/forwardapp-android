// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/ManageContextsScreen.kt

package com.romankozak.forwardappmobile.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.dialogs.UiContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageContextsScreen(
    initialContexts: List<UiContext>,
    onBack: () -> Unit,
    onSave: (updatedContexts: List<UiContext>) -> Unit,
) {
    val contexts = remember { mutableStateListOf<UiContext>().apply { addAll(initialContexts.map { it.copy() }) } }

    val hasNameErrors by remember {
        derivedStateOf {
            val names = contexts
                .filter { !it.isReserved }
                .map { it.name.lowercase().trim() }
                .filter { it.isNotEmpty() }
            names.size != names.distinct().size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Contexts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = !hasNameErrors,
                        onClick = { onSave(contexts.toList()) },
                    ) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        val reserved = contexts.filter { it.isReserved }
        val custom = contexts.filter { !it.isReserved }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Reserved section
            if (reserved.isNotEmpty()) {
                item { SectionHeader("Reserved") }
                items(reserved, key = { it.id }) { context ->
                    val index = contexts.indexOf(context)
                    ContextEditorItem(
                        context = context,
                        contextsList = contexts,
                        onValueChange = { if (index != -1) contexts[index] = it },
                        onDelete = {},
                    )
                }
            }

            // Custom section
            item { SectionHeader("Custom") }

            if (custom.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = "No custom contexts yet.\nTap the button below to add one.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(custom, key = { it.id }) { context ->
                    val index = contexts.indexOf(context)
                    ContextEditorItem(
                        context = context,
                        contextsList = contexts,
                        onValueChange = { if (index != -1) contexts[index] = it },
                        onDelete = { contexts.remove(context) },
                    )
                }
            }

            // Add new custom context
            item {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        val newContext = UiContext(name = "", tag = "", emoji = "", isReserved = false)
                        contexts.add(newContext)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Context")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Custom Context")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun ContextEditorItem(
    context: UiContext,
    contextsList: SnapshotStateList<UiContext>,
    onValueChange: (UiContext) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNameError = remember(context.name, contextsList.size) {
        !context.isReserved && context.name.isNotBlank() && contextsList.any {
            (it.id != context.id) && it.name.equals(context.name, ignoreCase = true)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = context.name,
                    onValueChange = {
                        onValueChange(
                            context.copy(
                                name = it.filter { char -> char.isLetterOrDigit() || (char == '-') },
                            ),
                        )
                    },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    readOnly = context.isReserved,
                    enabled = !context.isReserved,
                    singleLine = true,
                    isError = isNameError,
                    supportingText = { if (isNameError) Text("Name must be unique") },
                )
                if (!context.isReserved) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Context",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = context.emoji,
                    onValueChange = { onValueChange(context.copy(emoji = it.take(1))) }, // Only 1 emoji
                    label = { Text("Emoji") },
                    modifier = Modifier.width(90.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = context.tag,
                    onValueChange = { onValueChange(context.copy(tag = it)) },
                    label = { Text("Corresponding Tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
    }
}
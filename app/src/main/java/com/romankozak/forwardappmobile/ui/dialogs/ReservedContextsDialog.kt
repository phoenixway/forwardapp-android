// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/dialogs/ReservedContextsDialog.kt

package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

data class UiContext(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var tag: String,
    var emoji: String,
    val isReserved: Boolean
)

@Composable
fun ReservedContextsDialog(
    initialContexts: List<UiContext>,
    onDismiss: () -> Unit,
    onSave: (updatedContexts: List<UiContext>) -> Unit
) {
    val contexts = remember { mutableStateListOf<UiContext>().apply { addAll(initialContexts) } }
    // ✨ ДОДАНО: Стан для керування скролом та корутина для анімації
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Contexts") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                OutlinedButton(
                    onClick = {
                        contexts.add(UiContext(name = "", tag = "", emoji = "", isReserved = false))
                        // ✨ ДОДАНО: Запускаємо скрол до нового елемента
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(contexts.size - 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Context")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Custom Context")
                }
                Spacer(Modifier.height(16.dp))

                LazyColumn(state = lazyListState) { // ✨ ДОДАНО: Передаємо стан в LazyColumn
                    items(contexts, key = { it.id }) { context ->
                        ContextEditorItem(
                            context = context,
                            contextsList = contexts,
                            onValueChange = { updatedContext ->
                                val index = contexts.indexOfFirst { it.id == updatedContext.id }
                                if (index != -1) {
                                    contexts[index] = updatedContext
                                }
                            },
                            onDelete = {
                                contexts.remove(context)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val validContexts = contexts.filter { !it.isReserved && it.name.isNotBlank() || it.isReserved }
                onSave(validContexts)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ContextEditorItem(
    context: UiContext,
    contextsList: SnapshotStateList<UiContext>,
    onValueChange: (UiContext) -> Unit,
    onDelete: () -> Unit
) {
    val isNameError = remember(context.name, contextsList.size) {
        !context.isReserved && context.name.isNotBlank() && contextsList.any {
            it.id != context.id && it.name.equals(context.name, ignoreCase = true)
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = context.name,
                onValueChange = { onValueChange(context.copy(name = it.filter { char -> char.isLetterOrDigit() || char == '-' })) },
                label = { Text("Name") },
                modifier = Modifier.weight(1f),
                readOnly = context.isReserved,
                enabled = !context.isReserved,
                singleLine = true,
                isError = isNameError,
                supportingText = { if (isNameError) Text("Name must be unique") }
            )
            if (!context.isReserved) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Context", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = context.emoji,
                onValueChange = { onValueChange(context.copy(emoji = it)) },
                label = { Text("Emoji") },
                modifier = Modifier.width(90.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = context.tag,
                onValueChange = { onValueChange(context.copy(tag = it)) },
                label = { Text("Corresponding Tag") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

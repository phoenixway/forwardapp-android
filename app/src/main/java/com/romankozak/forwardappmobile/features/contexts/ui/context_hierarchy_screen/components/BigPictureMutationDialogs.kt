package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BigPictureStructuralItem

/** Selects persisted structural identity, never a semantic target or historical scope. */
@Composable
internal fun BigPictureDestinationDialog(
    items: List<BigPictureStructuralItem>,
    onDismiss: () -> Unit,
    onChoose: (H1StructuralParent) -> Unit,
    error: String? = null,
    enabled: Boolean = true,
) {
    var search by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Куди розмістити?") },
        text = {
            Column {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Знайти місце") })
                TextButton(enabled = enabled, onClick = { onChoose(H1StructuralParent.Root) }) { Text("Корінь Navigation") }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(items.filter { it.title.contains(search, ignoreCase = true) }, key = { it.ref.toString() }) { item ->
                        TextButton(enabled = enabled, onClick = { onChoose(H1StructuralParent.Node(item.ref)) }) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(item.title)
                                Text(
                                    when (val ref = item.ref) {
                                        is H1TypedNode.Container -> "Секція · ${ref.id}"
                                        is H1TypedNode.Occurrence -> "Розміщення · ${ref.id}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(enabled = enabled, onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
internal fun BigPictureReorderDialog(
    items: List<BigPictureStructuralItem>,
    onDismiss: () -> Unit,
    onApply: (List<H1TypedNode>) -> Unit,
) {
    // Frozen complete sibling set. A concurrent change is rejected by H1, never silently dropped.
    var ordered by remember { mutableStateOf(items) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Порядок елементів") },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(ordered, key = { it.ref.toString() }) { item ->
                    val index = ordered.indexOf(item)
                    Row {
                        Text(item.title, Modifier.weight(1f))
                        fun move(delta: Int) {
                            ordered = ordered.toMutableList().apply { add(index + delta, removeAt(index)) }
                        }
                        IconButton(onClick = { move(-1) }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, "Вище")
                        }
                        IconButton(onClick = { move(1) }, enabled = index < ordered.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, "Нижче")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(ordered.map { it.ref }) }) { Text("Зберегти") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

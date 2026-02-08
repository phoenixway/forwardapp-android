package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.direction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun DirectionView(
    items: List<DirectionItemEntity>,
    modifier: Modifier = Modifier,
    onAddItem: (String) -> Unit,
    onEditItem: (DirectionItemEntity, String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onLinkRequest: (String) -> Unit,
    onUnlinkRequest: (String) -> Unit,
    onOpenLinkedContext: (String) -> Unit,
    linkedContextNames: Map<String, String>,
) {
    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onMove(from.index, to.index)
        }

    var editingItem by remember { mutableStateOf<DirectionItemEntity?>(null) }
    var editingText by remember { mutableStateOf("") }
    val hapticFeedback = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No directions yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Use the input panel below to add a direction",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ReorderableItem(reorderableState, key = item.id) { isDragging ->
                        DirectionItemCard(
                            item = item,
                            linkedContextName = item.linkedContextId?.let { linkedContextNames[it] ?: "Context" },
                            isDragging = isDragging,
                            dragHandleModifier =
                                with(this@ReorderableItem) {
                                    Modifier
                                        .size(28.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                        )
                                },
                            onEdit = {
                                editingItem = item
                                editingText = item.text
                            },
                            onToggleLink = {
                                if (item.linkedContextId == null) {
                                    onLinkRequest(item.id)
                                } else {
                                    onUnlinkRequest(item.id)
                                }
                            },
                            onDelete = { onDeleteItem(item.id) },
                            onOpenLinkedContext = onOpenLinkedContext,
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit direction") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = editingItem ?: return@TextButton
                        onEditItem(item, editingText)
                        editingItem = null
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

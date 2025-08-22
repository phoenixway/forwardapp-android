package com.romankozak.forwardappmobile.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
//import sh.calvin.reorderable.draggableHandle

@Composable
fun ReorderableTestScreen() {
    val items = remember { mutableStateListOf("One", "Two", "Three", "Four") }
    val listState = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        Log.d("DND", "MOVE ${items[from.index]} -> ${items[to.index]}")
        items.add(to.index, items.removeAt(from.index))
    }

    LazyColumn(state = listState) {
        items(items, key = { it }) { item ->
            ReorderableItem(reorderState, key = item) { isDragging ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDragging) Color.LightGray else Color.White)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag",
                        modifier = Modifier
                            .longPressDraggableHandle(
                                onDragStarted = { Log.d("DND", "drag start $item") },
                                onDragStopped = { Log.d("DND", "drag stop $item") }
                            )
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

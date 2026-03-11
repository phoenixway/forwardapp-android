package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearAllHistory: () -> Unit,
) {
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.navigationBarsPadding()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Search History",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (searchHistory.isNotEmpty()) {
                        TextButton(onClick = onClearAllHistory) {
                            Text("Clear all")
                        }
                    }
                }
                if (searchHistory.isEmpty()) {
                    Text(
                        text = "No recent searches.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        val recent = searchHistory.take(3)
                        val older = searchHistory.drop(3)

                        item {
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                        items(recent, key = { "recent-$it" }) { query ->
                            ListItem(
                                headlineContent = { Text(query) },
                                leadingContent = {
                                    Icon(Icons.Outlined.History, contentDescription = "Search history item")
                                },
                                trailingContent = {
                                    IconButton(onClick = { onRemoveHistoryEntry(query) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete query from history",
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { onHistoryClick(query) },
                            )
                        }

                        if (older.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Older",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }
                        items(older, key = { "older-$it" }) { query ->
                            ListItem(
                                headlineContent = { Text(query) },
                                leadingContent = {
                                    Icon(Icons.Outlined.History, contentDescription = "Search history item")
                                },
                                trailingContent = {
                                    IconButton(onClick = { onRemoveHistoryEntry(query) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete query from history",
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { onHistoryClick(query) },
                            )
                        }
                    }
                }
            }
        }
    }
}

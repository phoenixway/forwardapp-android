package com.romankozak.forwardappmobile.features.mainscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
) {
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Search History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (searchHistory.isEmpty()) {
                    Text(
                        text = "No recent searches.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(searchHistory, key = { it }) { query ->
                            ListItem(
                                headlineContent = { Text(query) },
                                leadingContent = {
                                    Icon(Icons.Outlined.History, contentDescription = "Search history item")
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
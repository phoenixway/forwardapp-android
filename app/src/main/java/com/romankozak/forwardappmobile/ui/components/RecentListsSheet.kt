// File: ui/components/RecentListsSheet.kt
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentListsSheet(
    showSheet: Boolean,
    recentLists: List<GoalList>,
    onDismiss: () -> Unit,
    onListClick: (String) -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Нещодавно відкриті",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (recentLists.isEmpty()) {
                    Text(
                        text = "Історія порожня.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(recentLists, key = { it.id }) { list ->
                            ListItem(
                                headlineContent = { Text(list.name) },
                                leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                                modifier = Modifier.clickable { onListClick(list.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
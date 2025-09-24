package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentListsSheet(
    showSheet: Boolean,
    recentItems: List<com.romankozak.forwardappmobile.data.database.models.RecentItem>,
    onDismiss: () -> Unit,
    onItemClick: (com.romankozak.forwardappmobile.data.database.models.RecentItem) -> Unit,
) {
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Нещодавно відкриті",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (recentItems.isEmpty()) {
                    Text(
                        text = "Історія порожня.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(recentItems, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.displayName) },
                                leadingContent = {
                                    val icon = when (item.type) {
                                        com.romankozak.forwardappmobile.data.database.models.RecentItemType.PROJECT -> Icons.Outlined.Folder
                                        com.romankozak.forwardappmobile.data.database.models.RecentItemType.NOTE -> Icons.Outlined.Note
                                        com.romankozak.forwardappmobile.data.database.models.RecentItemType.CUSTOM_LIST -> Icons.Outlined.List
                                        com.romankozak.forwardappmobile.data.database.models.RecentItemType.OBSIDIAN_LINK -> Icons.Outlined.Link
                                    }
                                    Icon(icon, contentDescription = null)
                                },
                                modifier = Modifier.clickable { onItemClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

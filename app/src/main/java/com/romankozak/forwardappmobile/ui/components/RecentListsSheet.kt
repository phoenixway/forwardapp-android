package com.romankozak.forwardappmobile.ui.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecentListsSheet(
    showSheet: Boolean,
    recentItems: List<com.romankozak.forwardappmobile.data.database.models.RecentItem>,
    onDismiss: () -> Unit,
    onItemClick: (com.romankozak.forwardappmobile.data.database.models.RecentItem) -> Unit,
    onPinClick: (com.romankozak.forwardappmobile.data.database.models.RecentItem) -> Unit,
) {
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
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
                    val groupedItems = recentItems.groupBy { it.isPinned }
                    LazyColumn {
                        groupedItems[true]?.let { pinnedItems ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Text(
                                        text = "Закріплені",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            items(pinnedItems, key = { "pinned-${it.id}" }) { item ->
                                RecentItemRow(item, onItemClick, onPinClick)
                            }
                        }

                        groupedItems[false]?.let { unpinnedItems ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Text(
                                        text = "Недавні",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            items(unpinnedItems, key = { "unpinned-${it.id}" }) { item ->
                                RecentItemRow(item, onItemClick, onPinClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentItemRow(
    item: com.romankozak.forwardappmobile.data.database.models.RecentItem,
    onItemClick: (com.romankozak.forwardappmobile.data.database.models.RecentItem) -> Unit,
    onPinClick: (com.romankozak.forwardappmobile.data.database.models.RecentItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d("RecentItemClick", "onItemClick triggered for ${item.displayName}")
                onItemClick(item)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (item.type) {
                com.romankozak.forwardappmobile.data.database.models.RecentItemType.PROJECT -> Icons.Outlined.Folder
                com.romankozak.forwardappmobile.data.database.models.RecentItemType.NOTE -> Icons.Outlined.Note
                com.romankozak.forwardappmobile.data.database.models.RecentItemType.CUSTOM_LIST -> Icons.Outlined.List
                com.romankozak.forwardappmobile.data.database.models.RecentItemType.OBSIDIAN_LINK -> Icons.Outlined.Link
            }
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        IconButton(onClick = {
            Log.d("RecentItemClick", "onPinClick triggered for ${item.displayName}")
            onPinClick(item)
        }) {
            Icon(
                imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin",
                modifier = Modifier.size(20.dp),
                tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

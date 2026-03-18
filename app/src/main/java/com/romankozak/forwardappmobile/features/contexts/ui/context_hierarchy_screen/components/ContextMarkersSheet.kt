package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextMarkersSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    contextMarkers: List<UiContextMarker>,
    contextMarkerToEmojiMap: Map<String, String>,
    onManageContextMarkers: () -> Unit,
    onContextSelected: (String) -> Unit,
) {
    if (showSheet) {
        val sheetColor = MaterialTheme.colorScheme.surfaceContainerLow
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = sheetColor,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Context Markers",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            onDismiss()
                            onManageContextMarkers()
                        },
                    ) {
                        Text("Manage")
                    }
                }
                if (contextMarkers.isEmpty()) {
                    Text(
                        text = "Немає налаштованих маркерів контексту.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(contextMarkers, key = { it.name }) { contextMarker ->
                            ListItem(
                                headlineContent = { Text(contextMarker.name.replaceFirstChar { it.uppercase() }) },
                                leadingContent = {
                                    val markerKey = "@${contextMarker.name.lowercase()}"
                                    val emoji = contextMarkerToEmojiMap[markerKey]
                                    if (!emoji.isNullOrBlank()) {
                                        Text(emoji, fontSize = 24.sp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = contextMarker.name)
                                    }
                                },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = sheetColor,
                                    ),
                                modifier = Modifier.clickable { onContextSelected(contextMarker.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

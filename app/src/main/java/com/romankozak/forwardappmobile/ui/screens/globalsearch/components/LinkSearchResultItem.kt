// --- File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/globalsearch/components/LinkSearchResultItem.kt ---
package com.romankozak.forwardappmobile.ui.screens.globalsearch.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.data.database.models.LinkType

@Composable
fun LinkSearchResultItem(
    result: GlobalLinkSearchResult,
    onClick: () -> Unit,
    onOpenInObsidian: () -> Unit, // ADDED: Specific handler for opening in Obsidian
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick), // Main click navigates to the item in a list
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (result.link.linkData.type) {
                    LinkType.URL -> Icons.Default.Language
                    LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
                    LinkType.GOAL_LIST -> Icons.AutoMirrored.Filled.ListAlt
                    else -> Icons.Default.Link
                },
                contentDescription = "Link Icon",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.link.linkData.displayName ?: result.link.linkData.target,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "в списку: ${result.listName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            // Action icons on the right
            when (result.link.linkData.type) {
                LinkType.GOAL_LIST -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Відкрити в Forward",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinkType.OBSIDIAN -> {
                    // ADDED: Specific button to open in Obsidian
                    IconButton(onClick = onOpenInObsidian) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Note,
                            contentDescription = "Відкрити в Obsidian",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    // Placeholder for other potential actions
                }
            }
        }
    }
}
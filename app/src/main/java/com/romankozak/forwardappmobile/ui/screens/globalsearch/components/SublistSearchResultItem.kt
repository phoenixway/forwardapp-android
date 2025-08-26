// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/globalsearch/components/SublistSearchResultItem.kt

package com.romankozak.forwardappmobile.ui.screens.globalsearch.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.OpenInNew // MODIFIED: Import for the auto-mirrored icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GlobalSublistSearchResult

@Composable
fun SublistSearchResultItem(result: GlobalSublistSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // FIXED: Added trailing comma
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically, // FIXED: Added trailing comma
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ListAlt,
                contentDescription = "Sublist Icon",
                tint = MaterialTheme.colorScheme.primary, // FIXED: Added trailing comma
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.sublist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis, // FIXED: Added trailing comma
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "в списку: ${result.parentListName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // FIXED: Added trailing comma
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew, // FIXED: Used the recommended auto-mirrored icon
                contentDescription = "Відкрити в Forward",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, // FIXED: Added trailing comma
            )
        }
    }
}
package com.romankozak.forwardappmobile.ui.screens.goalsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink

@Composable
fun LinksTabContent(
    links: List<RelatedLink>,
    onAddProjectLink: () -> Unit,
    onAddWebLink: () -> Unit,
    onAddObsidianLink: () -> Unit,
    onRemoveLink: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onAddProjectLink) {
                Icon(Icons.Default.Add, contentDescription = "Add Project Link")
                Text("Project")
            }
            Button(onClick = onAddWebLink) {
                Icon(Icons.Default.Add, contentDescription = "Add Web Link")
                Text("Web")
            }
            Button(onClick = onAddObsidianLink) {
                Icon(Icons.Default.Add, contentDescription = "Add Obsidian Link")
                Text("Obsidian")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (links.isEmpty()) {
            Text("No links added yet.")
        } else {
            LazyColumn {
                items(links) { link ->
                    LinkItem(link = link, onRemoveClick = { onRemoveLink(link.target) })
                }
            }
        }
    }
}

@Composable
private fun LinkItem(link: RelatedLink, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = link.displayName ?: link.target,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = link.type?.name ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemoveClick) {
            Icon(Icons.Default.Delete, contentDescription = "Remove Link")
        }
    }
}

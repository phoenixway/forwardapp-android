package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.Context

@Composable
fun FocusedProjectHeader(
    project: Context,
    onMoreActionsClick: () -> Unit,
    onProjectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReservedRoleBadge(
                roleCode = project.roleCode,
                modifier = Modifier.padding(end = 2.dp),
            )
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).clickable(onClick = onProjectClick),
            )

            IconButton(onClick = onMoreActionsClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
            }
        }
    }
}

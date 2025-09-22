package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Project

@Composable
fun FocusedProjectHeader(
    project: Project,
    onMoreActionsClick: () -> Unit,
    onProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(40.dp)) // Add spacer for alignment
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).clickable(onClick = onProjectClick)
            )
            // TODO: Implement Add action if needed
            // IconButton(onClick = { /* TODO: Handle add action */ }) {
            //     Icon(Icons.Default.Add, contentDescription = "Add")
            // }
            IconButton(onClick = onMoreActionsClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
            }
        }
    }
}

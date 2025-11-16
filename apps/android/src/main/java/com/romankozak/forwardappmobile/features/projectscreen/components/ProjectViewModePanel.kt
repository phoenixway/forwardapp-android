package com.romankozak.forwardappmobile.features.projectscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode

@Composable
fun ProjectViewModePanel(
    currentMode: ProjectViewMode,
    onModeChange: (ProjectViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { onModeChange(ProjectViewMode.Backlog) }) {
            Text("Backlog")
        }
        Button(onClick = { onModeChange(ProjectViewMode.Inbox) }) {
            Text("Inbox")
        }
        Button(onClick = { onModeChange(ProjectViewMode.Advanced) }) {
            Text("Advanced")
        }
        Button(onClick = { onModeChange(ProjectViewMode.Attachments) }) {
            Text("Attachments")
        }
    }
}

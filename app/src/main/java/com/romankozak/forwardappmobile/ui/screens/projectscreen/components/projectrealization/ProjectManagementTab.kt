package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector

enum class ProjectManagementTab(
    val displayName: String,
    val icon: ImageVector,
) {
    Dashboard("Дашборд", Icons.Default.Dashboard),
    Artifact("Артефакт", Icons.Default.Description),
    Log("Історія", Icons.Default.History),
    Insights("Інсайти", Icons.Default.Lightbulb),
}

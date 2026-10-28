package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.navigation.NavTarget

@Composable
fun WorkspaceEntityNavigationHub(
    onOpen: (NavTarget) -> Unit,
    onShowRelations: () -> Unit,
    viewModel: WorkspaceEntityHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    com.romankozak.forwardappmobile.features.planning.WorkspacePlanningEntry(onOpen)
    WorkspaceDashboardRelationsCard(state, onOpen, onViewAll = {
        if (state.enabled) onShowRelations() else expanded = true
    })
    if (expanded) AlertDialog(onDismissRequest = { expanded = false }, title = { Text("Semantics") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                WorkspaceRelationsContent(state) { expanded = false; onOpen(it) }
            }
        },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text("Закрити") } })
}

@Composable
internal fun WorkspaceEntityHubGroups(groups: List<WorkspaceEntityHubGroup>, onOpen: (NavTarget) -> Unit, compact: Boolean = false) {
    groups.forEach { group ->
        Text(group.label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        group.rows.forEach { row ->
            Row(Modifier.fillMaxWidth().clickable(enabled = row.navigation != null) { row.navigation?.let(onOpen) }
                .heightIn(min = 48.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.title, style = MaterialTheme.typography.bodyLarge, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                    Text((if (compact) row.compactDescription() else row.description()) +
                        if (row.navigation == null) " · Перегляд недоступний" else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (row.navigation != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

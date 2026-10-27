package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.features.workspace.WorkspaceDashboardSection

@Composable
internal fun WorkspaceDashboardConnectionsCard(
    attachments: List<BacklogItemContent>,
    onAttachmentClick: (BacklogItemContent) -> Unit,
    onViewAll: () -> Unit,
    connectionAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    WorkspaceDashboardSection("Connections", Icons.Outlined.Link, modifier, actions = {
        if (attachments.size > 6) TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("View all", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp))
        }
        connectionAction()
    }) {
        if (attachments.isEmpty()) Text("Зв’язків поки немає.", Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        attachments.take(6).forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = colors.primary.copy(alpha = 0.08f))
            DashboardConnectionRow(item) { onAttachmentClick(item) }
        }
    }
}

@Composable
private fun DashboardConnectionRow(item: BacklogItemContent, onOpen: () -> Unit) {
    val title = when (item) {
        is BacklogItemContent.LinkItem -> item.link.linkData.displayName?.takeIf(String::isNotBlank) ?: item.link.linkData.target
        is BacklogItemContent.NoteDocumentItem -> item.document.name.ifBlank { "Document" }
        is BacklogItemContent.NoteItem -> item.note.title
        is BacklogItemContent.ChecklistItem -> item.checklist.name.ifBlank { "Checklist" }
        is BacklogItemContent.MusicNoteItem -> item.musicNote.name.ifBlank { "Music note" }
        is BacklogItemContent.ContextLinkItem -> item.project.name
        is BacklogItemContent.GoalItem -> item.goal.text
    }
    val subtitle = when (item) {
        is BacklogItemContent.LinkItem -> "Link"
        is BacklogItemContent.ContextLinkItem -> "Workspace"
        is BacklogItemContent.ChecklistItem -> "Checklist"
        is BacklogItemContent.MusicNoteItem -> "Music note"
        is BacklogItemContent.NoteDocumentItem -> "Document"
        is BacklogItemContent.NoteItem -> "Note"
        is BacklogItemContent.GoalItem -> "Task"
    }
    val icon = when (item) {
        is BacklogItemContent.LinkItem -> Icons.Outlined.Link
        is BacklogItemContent.NoteDocumentItem, is BacklogItemContent.NoteItem -> Icons.Outlined.Description
        is BacklogItemContent.ChecklistItem -> Icons.Outlined.Checklist
        is BacklogItemContent.MusicNoteItem -> Icons.Outlined.MusicNote
        else -> Icons.Default.Attachment
    }
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).background(colors.primaryContainer.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(23.dp), tint = colors.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = colors.primary)
    }
}

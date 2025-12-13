package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectDashboardView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.ProjectManagementTab
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.AttachmentsView
import com.romankozak.forwardappmobile.ui.screens.projectscreen.views.InboxView

private const val TAG = "BACKLOG_UI_DEBUG"

@Composable
fun GoalDetailContent(
  modifier: Modifier = Modifier,
  viewModel: BacklogViewModel,
  uiState: UiState,
  listState: LazyListState,
  inboxListState: LazyListState,
  onEditLog: (com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog) -> Unit,
  onDeleteLog: (com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog) -> Unit,
  onSaveArtifact: (String) -> Unit,
  onEditArtifact: (com.romankozak.forwardappmobile.data.database.models.ProjectArtifact) -> Unit,
  onRemindersClick: (ListItemContent) -> Unit,
  onShowProjectProperties: () -> Unit,
  onSwitchView: (ProjectViewMode) -> Unit,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val goalList by viewModel.project.collectAsStateWithLifecycle()
    val projectLogs by viewModel.projectLogs.collectAsStateWithLifecycle()
    val projectArtifact by viewModel.projectArtifact.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()



    when (uiState.currentView) {
        ProjectViewMode.BACKLOG -> {
            val listContent by viewModel.listContent.collectAsStateWithLifecycle()
            com.romankozak.forwardappmobile.ui.features.backlog.BacklogListScreen(
                items = listContent,
                modifier = modifier,
                listState = listState,
                showCheckboxes = uiState.showCheckboxes,
                selectedItemIds = uiState.selectedItemIds,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                swipedItemId = uiState.swipedItemId,
                swipeResetCounter = uiState.swipeResetCounter,
                onMove = { from, to -> viewModel.onMove(from, to) },
                onItemClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                onLongClick = { item -> viewModel.toggleSelection(item.listItem.id) },
                onCheckedChange = { item, isChecked ->
                    when (item) {
                        is ListItemContent.GoalItem -> viewModel.itemActionHandler.toggleGoalCompletedWithState(item.goal, isChecked)
                        is ListItemContent.SublistItem -> viewModel.onSubprojectCompletedChanged(item.project, isChecked)
                        else -> {}
                    }
                },
                onDelete = { item -> viewModel.itemActionHandler.deleteItem(item) },
                onDeleteEverywhere = { item -> viewModel.onDeleteEverywhere(item) },
                onMoveToTop = { item -> viewModel.onMoveToTop(item) },
                onAddToDayPlan = { item -> viewModel.addItemToDailyPlan(item) },
                onStartTracking = { item -> viewModel.onStartTrackingRequest(item) },
                onShowGoalTransportMenu = { item -> viewModel.itemActionHandler.onGoalTransportInitiated(item) {} },
                onRelatedLinkClick = viewModel.itemActionHandler::onRelatedLinkClick,
                onRemindersClick = onRemindersClick,
                onCopyContent = viewModel.itemActionHandler::copyContentRequest,
                onResetSwipe = viewModel::resetSwipeStatesExcept
            )
        }
        ProjectViewMode.INBOX -> {
            InboxView(
                modifier = modifier,
                viewModel = viewModel,
                inboxRecords = inboxRecords,
                listState = inboxListState,
                highlightedRecordId = uiState.inboxRecordToHighlight,
                navigationManager = viewModel.enhancedNavigationManager,
            )
        }
        ProjectViewMode.ADVANCED -> {
            ProjectDashboardView(
                modifier = modifier,
                project = goalList,
                projectLogs = projectLogs,
                projectArtifact = projectArtifact,
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onStatusUpdate = viewModel::onProjectStatusUpdate,
                projectTimeMetrics = uiState.projectTimeMetrics,
                onRecalculateTime = viewModel::onRecalculateTime,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
                onSaveArtifact = onSaveArtifact,
                onEditArtifact = onEditArtifact,
                selectedTab = uiState.selectedDashboardTab,
                onTabSelected = viewModel::onDashboardTabSelected
            )
        }
        ProjectViewMode.ATTACHMENTS -> {
            AttachmentsView(
                modifier = modifier,
                viewModel = viewModel,
                listContent = listContent
            )
        }
        ProjectViewMode.DASHBOARD -> {
            val attachments = listContent.filter {
                it is ListItemContent.LinkItem || it is ListItemContent.NoteDocumentItem || it is ListItemContent.ChecklistItem
            }
            val currentArtifact by viewModel.projectArtifact.collectAsStateWithLifecycle()
            DashboardOverview(
                modifier = modifier,
                project = goalList,
                attachments = attachments.take(6),
                onAttachmentClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                onShowArtifact = { currentArtifact?.let(viewModel::onEditArtifact) },
                onShowProperties = onShowProjectProperties,
                onSwitchView = onSwitchView,
            )
        }
    }
}

@Composable
private fun DashboardOverview(
    modifier: Modifier = Modifier,
    project: com.romankozak.forwardappmobile.data.database.models.Project?,
    attachments: List<ListItemContent>,
    onAttachmentClick: (ListItemContent) -> Unit,
    onShowArtifact: () -> Unit,
    onShowProperties: () -> Unit,
    onSwitchView: (ProjectViewMode) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Project Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!project?.description.isNullOrBlank()) {
                    Text(
                        text = project?.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onShowArtifact) {
                Icon(Icons.Outlined.Description, contentDescription = "Project artifact")
            }
            IconButton(onClick = onShowProperties) {
                Icon(Icons.Default.Settings, contentDescription = "Project properties")
            }
            IconButton(onClick = { onSwitchView(ProjectViewMode.BACKLOG) }) {
                Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = "Backlog view")
            }
            IconButton(onClick = { onSwitchView(ProjectViewMode.INBOX) }) {
                Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = "Inbox view")
            }
            IconButton(onClick = { onSwitchView(ProjectViewMode.ATTACHMENTS) }) {
                Icon(Icons.Default.Attachment, contentDescription = "Attachments view")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = project?.projectStatusText?.takeIf { it.isNotBlank() }
                        ?: project?.projectStatus ?: "No status",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Attachments",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (attachments.isEmpty()) {
                    Text(
                        text = "No attachments yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    attachments.forEach { item ->
                        AttachmentRowSummary(item = item, onClick = { onAttachmentClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentRowSummary(item: ListItemContent, onClick: () -> Unit) {
    val title = when (item) {
        is ListItemContent.LinkItem ->
            item.link.linkData.displayName?.takeIf { it.isNotBlank() }
                ?: item.link.linkData.target
        is ListItemContent.NoteDocumentItem -> item.document.name.ifBlank { "Document" }
        is ListItemContent.ChecklistItem -> item.checklist.name ?: "Checklist"
        else -> "Attachment"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val icon = when (item) {
            is ListItemContent.LinkItem -> Icons.Outlined.Link
            is ListItemContent.NoteDocumentItem -> Icons.Outlined.Description
            is ListItemContent.ChecklistItem -> Icons.Outlined.Checklist
            else -> Icons.Default.Attachment
        }
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.features.contexts.toggled_features.backlog.BacklogListScreen
import com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.projectrealization.ProjectDashboardView
import com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.views.AttachmentsView
import com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.views.InboxView

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
            BacklogListScreen(
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
                        is ListItemContent.GoalItem -> viewModel.itemActionHandler.toggleGoalCompletedWithState(
                            item.goal,
                            isChecked
                        )

                        is ListItemContent.SublistItem -> viewModel.onSubprojectCompletedChanged(
                            item.project,
                            isChecked
                        )

                        else -> {}
                    }
                },
                onDelete = { item -> viewModel.itemActionHandler.deleteItem(item) },
                onDeleteEverywhere = { item -> viewModel.onDeleteEverywhere(item) },
                onMoveToTop = { item -> viewModel.onMoveToTop(item) },
                onAddToDayPlan = { item -> viewModel.addItemToDailyPlan(item) },
                onStartTracking = { item -> viewModel.onStartTrackingRequest(item) },
                onShowGoalTransportMenu = { item ->
                    viewModel.itemActionHandler.onGoalTransportInitiated(
                        item
                    ) {}
                },
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
                onTabSelected = viewModel::onDashboardTabSelected,
                enableDashboard = uiState.enableDashboard,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
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
            DashboardOverview(
                modifier = modifier,
                project = goalList,
                attachments = attachments,
                onAttachmentClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                onShowProperties = onShowProjectProperties,
                enableAttachments = uiState.enableAttachments
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
    onShowProperties: () -> Unit,
    enableAttachments: Boolean
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Row with Title, Badge, and Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Project Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val statusText = project?.projectStatusText?.takeIf { it.isNotBlank() }
                    ?: project?.projectStatus
                if (!statusText.isNullOrBlank()){
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            IconButton(onClick = onShowProperties) {
                Icon(Icons.Default.Settings, contentDescription = "Project properties")
            }
        }

        // Attachments Section (Conditional and Horizontally Scrollable)
        if (enableAttachments) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Attachments",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (attachments.isEmpty()) {
                    Text(
                        text = "No attachments yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(attachments) { item ->
                            val (icon, title) = when (item) {
                                is ListItemContent.LinkItem ->
                                    Icons.Outlined.Link to (item.link.linkData.displayName?.takeIf { it.isNotBlank() }
                                        ?: item.link.linkData.target)
                                is ListItemContent.NoteDocumentItem -> Icons.Outlined.Description to item.document.name.ifBlank { "Document" }
                                is ListItemContent.ChecklistItem -> Icons.Outlined.Checklist to (item.checklist.name ?: "Checklist")
                                else -> Icons.Default.Attachment to "Attachment"
                            }
                            Card(
                                onClick = { onAttachmentClick(item) },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
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

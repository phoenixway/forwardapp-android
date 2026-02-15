package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.attachments.AttachmentsView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.BacklogListScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.direction.DirectionView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox.InboxView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ProjectDashboardView
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ContextManagementTab
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState

import java.util.Locale

private const val TAG = "BACKLOG_UI_DEBUG"

private fun ContextViewMode.displayName(): String {
    return this.name.lowercase(Locale.ROOT)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        .replace("_", " ")
}

@Composable
fun GoalDetailContent(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    uiState: ContextUiState,
    currentViewMode: ContextViewMode,
    listState: LazyListState,
    inboxListState: LazyListState,
    onEditLog: (ContextLog) -> Unit,
    onDeleteLog: (ContextLog) -> Unit,
    onSaveArtifact: (String, String) -> Unit,
    onEditArtifact: (ContextArtifact) -> Unit,
    onRemindersClick: (BacklogItemContent) -> Unit,
    onShowProjectProperties: () -> Unit,
    onSwitchView: (ContextViewMode) -> Unit,
    onLinkDirectionRequest: (String) -> Unit,
    onUnlinkDirectionRequest: (String) -> Unit,
    onOpenLinkedDirectionContext: (String) -> Unit,
    linkedContextNames: Map<String, String>,
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val attachmentItems by viewModel.attachmentItems.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val goalList by viewModel.project.collectAsStateWithLifecycle()
    val projectLogs = uiState.logs
    val projectArtifact by viewModel.contextArtifact.collectAsStateWithLifecycle()
    val isSelectionModeActive = uiState.isSelectionModeActive
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()

    when (currentViewMode) {
        ContextViewMode.BACKLOG -> {
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
                onLongClick = { item -> viewModel.toggleSelection(item.backlogItem.id) },
                onCheckedChange = { item, isChecked ->
                    when (item) {
                        is BacklogItemContent.GoalItem ->
                            viewModel.itemActionHandler.toggleGoalCompletedWithState(
                                item.goal,
                                isChecked,
                            )

                        is BacklogItemContent.ContextLinkItem ->
                            viewModel.onSubprojectCompletedChanged(
                                item.project,
                                isChecked,
                            )

                        else -> {}
                    }
                },
                onDelete = { item -> viewModel.itemActionHandler.deleteItem(item) },
                onDeleteEverywhere = { item -> viewModel.onDeleteEverywhere(item) },
                onAddToDayPlan = { item -> viewModel.addItemToDailyPlan(item) },
                onStartTracking = { item -> viewModel.onStartTrackingRequest(item) },
                onShowGoalTransportMenu = { item ->
                    viewModel.itemActionHandler.onGoalTransportInitiated(item)
                },
                onRelatedLinkClick = viewModel.itemActionHandler::onRelatedLinkClick,
                onRemindersClick = onRemindersClick,
                onCopyContent = viewModel.itemActionHandler::copyContentRequest,
                onResetSwipe = viewModel::resetSwipeStatesExcept,
            )
        }
        ContextViewMode.INBOX -> {
            InboxView(
                modifier = modifier,
                viewModel = viewModel,
                inboxRecords = inboxRecords,
                listState = inboxListState,
                highlightedRecordId = uiState.inboxRecordToHighlight,
                navigationManager = viewModel.enhancedNavigationManager,
            )
        }
        ContextViewMode.ADVANCED -> {
            ProjectDashboardView(
                modifier = modifier,
                project = goalList,
                projectLogs = projectLogs,
                contextArtifact = projectArtifact,
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onStatusUpdate = viewModel::onProjectStatusUpdate,
                contextTimeMetrics = uiState.contextTimeMetrics,
                onRecalculateTime = viewModel::onRecalculateTime,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
                onSaveArtifact = { content -> viewModel.onSaveArtifact(content) }, // Явно вказуємо один параметр                onEditArtifact = onEditArtifact,
                selectedTab = uiState.currentTab,
                onTabSelected = viewModel::onDashboardTabSelected,
                enableDashboard = uiState.enableDashboard,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
                onEditArtifact = {/*TODO*/},
            )
        }
        ContextViewMode.ATTACHMENTS -> {
            AttachmentsView(
                modifier = modifier,
                viewModel = viewModel,
                attachmentItems = attachmentItems,
            )
        }
        ContextViewMode.DIRECTION -> {
            DirectionView(
                items = uiState.directionItems,
                modifier = modifier,
                onAddItem = viewModel::addDirectionItem,
                onEditItem = viewModel::updateDirectionItemText,
                onDeleteItem = viewModel::deleteDirectionItem,
                onMove = viewModel::onMoveDirectionItem,
                onLinkRequest = onLinkDirectionRequest,
                onUnlinkRequest = onUnlinkDirectionRequest,
                onOpenLinkedContext = onOpenLinkedDirectionContext,
                onCopyItem = viewModel::copyDirectionItem,
                onCutItem = viewModel::cutDirectionItem,
                linkedContextNames = linkedContextNames,
            )
        }
        ContextViewMode.DASHBOARD -> {
            DashboardOverview(
                modifier = modifier,
                project = goalList,
                attachments = attachmentItems,
                onAttachmentClick = { item -> viewModel.itemActionHandler.onItemClick(item) },
                onShowProperties = onShowProjectProperties,
                currentViewMode = currentViewMode,
                onSwitchView = onSwitchView,
                enableDashboard = uiState.enableDashboard,
                enableAttachments = uiState.enableAttachments,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
            )
        }
        ContextViewMode.LOG -> {
            ProjectDashboardView(
                modifier = modifier,
                project = goalList,
                projectLogs = projectLogs,
                contextArtifact = projectArtifact,
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onStatusUpdate = viewModel::onProjectStatusUpdate,
                contextTimeMetrics = uiState.contextTimeMetrics,
                onRecalculateTime = viewModel::onRecalculateTime,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
                onSaveArtifact = { content -> viewModel.onSaveArtifact(content) },
                onEditArtifact = {/* TODO */},
                selectedTab = ContextManagementTab.Log,
                onTabSelected = viewModel::onDashboardTabSelected,
                enableDashboard = uiState.enableDashboard,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
            )
        }
        ContextViewMode.ARTIFACT -> {
            ProjectDashboardView(
                modifier = modifier,
                project = goalList,
                projectLogs = projectLogs,
                contextArtifact = projectArtifact,
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onStatusUpdate = viewModel::onProjectStatusUpdate,
                contextTimeMetrics = uiState.contextTimeMetrics,
                onRecalculateTime = viewModel::onRecalculateTime,
                onEditLog = onEditLog,
                onDeleteLog = onDeleteLog,
                onSaveArtifact = { content -> viewModel.onSaveArtifact(content) },
                onEditArtifact = {/* TODO */},
                selectedTab = ContextManagementTab.Artifact,
                onTabSelected = viewModel::onDashboardTabSelected,
                enableDashboard = uiState.enableDashboard,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
            )
        }
        ContextViewMode.NOTES, ContextViewMode.VET_CASE -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${currentViewMode.displayName()} View - Coming Soon!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DashboardOverview(
    modifier: Modifier = Modifier,
    project: Context?,
    attachments: List<BacklogItemContent>,
    onAttachmentClick: (BacklogItemContent) -> Unit,
    onShowProperties: () -> Unit,
    currentViewMode: ContextViewMode,
    onSwitchView: (ContextViewMode) -> Unit,
    enableDashboard: Boolean,
    enableAttachments: Boolean,
    enableLog: Boolean,
    enableArtifact: Boolean,
) {
    val activeViews =
        buildList {
            add(DashboardViewItem(ContextViewMode.BACKLOG, "Беклог", Icons.AutoMirrored.Outlined.List))
            add(DashboardViewItem(ContextViewMode.INBOX, "Інбокс", Icons.Outlined.Inbox))
            add(DashboardViewItem(ContextViewMode.DIRECTION, "Напрямок", Icons.Outlined.AccountTree))
            if (enableAttachments) add(DashboardViewItem(ContextViewMode.ATTACHMENTS, "Вкладення", Icons.Default.Attachment))
            if (enableDashboard) add(DashboardViewItem(ContextViewMode.DASHBOARD, "Дашборд", Icons.Default.Dashboard))
            if (enableLog) add(DashboardViewItem(ContextViewMode.LOG, "Лог", Icons.Outlined.History))
            if (enableArtifact) add(DashboardViewItem(ContextViewMode.ARTIFACT, "Артефакт", Icons.Outlined.Inventory2))
        }

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
                val statusText =
                    project?.contextStatusText?.takeIf { it.isNotBlank() }
                        ?: project?.contextStatus
                if (!statusText.isNullOrBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            IconButton(onClick = onShowProperties) {
                Icon(Icons.Default.Settings, contentDescription = "Project properties")
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Активні види",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activeViews.forEach { viewItem ->
                        FilterChip(
                            selected = currentViewMode == viewItem.mode,
                            onClick = { onSwitchView(viewItem.mode) },
                            label = {
                                Text(
                                    text = viewItem.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = viewItem.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        // Attachments Section (Conditional and Horizontally Scrollable)
        if (enableAttachments) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Attachments",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
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
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(attachments) { item ->
                            val (icon, title) =
                                when (item) {
                                    is BacklogItemContent.LinkItem ->
                                        Icons.Outlined.Link to (
                                            item.link.linkData.displayName?.takeIf { it.isNotBlank() }
                                                ?: item.link.linkData.target
                                        )
                                    is BacklogItemContent.NoteDocumentItem -> Icons.Outlined.Description to item.document.name.ifBlank { "Document" }
                                    is BacklogItemContent.ChecklistItem -> Icons.Outlined.Checklist to (item.checklist.name ?: "Checklist")
                                    else -> Icons.Default.Attachment to "Attachment"
                                }
                            Card(
                                onClick = { onAttachmentClick(item) },
                                modifier =
                                    Modifier
                                        .width(120.dp)
                                        .height(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    ),
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
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

private data class DashboardViewItem(
    val mode: ContextViewMode,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun AttachmentRowSummary(
    item: BacklogItemContent,
    onClick: () -> Unit,
) {
    val title =
        when (item) {
            is BacklogItemContent.LinkItem ->
                item.link.linkData.displayName?.takeIf { it.isNotBlank() }
                    ?: item.link.linkData.target
            is BacklogItemContent.NoteDocumentItem -> item.document.name.ifBlank { "Document" }
            is BacklogItemContent.ChecklistItem -> item.checklist.name ?: "Checklist"
            else -> "Attachment"
        }
    Row(
        modifier =
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val icon =
            when (item) {
                is BacklogItemContent.LinkItem -> Icons.Outlined.Link
                is BacklogItemContent.NoteDocumentItem -> Icons.Outlined.Description
                is BacklogItemContent.ChecklistItem -> Icons.Outlined.Checklist
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

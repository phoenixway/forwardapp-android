package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckFabDefaults
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.TacticalMissionList
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.TacticalMissionListCallbacks
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.TacticalMissionListLookups
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.TacticalMissionSelectionState
import com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.TacticalScopeLinksSheet
import com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.dialogs.TacticalAddObsidianDialog
import com.romankozak.forwardappmobile.features.missions.presentation.scopelinks.dialogs.TacticalAddUrlDialog
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.orderToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val LINK_PICKER_OPEN_DELAY_MS = 160L

private data class TacticalManagementUiState(
    val editingMission: TacticalMission?,
    val actionMenuMission: TacticalMission?,
    val activeLinkPickerTab: LinkPickerTab?,
    val pendingCreateAction: PickerCreateAction?,
    val showAddUrlDialog: Boolean,
    val showAddObsidianDialog: Boolean,
    val selectedMissionIds: Set<Long>,
    val statusMenuExpanded: Boolean,
    val isFabMenuExpanded: Boolean,
    val isActivitySlotsSheetVisible: Boolean,
    val isActivitySlotPickerVisible: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalManagementScreen(
    onLinkedProjectClick: (String) -> Unit = {},
    onLinkedAttachmentClick: (AttachmentOption) -> Unit = {},
    viewModel: TacticalMissionViewModel = hiltViewModel(),
    showFabMenu: Boolean = true,
) {
    TacticalManagementRoute(
        onLinkedProjectClick = onLinkedProjectClick,
        onLinkedAttachmentClick = onLinkedAttachmentClick,
        viewModel = viewModel,
        showFabMenu = showFabMenu,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TacticalManagementRoute(
    onLinkedProjectClick: (String) -> Unit,
    onLinkedAttachmentClick: (AttachmentOption) -> Unit,
    viewModel: TacticalMissionViewModel,
    showFabMenu: Boolean,
) {
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val visibleMissions by viewModel.visibleMissions.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val missionStreams by viewModel.missionStreams.collectAsStateWithLifecycle()
    val selectedMissionStreamId by viewModel.selectedMissionStreamId.collectAsStateWithLifecycle()
    val missionStreamCounts by viewModel.missionStreamCounts.collectAsStateWithLifecycle()
    val activitySlotContexts by viewModel.activitySlotContexts.collectAsStateWithLifecycle()
    val selectedActivitySlotContextId by viewModel.selectedActivitySlotContextId.collectAsStateWithLifecycle()
    val selectedPlanningContextId by viewModel.selectedPlanningContextId.collectAsStateWithLifecycle()
    val planningBacklogItems by viewModel.planningBacklogItems.collectAsStateWithLifecycle()
    val attachmentOptions by viewModel.attachmentOptions.collectAsStateWithLifecycle()
    val projectOptions by viewModel.projectOptions.collectAsStateWithLifecycle()
    val boardLinkedProjectIds by viewModel.boardLinkedProjectIds.collectAsStateWithLifecycle()
    val boardLinkedAttachmentIds by viewModel.boardLinkedAttachmentIds.collectAsStateWithLifecycle()
    val connectionsOrder by viewModel.connectionsOrder.collectAsStateWithLifecycle()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsStateWithLifecycle()
    val missionReminderTimes by viewModel.missionReminderTimes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val showAddDialog by viewModel.isAddMissionDialogOpen.collectAsStateWithLifecycle()
    val pendingScrollToMissionId by viewModel.pendingScrollToMissionId.collectAsStateWithLifecycle()
    val isMissionStreamsSheetVisible by viewModel.isMissionStreamsSheetVisible.collectAsStateWithLifecycle()
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) }
    var actionMenuMission by remember { mutableStateOf<TacticalMission?>(null) }
    var slotPickerMission by remember { mutableStateOf<TacticalMission?>(null) }
    var streamPickerMission by remember { mutableStateOf<TacticalMission?>(null) }
    var activeLinkPickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddObsidianDialog by remember { mutableStateOf(false) }
    var selectedMissionIds by remember { mutableStateOf(setOf<Long>()) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isActivitySlotsSheetVisible by remember { mutableStateOf(false) }
    var isActivitySlotPickerVisible by remember { mutableStateOf(false) }
    val missionListState = rememberLazyListState()
    val selectionMode = selectedMissionIds.isNotEmpty()
    val canPasteAsMissions by viewModel.canPasteAsMissions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(visibleMissions) {
        val existingIds = visibleMissions.map { it.id }.toSet()
        selectedMissionIds = selectedMissionIds.filter { it in existingIds }.toSet()
    }

    LaunchedEffect(pendingScrollToMissionId, visibleMissions) {
        val targetId = pendingScrollToMissionId ?: return@LaunchedEffect
        val targetIndex = visibleMissions.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            missionListState.animateScrollToItem(targetIndex)
            viewModel.consumePendingScrollToMission()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val uiState =
        TacticalManagementUiState(
            editingMission = editingMission,
            actionMenuMission = actionMenuMission,
            activeLinkPickerTab = activeLinkPickerTab,
            pendingCreateAction = pendingCreateAction,
            showAddUrlDialog = showAddUrlDialog,
            showAddObsidianDialog = showAddObsidianDialog,
            selectedMissionIds = selectedMissionIds,
            statusMenuExpanded = statusMenuExpanded,
            isFabMenuExpanded = isFabMenuExpanded,
            isActivitySlotsSheetVisible = isActivitySlotsSheetVisible,
            isActivitySlotPickerVisible = isActivitySlotPickerVisible,
        )

    TacticalManagementContent(
        missions = visibleMissions,
        allMissions = missions,
        attachmentOptions = attachmentOptions,
        projectOptions = projectOptions,
        selectedMode = selectedMode,
        missionStreams = missionStreams,
        selectedMissionStreamId = selectedMissionStreamId,
        missionStreamCounts = missionStreamCounts,
        activitySlotContexts = activitySlotContexts,
        selectedActivitySlotContextId = selectedActivitySlotContextId,
        selectedPlanningContextId = selectedPlanningContextId,
        planningBacklogItems = planningBacklogItems,
        uiState = uiState,
        selectionMode = selectionMode,
        missionListState = missionListState,
        canPasteAsMissions = canPasteAsMissions,
        snackbarHostState = snackbarHostState,
        showFabMenu = showFabMenu,
        onSelectedMissionIdsChange = { selectedMissionIds = it },
        onStatusMenuExpandedChange = { statusMenuExpanded = it },
        onFabMenuExpandedChange = { isFabMenuExpanded = it },
        onEditingMissionChange = { editingMission = it },
        onActionMenuMissionChange = { actionMenuMission = it },
        onLinkedProjectClick = onLinkedProjectClick,
        onLinkedAttachmentClick = onLinkedAttachmentClick,
        onPasteMissions = viewModel::pasteClipboardAsMissions,
        onOpenAddMission = viewModel::openAddMissionDialog,
        onToggleScopeLinksSheet = viewModel::toggleScopeLinksSheet,
        onOpenActivitySlotsSheet = { isActivitySlotsSheetVisible = true },
        onOpenMissionStreamsSheet = viewModel::openMissionStreamsSheet,
        onModeSelected = viewModel::selectMode,
        onMissionStreamSelected = viewModel::selectMissionStream,
        onActivitySlotSelected = viewModel::selectActivitySlot,
        onPlanningContextSelected = viewModel::selectPlanningContext,
        onTakeBacklogItem = viewModel::createMissionFromBacklogItem,
        onMissionStatusUpdate = { mission, status ->
            viewModel.updateMission(mission.copy(status = status))
        },
        onDeleteSelectedMissions = { ids ->
            ids.forEach(viewModel::deleteMission)
        },
        onCopySelectedMissions = viewModel::copyMissionsToEntityClipboard,
        onCutSelectedMissions = viewModel::cutMissionsToEntityClipboard,
        onMissionToggle = { mission -> viewModel.toggleMissionCompleted(mission) },
        onMissionsReordered = viewModel::reorderVisibleMissions,
    )

    actionMenuMission?.let { mission ->
        MissionActionSheet(
            mission = mission,
            onDismiss = { actionMenuMission = null },
            onEdit = {
                editingMission = mission
                actionMenuMission = null
            },
            onToggleCompleted = {
                viewModel.updateMission(
                    mission.copy(
                        status =
                            if (mission.status == MissionStatus.COMPLETED) {
                                MissionStatus.ACTIVE
                            } else {
                                MissionStatus.COMPLETED
                            },
                    ),
                )
                actionMenuMission = null
            },
            onAddToToday = {
                viewModel.addMissionToTodayPlan(mission)
                actionMenuMission = null
            },
            onAddToArc = {
                viewModel.addMissionToCurrentArc(mission)
                actionMenuMission = null
            },
            missionStreams = missionStreams,
            selectedMissionStreamId = selectedMissionStreamId,
            onAssignMissionStream = { streamId ->
                viewModel.assignMissionToStream(mission, streamId)
                actionMenuMission = null
            },
            onOpenStreamPicker = {
                streamPickerMission = mission
                actionMenuMission = null
            },
            activitySlotContexts = activitySlotContexts,
            selectedActivitySlotContextId = selectedActivitySlotContextId,
            onAssignActivitySlot = { slotId ->
                viewModel.assignMissionToActivitySlot(mission, slotId)
                actionMenuMission = null
            },
            onOpenSlotPicker = {
                slotPickerMission = mission
                actionMenuMission = null
            },
            onPostpone = {
                viewModel.updateMission(mission.copy(status = MissionStatus.INACTIVE))
                actionMenuMission = null
            },
            onContinue = {
                val oneWeekMs = 7L * 24L * 60L * 60L * 1000L
                viewModel.updateMission(
                    mission.copy(
                        deadline = System.currentTimeMillis() + oneWeekMs,
                    ),
                )
                actionMenuMission = null
            },
            onCopyMission = {
                viewModel.copyMissionToEntityClipboard(mission)
                actionMenuMission = null
            },
            onCutMission = {
                viewModel.cutMissionToEntityClipboard(mission)
                actionMenuMission = null
            },
            onDeleteMission = {
                viewModel.deleteMission(mission.id)
                actionMenuMission = null
            },
        )
    }

    streamPickerMission?.let { mission ->
        MissionStreamPickerSheet(
            mission = mission,
            missionStreams = missionStreams,
            onDismiss = { streamPickerMission = null },
            onAssignMissionStream = { streamId ->
                viewModel.assignMissionToStream(mission, streamId)
                streamPickerMission = null
            },
        )
    }

    slotPickerMission?.let { mission ->
        MissionSlotPickerSheet(
            mission = mission,
            activitySlotContexts = activitySlotContexts,
            onDismiss = { slotPickerMission = null },
            onAssignActivitySlot = { slotId ->
                viewModel.assignMissionToActivitySlot(mission, slotId)
                slotPickerMission = null
            },
        )
    }

    val availableProjectIds = projectOptions.map { it.id }.toSet()
    val availableAttachmentIds = attachmentOptions.map { it.id }.toSet()
    val validBoardLinkedProjectIds = boardLinkedProjectIds.filter { it in availableProjectIds }
    val validBoardLinkedAttachmentIds = boardLinkedAttachmentIds.filter { it in availableAttachmentIds }

    TacticalManagementOverlays(
        attachmentOptions = attachmentOptions,
        projectOptions = projectOptions,
        activitySlotContexts = activitySlotContexts,
        boardLinkedProjectIds = boardLinkedProjectIds,
        boardLinkedAttachmentIds = boardLinkedAttachmentIds,
        connectionsOrder = connectionsOrder,
        isScopeLinksSheetVisible = isScopeLinksSheetVisible,
        missionReminderTimes = missionReminderTimes,
        showAddDialog = showAddDialog,
        uiState = uiState,
        onLinkedProjectClick = onLinkedProjectClick,
        onLinkedAttachmentClick = onLinkedAttachmentClick,
        onEditingMissionChange = { editingMission = it },
        onActiveLinkPickerTabChange = { activeLinkPickerTab = it },
        onPendingCreateActionChange = { pendingCreateAction = it },
        onShowAddUrlDialogChange = { showAddUrlDialog = it },
        onShowAddObsidianDialogChange = { showAddObsidianDialog = it },
        onDismissScopeLinksSheet = viewModel::dismissScopeLinksSheet,
        onRemoveBoardProjectLink = viewModel::removeBoardProjectLink,
        onRemoveBoardAttachmentLink = viewModel::removeBoardAttachmentLink,
        onUpdateConnectionsOrder = { reordered ->
            viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
        },
        onAddMission = { title, description, deadline, status, projects, attachments ->
            viewModel.addMission(title, description, deadline, status, projects, attachments)
            viewModel.dismissAddMissionDialog()
        },
        onDismissAddMissionDialog = viewModel::dismissAddMissionDialog,
        onSetMissionReminder = viewModel::setMissionReminder,
        onClearMissionReminder = viewModel::clearMissionReminder,
        onUpdateMission = viewModel::updateMission,
        onCreateRootContext = viewModel::createRootContextForPicker,
        onCreateDocument = viewModel::createBoardDocumentForPicker,
        onAddBoardProjectLink = viewModel::addBoardProjectLink,
        onAddBoardAttachmentLink = viewModel::addBoardAttachmentLink,
        onAddBoardUrlLink = viewModel::addBoardUrlLink,
        onAddBoardObsidianLink = viewModel::addBoardObsidianLink,
        onAddActivitySlot = viewModel::addActivitySlot,
        onRemoveActivitySlot = viewModel::removeActivitySlot,
        missionStreams = missionStreams,
        missionStreamCounts = missionStreamCounts,
        isMissionStreamsSheetVisible = isMissionStreamsSheetVisible,
        onAddMissionStream = viewModel::addMissionStream,
        onUpdateMissionStream = viewModel::updateMissionStream,
        onArchiveMissionStream = viewModel::archiveMissionStream,
        onReorderMissionStreams = viewModel::reorderMissionStreams,
        onMissionStreamsSheetVisibleChange = { visible ->
            if (!visible) viewModel.dismissMissionStreamsSheet()
        },
        onActivitySlotsSheetVisibleChange = { isActivitySlotsSheetVisible = it },
        onActivitySlotPickerVisibleChange = { isActivitySlotPickerVisible = it },
        scope = scope,
    )
}

@Composable
private fun TacticalManagementContent(
    missions: List<TacticalMission>,
    allMissions: List<TacticalMission>,
    attachmentOptions: List<AttachmentOption>,
    projectOptions: List<ProjectOption>,
    selectedMode: TacticsWorkspaceMode,
    missionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    missionStreamCounts: Map<String, Int>,
    activitySlotContexts: List<Context>,
    selectedActivitySlotContextId: String?,
    selectedPlanningContextId: String?,
    planningBacklogItems: List<TacticalPlanBacklogItem>,
    uiState: TacticalManagementUiState,
    selectionMode: Boolean,
    missionListState: androidx.compose.foundation.lazy.LazyListState,
    canPasteAsMissions: Boolean,
    snackbarHostState: SnackbarHostState,
    showFabMenu: Boolean,
    onSelectedMissionIdsChange: (Set<Long>) -> Unit,
    onStatusMenuExpandedChange: (Boolean) -> Unit,
    onFabMenuExpandedChange: (Boolean) -> Unit,
    onEditingMissionChange: (TacticalMission?) -> Unit,
    onActionMenuMissionChange: (TacticalMission?) -> Unit,
    onLinkedProjectClick: (String) -> Unit,
    onLinkedAttachmentClick: (AttachmentOption) -> Unit,
    onPasteMissions: () -> Unit,
    onOpenAddMission: () -> Unit,
    onToggleScopeLinksSheet: () -> Unit,
    onOpenActivitySlotsSheet: () -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onMissionStreamSelected: (String) -> Unit,
    onActivitySlotSelected: (String?) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onTakeBacklogItem: (TacticalPlanBacklogItem) -> Unit,
    onMissionStatusUpdate: (TacticalMission, MissionStatus) -> Unit,
    onDeleteSelectedMissions: (Set<Long>) -> Unit,
    onCopySelectedMissions: (Set<Long>) -> Unit,
    onCutSelectedMissions: (Set<Long>) -> Unit,
    onMissionToggle: (TacticalMission) -> Unit,
    onMissionsReordered: (List<TacticalMission>) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectionMode) {
                SelectionToolbar(
                    selectedMissionIds = uiState.selectedMissionIds,
                    missions = missions,
                    statusMenuExpanded = uiState.statusMenuExpanded,
                    onStatusMenuExpandedChange = onStatusMenuExpandedChange,
                    onSelectedMissionIdsChange = onSelectedMissionIdsChange,
                    onMissionStatusUpdate = onMissionStatusUpdate,
                    onCopySelectedMissions = onCopySelectedMissions,
                    onCutSelectedMissions = onCutSelectedMissions,
                    onDeleteSelectedMissions = onDeleteSelectedMissions,
                )
            }

            if (selectedMode == TacticsWorkspaceMode.PLAN) {
                TacticsPlanningList(
                    planningBacklogItems = planningBacklogItems,
                    selectedPlanningContextId = selectedPlanningContextId,
                    selectedMissionStreamTitle =
                        missionStreams.firstOrNull { it.id == selectedMissionStreamId }?.title,
                    onTakeBacklogItem = onTakeBacklogItem,
                    modifier = Modifier.weight(1f),
                )
            } else if (missions.isEmpty()) {
                TacticsEmptyState(
                    selectedMode = selectedMode,
                    selectedMissionStreamId = selectedMissionStreamId,
                    missionStreams = missionStreams,
                    totalWeekMissions = allMissions.size,
                    modifier = Modifier.weight(1f),
                )
            } else {
                TacticalMissionList(
                    missions = missions,
                    lookups =
                        TacticalMissionListLookups(
                            projectOptions = projectOptions,
                            attachmentOptions = attachmentOptions,
                            missionStreamTitleById =
                                if (selectedMode == TacticsWorkspaceMode.ALL) {
                                    missionStreams.associate { it.id to it.title }
                                } else {
                                    emptyMap()
                                },
                        ),
                    selectionState =
                        TacticalMissionSelectionState(
                            selectedMissionIds = uiState.selectedMissionIds,
                            selectionMode = selectionMode,
                        ),
                    callbacks =
                        TacticalMissionListCallbacks(
                            onMissionToggled = onMissionToggle,
                            onMissionSelectionToggle = { mission ->
                                onSelectedMissionIdsChange(
                                    toggleMissionSelection(uiState.selectedMissionIds, mission.id),
                                )
                            },
                            onMissionClick = { mission ->
                                if (!selectionMode) {
                                    onEditingMissionChange(mission)
                                }
                            },
                            onMissionLongPress = { mission ->
                                onSelectedMissionIdsChange(
                                    if (mission.id in uiState.selectedMissionIds) {
                                        uiState.selectedMissionIds
                                    } else {
                                        uiState.selectedMissionIds + mission.id
                                    },
                                )
                            },
                            onMissionMoreClick = onActionMenuMissionChange,
                            onLinkedContextClick = onLinkedProjectClick,
                            onLinkedAttachmentClick = { attachmentId ->
                                onLinkedAttachmentClick(resolveAttachmentOption(attachmentOptions, attachmentId))
                            },
                            onMissionsReordered = onMissionsReordered,
                        ),
                    listState = missionListState,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (showFabMenu) {
            TacticalFabMenu(
                expanded = uiState.isFabMenuExpanded,
                canPasteAsMissions = canPasteAsMissions,
                selectedMode = selectedMode,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = CommandDeckFabDefaults.BottomPadding),
                onExpandedChange = onFabMenuExpandedChange,
                onPasteMissions = onPasteMissions,
                onOpenAddMission = onOpenAddMission,
                onModeSelected = onModeSelected,
                onOpenActivitySlotsSheet = onOpenActivitySlotsSheet,
                onOpenMissionStreamsSheet = onOpenMissionStreamsSheet,
                onToggleScopeLinksSheet = onToggleScopeLinksSheet,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
        )
    }
}

@Composable
private fun TacticsPlanningList(
    planningBacklogItems: List<TacticalPlanBacklogItem>,
    selectedPlanningContextId: String?,
    selectedMissionStreamTitle: String?,
    onTakeBacklogItem: (TacticalPlanBacklogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedPlanningContextId == null) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Вибери джерело беклогу для планування тижня",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    if (planningBacklogItems.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "У цьому беклозі немає елементів",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "target-stream") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Взяті елементи підуть у потік: ${selectedMissionStreamTitle ?: "General"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        items(planningBacklogItems, key = { it.item.id }) { planItem ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = planItem.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 3,
                        )
                        if (!planItem.description.isNullOrBlank()) {
                            Text(
                                text = planItem.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                        if (planItem.alreadyInWeek) {
                            Text(
                                text = "Вже в тижні",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    TextButton(
                        enabled = !planItem.alreadyInWeek,
                        onClick = { onTakeBacklogItem(planItem) },
                    ) {
                        Text("Взяти")
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticsEmptyState(
    selectedMode: TacticsWorkspaceMode,
    selectedMissionStreamId: String,
    missionStreams: List<MissionStream>,
    totalWeekMissions: Int,
    modifier: Modifier = Modifier,
) {
    val streamName = missionStreams.firstOrNull { it.id == selectedMissionStreamId }?.title
    val text =
        when (selectedMode) {
            TacticsWorkspaceMode.STREAMS ->
                if (streamName == null) {
                    "Немає місій у цьому потоці"
                } else {
                    "Немає місій у потоці: $streamName"
                }
            TacticsWorkspaceMode.ALL -> "Немає місій цього тижня"
            TacticsWorkspaceMode.PLAN -> "Вибери беклог для планування"
        }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selectedMode == TacticsWorkspaceMode.STREAMS && totalWeekMissions > 0) {
                Text(
                    text = "Перемкнись на “Усі”, щоб побачити місії в інших потоках",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SelectionToolbar(
    selectedMissionIds: Set<Long>,
    missions: List<TacticalMission>,
    statusMenuExpanded: Boolean,
    onStatusMenuExpandedChange: (Boolean) -> Unit,
    onSelectedMissionIdsChange: (Set<Long>) -> Unit,
    onMissionStatusUpdate: (TacticalMission, MissionStatus) -> Unit,
    onCopySelectedMissions: (Set<Long>) -> Unit,
    onCutSelectedMissions: (Set<Long>) -> Unit,
    onDeleteSelectedMissions: (Set<Long>) -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Вибрано: ${selectedMissionIds.size}",
                style = MaterialTheme.typography.labelLarge,
            )

            SelectionStatusMenu(
                expanded = statusMenuExpanded,
                onExpandedChange = onStatusMenuExpandedChange,
                onStatusSelected = { status ->
                    missions
                        .filter { it.id in selectedMissionIds }
                        .forEach { mission -> onMissionStatusUpdate(mission, status) }
                    onSelectedMissionIdsChange(emptySet())
                    onStatusMenuExpandedChange(false)
                },
            )

            OutlinedButton(
                onClick = {
                    onCopySelectedMissions(selectedMissionIds)
                    onSelectedMissionIdsChange(emptySet())
                },
            ) {
                Text("Копіювати")
            }

            OutlinedButton(
                onClick = {
                    onCutSelectedMissions(selectedMissionIds)
                    onSelectedMissionIdsChange(emptySet())
                },
            ) {
                Text("Вирізати")
            }

            OutlinedButton(
                onClick = {
                    missions
                        .filter { it.id in selectedMissionIds }
                        .forEach { mission -> onMissionStatusUpdate(mission, MissionStatus.COMPLETED) }
                    onSelectedMissionIdsChange(emptySet())
                },
            ) {
                Text("Виконані")
            }

            OutlinedButton(
                onClick = {
                    missions
                        .filter { it.id in selectedMissionIds }
                        .forEach { mission -> onMissionStatusUpdate(mission, MissionStatus.ACTIVE) }
                    onSelectedMissionIdsChange(emptySet())
                },
            ) {
                Text("Невиконані")
            }

            Button(
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                onClick = {
                    onDeleteSelectedMissions(selectedMissionIds)
                    onSelectedMissionIdsChange(emptySet())
                },
            ) {
                Text("Видалити", color = MaterialTheme.colorScheme.onErrorContainer)
            }

            TextButton(onClick = { onSelectedMissionIdsChange(emptySet()) }) {
                Text("Скасувати")
            }
        }
    }
}

@Composable
private fun SelectionStatusMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onStatusSelected: (MissionStatus) -> Unit,
) {
    Box {
        FilledTonalButton(onClick = { onExpandedChange(true) }) {
            Text("Змінити статус")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            listOf(
                MissionStatus.ACTIVE to "Активна",
                MissionStatus.INACTIVE to "Неактивна",
                MissionStatus.PAUSED to "На паузі",
            ).forEach { (status, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onStatusSelected(status) },
                )
            }
        }
    }
}

@Composable
private fun TacticalFabMenu(
    expanded: Boolean,
    canPasteAsMissions: Boolean,
    selectedMode: TacticsWorkspaceMode,
    modifier: Modifier = Modifier,
    onExpandedChange: (Boolean) -> Unit,
    onPasteMissions: () -> Unit,
    onOpenAddMission: () -> Unit,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onOpenActivitySlotsSheet: () -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
    onToggleScopeLinksSheet: () -> Unit,
) {
    Box(modifier = modifier) {
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(Icons.Default.Menu, contentDescription = "Меню дій тактик")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier =
                Modifier.background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            DropdownMenuItem(
                text = { Text("Вставити з буфера") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                enabled = canPasteAsMissions,
                onClick = {
                    onExpandedChange(false)
                    onPasteMissions()
                },
            )
            DropdownMenuItem(
                text = { Text("Додати місію") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onOpenAddMission()
                },
            )
            DropdownMenuItem(
                text = { Text("Потоки") },
                leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                enabled = selectedMode != TacticsWorkspaceMode.STREAMS,
                onClick = {
                    onExpandedChange(false)
                    onModeSelected(TacticsWorkspaceMode.STREAMS)
                },
            )
            DropdownMenuItem(
                text = { Text("Усі місії") },
                leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                enabled = selectedMode != TacticsWorkspaceMode.ALL,
                onClick = {
                    onExpandedChange(false)
                    onModeSelected(TacticsWorkspaceMode.ALL)
                },
            )
            DropdownMenuItem(
                text = { Text("План тижня") },
                leadingIcon = { Icon(Icons.Outlined.Today, contentDescription = null) },
                enabled = selectedMode != TacticsWorkspaceMode.PLAN,
                onClick = {
                    onExpandedChange(false)
                    onModeSelected(TacticsWorkspaceMode.PLAN)
                },
            )
            DropdownMenuItem(
                text = { Text("Керувати потоками") },
                leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onOpenMissionStreamsSheet()
                },
            )
            DropdownMenuItem(
                text = { Text("Режими активності") },
                leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onOpenActivitySlotsSheet()
                },
            )
            DropdownMenuItem(
                text = { Text("Показати зв'язки") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onToggleScopeLinksSheet()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun TacticalManagementOverlays(
    attachmentOptions: List<AttachmentOption>,
    projectOptions: List<ProjectOption>,
    activitySlotContexts: List<Context>,
    boardLinkedProjectIds: List<String>,
    boardLinkedAttachmentIds: List<String>,
    connectionsOrder: List<String>,
    isScopeLinksSheetVisible: Boolean,
    missionReminderTimes: Map<Long, Long>,
    showAddDialog: Boolean,
    uiState: TacticalManagementUiState,
    onLinkedProjectClick: (String) -> Unit,
    onLinkedAttachmentClick: (AttachmentOption) -> Unit,
    onEditingMissionChange: (TacticalMission?) -> Unit,
    onActiveLinkPickerTabChange: (LinkPickerTab?) -> Unit,
    onPendingCreateActionChange: (PickerCreateAction?) -> Unit,
    onShowAddUrlDialogChange: (Boolean) -> Unit,
    onShowAddObsidianDialogChange: (Boolean) -> Unit,
    onDismissScopeLinksSheet: () -> Unit,
    onRemoveBoardProjectLink: (String) -> Unit,
    onRemoveBoardAttachmentLink: (String) -> Unit,
    onUpdateConnectionsOrder: (List<com.romankozak.forwardappmobile.ui.components.ConnectionItemUi>) -> Unit,
    onAddMission: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
    onDismissAddMissionDialog: () -> Unit,
    onSetMissionReminder: (Long, Long) -> Unit,
    onClearMissionReminder: (Long) -> Unit,
    onUpdateMission: (Long, String, String?, Long, MissionStatus, List<String>, List<String>) -> Unit,
    onCreateRootContext: suspend (String) -> String?,
    onCreateDocument: suspend (NewDocumentDraft) -> String?,
    onAddBoardProjectLink: (String) -> Unit,
    onAddBoardAttachmentLink: (String) -> Unit,
    onAddBoardUrlLink: (String, String) -> Unit,
    onAddBoardObsidianLink: (String, String, String) -> Unit,
    onAddActivitySlot: (String) -> Unit,
    onRemoveActivitySlot: (String) -> Unit,
    missionStreams: List<MissionStream>,
    missionStreamCounts: Map<String, Int>,
    isMissionStreamsSheetVisible: Boolean,
    onAddMissionStream: (String) -> Unit,
    onUpdateMissionStream: (MissionStream, String, String?, Int?) -> Unit,
    onArchiveMissionStream: (MissionStream) -> Unit,
    onReorderMissionStreams: (List<MissionStream>) -> Unit,
    onMissionStreamsSheetVisibleChange: (Boolean) -> Unit,
    onActivitySlotsSheetVisibleChange: (Boolean) -> Unit,
    onActivitySlotPickerVisibleChange: (Boolean) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val availableProjectIds = projectOptions.map { it.id }.toSet()
    val availableAttachmentIds = attachmentOptions.map { it.id }.toSet()
    val validBoardLinkedProjectIds = boardLinkedProjectIds.filter { it in availableProjectIds }
    val validBoardLinkedAttachmentIds = boardLinkedAttachmentIds.filter { it in availableAttachmentIds }

    TacticalScopeLinksSheet(
        isVisible = isScopeLinksSheetVisible,
        projectOptions = projectOptions,
        attachmentOptions = attachmentOptions,
        linkedProjectIds = boardLinkedProjectIds,
        linkedAttachmentIds = boardLinkedAttachmentIds,
        onDismiss = onDismissScopeLinksSheet,
        onAddContextClick = {
            onDismissScopeLinksSheet()
            onPendingCreateActionChange(null)
            scope.launch {
                delay(LINK_PICKER_OPEN_DELAY_MS)
                onActiveLinkPickerTabChange(LinkPickerTab.CONTEXTS)
            }
        },
        onAddAttachmentClick = {
            onDismissScopeLinksSheet()
            onPendingCreateActionChange(null)
            scope.launch {
                delay(LINK_PICKER_OPEN_DELAY_MS)
                onActiveLinkPickerTabChange(LinkPickerTab.ATTACHMENTS)
            }
        },
        onAddExternalClick = { onShowAddUrlDialogChange(true) },
        onAddObsidianClick = { onShowAddObsidianDialogChange(true) },
        onCreateConnectionClick = { type ->
            onDismissScopeLinksSheet()
            onPendingCreateActionChange(type.toPickerCreateAction())
            scope.launch {
                delay(LINK_PICKER_OPEN_DELAY_MS)
                onActiveLinkPickerTabChange(
                    if (type == CreateConnectionType.CONTEXT) {
                        LinkPickerTab.CONTEXTS
                    } else {
                        LinkPickerTab.ATTACHMENTS
                    },
                )
            }
        },
        onContextClick = onLinkedProjectClick,
        onAttachmentClick = { attachmentId ->
            onLinkedAttachmentClick(resolveAttachmentOption(attachmentOptions, attachmentId))
        },
        onContextRemove = onRemoveBoardProjectLink,
        onAttachmentRemove = onRemoveBoardAttachmentLink,
        connectionOrder = connectionsOrder,
        onConnectionsReordered = onUpdateConnectionsOrder,
    )

    if (showAddDialog) {
        AddMissionDialog(
            attachmentOptions = attachmentOptions,
            onDismiss = onDismissAddMissionDialog,
            onConfirm = onAddMission,
        )
    }

    if (uiState.isActivitySlotsSheetVisible) {
        ActivitySlotsSheet(
            activitySlotContexts = activitySlotContexts,
            onDismiss = { onActivitySlotsSheetVisibleChange(false) },
            onAddSlotClick = { onActivitySlotPickerVisibleChange(true) },
            onRemoveSlot = onRemoveActivitySlot,
        )
    }

    if (isMissionStreamsSheetVisible) {
        MissionStreamsSheet(
            missionStreams = missionStreams,
            missionStreamCounts = missionStreamCounts,
            onDismiss = { onMissionStreamsSheetVisibleChange(false) },
            onAddStream = onAddMissionStream,
            onUpdateStream = onUpdateMissionStream,
            onArchiveStream = onArchiveMissionStream,
            onReorderStreams = onReorderMissionStreams,
        )
    }

    uiState.editingMission?.let { mission ->
        ModalBottomSheet(
            onDismissRequest = { onEditingMissionChange(null) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            MissionEditorScreen(
                mission = mission,
                attachmentOptions = attachmentOptions,
                projectOptions = projectOptions,
                reminderTime = missionReminderTimes[mission.id],
                onSetReminder = { reminderTime -> onSetMissionReminder(mission.id, reminderTime) },
                onClearReminder = { onClearMissionReminder(mission.id) },
                onDismiss = { onEditingMissionChange(null) },
                onConfirm = { title, desc, deadline, status, projects, attachments ->
                    onUpdateMission(
                        mission.id,
                        title,
                        desc,
                        deadline,
                        status,
                        projects,
                        attachments,
                    )
                    onEditingMissionChange(null)
                },
                onCreateRootContext = onCreateRootContext,
                onCreateDocument = onCreateDocument,
                sheetMode = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    uiState.activeLinkPickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions = projectOptions,
            attachmentOptions = attachmentOptions,
            preselectedContextIds = validBoardLinkedProjectIds.toSet(),
            preselectedAttachmentIds = validBoardLinkedAttachmentIds.toSet(),
            initialTab = initialTab,
            initialCreateAction = uiState.pendingCreateAction,
            onDismiss = {
                onActiveLinkPickerTabChange(null)
                onPendingCreateActionChange(null)
            },
            onContextSelected = { id ->
                onAddBoardProjectLink(id)
                onActiveLinkPickerTabChange(null)
                onPendingCreateActionChange(null)
            },
            onAttachmentSelected = { id ->
                onAddBoardAttachmentLink(id)
                onActiveLinkPickerTabChange(null)
                onPendingCreateActionChange(null)
            },
            onCreateRootContext = onCreateRootContext,
            onCreateDocument = onCreateDocument,
        )
    }

    if (uiState.isActivitySlotPickerVisible) {
        LinkedTargetsPickerDialog(
            contextOptions = projectOptions,
            attachmentOptions = emptyList(),
            preselectedContextIds = activitySlotContexts.map { it.id }.toSet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { onActivitySlotPickerVisibleChange(false) },
            onContextSelected = { id ->
                onAddActivitySlot(id)
                onActivitySlotPickerVisibleChange(false)
            },
            onAttachmentSelected = {},
            onCreateRootContext = onCreateRootContext,
            onCreateDocument = onCreateDocument,
        )
    }

    if (uiState.showAddUrlDialog) {
        TacticalAddUrlDialog(
            onDismiss = { onShowAddUrlDialogChange(false) },
            onConfirm = { url, name ->
                onAddBoardUrlLink(url, name)
                onShowAddUrlDialogChange(false)
            },
        )
    }

    if (uiState.showAddObsidianDialog) {
        TacticalAddObsidianDialog(
            onDismiss = { onShowAddObsidianDialogChange(false) },
            onConfirm = { noteName, displayName, vault ->
                onAddBoardObsidianLink(noteName, displayName, vault)
                onShowAddObsidianDialogChange(false)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitySlotsSheet(
    activitySlotContexts: List<Context>,
    onDismiss: () -> Unit,
    onAddSlotClick: () -> Unit,
    onRemoveSlot: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Режими активності",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Це контексти, які використовуються як режими виконання у тактичному просторі.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            if (activitySlotContexts.isEmpty()) {
                Text(
                    text = "Режимів активності ще немає",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                activitySlotContexts.forEach { context ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = context.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                        )
                        TextButton(onClick = { onRemoveSlot(context.id) }) {
                            Text("Прибрати")
                        }
                    }
                }
            }
            Button(
                onClick = onAddSlotClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Додати контекст як режим")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionStreamsSheet(
    missionStreams: List<MissionStream>,
    missionStreamCounts: Map<String, Int>,
    onDismiss: () -> Unit,
    onAddStream: (String) -> Unit,
    onUpdateStream: (MissionStream, String, String?, Int?) -> Unit,
    onArchiveStream: (MissionStream) -> Unit,
    onReorderStreams: (List<MissionStream>) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var newStreamTitle by remember { mutableStateOf("") }
    var editingStreamId by remember { mutableStateOf<String?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingBudgetText by remember { mutableStateOf("") }
    val defaultStreams = remember(missionStreams) { missionStreams.filter { it.isDefault } }
    val isBudgetOverLimit = missionStreams.sumOf { it.budgetPercent ?: 0 } > 100
    val reorderableStreams =
        remember {
            mutableStateListOf<MissionStream>()
        }
    LaunchedEffect(missionStreams) {
        reorderableStreams.clear()
        reorderableStreams.addAll(missionStreams.filterNot { it.isDefault })
    }
    val streamListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(streamListState) { from, to ->
            val fromStream = reorderableStreams.getOrNull(from.index) ?: return@rememberReorderableLazyListState
            reorderableStreams.removeAt(from.index)
            reorderableStreams.add(to.index, fromStream)
            onReorderStreams(defaultStreams + reorderableStreams)
        }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Потоки місій",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newStreamTitle,
                    onValueChange = { newStreamTitle = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Новий потік...") },
                )
                Button(
                    enabled = newStreamTitle.isNotBlank(),
                    onClick = {
                        onAddStream(newStreamTitle)
                        newStreamTitle = ""
                    },
                ) {
                    Text("Додати")
                }
            }
            HorizontalDivider()
            if (isBudgetOverLimit) {
                Text(
                    text = "Сума бюджетів потоків перевищує 100%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            defaultStreams.forEach { stream ->
                MissionStreamSheetRow(
                    stream = stream,
                    missionCount = missionStreamCounts[stream.id] ?: 0,
                    isBudgetOverLimit = isBudgetOverLimit,
                    editingStreamId = editingStreamId,
                    editingTitle = editingTitle,
                    editingBudgetText = editingBudgetText,
                    onEditingTitleChange = { editingTitle = it },
                    onEditingBudgetChange = { editingBudgetText = it },
                    onStartEdit = {
                        editingStreamId = stream.id
                        editingTitle = stream.title
                        editingBudgetText = stream.budgetPercent?.toString().orEmpty()
                    },
                    onSaveEdit = {
                        onUpdateStream(stream, editingTitle, stream.description, editingBudgetText.toBudgetPercentOrNull())
                        editingStreamId = null
                    },
                    onArchive = { onArchiveStream(stream) },
                    dragHandleModifier = Modifier,
                )
            }
            if (defaultStreams.isNotEmpty() && reorderableStreams.isNotEmpty()) {
                SubtleActionDivider()
            }
            LazyColumn(
                state = streamListState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(reorderableStreams, key = { _, stream -> stream.id }) { _, stream ->
                    ReorderableItem(reorderableState, key = stream.id) {
                        MissionStreamSheetRow(
                            stream = stream,
                            missionCount = missionStreamCounts[stream.id] ?: 0,
                            isBudgetOverLimit = isBudgetOverLimit,
                            editingStreamId = editingStreamId,
                            editingTitle = editingTitle,
                            editingBudgetText = editingBudgetText,
                            onEditingTitleChange = { editingTitle = it },
                            onEditingBudgetChange = { editingBudgetText = it },
                            onStartEdit = {
                                editingStreamId = stream.id
                                editingTitle = stream.title
                                editingBudgetText = stream.budgetPercent?.toString().orEmpty()
                            },
                            onSaveEdit = {
                                onUpdateStream(stream, editingTitle, stream.description, editingBudgetText.toBudgetPercentOrNull())
                                editingStreamId = null
                            },
                            onArchive = { onArchiveStream(stream) },
                            dragHandleModifier =
                                with(this@ReorderableItem) {
                                    Modifier.longPressDraggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                    )
                                },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MissionStreamSheetRow(
    stream: MissionStream,
    missionCount: Int,
    isBudgetOverLimit: Boolean,
    editingStreamId: String?,
    editingTitle: String,
    editingBudgetText: String,
    onEditingTitleChange: (String) -> Unit,
    onEditingBudgetChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onArchive: () -> Unit,
    dragHandleModifier: Modifier,
) {
    if (editingStreamId == stream.id) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = editingTitle,
                    onValueChange = onEditingTitleChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Назва") },
                )
                OutlinedTextField(
                    value = editingBudgetText,
                    onValueChange = onEditingBudgetChange,
                    modifier = Modifier.width(104.dp),
                    singleLine = true,
                    label = { Text("%") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = editingBudgetText.isNotBlank() && editingBudgetText.toBudgetPercentOrNull() == null,
                )
            }
            TextButton(
                enabled = editingTitle.isNotBlank() &&
                    (editingBudgetText.isBlank() || editingBudgetText.toBudgetPercentOrNull() != null),
                onClick = onSaveEdit,
            ) {
                Text("Зберегти")
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Змінити порядок",
                    modifier = dragHandleModifier.size(22.dp),
                    tint =
                        if (stream.isDefault) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stream.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        MissionStreamCountBadge(missionCount)
                        stream.budgetPercent?.let { percent ->
                            MissionStreamBudgetBadge(
                                budgetPercent = percent,
                                isOverLimit = isBudgetOverLimit,
                            )
                        }
                    }
                    if (stream.isDefault) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onStartEdit,
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Редагувати потік",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    enabled = !stream.isDefault,
                    onClick = onArchive,
                ) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = "Архівувати потік",
                        tint =
                            if (stream.isDefault) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionStreamCountBadge(missionCount: Int) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = missionCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MissionStreamBudgetBadge(
    budgetPercent: Int,
    isOverLimit: Boolean,
) {
    val color =
        if (isOverLimit) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.secondary
        }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            text = "$budgetPercent%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun String.toBudgetPercentOrNull(): Int? {
    if (isBlank()) return null
    return trim().toIntOrNull()?.takeIf { it in 0..100 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionActionSheet(
    mission: TacticalMission,
    missionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    activitySlotContexts: List<Context>,
    selectedActivitySlotContextId: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onAddToToday: () -> Unit,
    onAddToArc: () -> Unit,
    onAssignMissionStream: (String) -> Unit,
    onOpenStreamPicker: () -> Unit,
    onAssignActivitySlot: (String?) -> Unit,
    onOpenSlotPicker: () -> Unit,
    onPostpone: () -> Unit,
    onContinue: () -> Unit,
    onCopyMission: () -> Unit,
    onCutMission: () -> Unit,
    onDeleteMission: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = mission.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            HorizontalDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Edit,
                text = "Редагувати",
                onClick = onEdit,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.CheckCircle,
                text =
                    if (mission.status == MissionStatus.COMPLETED) {
                        "Позначити невиконаною"
                    } else {
                        "Позначити виконаною"
                    },
                onClick = onToggleCompleted,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Today,
                text = "Додати місію в план дня",
                onClick = onAddToToday,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.AccountTree,
                text = "Додати як ArcQuest",
                onClick = onAddToArc,
            )
            SubtleActionDivider()
            if (mission.normalizedMissionStreamId() != selectedMissionStreamId) {
                MissionActionSheetItem(
                    icon = Icons.Outlined.AccountTree,
                    text = "Перемістити в поточний потік",
                    onClick = { onAssignMissionStream(selectedMissionStreamId) },
                )
                SubtleActionDivider()
            }
            MissionActionSheetItem(
                icon = Icons.Outlined.ChevronRight,
                text = "Перемістити в потік",
                enabled = missionStreams.isNotEmpty(),
                onClick = onOpenStreamPicker,
            )
            SubtleActionDivider()
            if (
                selectedActivitySlotContextId != null &&
                mission.activitySlotContextId != selectedActivitySlotContextId
            ) {
                MissionActionSheetItem(
                    icon = Icons.Outlined.AccountTree,
                    text = "Перемістити в поточний режим активності",
                    onClick = { onAssignActivitySlot(selectedActivitySlotContextId) },
                )
                SubtleActionDivider()
            }
            MissionActionSheetItem(
                icon = Icons.Outlined.ChevronRight,
                text =
                    if (mission.activitySlotContextId == null) {
                        "Додати режим активності"
                    } else {
                        "Змінити режим активності"
                    },
                enabled = activitySlotContexts.isNotEmpty() || mission.activitySlotContextId != null,
                onClick = onOpenSlotPicker,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Today,
                text = "Postpone",
                onClick = onPostpone,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Today,
                text = "Продовжити",
                onClick = onContinue,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.ContentCopy,
                text = "Копіювати для вставки",
                onClick = onCopyMission,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.ContentCut,
                text = "Вирізати для вставки",
                onClick = onCutMission,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.DeleteOutline,
                text = "Видалити",
                textColor = MaterialTheme.colorScheme.error,
                onClick = onDeleteMission,
            )
            SubtleActionDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Close,
                text = "Скасувати",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionSlotPickerSheet(
    mission: TacticalMission,
    activitySlotContexts: List<Context>,
    onDismiss: () -> Unit,
    onAssignActivitySlot: (String?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Режим активності",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = mission.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            HorizontalDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Close,
                text =
                    if (mission.activitySlotContextId == null) {
                        "Без режиму · поточно"
                    } else {
                        "Без режиму"
                    },
                enabled = mission.activitySlotContextId != null,
                onClick = { onAssignActivitySlot(null) },
            )
            SubtleActionDivider()
            if (activitySlotContexts.isEmpty()) {
                Text(
                    text = "Режими активності ще не додані.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                activitySlotContexts.forEach { slot ->
                    MissionActionSheetItem(
                        icon = Icons.Outlined.AccountTree,
                        text =
                            if (mission.activitySlotContextId == slot.id) {
                                "${slot.name} · поточно"
                            } else {
                                slot.name
                            },
                        enabled = mission.activitySlotContextId != slot.id,
                        onClick = { onAssignActivitySlot(slot.id) },
                    )
                    SubtleActionDivider()
                }
            }
            MissionActionSheetItem(
                icon = Icons.Outlined.Close,
                text = "Скасувати",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionStreamPickerSheet(
    mission: TacticalMission,
    missionStreams: List<MissionStream>,
    onDismiss: () -> Unit,
    onAssignMissionStream: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Перемістити в потік",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = mission.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            HorizontalDivider()
            missionStreams.forEach { stream ->
                MissionActionSheetItem(
                    icon = Icons.Outlined.AccountTree,
                    text =
                        if (mission.normalizedMissionStreamId() == stream.id) {
                            "${stream.title} · поточний"
                        } else {
                            stream.title
                        },
                    enabled = mission.normalizedMissionStreamId() != stream.id,
                    onClick = { onAssignMissionStream(stream.id) },
                )
                SubtleActionDivider()
            }
            MissionActionSheetItem(
                icon = Icons.Outlined.Close,
                text = "Скасувати",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun toggleMissionSelection(
    selectedMissionIds: Set<Long>,
    missionId: Long,
): Set<Long> =
    if (missionId in selectedMissionIds) {
        selectedMissionIds - missionId
    } else {
        selectedMissionIds + missionId
    }

private fun resolveAttachmentOption(
    attachmentOptions: List<AttachmentOption>,
    attachmentId: String,
): AttachmentOption =
    attachmentOptions.firstOrNull { it.id == attachmentId }
        ?: AttachmentOption(id = attachmentId, name = attachmentId)

@Composable
private fun MissionActionSheetItem(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val resolvedTextColor =
        if (enabled) {
            textColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = resolvedTextColor.copy(alpha = 0.9f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = resolvedTextColor,
        )
    }
}

@Composable
private fun SubtleActionDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
    )
}

private fun CreateConnectionType.toPickerCreateAction(): PickerCreateAction =
    when (this) {
        CreateConnectionType.CONTEXT -> PickerCreateAction.CONTEXT
        CreateConnectionType.NOTE_DOCUMENT -> PickerCreateAction.NOTE
        CreateConnectionType.JOURNAL_DOCUMENT -> PickerCreateAction.JOURNAL_DOCUMENT
        CreateConnectionType.MUSIC_NOTE -> PickerCreateAction.MUSIC_NOTE
        CreateConnectionType.CHECKLIST -> PickerCreateAction.CHECKLIST
        CreateConnectionType.SCRIPT -> PickerCreateAction.NOTE
        CreateConnectionType.EXTERNAL_LINK -> PickerCreateAction.WEB_LINK
        CreateConnectionType.OBSIDIAN_NOTE -> PickerCreateAction.OBSIDIAN
    }

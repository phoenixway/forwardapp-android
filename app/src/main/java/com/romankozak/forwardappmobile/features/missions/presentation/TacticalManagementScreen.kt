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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.Context
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
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) }
    var actionMenuMission by remember { mutableStateOf<TacticalMission?>(null) }
    var slotPickerMission by remember { mutableStateOf<TacticalMission?>(null) }
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
        onModeSelected = viewModel::selectMode,
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
        missions = missions,
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
        onActionMenuMissionChange = { actionMenuMission = it },
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
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
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
            TacticsWorkspaceHeader(
                selectedMode = selectedMode,
                activitySlotContexts = activitySlotContexts,
                selectedActivitySlotContextId = selectedActivitySlotContextId,
                selectedPlanningContextId = selectedPlanningContextId,
                projectOptions = projectOptions,
                onModeSelected = onModeSelected,
                onActivitySlotSelected = onActivitySlotSelected,
                onPlanningContextSelected = onPlanningContextSelected,
                onOpenActivitySlotsSheet = onOpenActivitySlotsSheet,
            )
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
                    onTakeBacklogItem = onTakeBacklogItem,
                    modifier = Modifier.weight(1f),
                )
            } else if (missions.isEmpty()) {
                TacticsEmptyState(
                    selectedMode = selectedMode,
                    selectedSlotId = selectedActivitySlotContextId,
                    activitySlotContexts = activitySlotContexts,
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
                                onSelectedMissionIdsChange(toggleMissionSelection(uiState.selectedMissionIds, mission.id))
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
private fun TacticsWorkspaceHeader(
    selectedMode: TacticsWorkspaceMode,
    activitySlotContexts: List<Context>,
    selectedActivitySlotContextId: String?,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onActivitySlotSelected: (String?) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onOpenActivitySlotsSheet: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TacticsModeChip("По слотах", selectedMode == TacticsWorkspaceMode.SLOTS) {
                onModeSelected(TacticsWorkspaceMode.SLOTS)
            }
            TacticsModeChip("Усі", selectedMode == TacticsWorkspaceMode.ALL) {
                onModeSelected(TacticsWorkspaceMode.ALL)
            }
            TacticsModeChip("Слоти", selected = false) {
                onOpenActivitySlotsSheet()
            }
        }

        when (selectedMode) {
            TacticsWorkspaceMode.SLOTS ->
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedActivitySlotContextId == null,
                        onClick = { onActivitySlotSelected(null) },
                        label = { Text("Без слота") },
                    )
                    activitySlotContexts.forEach { context ->
                        FilterChip(
                            selected = selectedActivitySlotContextId == context.id,
                            onClick = { onActivitySlotSelected(context.id) },
                            label = { Text(context.name, maxLines = 1) },
                        )
                    }
                }
            TacticsWorkspaceMode.PLAN ->
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    activitySlotContexts.forEach { context ->
                        FilterChip(
                            selected = selectedPlanningContextId == context.id,
                            onClick = { onPlanningContextSelected(context.id) },
                            label = { Text(context.name, maxLines = 1) },
                        )
                    }
                    projectOptions.forEach { option ->
                        FilterChip(
                            selected = selectedPlanningContextId == option.id,
                            onClick = { onPlanningContextSelected(option.id) },
                            label = { Text(option.name, maxLines = 1) },
                        )
                    }
                }
            TacticsWorkspaceMode.ALL -> Unit
        }
    }
}

@Composable
private fun TacticsModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun TacticsPlanningList(
    planningBacklogItems: List<TacticalPlanBacklogItem>,
    selectedPlanningContextId: String?,
    onTakeBacklogItem: (TacticalPlanBacklogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedPlanningContextId == null) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Вибери слот або проєкт для планування тижня",
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
    selectedSlotId: String?,
    activitySlotContexts: List<Context>,
    totalWeekMissions: Int,
    modifier: Modifier = Modifier,
) {
    val slotName = activitySlotContexts.firstOrNull { it.id == selectedSlotId }?.name
    val text =
        when (selectedMode) {
            TacticsWorkspaceMode.SLOTS ->
                if (slotName == null) {
                    "Немає місій без слота"
                } else {
                    "Немає місій у слоті: $slotName"
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
            if (selectedMode == TacticsWorkspaceMode.SLOTS && totalWeekMissions > 0) {
                Text(
                    text = "Перемкнись на “Усі”, щоб побачити місії в інших слотах",
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
                text = { Text("Слоти") },
                leadingIcon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                enabled = selectedMode != TacticsWorkspaceMode.SLOTS,
                onClick = {
                    onExpandedChange(false)
                    onModeSelected(TacticsWorkspaceMode.SLOTS)
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
                text = { Text("Керувати слотами") },
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
    missions: List<TacticalMission>,
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
    onActionMenuMissionChange: (TacticalMission?) -> Unit,
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
                text = "Слоти активності",
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
                    text = "Слотів ще немає",
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
                Text("Додати контекст як слот")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionActionSheet(
    mission: TacticalMission,
    activitySlotContexts: List<Context>,
    selectedActivitySlotContextId: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onAddToToday: () -> Unit,
    onAddToArc: () -> Unit,
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
            if (selectedActivitySlotContextId != null && mission.activitySlotContextId != selectedActivitySlotContextId) {
                MissionActionSheetItem(
                    icon = Icons.Outlined.AccountTree,
                    text = "Перемістити в поточний слот",
                    onClick = { onAssignActivitySlot(selectedActivitySlotContextId) },
                )
                SubtleActionDivider()
            }
            MissionActionSheetItem(
                icon = Icons.Outlined.ChevronRight,
                text =
                    if (mission.activitySlotContextId == null) {
                        "Додати в слот"
                    } else {
                        "Змінити слот"
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
                text = "Додати в слот",
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
                        "Без слота · поточно"
                    } else {
                        "Без слота"
                    },
                enabled = mission.activitySlotContextId != null,
                onClick = { onAssignActivitySlot(null) },
            )
            SubtleActionDivider()
            if (activitySlotContexts.isEmpty()) {
                Text(
                    text = "Слоти активності ще не додані.",
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

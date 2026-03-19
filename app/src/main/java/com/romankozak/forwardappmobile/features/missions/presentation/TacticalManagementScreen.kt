package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalManagementScreen(
    onLinkedProjectClick: (String) -> Unit = {},
    onLinkedAttachmentClick: (AttachmentOption) -> Unit = {},
    viewModel: TacticalMissionViewModel = hiltViewModel(),
    showFabMenu: Boolean = true,
) {
    val missions by viewModel.missions.collectAsState()
    val attachmentOptions by viewModel.attachmentOptions.collectAsState()
    val projectOptions by viewModel.projectOptions.collectAsState()
    val boardLinkedProjectIds by viewModel.boardLinkedProjectIds.collectAsState()
    val boardLinkedAttachmentIds by viewModel.boardLinkedAttachmentIds.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    val scope = rememberCoroutineScope()
    val showAddDialog by viewModel.isAddMissionDialogOpen.collectAsState()
    var editingMission by remember { mutableStateOf<TacticalMission?>(null) }
    var actionMenuMission by remember { mutableStateOf<TacticalMission?>(null) }
    var activeLinkPickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddObsidianDialog by remember { mutableStateOf(false) }
    var selectedMissionIds by remember { mutableStateOf(setOf<Long>()) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val selectionMode = selectedMissionIds.isNotEmpty()

    LaunchedEffect(missions) {
        val existingIds = missions.map { it.id }.toSet()
        selectedMissionIds = selectedMissionIds.filter { it in existingIds }.toSet()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectionMode) {
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

                        Box {
                            FilledTonalButton(onClick = { statusMenuExpanded = true }) {
                                Text("Змінити статус")
                            }
                            DropdownMenu(
                                expanded = statusMenuExpanded,
                                onDismissRequest = { statusMenuExpanded = false },
                            ) {
                                listOf(
                                    MissionStatus.ACTIVE to "Активна",
                                    MissionStatus.INACTIVE to "Неактивна",
                                    MissionStatus.PAUSED to "На паузі",
                                ).forEach { (status, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            missions
                                                .filter { it.id in selectedMissionIds }
                                                .forEach { mission ->
                                                    viewModel.updateMission(mission.copy(status = status))
                                                }
                                            selectedMissionIds = emptySet()
                                            statusMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                missions
                                    .filter { it.id in selectedMissionIds }
                                    .forEach { mission ->
                                        viewModel.updateMission(mission.copy(status = MissionStatus.COMPLETED))
                                    }
                                selectedMissionIds = emptySet()
                            },
                        ) {
                            Text("Виконані")
                        }

                        OutlinedButton(
                            onClick = {
                                missions
                                    .filter { it.id in selectedMissionIds }
                                    .forEach { mission ->
                                        viewModel.updateMission(mission.copy(status = MissionStatus.ACTIVE))
                                    }
                                selectedMissionIds = emptySet()
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
                                selectedMissionIds.forEach { id ->
                                    viewModel.deleteMission(id)
                                }
                                selectedMissionIds = emptySet()
                            },
                        ) {
                            Text("Видалити", color = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        TextButton(onClick = { selectedMissionIds = emptySet() }) {
                            Text("Скасувати")
                        }
                    }
                }
            }

            TacticalMissionList(
                missions = missions,
                lookups =
                    TacticalMissionListLookups(
                        projectOptions = projectOptions,
                        attachmentOptions = attachmentOptions,
                    ),
                selectionState =
                    TacticalMissionSelectionState(
                        selectedMissionIds = selectedMissionIds,
                        selectionMode = selectionMode,
                    ),
                callbacks =
                    TacticalMissionListCallbacks(
                        onMissionToggled = { viewModel.toggleMissionCompleted(it) },
                        onMissionSelectionToggle = { mission ->
                            selectedMissionIds =
                                if (mission.id in selectedMissionIds) {
                                    selectedMissionIds - mission.id
                                } else {
                                    selectedMissionIds + mission.id
                                }
                        },
                        onMissionClick = { mission ->
                            if (!selectionMode) {
                                editingMission = mission
                            }
                        },
                        onMissionLongPress = { mission ->
                            selectedMissionIds =
                                if (mission.id in selectedMissionIds) {
                                    selectedMissionIds
                                } else {
                                    selectedMissionIds + mission.id
                                }
                        },
                        onMissionMoreClick = { mission -> actionMenuMission = mission },
                        onLinkedContextClick = onLinkedProjectClick,
                        onLinkedAttachmentClick = { attachmentId ->
                            val option =
                                attachmentOptions.firstOrNull { it.id == attachmentId }
                                    ?: AttachmentOption(id = attachmentId, name = attachmentId)
                            onLinkedAttachmentClick(option)
                        },
                        onMissionsReordered = viewModel::reorderMissions,
                    ),
                modifier = Modifier.weight(1f),
            )
        }

        if (showFabMenu) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = CommandDeckFabDefaults.BottomPadding),
            ) {
                FloatingActionButton(onClick = { isFabMenuExpanded = !isFabMenuExpanded }) {
                    Icon(Icons.Default.Menu, contentDescription = "Меню дій тактик")
                }
                DropdownMenu(
                    expanded = isFabMenuExpanded,
                    onDismissRequest = { isFabMenuExpanded = false },
                    modifier =
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                        ),
                ) {
                    DropdownMenuItem(
                        text = { Text("Додати місію") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            viewModel.openAddMissionDialog()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Показати зв'язки") },
                        leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            viewModel.toggleScopeLinksSheet()
                        },
                    )
                }
            }
        }
    }

    actionMenuMission?.let { mission ->
        ModalBottomSheet(
            onDismissRequest = { actionMenuMission = null },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
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
                    onClick = {
                        editingMission = mission
                        actionMenuMission = null
                    },
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
                    onClick = {
                        val nextStatus =
                            if (mission.status == MissionStatus.COMPLETED) {
                                MissionStatus.ACTIVE
                            } else {
                                MissionStatus.COMPLETED
                            }
                        viewModel.updateMission(mission.copy(status = nextStatus))
                        actionMenuMission = null
                    },
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.DeleteOutline,
                    text = "Видалити",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.deleteMission(mission.id)
                        actionMenuMission = null
                    },
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.Close,
                    text = "Скасувати",
                    onClick = { actionMenuMission = null },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

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
        onDismiss = viewModel::dismissScopeLinksSheet,
        onAddContextClick = {
            viewModel.dismissScopeLinksSheet()
            pendingCreateAction = null
            scope.launch {
                delay(LINK_PICKER_OPEN_DELAY_MS)
                activeLinkPickerTab = LinkPickerTab.CONTEXTS
            }
        },
        onAddAttachmentClick = {
            viewModel.dismissScopeLinksSheet()
            pendingCreateAction = null
            scope.launch {
                delay(LINK_PICKER_OPEN_DELAY_MS)
                activeLinkPickerTab = LinkPickerTab.ATTACHMENTS
            }
        },
        onAddExternalClick = { showAddUrlDialog = true },
        onAddObsidianClick = { showAddObsidianDialog = true },
        onCreateConnectionClick = { type ->
            viewModel.dismissScopeLinksSheet()
            pendingCreateAction = type.toPickerCreateAction()
            scope.launch {
                delay(LINK_PICKER_OPEN_DELAY_MS)
                activeLinkPickerTab =
                    if (type == CreateConnectionType.CONTEXT) {
                        LinkPickerTab.CONTEXTS
                    } else {
                        LinkPickerTab.ATTACHMENTS
                    }
            }
        },
        onContextClick = onLinkedProjectClick,
        onAttachmentClick = { attachmentId ->
            val option =
                attachmentOptions.firstOrNull { it.id == attachmentId }
                    ?: AttachmentOption(id = attachmentId, name = attachmentId)
            onLinkedAttachmentClick(option)
        },
        onContextRemove = viewModel::removeBoardProjectLink,
        onAttachmentRemove = viewModel::removeBoardAttachmentLink,
        connectionOrder = connectionsOrder,
        onConnectionsReordered = { reordered ->
            viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
        },
    )

    if (showAddDialog) {
        AddMissionDialog(
            attachmentOptions = attachmentOptions,
            onDismiss = viewModel::dismissAddMissionDialog,
            onConfirm = { title, description, deadline, status, projects, attachments ->
                viewModel.addMission(title, description, deadline, status, projects, attachments)
                viewModel.dismissAddMissionDialog()
            },
        )
    }

    editingMission?.let { mission ->
        Dialog(
            onDismissRequest = { editingMission = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                MissionEditorScreen(
                    mission = mission,
                    attachmentOptions = attachmentOptions,
                    projectOptions = projectOptions,
                    onDismiss = { editingMission = null },
                    onConfirm = { title, desc, deadline, status, projects, attachments ->
                        viewModel.updateMission(
                            mission.id,
                            title,
                            desc,
                            deadline,
                            status,
                            projects,
                            attachments,
                        )
                        editingMission = null
                    },
                    onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
                    onCreateDocument = { draft -> viewModel.createBoardDocumentForPicker(draft) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    activeLinkPickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions = projectOptions,
            attachmentOptions = attachmentOptions,
            preselectedContextIds = validBoardLinkedProjectIds.toSet(),
            preselectedAttachmentIds = validBoardLinkedAttachmentIds.toSet(),
            initialTab = initialTab,
            initialCreateAction = pendingCreateAction,
            onDismiss = {
                activeLinkPickerTab = null
                pendingCreateAction = null
            },
            onContextSelected = { id ->
                viewModel.addBoardProjectLink(id)
                activeLinkPickerTab = null
                pendingCreateAction = null
            },
            onAttachmentSelected = { id ->
                viewModel.addBoardAttachmentLink(id)
                activeLinkPickerTab = null
                pendingCreateAction = null
            },
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = { draft -> viewModel.createBoardDocumentForPicker(draft) },
        )
    }

    if (showAddUrlDialog) {
        TacticalAddUrlDialog(
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url, name ->
                viewModel.addBoardUrlLink(url, name)
                showAddUrlDialog = false
            },
        )
    }

    if (showAddObsidianDialog) {
        TacticalAddObsidianDialog(
            onDismiss = { showAddObsidianDialog = false },
            onConfirm = { noteName, displayName ->
                viewModel.addBoardObsidianLink(noteName, displayName)
                showAddObsidianDialog = false
            },
        )
    }
}

@Composable
private fun MissionActionSheetItem(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = textColor.copy(alpha = 0.9f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
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
        CreateConnectionType.MUSIC_NOTE -> PickerCreateAction.MUSIC_NOTE
        CreateConnectionType.CHECKLIST -> PickerCreateAction.CHECKLIST
        CreateConnectionType.SCRIPT -> PickerCreateAction.NOTE
        CreateConnectionType.EXTERNAL_LINK -> PickerCreateAction.WEB_LINK
        CreateConnectionType.OBSIDIAN_NOTE -> PickerCreateAction.OBSIDIAN
    }

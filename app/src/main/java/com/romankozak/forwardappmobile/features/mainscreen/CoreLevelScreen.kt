package com.romankozak.forwardappmobile.features.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
import com.romankozak.forwardappmobile.features.mainscreen.core.CoreScopeLinksSheet
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconEditorSheet
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconEditorState
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconCardUi
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconCardLinkUi
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconLevelStatusSheet
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconGroupUi
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconMultiSelectDialog
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconSelectableItem
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentChooserScreen
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.orderToken
import java.net.URLEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ATTACHMENT_ID_PREVIEW_LENGTH = 8
private const val PICKER_OPEN_DELAY_MILLIS = 160L

private enum class MainBeaconLinkActionTarget {
    CORE_SCOPE,
    EDITOR,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreLevelScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: CoreLevelViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val attachmentOptions by viewModel.attachmentOptions.collectAsState()
    val linkedAttachmentIds by viewModel.linkedAttachmentIds.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()
    val scope = rememberCoroutineScope()

    var showAttachmentChooser by remember { mutableStateOf(false) }
    var activeLinkPickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddObsidianDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var editingBeacon by remember { mutableStateOf<MainBeaconEditorState?>(null) }
    var editingGroup by remember { mutableStateOf<MainBeaconGroupUi?>(null) }
    var isCreatingGroup by remember { mutableStateOf(false) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showDocumentPicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var beaconPendingDeleteId by remember { mutableStateOf<String?>(null) }
    var groupPendingDeleteId by remember { mutableStateOf<String?>(null) }
    var editingLevelIndex by remember { mutableStateOf<Int?>(null) }
    var linkActionTarget by remember { mutableStateOf(MainBeaconLinkActionTarget.CORE_SCOPE) }
    val beaconListState = rememberLazyListState()
    val expandedBeaconIds = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(uiState.beacons) {
        val validIds = uiState.beacons.mapTo(mutableSetOf()) { it.id }
        expandedBeaconIds.keys.toList().forEach { beaconId ->
            if (beaconId !in validIds) expandedBeaconIds.remove(beaconId)
        }
    }

    val openTarget: (NavTarget, Boolean) -> Unit = { target, recordInHistory ->
        navigationManager.navigateOrFallback(
            navController = navController,
            target = target,
            recordInHistory = recordInHistory,
        )
    }

    val navigateToCoreChooser: () -> Unit = {
        val disabledIds = uiState.projects.joinToString(",") { it.id }.ifBlank { null }
        openTarget(
            NavTarget.ListChooser(
                title = "Додати контекст у ядро",
                disabledIds = disabledIds,
            ),
            false,
        )
    }

    LaunchedEffect(navController) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle
            ?.getStateFlow<String?>("list_chooser_result", null)
            ?.collect { result ->
                if (result != null) {
                    savedStateHandle["list_chooser_result"] = null
                    if (result != "root") {
                        viewModel.addCoreLink(result)
                    }
                }
            }
    }

    val availableAttachmentById = attachmentOptions.associateBy { it.id }
    val validAttachmentIds = linkedAttachmentIds.filter { it in availableAttachmentById.keys }
    val connectionItems =
        buildList {
            addAll(uiState.projects.map { ConnectionItemUi(it.id, it.name, ConnectionType.CONTEXT) })
            addAll(
                validAttachmentIds.map { id ->
                    val option = availableAttachmentById[id]
                    ConnectionItemUi(
                        id = id,
                        title = option?.name ?: "Вкладення ${id.take(ATTACHMENT_ID_PREVIEW_LENGTH)}",
                        type =
                            when {
                                option?.linkType == LinkType.URL -> ConnectionType.URL
                                option?.linkType == LinkType.OBSIDIAN -> ConnectionType.OBSIDIAN_NOTE
                                option?.attachmentType == "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
                                option?.attachmentType == "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
                                option?.attachmentType == "CHECKLIST" -> ConnectionType.CHECKLIST
                                option?.attachmentType == "SCRIPT" -> ConnectionType.SCRIPT
                                else -> ConnectionType.ATTACHMENT
                            },
                        vault = option?.vault,
                    )
                },
            )
        }

    val onConnectionClick: (ConnectionItemUi) -> Unit = { item ->
        if (item.type == ConnectionType.CONTEXT) {
            openTarget(NavTarget.ContextDetail(contextId = item.id), true)
        } else {
            val option = availableAttachmentById[item.id]
            when {
                option?.attachmentType == "NOTE_DOCUMENT" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.NoteDocument(id = option.entityId), false)
                option?.attachmentType == "JOURNAL_DOCUMENT" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.JournalDocument(id = option.entityId), false)
                option?.attachmentType == "MUSIC_NOTE" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.MusicNote(id = option.entityId), false)
                option?.attachmentType == "CHECKLIST" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.Checklist(id = option.entityId), false)
                option?.linkType == LinkType.CONTEXT && !option.target.isNullOrBlank() ->
                    openTarget(NavTarget.ContextDetail(contextId = option.target), true)
                (option?.linkType == LinkType.URL || option?.linkType == LinkType.OBSIDIAN) &&
                    !option.target.isNullOrBlank() -> {
                    val resolvedTarget =
                        buildExternalTarget(
                            option.linkType,
                            option.target,
                            option.vault,
                            obsidianVaultName,
                        )
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(resolvedTarget)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }
                }
                else -> {
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.AttachmentsLibrary,
                    ) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    val allContextOptions =
        remember(uiState.allProjects) {
            uiState.allProjects.map { contextItem ->
                MainBeaconSelectableItem(id = contextItem.id, label = contextItem.name)
            }
        }
    val allDocumentOptions =
        remember(attachmentOptions) {
            attachmentOptions.map { option ->
                MainBeaconSelectableItem(id = option.id, label = option.name)
            }
        }
    val allGroupOptions =
        remember(uiState.groups) {
            uiState.groups.map { group ->
                MainBeaconSelectableItem(id = group.id, label = group.title)
            }
        }
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                Box(modifier = Modifier.padding(bottom = CommandDeckFabDefaults.BottomPadding)) {
                    FloatingActionButton(onClick = { isFabMenuExpanded = !isFabMenuExpanded }) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню дій ядра")
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
                            text = { Text("Новий MainBeacon") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                isFabMenuExpanded = false
                                editingBeacon = viewModel.buildEditorState(null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Нова група") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                isFabMenuExpanded = false
                                isCreatingGroup = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Connections") },
                            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.toggleScopeLinksSheet()
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    if (uiState.beacons.isEmpty() && uiState.groups.isEmpty()) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "У core ще немає MainBeacon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Створи перший через FAB. Connections залишаються доступними окремо.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    } else {
                        LazyColumn(
                            state = beaconListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 120.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            uiState.groups.forEach { group ->
                                val groupBeacons = uiState.beacons.filter { group.id in it.groupIds }
                                item(key = "group-${group.id}") {
                                    MainBeaconGroupHeader(
                                        title = group.title,
                                        count = groupBeacons.size,
                                        onEditClick = { editingGroup = group },
                                    )
                                }
                                items(groupBeacons, key = { "${group.id}-${it.id}" }) { beacon ->
                                    MainBeaconCardFromUi(
                                        beacon = beacon,
                                        allProjects = uiState.allProjects,
                                        attachmentOptions = attachmentOptions,
                                        connectionItems = connectionItems,
                                        isExpanded = expandedBeaconIds[beacon.id] == true,
                                        onToggleExpanded = {
                                            expandedBeaconIds[beacon.id] = expandedBeaconIds[beacon.id] != true
                                        },
                                        onEditClick = { editingBeacon = viewModel.buildEditorState(beacon.id) },
                                        onContextClick = { contextId ->
                                            openTarget(NavTarget.ContextDetail(contextId = contextId), true)
                                        },
                                        onConnectionClick = onConnectionClick,
                                    )
                                }
                            }

                            val noGroupBeacons = uiState.beacons.filter { it.groupIds.isEmpty() }
                            if (noGroupBeacons.isNotEmpty()) {
                                item(key = "group-no-group") {
                                    MainBeaconGroupHeader(
                                        title = "No group",
                                        count = noGroupBeacons.size,
                                        onEditClick = null,
                                    )
                                }
                                items(noGroupBeacons, key = { "no-group-${it.id}" }) { beacon ->
                                    MainBeaconCardFromUi(
                                        beacon = beacon,
                                        allProjects = uiState.allProjects,
                                        attachmentOptions = attachmentOptions,
                                        connectionItems = connectionItems,
                                        isExpanded = expandedBeaconIds[beacon.id] == true,
                                        onToggleExpanded = {
                                            expandedBeaconIds[beacon.id] = expandedBeaconIds[beacon.id] != true
                                        },
                                        onEditClick = { editingBeacon = viewModel.buildEditorState(beacon.id) },
                                        onContextClick = { contextId ->
                                            openTarget(NavTarget.ContextDetail(contextId = contextId), true)
                                        },
                                        onConnectionClick = onConnectionClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingBeacon?.let { editor ->
        val editorConnectionItems =
            buildList {
                addAll(
                    editor.relatedContextIds.mapNotNull { id ->
                        uiState.allProjects.firstOrNull { it.id == id }?.let {
                            ConnectionItemUi(id = it.id, title = it.name, type = ConnectionType.CONTEXT)
                        }
                    },
                )
                addAll(
                    editor.relatedAttachmentIds.mapNotNull { id ->
                        attachmentOptions.firstOrNull { it.id == id }?.let { option ->
                            ConnectionItemUi(
                                id = option.id,
                                title = option.name,
                                type =
                                    when {
                                        option.linkType == LinkType.URL -> ConnectionType.URL
                                        option.linkType == LinkType.OBSIDIAN -> ConnectionType.OBSIDIAN_NOTE
                                        option.attachmentType == "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
                                        option.attachmentType == "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
                                        option.attachmentType == "CHECKLIST" -> ConnectionType.CHECKLIST
                                        option.attachmentType == "SCRIPT" -> ConnectionType.SCRIPT
                                        else -> ConnectionType.ATTACHMENT
                                    },
                                vault = option.vault,
                            )
                        }
                    },
                )
        }
        MainBeaconEditorSheet(
            state = editor,
            connectionItems = editorConnectionItems,
            groupItems =
                editor.groupIds.mapNotNull { groupId ->
                    uiState.groups.firstOrNull { it.id == groupId }?.let { group ->
                        MainBeaconCardLinkUi(id = group.id, title = group.title)
                    }
                },
            onDismiss = { editingBeacon = null },
            onStateChange = { editingBeacon = it },
            onEditGroups = { showGroupPicker = true },
            onConnectionClick = { item ->
                if (item.type == ConnectionType.CONTEXT) {
                    openTarget(NavTarget.ContextDetail(contextId = item.id), true)
                } else {
                    editorConnectionItems.firstOrNull { it.id == item.id }?.let(onConnectionClick)
                }
            },
            onConnectionRemove = { item ->
                editingBeacon =
                    if (item.type == ConnectionType.CONTEXT) {
                        editingBeacon?.copy(
                            relatedContextIds = editingBeacon?.relatedContextIds.orEmpty() - item.id,
                        )
                    } else {
                        editingBeacon?.copy(
                            relatedAttachmentIds =
                                editingBeacon?.relatedAttachmentIds.orEmpty() - item.id,
                        )
                    }
            },
            onAddConnection = { type ->
                linkActionTarget = MainBeaconLinkActionTarget.EDITOR
                when (type) {
                    AddConnectionType.CONTEXT -> showContextPicker = true
                    AddConnectionType.ATTACHMENT -> showDocumentPicker = true
                    AddConnectionType.EXTERNAL_LINK -> showAddUrlDialog = true
                    AddConnectionType.OBSIDIAN_NOTE -> showAddObsidianDialog = true
                }
            },
            onCreateConnection = { type ->
                linkActionTarget = MainBeaconLinkActionTarget.EDITOR
                pendingCreateAction = type.toPickerCreateAction()
                scope.launch {
                    delay(PICKER_OPEN_DELAY_MILLIS)
                    activeLinkPickerTab =
                        if (type == CreateConnectionType.CONTEXT) {
                            LinkPickerTab.CONTEXTS
                        } else {
                            LinkPickerTab.ATTACHMENTS
                        }
                }
            },
            onEditLevel = { index -> editingLevelIndex = index },
            onSave = {
                viewModel.saveBeacon(editor)
                editingBeacon = null
            },
            onDuplicate = {
                editingBeacon =
                    editor.copy(
                        id = null,
                        title = "${editor.title} copy".trim(),
                        createdAt = null,
                        updatedAt = null,
                        isNew = true,
                    )
                editingLevelIndex = null
            },
            onDelete =
                editor.id?.let { beaconId ->
                    {
                        beaconPendingDeleteId = beaconId
                    }
                },
        )
    }

    val activeLevelIndex = editingLevelIndex
    if (
        editingBeacon != null &&
        activeLevelIndex != null &&
        activeLevelIndex in editingBeacon!!.levelStatuses.indices
    ) {
        val levelState = editingBeacon!!.levelStatuses[activeLevelIndex]
        MainBeaconLevelStatusSheet(
            state = levelState,
            onDismiss = { editingLevelIndex = null },
            onStateChange = { updated ->
                editingBeacon =
                    editingBeacon?.copy(
                        levelStatuses =
                            editingBeacon!!.levelStatuses.toMutableList().apply {
                                this[activeLevelIndex] = updated
                            },
                    )
            },
            onSave = { editingLevelIndex = null },
        )
    }

    if (showContextPicker && editingBeacon != null) {
        MainBeaconMultiSelectDialog(
            title = "Пов’язані contexts",
            options = allContextOptions,
            selectedIds = editingBeacon?.relatedContextIds.orEmpty(),
            onDismiss = { showContextPicker = false },
            onConfirm = { selected ->
                editingBeacon = editingBeacon?.copy(relatedContextIds = selected)
                showContextPicker = false
            },
        )
    }

    if (showDocumentPicker && editingBeacon != null) {
        MainBeaconMultiSelectDialog(
            title = "Пов’язані documents",
            options = allDocumentOptions,
            selectedIds = editingBeacon?.relatedAttachmentIds.orEmpty(),
            onDismiss = { showDocumentPicker = false },
            onConfirm = { selected ->
                editingBeacon = editingBeacon?.copy(relatedAttachmentIds = selected)
                showDocumentPicker = false
            },
        )
    }

    if (showGroupPicker && editingBeacon != null) {
        MainBeaconMultiSelectDialog(
            title = "Groups",
            options = allGroupOptions,
            selectedIds = editingBeacon?.groupIds.orEmpty(),
            onDismiss = { showGroupPicker = false },
            onConfirm = { selected ->
                editingBeacon = editingBeacon?.copy(groupIds = selected)
                showGroupPicker = false
            },
        )
    }

    if (isCreatingGroup || editingGroup != null) {
        MainBeaconGroupEditorDialog(
            group = editingGroup,
            onDismiss = {
                isCreatingGroup = false
                editingGroup = null
            },
            onSave = { title, description ->
                editingGroup?.let { group ->
                    viewModel.updateBeaconGroup(group.id, title, description)
                } ?: viewModel.createBeaconGroup(title, description)
                isCreatingGroup = false
                editingGroup = null
            },
            onDelete =
                editingGroup?.let { group ->
                    {
                        groupPendingDeleteId = group.id
                        editingGroup = null
                        isCreatingGroup = false
                    }
                },
        )
    }

    beaconPendingDeleteId?.let { beaconId ->
        AlertDialog(
            onDismissRequest = { beaconPendingDeleteId = null },
            title = { Text("Видалити MainBeacon?") },
            text = { Text("Цю дію не буде скасовано.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBeacon(beaconId)
                        editingBeacon = null
                        beaconPendingDeleteId = null
                    },
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { beaconPendingDeleteId = null }) {
                    Text("Скасувати")
                }
            },
        )
    }

    groupPendingDeleteId?.let { groupId ->
        AlertDialog(
            onDismissRequest = { groupPendingDeleteId = null },
            title = { Text("Видалити групу?") },
            text = { Text("Орієнтири залишаться без цієї групи.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBeaconGroup(groupId)
                        editingGroup = null
                        isCreatingGroup = false
                        groupPendingDeleteId = null
                    },
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupPendingDeleteId = null }) {
                    Text("Скасувати")
                }
            },
        )
    }

    CoreScopeLinksSheet(
        isVisible = isScopeLinksSheetVisible,
        projectOptions =
            uiState.allProjects.map {
                ProjectOption(id = it.id, name = it.name, parentId = it.parentId)
            },
        attachmentOptions = attachmentOptions,
        linkedProjectIds = uiState.projects.map { it.id },
        linkedAttachmentIds = linkedAttachmentIds,
        onDismiss = viewModel::dismissScopeLinksSheet,
        onAddContextClick = {
            linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            viewModel.dismissScopeLinksSheet()
            navigateToCoreChooser()
        },
        onAddAttachmentClick = {
            linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            viewModel.dismissScopeLinksSheet()
            pendingCreateAction = null
            scope.launch {
                delay(PICKER_OPEN_DELAY_MILLIS)
                showAttachmentChooser = true
            }
        },
        onAddExternalClick = {
            linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            viewModel.dismissScopeLinksSheet()
            showAddUrlDialog = true
        },
        onAddObsidianClick = {
            linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            viewModel.dismissScopeLinksSheet()
            showAddObsidianDialog = true
        },
        onCreateConnectionClick = { type ->
            linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            viewModel.dismissScopeLinksSheet()
            pendingCreateAction = type.toPickerCreateAction()
            scope.launch {
                delay(PICKER_OPEN_DELAY_MILLIS)
                activeLinkPickerTab =
                    if (type == CreateConnectionType.CONTEXT) {
                        LinkPickerTab.CONTEXTS
                    } else {
                        LinkPickerTab.ATTACHMENTS
                    }
            }
        },
        onContextClick = { id -> openTarget(NavTarget.ContextDetail(contextId = id), true) },
        onAttachmentClick = { id ->
            connectionItems.firstOrNull { it.id == id }?.let(onConnectionClick)
        },
        onContextRemove = viewModel::removeCoreLink,
        onAttachmentRemove = viewModel::removeAttachmentLink,
        connectionOrder = connectionsOrder,
        onConnectionsReordered = { reordered ->
            viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
        },
    )

    if (showAttachmentChooser) {
        AttachmentChooserScreen(
            options =
                attachmentOptions.map {
                    AttachmentOption(id = it.id, name = it.name, linkType = it.linkType)
                },
            preselected = linkedAttachmentIds.toSet(),
            onDismiss = {
                showAttachmentChooser = false
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
            onConfirm = { selected ->
                if (linkActionTarget == MainBeaconLinkActionTarget.EDITOR) {
                    editingBeacon =
                        editingBeacon?.copy(
                            relatedAttachmentIds = (editingBeacon?.relatedAttachmentIds.orEmpty() + selected).toSet(),
                        )
                } else {
                    selected.forEach(viewModel::addAttachmentLink)
                }
                showAttachmentChooser = false
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
        )
    }

    activeLinkPickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions =
                uiState.allProjects.map {
                    ProjectOption(id = it.id, name = it.name, parentId = it.parentId)
                },
            attachmentOptions =
                attachmentOptions.map {
                    AttachmentOption(
                        id = it.id,
                        name = it.name,
                        linkType = it.linkType,
                        attachmentType = it.attachmentType,
                        entityId = it.entityId,
                        target = it.target,
                    )
                },
            preselectedContextIds = uiState.projects.map { it.id }.toSet(),
            preselectedAttachmentIds = linkedAttachmentIds.toSet(),
            initialTab = initialTab,
            initialCreateAction = pendingCreateAction,
            onDismiss = {
                activeLinkPickerTab = null
                pendingCreateAction = null
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
            onContextSelected = { id ->
                if (linkActionTarget == MainBeaconLinkActionTarget.EDITOR) {
                    editingBeacon =
                        editingBeacon?.copy(
                            relatedContextIds = editingBeacon?.relatedContextIds.orEmpty() + id,
                        )
                } else {
                    viewModel.addCoreLink(id)
                }
                activeLinkPickerTab = null
                pendingCreateAction = null
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
            onAttachmentSelected = { id ->
                if (linkActionTarget == MainBeaconLinkActionTarget.EDITOR) {
                    editingBeacon =
                        editingBeacon?.copy(
                            relatedAttachmentIds = editingBeacon?.relatedAttachmentIds.orEmpty() + id,
                        )
                } else {
                    viewModel.addAttachmentLink(id)
                }
                activeLinkPickerTab = null
                pendingCreateAction = null
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = { draft -> viewModel.createCoreDocumentForPicker(draft) },
        )
    }

    if (showAddUrlDialog) {
        AddWebLinkDialog(
            onDismiss = {
                showAddUrlDialog = false
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
            onConfirm = { url, name ->
                if (linkActionTarget == MainBeaconLinkActionTarget.EDITOR) {
                    scope.launch {
                        val attachmentId =
                            viewModel.createCoreDocumentForPicker(NewDocumentDraft.WebLink(url = url, name = name))
                        if (attachmentId != null) {
                            editingBeacon =
                                editingBeacon?.copy(
                                    relatedAttachmentIds = editingBeacon?.relatedAttachmentIds.orEmpty() + attachmentId,
                                )
                        }
                    }
                } else {
                    viewModel.addUrlLink(url, name)
                }
                showAddUrlDialog = false
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
        )
    }

    if (showAddObsidianDialog) {
        AddObsidianLinkDialog(
            onDismiss = {
                showAddObsidianDialog = false
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
            onConfirm = { noteName, displayName, vault ->
                if (linkActionTarget == MainBeaconLinkActionTarget.EDITOR) {
                    scope.launch {
                        val attachmentId =
                            viewModel.createCoreDocumentForPicker(
                                NewDocumentDraft.Obsidian(
                                    noteName = noteName,
                                    displayName = displayName,
                                    vault = vault.takeIf { it.isNotBlank() },
                                ),
                            )
                        if (attachmentId != null) {
                            editingBeacon =
                                editingBeacon?.copy(
                                    relatedAttachmentIds = editingBeacon?.relatedAttachmentIds.orEmpty() + attachmentId,
                                )
                        }
                    }
                } else {
                    viewModel.addObsidianLink(noteName, displayName, vault)
                }
                showAddObsidianDialog = false
                linkActionTarget = MainBeaconLinkActionTarget.CORE_SCOPE
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainBeaconGroupHeader(
    title: String,
    count: Int,
    onEditClick: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            onEditClick?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit group")
                }
            }
        }
    }
}

@Composable
private fun MainBeaconGroupEditorDialog(
    group: MainBeaconGroupUi?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(group) { mutableStateOf(group?.title.orEmpty()) }
    var description by remember(group) { mutableStateOf(group?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (group == null) "Нова група" else "Група") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.trim().isNotBlank(),
                onClick = { onSave(title, description.ifBlank { null }) },
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text("Видалити")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Скасувати")
                }
            }
        },
    )
}

@Composable
private fun MainBeaconCardFromUi(
    beacon: MainBeaconCardUi,
    allProjects: List<com.romankozak.forwardappmobile.core.data.models.entities.Context>,
    attachmentOptions: List<com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption>,
    connectionItems: List<ConnectionItemUi>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEditClick: () -> Unit,
    onContextClick: (String) -> Unit,
    onConnectionClick: (ConnectionItemUi) -> Unit,
) {
    val relatedContexts =
        beacon.relatedContextIds.mapNotNull { relatedId ->
            allProjects.firstOrNull { it.id == relatedId }?.let { contextItem ->
                MainBeaconCardLinkUi(
                    id = contextItem.id,
                    title = contextItem.name,
                )
            }
        }
    val relatedDocuments =
        beacon.relatedAttachmentIds.mapNotNull { relatedId ->
            attachmentOptions.firstOrNull { it.id == relatedId }?.let { option ->
                MainBeaconCardLinkUi(
                    id = option.id,
                    title = option.name,
                )
            }
        }

    MainBeaconCard(
        title = beacon.title,
        readinessStatus = beacon.readinessStatus,
        highestCompletedLevel = beacon.highestCompletedLevel,
        breakPointLevel = beacon.breakPointLevel,
        blockReason = beacon.blockReason,
        nextRequiredAction = beacon.nextRequiredAction,
        isExpanded = isExpanded,
        relatedContexts = relatedContexts,
        relatedDocuments = relatedDocuments,
        onToggleExpanded = onToggleExpanded,
        onEditClick = onEditClick,
        onContextClick = onContextClick,
        onDocumentClick = { attachmentId ->
            connectionItems
                .firstOrNull { it.id == attachmentId }
                ?.let(onConnectionClick)
                ?: attachmentOptions
                    .firstOrNull { it.id == attachmentId }
                    ?.let { option ->
                        onConnectionClick(
                            ConnectionItemUi(
                                id = option.id,
                                title = option.name,
                                type =
                                    when {
                                        option.linkType == LinkType.URL -> ConnectionType.URL
                                        option.linkType == LinkType.OBSIDIAN -> ConnectionType.OBSIDIAN_NOTE
                                        option.attachmentType == "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
                                        option.attachmentType == "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
                                        option.attachmentType == "CHECKLIST" -> ConnectionType.CHECKLIST
                                        option.attachmentType == "SCRIPT" -> ConnectionType.SCRIPT
                                        else -> ConnectionType.ATTACHMENT
                                    },
                                vault = option.vault,
                            ),
                        )
                    }
        },
    )
}

@Composable
private fun MainBeaconCard(
    title: String,
    readinessStatus: MainBeaconReadinessStatus,
    highestCompletedLevel: String,
    breakPointLevel: String,
    blockReason: String,
    nextRequiredAction: String,
    isExpanded: Boolean,
    relatedContexts: List<MainBeaconCardLinkUi>,
    relatedDocuments: List<MainBeaconCardLinkUi>,
    onToggleExpanded: () -> Unit,
    onEditClick: () -> Unit,
    onContextClick: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                        .width(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(readinessStatus.statusAccentColor()),
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 11.dp, end = 14.dp, top = 11.dp, bottom = 11.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onToggleExpanded)
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MainBeaconStatusChip(status = readinessStatus)
                }

                CompactSummaryRow(
                    label = "Break point",
                    value = breakPointLevel,
                    priority = CompactSummaryPriority.PRIMARY,
                )
                CompactSummaryRow(
                    label = "Next",
                    value = nextRequiredAction,
                    priority = CompactSummaryPriority.PRIMARY,
                )
                CompactSummaryRow(
                    label = "Why",
                    value = blockReason,
                    priority = CompactSummaryPriority.SECONDARY,
                )
                CompactSummaryRow(
                    label = "Highest completed",
                    value = highestCompletedLevel,
                    priority = CompactSummaryPriority.QUIET,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Редагувати орієнтир",
                        )
                    }
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            imageVector =
                                if (isExpanded) {
                                    Icons.Outlined.ExpandLess
                                } else {
                                    Icons.Outlined.ExpandMore
                                },
                            contentDescription =
                                if (isExpanded) {
                                    "Згорнути картку орієнтиру"
                                } else {
                                    "Розгорнути картку орієнтиру"
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (isExpanded) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 6.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                    MainBeaconLinksSection(
                        title = "Контексти",
                        items = relatedContexts,
                        emptyLabel = "Немає зв’язаних контекстів",
                        onItemClick = onContextClick,
                    )
                    MainBeaconLinksSection(
                        title = "Документи",
                        items = relatedDocuments,
                        emptyLabel = "Немає зв’язаних документів",
                        onItemClick = onDocumentClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainBeaconLinksSection(
    title: String,
    items: List<MainBeaconCardLinkUi>,
    emptyLabel: String,
    onItemClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (items.isEmpty()) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEach { item ->
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onItemClick(item.id) }
                                .padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBeaconStatusChip(status: MainBeaconReadinessStatus) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = status.statusContainerColor(),
    ) {
        Text(
            text = status.mainStatusChipLabel(),
            color = status.statusColor(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CompactSummaryRow(
    label: String,
    value: String,
    priority: CompactSummaryPriority,
) {
    val resolvedValue = value.ifBlank { "—" }
    val labelColor =
        when (priority) {
            CompactSummaryPriority.PRIMARY -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            CompactSummaryPriority.SECONDARY -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
            CompactSummaryPriority.QUIET -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
        }
    val valueColor =
        when {
            resolvedValue == "—" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            priority == CompactSummaryPriority.PRIMARY -> MaterialTheme.colorScheme.onSurface
            priority == CompactSummaryPriority.SECONDARY -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        }
    val valueWeight =
        when {
            resolvedValue == "—" -> FontWeight.Normal
            priority == CompactSummaryPriority.PRIMARY -> FontWeight.SemiBold
            priority == CompactSummaryPriority.SECONDARY -> FontWeight.Normal
            else -> FontWeight.Normal
        }

    Text(
        text =
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = labelColor,
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                    append("$label: ")
                }
                withStyle(
                    SpanStyle(
                        color = valueColor,
                        fontWeight = valueWeight,
                    ),
                ) {
                    append(resolvedValue)
                }
            },
        style =
            if (priority == CompactSummaryPriority.PRIMARY) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private enum class CompactSummaryPriority {
    PRIMARY,
    SECONDARY,
    QUIET,
}

@Composable
private fun MainBeaconReadinessStatus.statusAccentColor(): Color =
    when (this) {
        MainBeaconReadinessStatus.READY -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f)
        MainBeaconReadinessStatus.CONDITIONAL -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.66f)
        MainBeaconReadinessStatus.BLOCKED -> MaterialTheme.colorScheme.error.copy(alpha = 0.74f)
        MainBeaconReadinessStatus.DEFECTED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
    }

@Composable
private fun MainBeaconReadinessStatus.statusContainerColor(): Color =
    when (this) {
        MainBeaconReadinessStatus.READY ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
        MainBeaconReadinessStatus.CONDITIONAL ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        MainBeaconReadinessStatus.BLOCKED ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        MainBeaconReadinessStatus.DEFECTED ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    }

@Composable
private fun MainBeaconReadinessStatus.statusColor(): Color =
    when (this) {
        MainBeaconReadinessStatus.READY -> MaterialTheme.colorScheme.onTertiaryContainer
        MainBeaconReadinessStatus.CONDITIONAL -> MaterialTheme.colorScheme.onSurfaceVariant
        MainBeaconReadinessStatus.BLOCKED -> MaterialTheme.colorScheme.error
        MainBeaconReadinessStatus.DEFECTED -> MaterialTheme.colorScheme.onSurface
    }

private fun MainBeaconReadinessStatus.mainStatusChipLabel(): String =
    when (this) {
        MainBeaconReadinessStatus.READY -> "Ready"
        MainBeaconReadinessStatus.CONDITIONAL -> "Conditional"
        MainBeaconReadinessStatus.BLOCKED -> "Blocked"
        MainBeaconReadinessStatus.DEFECTED -> "Defected"
    }


@Suppress("UnusedPrivateMember")
private fun summarizeSelection(
    selectedIds: Set<String>,
    options: List<MainBeaconSelectableItem>,
): String {
    val labels = options.filter { it.id in selectedIds }.map { it.label }
    return when {
        selectedIds.isEmpty() -> "0"
        labels.isEmpty() -> selectedIds.size.toString()
        labels.size <= 2 -> labels.joinToString(", ")
        else -> "${labels.take(2).joinToString(", ")} +${labels.size - 2}"
    }
}

private fun buildExternalTarget(
    linkType: LinkType?,
    target: String,
    vault: String? = null,
    globalObsidianVaultName: String? = null,
): String {
    val trimmed = target.trim()
    if (linkType == LinkType.OBSIDIAN && !trimmed.startsWith("obsidian://", ignoreCase = true)) {
        val vaultName = vault?.takeIf { it.isNotBlank() } ?: globalObsidianVaultName?.takeIf { it.isNotBlank() }
        val encodedFile = URLEncoder.encode(trimmed, "UTF-8")
        return if (vaultName != null) {
            "obsidian://open?vault=${URLEncoder.encode(vaultName, "UTF-8")}&file=$encodedFile"
        } else {
            "obsidian://open?file=$encodedFile"
        }
    }
    return trimmed
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

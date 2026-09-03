package com.romankozak.forwardappmobile.features.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentChooserScreen
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.orderToken
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.YearMonth
import java.net.URLEncoder

private const val ATTACHMENT_ID_PREVIEW_LENGTH = 8
private const val PICKER_OPEN_DELAY_MILLIS = 160L
private const val SHEET_BOTTOM_SPACER_DP = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategicArcScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: StrategicArcViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
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
    var showQuestContextPicker by remember { mutableStateOf(false) }
    var showQuestBeaconPicker by remember { mutableStateOf(false) }
    var showQuestBeaconGroupPicker by remember { mutableStateOf(false) }
    val openTarget: (NavTarget, Boolean) -> Unit = { target, recordInHistory ->
        navigationManager.navigateOrFallback(
            navController = navController,
            target = target,
            recordInHistory = recordInHistory,
        )
    }
    val navigateToArcChooser: () -> Unit = {
        val disabledIds = uiState.projects.joinToString(",") { it.id }.ifBlank { null }
        openTarget(
            NavTarget.ListChooser(
                title = "Додати стратегічну арку",
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
                        viewModel.addArcLink(result)
                    }
                }
            }
    }

    val availableAttachmentById = attachmentOptions.associateBy { it.id }
    val validAttachmentIds = linkedAttachmentIds.filter { it in availableAttachmentById.keys }
    val urlIds = validAttachmentIds.filter { id -> availableAttachmentById[id]?.linkType == LinkType.URL }
    val obsidianIds = validAttachmentIds.filter { id -> availableAttachmentById[id]?.linkType == LinkType.OBSIDIAN }
    val generalAttachmentIds =
        validAttachmentIds.filter { id ->
            availableAttachmentById[id]?.linkType !in setOf(LinkType.URL, LinkType.OBSIDIAN)
        }
    val items =
        buildList {
            addAll(uiState.projects.map { ConnectionItemUi(it.id, it.name, ConnectionType.CONTEXT) })
            addAll(
                generalAttachmentIds.map { id ->
                    val option = availableAttachmentById[id]
                    ConnectionItemUi(
                        id = id,
                        title = option?.name ?: "Вкладення ${id.take(ATTACHMENT_ID_PREVIEW_LENGTH)}",
                        type =
                            when (option?.attachmentType) {
                                "NOTE_DOCUMENT" -> ConnectionType.NOTE_DOCUMENT
                                "MUSIC_NOTE" -> ConnectionType.MUSIC_NOTE
                                "CHECKLIST" -> ConnectionType.CHECKLIST
                                "SCRIPT" -> ConnectionType.SCRIPT
                                else -> ConnectionType.ATTACHMENT
                            },
                    )
                },
            )
            addAll(
                urlIds.map { id ->
                    ConnectionItemUi(
                        id = id,
                        title =
                            availableAttachmentById[id]?.name
                                ?: "URL ${id.take(ATTACHMENT_ID_PREVIEW_LENGTH)}",
                        type = ConnectionType.URL,
                    )
                },
            )
            addAll(
                obsidianIds.map { id ->
                    val option = availableAttachmentById[id]
                    ConnectionItemUi(
                        id = id,
                        title =
                            option?.name
                                ?: "Obsidian ${id.take(ATTACHMENT_ID_PREVIEW_LENGTH)}",
                        type = ConnectionType.OBSIDIAN_NOTE,
                        vault = option?.vault,
                    )
                },
            )
        }
    val sortedItems = sortConnectionsByOrder(items, connectionsOrder)

    val onConnectionClick: (ConnectionItemUi) -> Unit = { item ->
        if (item.type == ConnectionType.CONTEXT) {
            openTarget(NavTarget.ContextDetail(contextId = item.id), true)
        } else {
            val option = availableAttachmentById[item.id]
            when {
                option?.attachmentType == "NOTE_DOCUMENT" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.NoteDocument(id = option.entityId), false)
                option?.attachmentType == "MUSIC_NOTE" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.MusicNote(id = option.entityId), false)
                option?.attachmentType == "CHECKLIST" && !option.entityId.isNullOrBlank() ->
                    openTarget(NavTarget.Checklist(id = option.entityId), false)
                option?.linkType == LinkType.CONTEXT && !option.target.isNullOrBlank() ->
                    openTarget(NavTarget.ContextDetail(contextId = option.target), true)
                (option?.linkType == LinkType.URL || option?.linkType == LinkType.OBSIDIAN) &&
                    !option.target.isNullOrBlank() -> {
                    val resolvedTarget = buildExternalTarget(option.linkType, option.target, option.vault, obsidianVaultName)
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(resolvedTarget)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }.onFailure {
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.AttachmentsLibrary,
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                        runCatching {
                            navController.getBackStackEntry("attachments_library_screen")
                                .savedStateHandle["attachment_library_query"] = item.id
                        }
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
                    runCatching {
                        navController.getBackStackEntry("attachments_library_screen")
                            .savedStateHandle["attachment_library_query"] = item.id
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error!!)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                StrategicArcMonthHeader(
                    arcKey = uiState.currentArcKey,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
                when (selectedTab) {
                    StrategicArcTab.QUESTS ->
                        ArcQuestList(
                            quests = uiState.arcQuests,
                            contextNames = uiState.allProjects.associate { it.id to it.name },
                            onQuestClick = { quest ->
                                quest.linkedContextId?.let {
                                    openTarget(NavTarget.ContextDetail(contextId = it), true)
                                }
                            },
                            onOpenContext = { contextId ->
                                openTarget(NavTarget.ContextDetail(contextId = contextId), true)
                            },
                            onCreateMission = viewModel::createMissionFromArcQuest,
                            onEditQuest = viewModel::updateArcQuest,
                            onDeleteQuest = viewModel::deleteArcQuest,
                            onReorder = viewModel::reorderArcQuests,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    StrategicArcTab.ARTIFACT ->
                        ArcArtifactPanel(
                            arcKey = uiState.currentArcKey,
                            onOpenArtifact = {
                                viewModel.openOrCreateArcArtifact { documentId ->
                                    openTarget(NavTarget.NoteDocument(id = documentId, startEdit = true), false)
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                }
            }
        }

        if (!uiState.isLoading && uiState.error == null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = CommandDeckFabDefaults.BottomPadding),
            ) {
                FloatingActionButton(onClick = { isFabMenuExpanded = !isFabMenuExpanded }) {
                    Icon(Icons.Default.Menu, contentDescription = "Меню дій стратегічної арки")
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
                        text = { Text("Попередня арка") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        },
                        enabled = uiState.previousArcKey != null,
                        onClick = {
                            isFabMenuExpanded = false
                            viewModel.navigateToPreviousArcMonth()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Наступна арка") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        },
                        onClick = {
                            isFabMenuExpanded = false
                            viewModel.navigateToNextArcMonth()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Контекст як ArcQuest") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            showQuestContextPicker = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Орієнтир як ArcQuest") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            showQuestBeaconPicker = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Група орієнтирів як ArcQuest") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            showQuestBeaconGroupPicker = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Відкрити артефакт") },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            viewModel.openOrCreateArcArtifact { documentId ->
                                openTarget(NavTarget.NoteDocument(id = documentId, startEdit = true), false)
                            }
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

    if (isScopeLinksSheetVisible) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissScopeLinksSheet) {
            ConnectionsPanel(
                items = sortedItems,
                onConnectionClick = onConnectionClick,
                onConnectionRemove = { item ->
                    if (item.type == ConnectionType.CONTEXT) {
                        viewModel.removeArcLink(item.id)
                    } else {
                        viewModel.removeAttachmentLink(item.id)
                    }
                },
                onAddButtonClick = {
                    viewModel.dismissScopeLinksSheet()
                    pendingCreateAction = null
                    scope.launch {
                        delay(PICKER_OPEN_DELAY_MILLIS)
                        activeLinkPickerTab = LinkPickerTab.CONTEXTS
                    }
                },
                onAddConnection = { type ->
                    when (type) {
                        AddConnectionType.CONTEXT -> {
                            navigateToArcChooser()
                        }
                        AddConnectionType.ATTACHMENT -> {
                            viewModel.dismissScopeLinksSheet()
                            pendingCreateAction = null
                            scope.launch {
                                delay(PICKER_OPEN_DELAY_MILLIS)
                                showAttachmentChooser = true
                            }
                        }
                        AddConnectionType.EXTERNAL_LINK -> showAddUrlDialog = true
                        AddConnectionType.OBSIDIAN_NOTE -> showAddObsidianDialog = true
                    }
                },
                onCreateConnection = { type ->
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
                preferActionsBesideTitleWhenWide = true,
                onConnectionsReordered = { reordered ->
                    viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
                },
            )
            Spacer(modifier = Modifier.height(SHEET_BOTTOM_SPACER_DP.dp))
        }
    }

    if (showAttachmentChooser) {
        StrategicAttachmentChooser(
            options = attachmentOptions,
            preselected = linkedAttachmentIds.toSet(),
            onDismiss = { showAttachmentChooser = false },
            onConfirm = { selected ->
                selected.forEach(viewModel::addAttachmentLink)
                showAttachmentChooser = false
            },
        )
    }

    if (showQuestContextPicker) {
        LinkedTargetsPickerDialog(
            contextOptions =
                uiState.allProjects.map {
                    ProjectOption(id = it.id, name = it.name, parentId = it.parentId)
                },
            attachmentOptions = emptyList(),
            preselectedContextIds = uiState.arcQuests.mapNotNull { it.linkedContextId }.toSet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { showQuestContextPicker = false },
            onContextSelected = { id ->
                viewModel.addArcQuestFromContext(id)
                showQuestContextPicker = false
            },
            onAttachmentSelected = {},
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = null,
        )
    }

    if (showQuestBeaconPicker) {
        ArcQuestBeaconPickerDialog(
            beacons = uiState.beacons,
            usedSourceIds =
                uiState.arcQuests
                    .filter { it.sourceType == ArcQuestSourceType.BEACON.name }
                    .mapNotNull { it.sourceId }
                    .toSet(),
            onDismiss = { showQuestBeaconPicker = false },
            onSelected = { beaconId ->
                viewModel.addArcQuestFromBeacon(beaconId)
                showQuestBeaconPicker = false
            },
        )
    }

    if (showQuestBeaconGroupPicker) {
        ArcQuestBeaconGroupPickerDialog(
            groups = uiState.beaconGroups,
            usedSourceIds =
                uiState.arcQuests
                    .filter { it.sourceType == ArcQuestSourceType.BEACON_GROUP.name }
                    .mapNotNull { it.sourceId }
                    .toSet(),
            onDismiss = { showQuestBeaconGroupPicker = false },
            onSelected = { groupId ->
                viewModel.addArcQuestFromBeaconGroup(groupId)
                showQuestBeaconGroupPicker = false
            },
        )
    }

    activeLinkPickerTab?.let { initialTab ->
        val availableAttachmentIds = attachmentOptions.map { it.id }.toSet()
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
            preselectedAttachmentIds = linkedAttachmentIds.filter { it in availableAttachmentIds }.toSet(),
            initialTab = initialTab,
            initialCreateAction = pendingCreateAction,
            onDismiss = {
                activeLinkPickerTab = null
                pendingCreateAction = null
            },
            onContextSelected = { id ->
                viewModel.addArcLink(id)
                activeLinkPickerTab = null
                pendingCreateAction = null
            },
            onAttachmentSelected = { id ->
                viewModel.addAttachmentLink(id)
                activeLinkPickerTab = null
                pendingCreateAction = null
            },
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = { draft -> viewModel.createArcDocumentForPicker(draft) },
        )
    }

    if (showAddUrlDialog) {
        AddWebLinkDialog(
            onDismiss = { showAddUrlDialog = false },
            onConfirm = { url, name ->
                viewModel.addUrlLink(url, name)
                showAddUrlDialog = false
            },
        )
    }

    if (showAddObsidianDialog) {
        AddObsidianLinkDialog(
            onDismiss = { showAddObsidianDialog = false },
            onConfirm = { noteName, displayName, vault ->
                viewModel.addObsidianLink(noteName, displayName, vault)
                showAddObsidianDialog = false
            },
        )
    }
}

@Composable
private fun StrategicArcMonthHeader(
    arcKey: String,
    modifier: Modifier = Modifier,
) {
    val isCurrent = arcKey == YearMonth.now().toString()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color =
                if (isCurrent) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f)
                },
            contentColor =
                if (isCurrent) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            border =
                androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color =
                        if (isCurrent) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.58f)
                        },
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Arc",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                )
                Text(
                    text = formatStrategicArcMonth(arcKey),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (isCurrent) {
                    Text(
                        text = "current",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcQuestList(
    quests: List<ArcQuestEntity>,
    contextNames: Map<String, String>,
    onQuestClick: (ArcQuestEntity) -> Unit,
    onOpenContext: (String) -> Unit,
    onCreateMission: (ArcQuestEntity) -> Unit,
    onEditQuest: (ArcQuestEntity, String, String?) -> Unit,
    onDeleteQuest: (ArcQuestEntity) -> Unit,
    onReorder: (List<ArcQuestEntity>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    var internalQuests by remember(quests) { mutableStateOf(quests) }
    var hasPendingReorder by remember { mutableStateOf(false) }
    val persistPendingReorder = {
        if (hasPendingReorder) {
            onReorder(internalQuests)
        }
        hasPendingReorder = false
    }
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            if (from.index == to.index) return@rememberReorderableLazyListState
            if (from.index !in internalQuests.indices || to.index !in internalQuests.indices) {
                return@rememberReorderableLazyListState
            }
            internalQuests =
                internalQuests.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            hasPendingReorder = true
        }

    if (quests.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Немає ArcQuest для поточної арки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(internalQuests, key = { _, quest -> quest.id }) { _, quest ->
            ReorderableItem(reorderableState, key = quest.id) {
                ArcQuestCard(
                    quest = quest,
                    contextName = quest.linkedContextId?.let(contextNames::get),
                    onQuestClick = { onQuestClick(quest) },
                    onOpenContext = onOpenContext,
                    onCreateMission = { onCreateMission(quest) },
                    onEditQuest = { title, description -> onEditQuest(quest, title, description) },
                    onDeleteQuest = { onDeleteQuest(quest) },
                    dragHandleModifier =
                        with(this@ReorderableItem) {
                            Modifier.longPressDraggableHandle(
                                onDragStopped = { persistPendingReorder() },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun ArcQuestBeaconPickerDialog(
    beacons: List<MainBeaconWithRelations>,
    usedSourceIds: Set<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredBeacons =
        remember(beacons, query) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                beacons
            } else {
                beacons.filter { item ->
                    item.beacon.title.contains(normalizedQuery, ignoreCase = true) ||
                        item.beacon.description.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати орієнтир як ArcQuest") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Пошук орієнтирів") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (beacons.isEmpty()) {
                    Text(
                        text = "Орієнтирів немає",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (filteredBeacons.isEmpty()) {
                    Text(
                        text = "Нічого не знайдено",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(filteredBeacons, key = { _, item -> item.beacon.id }) { _, item ->
                            val beacon = item.beacon
                            val alreadyAdded = beacon.id in usedSourceIds
                            PickerOptionRow(
                                title = beacon.title,
                                subtitle = if (alreadyAdded) "Уже є в квестах арки" else beacon.description,
                                enabled = !alreadyAdded,
                                onClick = { onSelected(beacon.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )
}

@Composable
private fun ArcQuestBeaconGroupPickerDialog(
    groups: List<MainBeaconGroup>,
    usedSourceIds: Set<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Додати групу орієнтирів як ArcQuest") },
        text = {
            if (groups.isEmpty()) {
                Text(
                    text = "Груп орієнтирів немає",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(groups, key = { _, group -> group.id }) { _, group ->
                        val alreadyAdded = group.id in usedSourceIds
                        PickerOptionRow(
                            title = group.title,
                            subtitle = if (alreadyAdded) "Уже є в квестах арки" else group.description,
                            enabled = !alreadyAdded,
                            onClick = { onSelected(group.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )
}

@Composable
private fun PickerOptionRow(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
        }
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = contentColor,
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArcQuestCard(
    quest: ArcQuestEntity,
    contextName: String?,
    onQuestClick: () -> Unit,
    onOpenContext: (String) -> Unit,
    onCreateMission: () -> Unit,
    onEditQuest: (String, String?) -> Unit,
    onDeleteQuest: () -> Unit,
    dragHandleModifier: Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var editVisible by remember { mutableStateOf(false) }
    var editTitle by remember(quest.id, quest.title) { mutableStateOf(quest.title) }
    var editDescription by remember(quest.id, quest.description) { mutableStateOf(quest.description.orEmpty()) }
    val linkedContextId = quest.linkedContextId

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onQuestClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.RocketLaunch,
                contentDescription = "ArcQuest",
                tint = MaterialTheme.colorScheme.primary,
                modifier = dragHandleModifier.size(28.dp),
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!quest.description.isNullOrBlank()) {
                    Text(
                        text = quest.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!contextName.isNullOrBlank()) {
                    Text(
                        text = contextName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "ArcQuest actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                ) {
                    DropdownMenuItem(
                        text = { Text("Редагувати") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            editVisible = true
                        },
                    )
                    if (linkedContextId != null) {
                        DropdownMenuItem(
                            text = { Text("Відкрити контекст") },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenContext(linkedContextId)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Створити місію") },
                        leadingIcon = { Icon(Icons.Outlined.RocketLaunch, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCreateMission()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Видалити") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDeleteQuest()
                        },
                    )
                }
            }
        }
    }

    if (editVisible) {
        AlertDialog(
            onDismissRequest = { editVisible = false },
            title = { Text("Редагувати ArcQuest") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Назва") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Опис") },
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditQuest(editTitle, editDescription)
                        editVisible = false
                    },
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { editVisible = false }) {
                    Text("Скасувати")
                }
            },
        )
    }
}

@Composable
private fun ArcArtifactPanel(
    arcKey: String,
    onOpenArtifact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Артефакт стратегічної арки $arcKey",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onOpenArtifact) {
                Text("Відкрити або створити документ")
            }
        }
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

private fun formatStrategicArcMonth(arcKey: String): String {
    val month = runCatching { YearMonth.parse(arcKey) }.getOrNull() ?: return arcKey
    val title =
        when (month.monthValue) {
            1 -> "Січень"
            2 -> "Лютий"
            3 -> "Березень"
            4 -> "Квітень"
            5 -> "Травень"
            6 -> "Червень"
            7 -> "Липень"
            8 -> "Серпень"
            9 -> "Вересень"
            10 -> "Жовтень"
            11 -> "Листопад"
            12 -> "Грудень"
            else -> return arcKey
        }
    return "$title ${month.year}"
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

@Composable
private fun StrategicAttachmentChooser(
    options: List<ScopeAttachmentOption>,
    preselected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    AttachmentChooserScreen(
        options = options.map { AttachmentOption(id = it.id, name = it.name, linkType = it.linkType) },
        preselected = preselected,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

package com.romankozak.forwardappmobile.features.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
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
                    pendingCreateAction = null
                    activeLinkPickerTab = LinkPickerTab.CONTEXTS
                },
                onAddConnection = { type ->
                    when (type) {
                        AddConnectionType.CONTEXT -> {
                            navigateToArcChooser()
                        }
                        AddConnectionType.ATTACHMENT -> {
                            pendingCreateAction = null
                            showAttachmentChooser = true
                        }
                        AddConnectionType.EXTERNAL_LINK -> showAddUrlDialog = true
                        AddConnectionType.OBSIDIAN_NOTE -> showAddObsidianDialog = true
                    }
                },
                onCreateConnection = { type ->
                    pendingCreateAction = type.toPickerCreateAction()
                    activeLinkPickerTab =
                        if (type == CreateConnectionType.CONTEXT) {
                            LinkPickerTab.CONTEXTS
                        } else {
                            LinkPickerTab.ATTACHMENTS
                        }
                },
                preferActionsBesideTitleWhenWide = true,
                onConnectionsReordered = { reordered ->
                    viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
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
                        text = { Text("Додати посилання") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            navigateToArcChooser()
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

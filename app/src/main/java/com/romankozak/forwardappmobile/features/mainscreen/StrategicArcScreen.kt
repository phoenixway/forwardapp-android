package com.romankozak.forwardappmobile.features.mainscreen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.romankozak.forwardappmobile.core.navigation.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
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
import com.romankozak.forwardappmobile.ui.components.ContextLinkList
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType
import com.romankozak.forwardappmobile.ui.components.orderToken
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder
import java.net.URLEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategicArcScreen(
    navController: NavController,
    viewModel: StrategicArcViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val attachmentOptions by viewModel.attachmentOptions.collectAsState()
    val linkedAttachmentIds by viewModel.linkedAttachmentIds.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    val scope = rememberCoroutineScope()
    var showAttachmentChooser by remember { mutableStateOf(false) }
    var activeLinkPickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember { mutableStateOf<PickerCreateAction?>(null) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var showAddObsidianDialog by remember { mutableStateOf(false) }
    val mainScreenViewModel: ContextHierarchyScreenViewModel =
        hiltViewModel(navController.getBackStackEntry(MAIN_GRAPH_ROUTE))

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

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.error!!)
        }
    } else {
        ContextLinkList(
            items = uiState.projects,
            onAddClick = null,
            onItemClick = { project ->
                navController.navigate("goal_detail_screen/${project.id}")
            },
            onRevealClick = { project ->
                mainScreenViewModel.onEvent(ContextHierarchyScreenEvent.RevealContextInHierarchy(project.id))
                navController.popBackStack()
            },
            onRemoveClick = { project ->
                viewModel.removeArcLink(project.id)
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
        )
    }

    if (isScopeLinksSheetVisible) {
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
                        ConnectionItemUi(
                            id = id,
                            title = availableAttachmentById[id]?.name ?: "Вкладення ${id.take(8)}",
                            type = ConnectionType.ATTACHMENT,
                        )
                    },
                )
                addAll(
                    urlIds.map { id ->
                        ConnectionItemUi(
                            id = id,
                            title = availableAttachmentById[id]?.name ?: "URL ${id.take(8)}",
                            type = ConnectionType.URL,
                        )
                    },
                )
                addAll(
                    obsidianIds.map { id ->
                        ConnectionItemUi(
                            id = id,
                            title = availableAttachmentById[id]?.name ?: "Obsidian ${id.take(8)}",
                            type = ConnectionType.OBSIDIAN_NOTE,
                        )
                    },
                )
            }
        val sortedItems = sortConnectionsByOrder(items, connectionsOrder)

        ModalBottomSheet(onDismissRequest = viewModel::dismissScopeLinksSheet) {
            ConnectionsPanel(
                items = sortedItems,
                onConnectionClick = { item ->
                    if (item.type == ConnectionType.CONTEXT) {
                        navController.navigate("goal_detail_screen/${item.id}")
                    } else {
                        val option = availableAttachmentById[item.id]
                        when {
                            option?.attachmentType == "NOTE_DOCUMENT" && !option.entityId.isNullOrBlank() ->
                                navController.navigate("note_document_screen/${option.entityId}")
                            option?.attachmentType == "MUSIC_NOTE" && !option.entityId.isNullOrBlank() ->
                                navController.navigate("music_note_screen/${option.entityId}")
                            option?.attachmentType == "CHECKLIST" && !option.entityId.isNullOrBlank() ->
                                navController.navigate("checklist_screen?checklistId=${option.entityId}")
                            option?.linkType == LinkType.CONTEXT && !option.target.isNullOrBlank() ->
                                navController.navigate("goal_detail_screen/${option.target}")
                            (option?.linkType == LinkType.URL || option?.linkType == LinkType.OBSIDIAN) &&
                                !option.target.isNullOrBlank() -> {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(option.target)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }.onFailure {
                                    if (it !is ActivityNotFoundException) {
                                        navController.navigate("attachments_library_screen") {
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
                            else -> {
                                navController.navigate("attachments_library_screen") {
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
                },
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
                        delay(160)
                        activeLinkPickerTab = LinkPickerTab.CONTEXTS
                    }
                },
                onAddConnection = { type ->
                    when (type) {
                        AddConnectionType.CONTEXT -> {
                            val disabledIds = uiState.projects.joinToString(",") { it.id }
                            val title = URLEncoder.encode("Додати стратегічну арку", "UTF-8")
                            val route =
                                if (disabledIds.isBlank()) {
                                    "list_chooser_screen/$title"
                                } else {
                                    "list_chooser_screen/$title?disabledIds=$disabledIds"
                                }
                            navController.navigate(route)
                        }
                        AddConnectionType.ATTACHMENT -> {
                            viewModel.dismissScopeLinksSheet()
                            pendingCreateAction = null
                            scope.launch {
                                delay(160)
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
                        delay(160)
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
            Spacer(modifier = Modifier.height(12.dp))
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
            contextOptions = uiState.projects.map { ProjectOption(id = it.id, name = it.name, parentId = it.parentId) },
            attachmentOptions = attachmentOptions.map { AttachmentOption(id = it.id, name = it.name, linkType = it.linkType, attachmentType = it.attachmentType, entityId = it.entityId, target = it.target) },
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
            onConfirm = { noteName, displayName ->
                viewModel.addObsidianLink(noteName, displayName)
                showAddObsidianDialog = false
            },
        )
    }
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

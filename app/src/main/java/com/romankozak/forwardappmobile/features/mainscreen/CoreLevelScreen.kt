package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.navigation.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.features.attachments.ui.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.features.attachments.ui.AddWebLinkDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentChooserScreen
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.ContextLinkList
import com.romankozak.forwardappmobile.ui.components.orderToken
import com.romankozak.forwardappmobile.ui.components.sortConnectionsByOrder
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreLevelScreen(
    navController: NavController,
    viewModel: CoreLevelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val attachmentOptions by viewModel.attachmentOptions.collectAsState()
    val linkedAttachmentIds by viewModel.linkedAttachmentIds.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    var showAttachmentChooser by remember { mutableStateOf(false) }
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
                        viewModel.addCoreLink(result)
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
                viewModel.removeCoreLink(project.id)
            },
            contentPadding = PaddingValues(bottom = 20.dp),
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
                        navController.navigate("attachments_library_screen") {
                            launchSingleTop = true
                            restoreState = true
                        }
                        runCatching {
                            navController.getBackStackEntry("attachments_library_screen")
                                .savedStateHandle["attachment_library_query"] = item.id
                        }
                    }
                },
                onConnectionRemove = { item ->
                    if (item.type == ConnectionType.CONTEXT) {
                        viewModel.removeCoreLink(item.id)
                    } else {
                        viewModel.removeAttachmentLink(item.id)
                    }
                },
                onAddConnection = { type ->
                    when (type) {
                        AddConnectionType.CONTEXT -> {
                            val disabledIds = uiState.projects.joinToString(",") { it.id }
                            val title = URLEncoder.encode("Додати контекст у ядро", "UTF-8")
                            val route =
                                if (disabledIds.isBlank()) {
                                    "list_chooser_screen/$title"
                                } else {
                                    "list_chooser_screen/$title?disabledIds=$disabledIds"
                                }
                            navController.navigate(route)
                        }

                        AddConnectionType.ATTACHMENT -> showAttachmentChooser = true
                        AddConnectionType.EXTERNAL_LINK -> showAddUrlDialog = true
                        AddConnectionType.OBSIDIAN_NOTE -> showAddObsidianDialog = true
                    }
                },
                onConnectionsReordered = { reordered ->
                    viewModel.updateConnectionsOrder(reordered.map { it.orderToken() })
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAttachmentChooser) {
        AttachmentChooserScreen(
            options = attachmentOptions.map { AttachmentOption(id = it.id, name = it.name, linkType = it.linkType) },
            preselected = linkedAttachmentIds.toSet(),
            onDismiss = { showAttachmentChooser = false },
            onConfirm = { selected ->
                selected.forEach(viewModel::addAttachmentLink)
                showAttachmentChooser = false
            },
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

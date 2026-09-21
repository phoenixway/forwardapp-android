package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.routes.COMMAND_DECK_ROUTE
import com.romankozak.forwardappmobile.core.navigation.routes.MAIN_GRAPH_ROUTE
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ProjectHierarchyScreenScaffold
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.findOrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.findOrientationHierarchyItemIndex
import com.romankozak.forwardappmobile.features.mainscreen.CoreLevelViewModel
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconCardLinkUi
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconEditorSheet
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconEditorState
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconGroupEditorDialog
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionType
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder

private const val UI_TAG = "ProjectHierarchyScreenUI_DEBUG"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: ContextHierarchyScreenViewModel = hiltViewModel(),
    coreLevelViewModel: CoreLevelViewModel = hiltViewModel(),
    navigationManager: EnhancedNavigationManager? = null,
    projectIdToReveal: String? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coreUiState by coreLevelViewModel.uiState.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val focusedContextIds by viewModel.focusedContextIds.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    var editingBeacon by remember { mutableStateOf<MainBeaconEditorState?>(null) }
    var beaconPendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collectLatest { event ->
            when (event) {
                is ProjectUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.Sync,
                    )
                }
                is ProjectUiEvent.NavigateToDetails ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.ContextDetail(contextId = event.projectId),
                        recordInHistory = true,
                    )
                is ProjectUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is ProjectUiEvent.NavigateToGlobalSearch -> {
                    val encoded = URLEncoder.encode(event.query, "UTF-8")
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.GlobalSearch(query = encoded),
                        recordInHistory = true,
                    )
                }
                is ProjectUiEvent.NavigateToSettings ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.Settings,
                    )
                is ProjectUiEvent.NavigateToEditProjectScreen ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.ProjectSettings(projectId = event.projectId),
                    )
                is ProjectUiEvent.Navigate ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = event.target,
                    )
                is ProjectUiEvent.NavigateToDayPlan ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.DayManagement(date = event.date, startTab = event.startTab),
                    )
                is ProjectUiEvent.NavigateToStrategicManagement ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.StrategicManagement,
                    )
                is ProjectUiEvent.FocusSearchField -> {
                }
                is ProjectUiEvent.HideKeyboard -> {
                    focusManager.clearFocus()
                }
                is ProjectUiEvent.OpenUri -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, event.uri.toUri())
                    navController.context.startActivity(intent)
                }
                is ProjectUiEvent.ScrollToIndex -> { }
            }
        }
    }

    LaunchedEffect(projectIdToReveal) {
        projectIdToReveal?.let {
            viewModel.onEvent(
                ContextHierarchyScreenEvent.RevealContextInHierarchy(projectId = it),
            )
        }
    }

    DisposableEffect(navController, lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    android.util.Log.d("ProjectRevealDebug", "ProjectHierarchyScreen ON_RESUME")
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String?>("list_chooser_result")
                        ?.let { result ->
                            viewModel.onEvent(ContextHierarchyScreenEvent.ListChooserResult(result))
                        }

                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Boolean>("open_search_dialog")
                        ?.let { shouldOpen ->
                            if (shouldOpen == true) {
                                viewModel.onEvent(ContextHierarchyScreenEvent.ShowSearchDialog)
                            }
                        }

                    val projectIdToReveal =
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.remove<String>("projectIdToReveal")
                            ?: viewModel.consumePendingProjectToReveal()

                    projectIdToReveal?.let { projectId ->
                        android.util.Log.d("ProjectRevealDebug", "Retrieved projectIdToReveal: $projectId")
                        android.util.Log.d("ProjectRevealDebug", "Calling RevealProjectInHierarchy event")
                        viewModel.onEvent(
                            ContextHierarchyScreenEvent.RevealContextInHierarchy(
                                projectId = projectId,
                            ),
                        )
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    viewModel.enhancedNavigationManager?.let { navManager ->
        ProjectHierarchyScreenScaffold(
            uiState = uiState,
            focusedContextIds = focusedContextIds,
            onEvent = viewModel::onEvent,
            enhancedNavigationManager = navManager,
            lastOngoingActivity = lastOngoingActivity,
            viewModel = viewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onCloseScreen = {
                navController.navigate(COMMAND_DECK_ROUTE) {
                    popUpTo(MAIN_GRAPH_ROUTE) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            onEditBeacon = { beaconId ->
                editingBeacon = coreLevelViewModel.buildEditorState(beaconId)
            },
            onDeleteBeacon = { beaconId ->
                beaconPendingDeleteId = beaconId
            },
            onAddMainBeacon = {
                val parentBeaconId = parentBeaconIdForNewBeacon(uiState)
                val groupIds = groupIdsForNewBeacon(uiState)
                editingBeacon =
                    coreLevelViewModel.buildEditorState(null)
                        .copy(
                            parentBeaconId = parentBeaconId,
                            groupIds = groupIds,
                        )
            },
            onAddMainBeaconGroup = { showCreateGroupDialog = true },
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }

    editingBeacon?.let { editor ->
        val connectionItems =
            buildList {
                addAll(
                    editor.relatedContextIds.mapNotNull { contextId ->
                        coreUiState.allProjects.firstOrNull { it.id == contextId }?.let { context ->
                            ConnectionItemUi(
                                id = context.id,
                                title = context.name,
                                type = ConnectionType.CONTEXT,
                            )
                        }
                    },
                )
                addAll(
                    editor.relatedAttachmentIds.map { attachmentId ->
                        ConnectionItemUi(
                            id = attachmentId,
                            title = "Attachment ${attachmentId.take(8)}",
                            type = ConnectionType.ATTACHMENT,
                        )
                    },
                )
            }
        MainBeaconEditorSheet(
            state = editor,
            connectionItems = connectionItems,
            groupItems =
                editor.groupIds.mapNotNull { groupId ->
                    coreUiState.groups.firstOrNull { it.id == groupId }?.let { group ->
                        MainBeaconCardLinkUi(id = group.id, title = group.title)
                    }
                },
            parentBeaconItem =
                editor.parentBeaconId?.let { parentId ->
                    coreUiState.beacons.firstOrNull { it.id == parentId }?.let { parent ->
                        MainBeaconCardLinkUi(id = parent.id, title = parent.title)
                    }
                },
            onDismiss = { editingBeacon = null },
            onStateChange = { editingBeacon = it },
            onEditGroups = {
                Toast.makeText(navController.context, "Group picker is available on Core tab", Toast.LENGTH_SHORT).show()
            },
            onEditParentBeacon = {
                Toast.makeText(navController.context, "Parent beacon picker is available on Core tab", Toast.LENGTH_SHORT).show()
            },
            onClearParentBeacon = { editingBeacon = editingBeacon?.copy(parentBeaconId = null) },
            onConnectionClick = { item ->
                if (item.type == ConnectionType.CONTEXT) {
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.ContextDetail(contextId = item.id),
                        recordInHistory = true,
                    )
                }
            },
            onConnectionRemove = { item ->
                editingBeacon =
                    if (item.type == ConnectionType.CONTEXT) {
                        editingBeacon?.copy(relatedContextIds = editingBeacon?.relatedContextIds.orEmpty() - item.id)
                    } else {
                        editingBeacon?.copy(relatedAttachmentIds = editingBeacon?.relatedAttachmentIds.orEmpty() - item.id)
                    }
            },
            onAddConnection = {
                Toast.makeText(navController.context, "Connection picker is available on Core tab", Toast.LENGTH_SHORT).show()
            },
            onCreateConnection = {
                Toast.makeText(navController.context, "Connection creation is available on Core tab", Toast.LENGTH_SHORT).show()
            },
            onEditLevel = {
                Toast.makeText(navController.context, "Level editor is available on Core tab", Toast.LENGTH_SHORT).show()
            },
            onSave = {
                coreLevelViewModel.saveBeacon(editor)
                editingBeacon = null
            },
            onDuplicate = null,
            onDelete = null,
        )
    }

    beaconPendingDeleteId?.let { beaconId ->
        AlertDialog(
            onDismissRequest = { beaconPendingDeleteId = null },
            title = { Text("Видалити головний орієнтир?") },
            text = { Text("Цю дію не буде скасовано.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coreLevelViewModel.deleteBeacon(beaconId)
                        if (editingBeacon?.id == beaconId) {
                            editingBeacon = null
                        }
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

    if (showCreateGroupDialog) {
        MainBeaconGroupEditorDialog(
            group = null,
            onDismiss = { showCreateGroupDialog = false },
            onSave = { title, description ->
                coreLevelViewModel.createBeaconGroup(title, description)
                showCreateGroupDialog = false
            },
            onDelete = null,
        )
    }
}

private fun parentBeaconIdForNewBeacon(uiState: ProjectHierarchyScreenUiState): String? {
    val activeIdentity =
        when (val subState = uiState.currentSubState) {
            is ProjectHierarchyScreenSubState.ProjectFocused ->
                subState.projectId to subState.placementId
            is ProjectHierarchyScreenSubState.OrientationFocused ->
                subState.nodeId to subState.placementId
            else -> null
        } ?: return null

    val (activeNodeId, activePlacementId) = activeIdentity
    val activeItem =
        findOrientationHierarchyItem(
            items = uiState.orientationHierarchy,
            nodeId = activeNodeId,
            placementId = activePlacementId,
        )
    return when (activeItem?.node) {
        is OrientationHierarchyNode.Beacon -> activeItem.node.id
        null -> null
        else ->
            nearestAncestorBeaconId(
                orientationHierarchy = uiState.orientationHierarchy,
                nodeId = activeNodeId,
                placementId = activePlacementId,
            )
    }
}

private fun groupIdsForNewBeacon(uiState: ProjectHierarchyScreenUiState): Set<String> {
    val subState =
        uiState.currentSubState as? ProjectHierarchyScreenSubState.OrientationFocused
            ?: return emptySet()
    val activeNode =
        findOrientationHierarchyItem(
            items = uiState.orientationHierarchy,
            nodeId = subState.nodeId,
            placementId = subState.placementId,
        )?.node
    return if (activeNode is OrientationHierarchyNode.Group) {
        setOf(activeNode.id)
    } else {
        emptySet()
    }
}

private fun nearestAncestorBeaconId(
    orientationHierarchy: List<OrientationHierarchyItem>,
    nodeId: String,
    placementId: String?,
): String? {
    val nodeIndex =
        findOrientationHierarchyItemIndex(
            items = orientationHierarchy,
            nodeId = nodeId,
            placementId = placementId,
        )
    if (nodeIndex <= 0) return null
    val nodeLevel = orientationHierarchy[nodeIndex].level
    for (index in nodeIndex - 1 downTo 0) {
        val candidate = orientationHierarchy[index]
        if (candidate.level < nodeLevel && candidate.node is OrientationHierarchyNode.Beacon) {
            return candidate.node.id
        }
    }
    return null
}

package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ProjectHierarchyScreenTopAppBar
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.hierarchy.ProjectHierarchyView
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.bigPictureDestination
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.bigPictureSemanticDestination
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.structuralDestination
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.BigPictureMutationControls
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.BigPictureSourceDeleteDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.BigPictureWorkspaceRenameDialog
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY

/**
 * Structural controls write through H1; canonical source actions use their
 * domain authorities. GroupScope coverage keeps native/historical transport admissible.
 */
internal const val BIG_PICTURE_MUTATION_UX_ENABLED = true

/** The existing hierarchy destination now presents canonical GENERAL occurrences. */
@Composable
fun ProjectHierarchyScreen(
    navController: NavController,
    viewModel: BigPictureViewModel = hiltViewModel(),
    mutations: BigPictureMutationViewModel = hiltViewModel(),
    sourceDeletion: BigPictureSourceDeleteViewModel = hiltViewModel(),
    workspaceRename: BigPictureWorkspaceRenameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val ready = state as? BigPictureUiState.Ready
    val mutation by mutations.state.collectAsStateWithLifecycle()
    val sourceState by sourceDeletion.state.collectAsStateWithLifecycle()
    val renameState by workspaceRename.state.collectAsStateWithLifecycle()
    val sourceTargets by sourceDeletion.targets.collectAsStateWithLifecycle()
    val clipboard by mutations.clipboard.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val entry = remember(navController) { navController.currentBackStackEntry!! }
    val pickerResult by entry.savedStateHandle
        .getStateFlow<List<String>?>(CANONICAL_ENTITY_PICKER_RESULT_KEY, null).collectAsStateWithLifecycle()
    LaunchedEffect(pickerResult) { mutations.acceptPickerResult(entry.savedStateHandle) }
    LaunchedEffect(mutation.message) {
        mutation.message?.let { snackbar.showSnackbar(it); mutations.clearMessage() }
    }
    LaunchedEffect(sourceState.message) {
        sourceState.message?.let { snackbar.showSnackbar(it); sourceDeletion.clearMessage() }
    }
    LaunchedEffect(ready) {
        viewModel.recoverMissingFocus()
        ready?.structure?.items?.mapTo(hashSetOf()) { it.ref }?.let(mutations::discardMissingClipboard)
    }
    BigPictureSourceDeleteDialog(sourceState, sourceDeletion::confirm, sourceDeletion::cancel)
    BigPictureWorkspaceRenameDialog(renameState, workspaceRename::edit, workspaceRename::confirm, workspaceRename::cancel)
    BackHandler(enabled = ready?.activeNode != null || ready?.activeScope != null) { viewModel.goUp() }
    LaunchedEffect(ready?.activeNode?.placement?.id, ready?.activeScope?.id) {
        listState.scrollToItem(0)
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ProjectHierarchyScreenTopAppBar(
                onBackClick = { navController.popBackStack() },
                showHierarchyBack = ready?.activeNode != null || ready?.activeScope != null,
                onHierarchyBackClick = viewModel::goUp,
                isSelectionMode = false,
                selectedCount = 0,
                canPasteToFocusedContext = BIG_PICTURE_MUTATION_UX_ENABLED && clipboard != null && ready?.structuralDestination() != null && !mutation.busy && !sourceState.busy,
                onCopySelection = {},
                onCutSelection = {},
                onPasteToFocusedContext = { ready?.structuralDestination()?.let(mutations::paste) },
                isSiblingReorderMode = false,
                onToggleSiblingReorderMode = {},
                onSearchClick = {},
                readOnly = true,
                title = "Navigation",
                pasteDescription = "Вставити / перемістити сюди",
                actions = {
                    if (BIG_PICTURE_MUTATION_UX_ENABLED) ready?.let {
                        BigPictureMutationControls(it, mutations, blocked = sourceState.busy || renameState.busy) { destination ->
                            mutations.beginAdd(destination)
                            entry.savedStateHandle[CANONICAL_ENTITY_PICKER_RESULT_KEY] = null
                            entry.savedStateHandle["bigPicturePickerPending"] = true
                            navController.navigate(NavTargetRouter.routeOf(NavTarget.CanonicalEntityPicker(mutations.pickerRequest())))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            BigPictureUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is BigPictureUiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(current.message)
                }
            is BigPictureUiState.Ready ->
                ProjectHierarchyView(
                    state = current,
                    listState = listState,
                    onFocus = viewModel::focusPlacement,
                    onFocusScope = viewModel::focusScope,
                    onOpen = { node ->
                        bigPictureDestination(node)?.let { target ->
                            navController.navigate(NavTargetRouter.routeOf(target))
                        }
                    },
                    onOpenSemantic = { node ->
                        bigPictureSemanticDestination(node)?.let { target ->
                            navController.navigate(NavTargetRouter.routeOf(target))
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                    mutationEnabled = BIG_PICTURE_MUTATION_UX_ENABLED && !mutation.busy && !sourceState.busy && !renameState.busy,
                    onCut = mutations::cut,
                    onRemove = mutations::remove,
                    onRenameSection = mutations::renameSection,
                    onRenameWorkspace = workspaceRename::request,
                    onRemoveSubtree = { node ->
                        viewModel.rememberCurrentLocation()
                        mutations.removeSubtree(node)
                    },
                    sourceDeleteTargets = sourceTargets,
                    onDeleteSource = { target ->
                        viewModel.rememberCurrentLocation()
                        sourceDeletion.request(target)
                    },
                )
        }
    }
}

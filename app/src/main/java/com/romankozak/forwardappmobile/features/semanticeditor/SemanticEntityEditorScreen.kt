package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemanticEntityEditorScreen(
    navController: NavHostController,
    viewModel: SemanticEntityEditorViewModel = hiltViewModel(),
    workspaceCreation: EmbodyingWorkspaceViewModel = hiltViewModel(),
    deletion: SemanticEntityDeleteViewModel = hiltViewModel(),
    conversion: SemanticConversionViewModel = hiltViewModel(),
    kindChange: OrientationKindViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val workspaceState by workspaceCreation.state.collectAsStateWithLifecycle()
    val deleteState by deletion.state.collectAsStateWithLifecycle()
    val conversionState by conversion.state.collectAsStateWithLifecycle()
    val kindState by kindChange.state.collectAsStateWithLifecycle()
    OrientationKindDialog(kindState, kindChange)
    LaunchedEffect(kindState.completed) { if (kindChange.consumeCompleted()) viewModel.load() }
    SemanticConversionDialog(conversionState, conversion)
    LaunchedEffect(conversionState.completed) { if (conversion.consumeCompleted()) viewModel.load() }
    LaunchedEffect(deleteState.deleted) {
        if (deletion.consumeDeleted()) navController.popBackStack()
    }
    SemanticEntityDeleteDialog(deleteState, deletion::confirm, deletion::cancel)
    LaunchedEffect(workspaceState.createdId) {
        workspaceCreation.consumeCreatedId()?.let { id ->
            viewModel.refreshWorkspaceBindings()
            navController.navigate(NavTargetRouter.routeOf(NavTarget.Workspace(id)))
        }
    }
    EmbodyingWorkspaceDialog(workspaceState, workspaceCreation)
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val loaded = state as? SemanticEntityEditorUiState.Loaded
    LaunchedEffect(loaded?.model?.version) { kindChange.refreshEligibility() }
    LaunchedEffect(loaded?.model?.id, loaded?.model?.version) { deletion.prepare(loaded?.model?.id) }
    val editorEntry = remember(navController) { requireNotNull(navController.currentBackStackEntry) }
    DisposableEffect(editorEntry, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshRelations()
        }
        editorEntry.lifecycle.addObserver(observer)
        onDispose { editorEntry.lifecycle.removeObserver(observer) }
    }
    val pickerResult by
        editorEntry.savedStateHandle
            .getStateFlow<List<String>?>(CANONICAL_ENTITY_PICKER_RESULT_KEY, null)
            .collectAsStateWithLifecycle()
    val pendingPickerKind by
        editorEntry.savedStateHandle
            .getStateFlow<String?>(PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY, null)
            .collectAsStateWithLifecycle()

    fun requestBack() {
        if (deleteState.busy || conversionState.busy || kindState.busy) return
        if (loaded?.model?.isDirty == true) confirmDiscard = true else navController.popBackStack()
    }

    BackHandler(onBack = ::requestBack)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SemanticEntityEditorEvent.Saved -> snackbarHostState.showSnackbar("Зміни збережено")
                is SemanticEntityEditorEvent.RelationUpdated -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    LaunchedEffect(pickerResult, pendingPickerKind) {
        consumeSemanticRelationPickerResult(editorEntry.savedStateHandle, viewModel::acceptPickerResult)
    }

    fun openPicker(kind: SemanticRelationPickerKind) {
        val request =
            when (kind) {
                SemanticRelationPickerKind.ASPECT_PARENT -> viewModel.aspectParentPickerRequest()
                SemanticRelationPickerKind.SEMANTIC -> viewModel.semanticPickerRequest()
                SemanticRelationPickerKind.ASPECT -> viewModel.aspectPickerRequest()
                SemanticRelationPickerKind.WORKSPACE -> viewModel.workspacePickerRequest()
                SemanticRelationPickerKind.ORIENTATION -> viewModel.orientationPickerRequest()
            } ?: return
        editorEntry.savedStateHandle[PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY] = kind.name
        navController.navigate(NavTargetRouter.routeOf(NavTarget.CanonicalEntityPicker(request)))
    }

    loaded?.model?.let { model ->
        com.romankozak.forwardappmobile.features.recent.RecordRecentSemanticAccess(model.id, model.persistedName)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор сутності",
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    loaded?.model?.let { model ->
                        EmbodyingWorkspaceAction(model, workspaceState,
                            onDelete = (deletion::request).takeIf { deleteState.target?.subjectId == model.id },
                            deleteLabel = "Видалити ${deleteState.target?.classLabel ?: "сутність"}…",
                            blocked = deleteState.busy || conversionState.busy || kindState.busy,
                            onConvert = (conversion::open).takeIf { conversionState.eligible },
                            onChangeKind = (kindChange::open).takeIf { kindState.eligible },
                        )
                        IconButton(onClick = viewModel::save, enabled = model.canSave && !workspaceState.busy && !deleteState.busy && !conversionState.busy && !kindState.busy) {
                            if (model.isSaving) {
                                CircularProgressIndicator()
                            } else {
                                Icon(Icons.Default.Save, contentDescription = "Зберегти")
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            SemanticEntityEditorUiState.Loading -> LoadingContent(padding)
            is SemanticEntityEditorUiState.Unavailable -> UnavailableContent(current.message, padding)
            is SemanticEntityEditorUiState.Loaded ->
                SemanticEditorContent(
                    model = if (deleteState.busy || conversionState.busy || kindState.busy) current.model.copy(isSaving = true, isRelationMutationInFlight = true) else current.model,
                    padding = padding,
                    onNameChange = viewModel::setName,
                    onOpenPlanning = { navController.navigate(NavTargetRouter.routeOf(NavTarget.OrientationPlanning(current.model.id))) },
                    onAddSemantic = { openPicker(SemanticRelationPickerKind.SEMANTIC) },
                    onChooseAspectParent = { openPicker(SemanticRelationPickerKind.ASPECT_PARENT) },
                    onMakeAspectRoot = viewModel::requestAspectRoot,
                    onOpenSemantic = { id -> navController.navigate(NavTargetRouter.routeOf(NavTarget.SemanticEntityEditor(id))) },
                    onRemoveAspect = viewModel::removeAspectAssociation,
                    onAddWorkspace = { openPicker(SemanticRelationPickerKind.WORKSPACE) },
                    onRemoveWorkspace = viewModel::removeWorkspaceBinding,
                    onOpenWorkspace = { binding -> navController.navigate(NavTargetRouter.routeOf(binding.destination())) },
                    onRemoveOrientation = viewModel::removeOrientationRelation,
                    onCreateWorkspace = { workspaceCreation.open(current.model) },
                    workspaceCreationBusy = workspaceState.busy || deleteState.busy || conversionState.busy || kindState.busy,
                )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Відкинути зміни?") },
            text = { Text("Незбережена назва буде втрачена.") },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) { Text("Відкинути") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Продовжити редагування") }
            },
        )
    }

    loaded?.model?.let { model ->
        (model.pendingRelation as? PendingSemanticRelation.AspectParent)?.let { pending ->
            AspectParentConfirmation(model, pending, viewModel::cancelPendingRelation, viewModel::confirmAspectParent)
        }
    }
    SemanticRelationTypeDialog(
        pending = loaded?.model?.pendingRelation,
        currentTitle = loaded?.model?.let { it.name.ifBlank { it.persistedName } } ?: "Поточна сутність",
        relatedTitles = loaded?.model?.relationTitles.orEmpty(),
        mutationInFlight = loaded?.model?.isRelationMutationInFlight == true,
        onDismiss = viewModel::cancelPendingRelation,
        onAddAspect = viewModel::addAspectAssociation,
        onAddWorkspace = viewModel::addWorkspaceBinding,
        onAddOrientation = viewModel::addOrientationRelation,
    )
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun UnavailableContent(message: String, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}


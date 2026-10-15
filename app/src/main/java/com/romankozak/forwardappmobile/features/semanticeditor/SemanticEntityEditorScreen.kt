package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semantic.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemanticEntityEditorScreen(
    navController: NavHostController,
    viewModel: SemanticEntityEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val loaded = state as? SemanticEntityEditorUiState.Loaded
    val editorEntry = remember(navController) { requireNotNull(navController.currentBackStackEntry) }
    val pickerResult by
        editorEntry.savedStateHandle
            .getStateFlow<List<String>?>(CANONICAL_ENTITY_PICKER_RESULT_KEY, null)
            .collectAsStateWithLifecycle()
    val pendingPickerKind by
        editorEntry.savedStateHandle
            .getStateFlow<String?>(PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY, null)
            .collectAsStateWithLifecycle()

    fun requestBack() {
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
                SemanticRelationPickerKind.ASPECT -> viewModel.aspectPickerRequest()
                SemanticRelationPickerKind.WORKSPACE -> viewModel.workspacePickerRequest()
                SemanticRelationPickerKind.ORIENTATION -> viewModel.orientationPickerRequest()
            } ?: return
        editorEntry.savedStateHandle[PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY] = kind.name
        navController.navigate(NavTargetRouter.routeOf(NavTarget.CanonicalEntityPicker(request)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Семантична сутність") },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    loaded?.model?.let { model ->
                        IconButton(onClick = viewModel::save, enabled = model.canSave) {
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
                EditorContent(
                    model = current.model,
                    padding = padding,
                    onNameChange = viewModel::setName,
                    onAddAspect = { openPicker(SemanticRelationPickerKind.ASPECT) },
                    onRemoveAspect = viewModel::removeAspectAssociation,
                    onAddWorkspace = { openPicker(SemanticRelationPickerKind.WORKSPACE) },
                    onRemoveWorkspace = viewModel::removeWorkspaceBinding,
                    onAddOrientation = { openPicker(SemanticRelationPickerKind.ORIENTATION) },
                    onRemoveOrientation = viewModel::removeOrientationRelation,
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

    SemanticRelationTypeDialog(
        pending = loaded?.model?.pendingRelation,
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

@Composable
private fun EditorContent(
    model: SemanticEntityEditorModel,
    padding: PaddingValues,
    onNameChange: (String) -> Unit,
    onAddAspect: () -> Unit,
    onRemoveAspect: (String) -> Unit,
    onAddWorkspace: () -> Unit,
    onRemoveWorkspace: (String) -> Unit,
    onAddOrientation: () -> Unit,
    onRemoveOrientation: (String, Long) -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SemanticIdentitySection(model, onNameChange)
        if (model.entityClass == SemanticEntityClass.ORIENTATION) {
            OrientationTypeSection(model)
        }
        HorizontalDivider()
        if (model.canEditAspects) {
            AspectAssociationsSection(model, onAddAspect, onRemoveAspect)
        }
        WorkspaceBindingsSection(model, onAddWorkspace, onRemoveWorkspace)
        if (model.canEditOrientationRelations) {
            OrientationGraphRelationsSection(model, onAddOrientation, onRemoveOrientation)
        }
        model.errorMessage?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SemanticIdentitySection(model: SemanticEntityEditorModel, onNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = model.name,
            onValueChange = onNameChange,
            label = { Text("Назва") },
            singleLine = true,
            enabled = !model.isSaving,
            isError = model.name.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Сутність: ${model.entityClass.displayLabel()}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OrientationTypeSection(model: SemanticEntityEditorModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Тип орієнтира", style = MaterialTheme.typography.labelLarge)
        Text(
            model.orientationKind?.displayLabel().orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Тип незмінний після створення", style = MaterialTheme.typography.bodySmall)
    }
}



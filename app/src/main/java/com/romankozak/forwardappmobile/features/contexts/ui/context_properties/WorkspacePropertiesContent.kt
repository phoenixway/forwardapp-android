package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkspacePropertiesContent(
    navController: NavController,
    viewModel: WorkspacePropertiesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val entry = remember(navController) { requireNotNull(navController.currentBackStackEntry) }
    val pickerResult by entry.savedStateHandle
        .getStateFlow<List<String>?>(CANONICAL_ENTITY_PICKER_RESULT_KEY, null)
        .collectAsStateWithLifecycle()
    val pickerPending by entry.savedStateHandle
        .getStateFlow(PENDING_WORKSPACE_BINDING_PICKER_KEY, false)
        .collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    val model = (state as? WorkspacePropertiesUiState.Ready)?.model

    fun requestBack() {
        if (model?.isDirty == true) confirmDiscard = true else navController.popBackStack()
    }

    BackHandler(onBack = ::requestBack)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WorkspacePropertiesEvent.Saved ->
                    snackbar.showSnackbar("Workspace збережено")
                is WorkspacePropertiesEvent.Message ->
                    snackbar.showSnackbar(event.text)
                is WorkspacePropertiesEvent.OpenLegacyContextSettings ->
                    navController.navigate(
                        NavTargetRouter.routeOf(
                            NavTarget.ProjectSettings(projectId = event.contextId),
                        ),
                    )
            }
        }
    }
    LaunchedEffect(pickerResult, pickerPending) {
        consumeWorkspaceBindingPickerResult(entry.savedStateHandle, viewModel::acceptPickerResult)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Властивості Workspace") },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    model?.let {
                        IconButton(onClick = viewModel::save, enabled = it.canSave) {
                            if (it.isSaving) CircularProgressIndicator()
                            else Icon(Icons.Default.Save, contentDescription = "Зберегти")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            WorkspacePropertiesUiState.Loading ->
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

            is WorkspacePropertiesUiState.Unavailable ->
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { Text(current.message) }

            is WorkspacePropertiesUiState.Ready ->
                WorkspacePropertiesForm(
                    model = current.model,
                    onNameChange = viewModel::onNameChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onAdd = {
                        entry.savedStateHandle[PENDING_WORKSPACE_BINDING_PICKER_KEY] = true
                        navController.navigate(
                            NavTargetRouter.routeOf(
                                NavTarget.CanonicalEntityPicker(viewModel.semanticEntityPickerRequest()),
                            ),
                        )
                    },
                    onRemove = viewModel::removeBinding,
                    onOpenLegacySettings = viewModel::openLegacyContextSettings,
                )
        }
    }

    WorkspaceBindingTypeDialog(
        subjectKey = model?.pendingSubject?.id,
        inFlight = model?.isRelationMutationInFlight == true,
        onDismiss = viewModel::dismissPendingBinding,
        onConfirm = viewModel::addBinding,
    )
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Відкинути зміни?") },
            text = { Text("Незбережені властивості Workspace буде втрачено.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; navController.popBackStack() }) {
                    Text("Відкинути")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Скасувати") } },
        )
    }
}

@Composable
private fun WorkspacePropertiesForm(
    model: WorkspacePropertiesModel,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onOpenLegacySettings: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Workspace", style = MaterialTheme.typography.titleMedium)
        if (model.isSystem) Text("Системний Workspace", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = model.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Назва") },
            isError = model.name.isBlank(),
            singleLine = true,
        )
        OutlinedTextField(
            value = model.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Опис") },
            minLines = 3,
        )
        model.roleCode?.let { Text("Роль: $it", style = MaterialTheme.typography.bodySmall) }
        if (!model.isSystem && model.sourceContextId == model.workspaceId) {
            Text(
                "Окремі операційні налаштування пов’язаного Context",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onOpenLegacySettings,
                enabled = !model.isDirty && !model.isSaving && !model.isRelationMutationInFlight,
            ) {
                Text("Налаштування Context (сумісність)")
            }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Семантичні зв’язки", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onAdd, enabled = !model.isRelationMutationInFlight) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Додати")
            }
        }
        if (model.bindings.isEmpty()) {
            Text("Немає зв’язків", style = MaterialTheme.typography.bodyMedium)
        } else {
            model.bindings.forEach { binding ->
                ListItem(
                    headlineContent = { Text(binding.title) },
                    supportingContent = {
                        Text(
                            "${binding.entityClass.displayLabel()} · ${binding.bindingType.displayLabel()}" +
                                if (binding.isPrimary) " · основний" else "",
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { onRemove(binding.bindingId) },
                            enabled = !model.isRelationMutationInFlight,
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити зв’язок")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WorkspaceBindingTypeDialog(
    subjectKey: String?,
    inFlight: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (WorkspaceBindingType) -> Unit,
) {
    if (subjectKey == null) return
    var selected by remember(subjectKey) { mutableStateOf<WorkspaceBindingType?>(null) }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        title = { Text("Тип зв’язку") },
        text = {
            Column {
                WorkspaceBindingType.entries.forEach { type ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !inFlight) { selected = type },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == type, onClick = null)
                        Text(type.displayLabel())
                    }
                }
                if (selected == WorkspaceBindingType.EMBODIES) {
                    Text("EMBODIES є основним і унікальним для сутності та Workspace")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null && !inFlight,
            ) { Text("Додати") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !inFlight) { Text("Скасувати") } },
    )
}

private fun WorkspaceBindingType.displayLabel() =
    when (this) {
        WorkspaceBindingType.EMBODIES -> "Втілює"
        WorkspaceBindingType.REALIZES -> "Реалізує"
        WorkspaceBindingType.SUPPORTS -> "Підтримує"
        WorkspaceBindingType.MONITORS -> "Моніторить"
    }

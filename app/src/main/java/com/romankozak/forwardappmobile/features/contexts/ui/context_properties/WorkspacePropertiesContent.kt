package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.workspace.WorkspaceSurfaceCopy
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreenActions
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreenState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.DisplayTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabContent

internal val workspacePropertiesBaseTabs = listOf("general", "semantics", "display", "features", "reminders")

/** The existing tabbed properties shell, with separate canonical command owners. */
@Composable
internal fun WorkspacePropertiesContent(
    navController: NavController,
    viewModel: WorkspacePropertiesViewModel = hiltViewModel(),
    operations: WorkspaceOperationalSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val operational by operations.state.collectAsStateWithLifecycle()
    val selectedKey by operations.selectedTab.collectAsStateWithLifecycle()
    val model = (state as? WorkspacePropertiesUiState.Ready)?.model
    val snackbar = remember { SnackbarHostState() }
    val entry = remember(navController) { requireNotNull(navController.currentBackStackEntry) }
    val pickerResult by entry.savedStateHandle.getStateFlow<List<String>?>(CANONICAL_ENTITY_PICKER_RESULT_KEY, null)
        .collectAsStateWithLifecycle()
    val pickerPending by entry.savedStateHandle.getStateFlow(PENDING_WORKSPACE_BINDING_PICKER_KEY, false)
        .collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    fun requestBack() {
        if (model?.isDirty == true) confirmDiscard = true else navController.popBackStack()
    }
    BackHandler(onBack = ::requestBack)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WorkspacePropertiesEvent.Saved -> snackbar.showSnackbar("Робочий простір збережено")
                is WorkspacePropertiesEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }
    LaunchedEffect(pickerResult, pickerPending) {
        consumeWorkspaceBindingPickerResult(entry.savedStateHandle, viewModel::acceptPickerResult)
    }
    LaunchedEffect(operational.error) {
        operational.error?.let { snackbar.showSnackbar(it) }
    }
    val capabilityTabs = operations.capabilityTabs()
    val keys = workspacePropertiesBaseTabs + capabilityTabs.map { "cap:${it.descriptor.id}" }
    val selected = keys.indexOf(selectedKey).takeIf { it >= 0 } ?: 0
    val labels = listOf("Загальне", "Семантичні зв’язки", "Відображення", "Можливості", "Нагадування") +
        capabilityTabs.map { it.descriptor.tabTitle }
    val icons = listOf(Icons.Default.Settings, Icons.Default.Link, Icons.Default.Style, Icons.Default.Build,
        Icons.Default.Notifications) + List(capabilityTabs.size) { Icons.Default.Build }
    SettingsScreen(
        state = SettingsScreenState(WorkspaceSurfaceCopy.PROPERTIES, labels, icons, selected,
            keys[selected] == "general" && model?.canSave == true),
        actions = SettingsScreenActions(onTabSelected = { operations.selectTab(keys[it]) }, onSave = viewModel::save),
        navController = navController,
        onBack = ::requestBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { index ->
        val key = keys.getOrNull(index) ?: "general" // An outgoing AnimatedContent tab may have just been disabled.
        when (val current = state) {
            WorkspacePropertiesUiState.Loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            is WorkspacePropertiesUiState.Unavailable -> Text(current.message, Modifier.padding(24.dp))
            is WorkspacePropertiesUiState.Ready -> when (key) {
                "general" -> WorkspaceIdentityForm(current.model, viewModel::onNameChange, viewModel::onDescriptionChange,
                    operational.tags, operations::addTag, operations::removeTag)
                "semantics" -> WorkspaceSemanticLinks(current.model, onAdd = {
                    entry.savedStateHandle[PENDING_WORKSPACE_BINDING_PICKER_KEY] = true
                    navController.navigate(NavTargetRouter.routeOf(NavTarget.CanonicalEntityPicker(viewModel.semanticEntityPickerRequest())))
                }, onRemove = viewModel::removeBinding)
                else -> if (!operational.ready) CircularProgressIndicator(Modifier.padding(24.dp))
                else if (!operational.available) Text(operational.error ?: "Workspace недоступний", Modifier.padding(24.dp))
                else Column {
                    operational.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                    // These actions save independently; the header Save owns identity fields only.
                    when (key) {
                        "display" -> DisplayTabContent(operational.showCheckboxes, operations::setCheckboxes, !operational.busy)
                        "features" -> WorkspaceFeaturesContent(operational, operations::setCapability)
                        "reminders" -> if (operational.busy) CircularProgressIndicator(Modifier.padding(24.dp))
                            else RemindersTabContent(operational.reminderTime, operations)
                        else -> capabilityTabs.getOrNull(index - workspacePropertiesBaseTabs.size)?.let { tab ->
                            Column(Modifier.padding(16.dp)) { tab.Content(current.model.workspaceId) }
                        }
                    }
                }
            }
        }
    }
    WorkspaceBindingTypeDialog(model?.pendingSubject?.id, model?.isRelationMutationInFlight == true,
        viewModel::dismissPendingBinding, viewModel::addBinding)
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false }, title = { Text("Відкинути зміни?") },
        text = { Text("Незбережені назву й опис буде втрачено. Операційні зміни зберігаються окремо.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; navController.popBackStack() }) { Text("Відкинути") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Скасувати") } },
    )
}

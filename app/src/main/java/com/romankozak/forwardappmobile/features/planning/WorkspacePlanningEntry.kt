package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.workspace.*
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

internal fun workspacePlanningChoices(workspaceId: String, bindings: List<WorkspaceBindingEntity>, catalog: List<CanonicalEntitySummary>): List<WorkspaceLinkedSemanticEntity> {
    if (catalog.none { it.ref == CanonicalEntityRef.Workspace(workspaceId) }) return emptyList()
    return projectWorkspaceSemanticLinks(bindings.filter { it.workspaceId == workspaceId }, catalog)
        .filter { it.isNavigable && it.ref is CanonicalEntityRef.Orientation }
        .sortedWith(compareBy({ "EMBODIES" !in it.bindingTypes }, { it.title }, { it.ref?.id }))
}

@HiltViewModel
class WorkspacePlanningEntryViewModel @Inject constructor(saved: SavedStateHandle, bindings: CanonicalWorkspaceBindingRepository,
    catalog: CanonicalEntityCatalogRepository) : ViewModel() {
    private val workspaceId = saved.get<String>("workspaceId").orEmpty()
    internal val choices = (if (workspaceId.isBlank()) flowOf(emptyList()) else
        combine(bindings.observeBindingsForWorkspace(workspaceId), catalog.observeCatalog()) { rows, entities ->
            workspacePlanningChoices(workspaceId, rows, entities)
        }).catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun WorkspacePlanningEntry(onOpen: (NavTarget) -> Unit, model: WorkspacePlanningEntryViewModel = hiltViewModel()) {
    val choices by model.choices.collectAsStateWithLifecycle()
    WorkspacePlanningEntryContent(choices, onOpen)
}

@Composable
internal fun WorkspacePlanningEntryContent(choices: List<WorkspaceLinkedSemanticEntity>, onOpen: (NavTarget) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    if (choices.isEmpty()) return
    TextButton(onClick = {
        if (choices.size == 1) choices.single().ref?.let(::orientationPlanningTarget)?.let(onOpen)
        else choosing = true
    }) { Text("Planning") }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Planning for…") }, text = {
        Column { choices.forEach { choice ->
            TextButton(onClick = { choosing = false; choice.ref?.let(::orientationPlanningTarget)?.let(onOpen) }) {
                Column { Text(choice.title); Text(choice.bindingTypes.joinToString(" · "), style = MaterialTheme.typography.labelSmall) }
            }
        } }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = { choosing = false }) { Text("Скасувати") } })
}

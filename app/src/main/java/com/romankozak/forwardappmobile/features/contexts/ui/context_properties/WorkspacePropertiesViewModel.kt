package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjectionRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspacePropertiesSnapshot
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityPickerRequest
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntitySelectionMode
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class WorkspaceBindingPresentation(
    val bindingId: String,
    val subjectId: String,
    val title: String,
    val entityClass: CanonicalEntityClass,
    val bindingType: WorkspaceBindingType,
    val isPrimary: Boolean,
    val version: Long,
)

data class WorkspacePropertiesModel(
    val workspaceId: String,
    val name: String,
    val description: String,
    val persistedName: String,
    val persistedDescription: String,
    val roleCode: String?,
    val sourceContextId: String?,
    val isSystem: Boolean,
    val version: Long,
    val bindings: List<WorkspaceBindingPresentation>,
    val pendingSubject: CanonicalEntityRef? = null,
    val isSaving: Boolean = false,
    val isRelationMutationInFlight: Boolean = false,
) {
    val isDirty: Boolean = name != persistedName || description != persistedDescription
    val canSave: Boolean = name.isNotBlank() && isDirty && !isSaving
}

sealed interface WorkspacePropertiesUiState {
    data object Loading : WorkspacePropertiesUiState
    data class Ready(val model: WorkspacePropertiesModel) : WorkspacePropertiesUiState
    data class Unavailable(val message: String) : WorkspacePropertiesUiState
}

sealed interface WorkspacePropertiesEvent {
    data object Saved : WorkspacePropertiesEvent
    data class OpenLegacyContextSettings(val contextId: String) : WorkspacePropertiesEvent
    data class Message(val text: String) : WorkspacePropertiesEvent
}

@HiltViewModel
class WorkspacePropertiesViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val workspaceRepository: CanonicalWorkspaceRepository,
        private val compatibilityRepository: WorkspaceCompatibilityProjectionRepository,
        private val bindingRepository: CanonicalWorkspaceBindingRepository,
        private val catalogRepository: CanonicalEntityCatalogRepository,
    ) : ViewModel() {
        private val workspaceId = savedStateHandle.get<String>("workspaceId").orEmpty()
        private val _uiState = MutableStateFlow<WorkspacePropertiesUiState>(WorkspacePropertiesUiState.Loading)
        val uiState: StateFlow<WorkspacePropertiesUiState> = _uiState.asStateFlow()

        private val _events = Channel<WorkspacePropertiesEvent>()
        val events = _events.receiveAsFlow()

        init {
            if (workspaceId.isBlank()) {
                _uiState.value = WorkspacePropertiesUiState.Unavailable("Некоректний ідентифікатор робочого простору")
            } else {
                observeWorkspace()
            }
        }

        fun semanticEntityPickerRequest() =
            CanonicalEntityPickerRequest(
                allowedClasses = setOf(CanonicalEntityClass.ORIENTATION, CanonicalEntityClass.ASPECT),
                selectionMode = CanonicalEntitySelectionMode.SINGLE,
            )

        fun onNameChange(value: String) = updateReady { copy(name = value) }

        fun onDescriptionChange(value: String) = updateReady { copy(description = value) }

        fun acceptPickerResult(refs: Set<CanonicalEntityRef>) {
            val ref = refs.singleOrNull() ?: return
            require(ref.entityClass == CanonicalEntityClass.ORIENTATION || ref.entityClass == CanonicalEntityClass.ASPECT) {
                "Workspace binding picker returned an unsupported entity class"
            }
            updateReady { copy(pendingSubject = ref) }
        }

        fun dismissPendingBinding() = updateReady { copy(pendingSubject = null) }

        fun openLegacyContextSettings() {
            val model = readyModel() ?: return
            val contextId = model.sourceContextId ?: return
            if (model.isSystem || contextId != model.workspaceId || model.isDirty || model.isSaving || model.isRelationMutationInFlight) return

            viewModelScope.launch {
                val admissible =
                    runCatching { compatibilityRepository.get(model.workspaceId) }
                        .getOrNull()
                        ?.let { projection ->
                            !projection.workspace.isDeleted &&
                                projection.workspace.id == model.workspaceId &&
                                projection.workspace.sourceContextId == contextId &&
                                projection.sourceContext?.let {
                                    !it.isDeleted && it.id == contextId
                                } == true
                        } == true

                // Admission must still hold after the suspending repository read.
                val current = readyModel() ?: return@launch
                if (
                    current.workspaceId != model.workspaceId ||
                    current.sourceContextId != contextId ||
                    current.version != model.version ||
                    current.isSystem ||
                    current.isDirty ||
                    current.isSaving ||
                    current.isRelationMutationInFlight
                ) {
                    return@launch
                }

                if (admissible) {
                    _events.send(WorkspacePropertiesEvent.OpenLegacyContextSettings(contextId))
                } else {
                    _events.send(WorkspacePropertiesEvent.Message("Пов’язаний Context недоступний"))
                }
            }
        }

        fun save() {
            val model = readyModel() ?: return
            if (!model.canSave) return
            updateReady { copy(isSaving = true) }
            viewModelScope.launch {
                runCatching {
                    workspaceRepository.updatePropertiesPresentation(
                        id = model.workspaceId,
                        name = model.name,
                        description = model.description,
                    )
                }.onSuccess {
                    _events.send(WorkspacePropertiesEvent.Saved)
                }.onFailure {
                    updateReady { copy(isSaving = false) }
                    _events.send(WorkspacePropertiesEvent.Message(it.message ?: "Не вдалося зберегти Workspace"))
                }
            }
        }

        fun addBinding(type: WorkspaceBindingType) {
            val model = readyModel() ?: return
            val subject = model.pendingSubject ?: return
            if (model.isRelationMutationInFlight) return
            updateReady { copy(isRelationMutationInFlight = true) }
            viewModelScope.launch {
                runCatching {
                    bindingRepository.bind(subject.id, model.workspaceId, type)
                }.onSuccess {
                    updateReady { copy(pendingSubject = null, isRelationMutationInFlight = false) }
                    _events.send(WorkspacePropertiesEvent.Message("Семантичний зв’язок додано"))
                }.onFailure {
                    updateReady { copy(isRelationMutationInFlight = false) }
                    _events.send(WorkspacePropertiesEvent.Message(it.message ?: "Не вдалося додати зв’язок"))
                }
            }
        }

        fun removeBinding(bindingId: String) {
            val model = readyModel() ?: return
            if (model.isRelationMutationInFlight) return
            updateReady { copy(isRelationMutationInFlight = true) }
            viewModelScope.launch {
                runCatching { bindingRepository.unbind(bindingId) }
                    .onSuccess {
                        updateReady { copy(isRelationMutationInFlight = false) }
                        _events.send(WorkspacePropertiesEvent.Message("Семантичний зв’язок видалено"))
                    }.onFailure {
                        updateReady { copy(isRelationMutationInFlight = false) }
                        _events.send(WorkspacePropertiesEvent.Message(it.message ?: "Не вдалося видалити зв’язок"))
                    }
            }
        }

        private fun observeWorkspace() {
            viewModelScope.launch {
                combine(
                    workspaceRepository.observeProperties(workspaceId),
                    bindingRepository.observeBindingsForWorkspace(workspaceId),
                    catalogRepository.observeCatalog(),
                    ::CatalogFrame,
                ).catch {
                    _uiState.value = WorkspacePropertiesUiState.Unavailable("Не вдалося завантажити Workspace")
                }.collect { frame ->
                    val workspace = frame.workspace
                    if (workspace == null) {
                        _uiState.value = WorkspacePropertiesUiState.Unavailable("Робочий простір недоступний")
                    } else {
                        _uiState.value =
                            try {
                                WorkspacePropertiesUiState.Ready(frame.toModel(readyModel()))
                            } catch (_: IllegalArgumentException) {
                                WorkspacePropertiesUiState.Unavailable(
                                    "Некоректний канонічний семантичний зв’язок Workspace",
                                )
                            }
                    }
                }
            }
        }

        private fun readyModel() = (_uiState.value as? WorkspacePropertiesUiState.Ready)?.model

        private fun updateReady(transform: WorkspacePropertiesModel.() -> WorkspacePropertiesModel) {
            val current = _uiState.value as? WorkspacePropertiesUiState.Ready ?: return
            _uiState.value = WorkspacePropertiesUiState.Ready(current.model.transform())
        }
    }

private data class CatalogFrame(
    val workspace: WorkspacePropertiesSnapshot?,
    val bindings: List<WorkspaceBindingEntity>,
    val catalog: List<CanonicalEntitySummary>,
)

private fun CatalogFrame.toModel(previous: WorkspacePropertiesModel?): WorkspacePropertiesModel {
    val workspace = requireNotNull(workspace)
    val preserveDraft = previous?.version == workspace.version
    val summaries = catalog.filterNot { it.ref is CanonicalEntityRef.Workspace }.associateBy { it.ref.id }
    return WorkspacePropertiesModel(
        workspaceId = workspace.id,
        name = if (preserveDraft) requireNotNull(previous).name else workspace.name,
        description = if (preserveDraft) requireNotNull(previous).description else workspace.description.orEmpty(),
        persistedName = workspace.name,
        persistedDescription = workspace.description.orEmpty(),
        roleCode = workspace.roleCode,
        sourceContextId = workspace.sourceContextId,
        isSystem = workspace.isSystem,
        version = workspace.version,
        bindings = bindings.map { it.toPresentation(summaries) },
        pendingSubject = previous?.pendingSubject,
        isSaving = if (preserveDraft) previous?.isSaving == true else false,
        isRelationMutationInFlight = previous?.isRelationMutationInFlight ?: false,
    )
}

private fun WorkspaceBindingEntity.toPresentation(
    summaries: Map<String, CanonicalEntitySummary>,
): WorkspaceBindingPresentation {
    val summary = requireNotNull(summaries[subjectId]) {
        "Workspace binding target is not an admitted canonical semantic entity: $subjectId"
    }
    return WorkspaceBindingPresentation(
        bindingId = id,
        subjectId = subjectId,
        title = summary.title,
        entityClass = summary.ref.entityClass,
        bindingType = WorkspaceBindingType.valueOf(bindingType),
        isPrimary = isPrimary,
        version = version,
    )
}

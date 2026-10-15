package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectLinksRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityPickerRequest
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntitySelectionMode
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class AspectAssociationItem(
    val refId: String,
    val aspectId: String,
    val title: String,
    val relationType: AspectOrientationRelationType,
    val isPrimary: Boolean,
)

enum class OrientationRelationEditDirection {
    OUTGOING,
    INCOMING,
}

data class OrientationGraphRelationItem(
    val relationId: String,
    val otherOrientationId: String,
    val title: String,
    val relationType: OrientationRelationType,
    val direction: OrientationRelationEditDirection,
    val version: Long,
) {
    val isEditable: Boolean get() = relationType != OrientationRelationType.PART_OF
}

data class WorkspaceBindingItem(
    val bindingId: String,
    val workspaceId: String,
    val title: String,
    val bindingType: WorkspaceBindingType,
    val isPrimary: Boolean,
)

sealed interface PendingSemanticRelation {
    data class Aspect(val aspectId: String) : PendingSemanticRelation

    data class Workspace(val workspaceId: String) : PendingSemanticRelation

    data class Orientation(val orientationId: String) : PendingSemanticRelation
}

enum class SemanticRelationPickerKind { ASPECT, WORKSPACE, ORIENTATION }

data class SemanticEntityEditorModel(
    val id: String,
    val entityClass: SemanticEntityClass,
    val name: String,
    val persistedName: String,
    val description: String?,
    val version: Long,
    val orientationKind: OrientationKind? = null,
    val aspectAssociations: List<AspectAssociationItem> = emptyList(),
    val workspaceBindings: List<WorkspaceBindingItem> = emptyList(),
    val orientationRelations: List<OrientationGraphRelationItem> = emptyList(),
    val pendingRelation: PendingSemanticRelation? = null,
    val isSaving: Boolean = false,
    val isRelationMutationInFlight: Boolean = false,
    val errorMessage: String? = null,
) {
    val isDirty: Boolean get() = name != persistedName
    val canSave: Boolean get() = name.isNotBlank() && isDirty && !isSaving && !isRelationMutationInFlight
    val canEditAspects: Boolean get() = entityClass == SemanticEntityClass.ORIENTATION
    val canEditOrientationRelations: Boolean get() = entityClass == SemanticEntityClass.ORIENTATION
}

sealed interface SemanticEntityEditorUiState {
    data object Loading : SemanticEntityEditorUiState
    data class Loaded(val model: SemanticEntityEditorModel) : SemanticEntityEditorUiState
    data class Unavailable(val message: String) : SemanticEntityEditorUiState
}

sealed interface SemanticEntityEditorEvent {
    data object Saved : SemanticEntityEditorEvent
    data class RelationUpdated(val message: String) : SemanticEntityEditorEvent
}

@HiltViewModel
class SemanticEntityEditorViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val orientationRepository: CanonicalOrientationRepository,
        private val aspectRepository: CanonicalAspectRepository,
        private val aspectLinksRepository: CanonicalAspectLinksRepository,
        private val workspaceBindingRepository: CanonicalWorkspaceBindingRepository,
        private val graphRepository: CanonicalOrientationGraphRepository,
        private val catalogRepository: CanonicalEntityCatalogRepository,
    ) : ViewModel() {
        private val subjectId = savedStateHandle.get<String>(SUBJECT_ID_ARGUMENT).orEmpty()
        private val _uiState = MutableStateFlow<SemanticEntityEditorUiState>(SemanticEntityEditorUiState.Loading)
        val uiState = _uiState.asStateFlow()
        private val _events = MutableSharedFlow<SemanticEntityEditorEvent>(extraBufferCapacity = 1)
        val events = _events.asSharedFlow()

        init { load() }

        fun setName(name: String) = updateModel { copy(name = name, errorMessage = null) }

        fun load() {
            if (subjectId.isBlank()) {
                _uiState.value = SemanticEntityEditorUiState.Unavailable("Семантичну сутність не вказано")
                return
            }
            _uiState.value = SemanticEntityEditorUiState.Loading
            viewModelScope.launch {
                runCatching { loadModel() }.onSuccess(::publishLoaded).onFailure(::publishUnavailable)
            }
        }

        fun save() {
            val request = loadedModel() ?: return
            if (request.name.isBlank()) {
                updateModel { copy(errorMessage = "Назва не може бути порожньою") }
                return
            }
            if (!request.canSave) return
            updateModel { copy(isSaving = true, errorMessage = null) }
            viewModelScope.launch {
                runCatching {
                    when (request.entityClass) {
                        SemanticEntityClass.ORIENTATION -> orientationRepository.updateOrientationSemantics(
                            request.id, request.name, request.description, request.version,
                        )
                        SemanticEntityClass.ASPECT -> aspectRepository.updateDetails(
                            request.id, request.name, request.description, request.version,
                        )
                    }
                }.onSuccess {
                    _events.emit(SemanticEntityEditorEvent.Saved)
                    publishLoaded(loadModel())
                }.onFailure { failure -> publishMutationFailure(request, failure) }
            }
        }

        fun aspectPickerRequest(): CanonicalEntityPickerRequest? {
            val model = loadedModel()?.takeIf(SemanticEntityEditorModel::canEditAspects) ?: return null
            return CanonicalEntityPickerRequest(
                allowedClasses = setOf(CanonicalEntityClass.ASPECT),
                excludedRefs = model.aspectAssociations.mapTo(hashSetOf()) { CanonicalEntityRef.Aspect(it.aspectId) },
                selectionMode = CanonicalEntitySelectionMode.SINGLE,
            )
        }

        fun orientationPickerRequest(): CanonicalEntityPickerRequest? {
            val model =
                loadedModel()?.takeIf(SemanticEntityEditorModel::canEditOrientationRelations)
                    ?: return null
            if (model.isRelationMutationInFlight || model.isSaving) return null
            return CanonicalEntityPickerRequest(
                allowedClasses = setOf(CanonicalEntityClass.ORIENTATION),
                excludedRefs = setOf(CanonicalEntityRef.Orientation(model.id)),
                selectionMode = CanonicalEntitySelectionMode.SINGLE,
            )
        }

        fun workspacePickerRequest(): CanonicalEntityPickerRequest? {
            val model = loadedModel() ?: return null
            return CanonicalEntityPickerRequest(
                allowedClasses = setOf(CanonicalEntityClass.WORKSPACE),
                excludedRefs = model.workspaceBindings.mapTo(hashSetOf()) { CanonicalEntityRef.Workspace(it.workspaceId) },
                selectionMode = CanonicalEntitySelectionMode.SINGLE,
            )
        }

        fun acceptPickerResult(kind: SemanticRelationPickerKind, refs: Set<CanonicalEntityRef>) {
            val ref = refs.singleOrNull() ?: return relationError("Пікер має повернути одну сутність")
            when (kind) {
                SemanticRelationPickerKind.ASPECT -> {
                    if (ref !is CanonicalEntityRef.Aspect || loadedModel()?.canEditAspects != true) {
                        return relationError("Неприпустимий Aspect для цієї сутності")
                    }
                    updateModel { copy(pendingRelation = PendingSemanticRelation.Aspect(ref.id), errorMessage = null) }
                }
                SemanticRelationPickerKind.WORKSPACE -> {
                    if (ref !is CanonicalEntityRef.Workspace) {
                        return relationError("Неприпустимий Workspace для цієї сутності")
                    }
                    updateModel { copy(pendingRelation = PendingSemanticRelation.Workspace(ref.id), errorMessage = null) }
                }
                SemanticRelationPickerKind.ORIENTATION -> {
                    val model = loadedModel()
                    if (
                        ref !is CanonicalEntityRef.Orientation ||
                            model?.canEditOrientationRelations != true ||
                            model.id == ref.id
                    ) {
                        return relationError("Неприпустимий Orientation для графового зв’язку")
                    }
                    updateModel {
                        copy(pendingRelation = PendingSemanticRelation.Orientation(ref.id), errorMessage = null)
                    }
                }
            }
        }

        fun cancelPendingRelation() = updateModel { copy(pendingRelation = null) }

        fun addAspectAssociation(type: AspectOrientationRelationType, makePrimary: Boolean) {
            val model = loadedModel() ?: return
            val pending = model.pendingRelation as? PendingSemanticRelation.Aspect ?: return
            if (makePrimary && type != AspectOrientationRelationType.BELONGS_TO) {
                return relationError("Лише BELONGS_TO може бути основним Aspect")
            }
            mutateRelation("Зв’язок з Aspect додано") {
                aspectLinksRepository.linkOrientation(pending.aspectId, model.id, type, makePrimary)
            }
        }

        fun removeAspectAssociation(refId: String) =
            mutateRelation("Зв’язок з Aspect видалено") {
                aspectLinksRepository.unlinkOrientation(refId)
            }

        fun addWorkspaceBinding(type: WorkspaceBindingType) {
            val model = loadedModel() ?: return
            val pending = model.pendingRelation as? PendingSemanticRelation.Workspace ?: return
            mutateRelation("Зв’язок з Workspace додано") {
                workspaceBindingRepository.bind(model.id, pending.workspaceId, type)
            }
        }

        fun removeWorkspaceBinding(bindingId: String) =
            mutateRelation("Зв’язок з Workspace видалено") {
                workspaceBindingRepository.unbind(bindingId)
            }

        fun addOrientationRelation(
            type: OrientationRelationType,
            direction: OrientationRelationEditDirection?,
        ) {
            val model = loadedModel()?.takeIf(SemanticEntityEditorModel::canEditOrientationRelations)
                ?: return
            val pending = model.pendingRelation as? PendingSemanticRelation.Orientation ?: return
            if (type == OrientationRelationType.PART_OF) {
                return relationError("PART_OF редагується через ієрархію")
            }
            if (pending.orientationId == model.id) {
                return relationError("Orientation не може посилатися на себе")
            }
            if (type != OrientationRelationType.CONFLICTS_WITH && direction == null) {
                return relationError("Вибери напрямок зв’язку")
            }

            val incoming = direction == OrientationRelationEditDirection.INCOMING
            val fromId = if (incoming) pending.orientationId else model.id
            val toId = if (incoming) model.id else pending.orientationId
            mutateRelation("Зв’язок між орієнтирами додано") {
                graphRepository.addRelation(fromId, toId, type)
            }
        }

        fun removeOrientationRelation(relationId: String, expectedVersion: Long) {
            val model = loadedModel()?.takeIf(SemanticEntityEditorModel::canEditOrientationRelations)
                ?: return
            val edge = model.orientationRelations.singleOrNull { it.relationId == relationId }
                ?: return relationError("Графовий зв’язок недоступний")
            if (!edge.isEditable) {
                return relationError("PART_OF редагується через ієрархію")
            }
            if (edge.version != expectedVersion) {
                return relationError("Версія графового зв’язку змінилася")
            }
            mutateRelation("Зв’язок між орієнтирами видалено") {
                graphRepository.removeRelation(relationId, expectedVersion)
            }
        }

        private fun mutateRelation(message: String, mutation: suspend () -> Unit) {
            val request = loadedModel() ?: return
            if (request.isRelationMutationInFlight || request.isSaving) return
            updateModel { copy(isRelationMutationInFlight = true, errorMessage = null) }
            viewModelScope.launch {
                runCatching {
                    mutation()
                    loadModel()
                }.onSuccess { reloaded ->
                    publishLoaded(reloaded)
                    _events.emit(SemanticEntityEditorEvent.RelationUpdated(message))
                }.onFailure { failure -> publishMutationFailure(request, failure) }
            }
        }

        private suspend fun loadModel(): SemanticEntityEditorModel? {
            val aspect = aspectRepository.get(subjectId)
            val base = if (aspect != null) {
                require(aspect.subject.subjectType == ManagedSubjectType.ASPECT.name) { "Aspect subject type is invalid" }
                if (aspect.subject.isDeleted) return null
                SemanticEntityEditorModel(
                    aspect.subject.id, SemanticEntityClass.ASPECT, aspect.subject.title,
                    aspect.subject.title, aspect.subject.description, aspect.subject.version,
                )
            } else {
                val semantics = orientationRepository.getOrientationSemantics(subjectId) ?: return null
                if (semantics.subject.isDeleted) return null
                SemanticEntityEditorModel(
                    semantics.subject.id, SemanticEntityClass.ORIENTATION, semantics.subject.title,
                    semantics.subject.title, semantics.subject.description, semantics.subject.version,
                    orientationKind = semantics.kind,
                )
            }
            val aspectFlow =
                if (base.canEditAspects) aspectLinksRepository.observeMemberships(base.id) else flowOf(emptyList())
            val orientationFlow =
                if (base.canEditOrientationRelations) graphRepository.observeRelations(base.id)
                else flowOf(emptyList())
            return combine(
                catalogRepository.observeCatalog(),
                aspectFlow,
                workspaceBindingRepository.observeBindings(base.id),
                orientationFlow,
            ) { catalog, refs, bindings, relations ->
                base.withRelations(catalog, refs, bindings, relations)
            }.first()
        }

        private fun publishLoaded(model: SemanticEntityEditorModel?) {
            _uiState.value = model?.let(SemanticEntityEditorUiState::Loaded)
                ?: SemanticEntityEditorUiState.Unavailable("Семантична сутність недоступна")
        }

        private fun publishUnavailable(failure: Throwable) {
            _uiState.value = SemanticEntityEditorUiState.Unavailable(
                failure.message ?: "Не вдалося завантажити семантичну сутність",
            )
        }

        private fun publishMutationFailure(original: SemanticEntityEditorModel, failure: Throwable) {
            _uiState.value = SemanticEntityEditorUiState.Loaded(
                original.copy(
                    isSaving = false,
                    isRelationMutationInFlight = false,
                    errorMessage = failure.message ?: "Не вдалося зберегти зміни",
                ),
            )
        }

        private fun relationError(message: String) = updateModel { copy(errorMessage = message) }
        private fun loadedModel() = (_uiState.value as? SemanticEntityEditorUiState.Loaded)?.model

        private fun updateModel(change: SemanticEntityEditorModel.() -> SemanticEntityEditorModel) {
            val loaded = _uiState.value as? SemanticEntityEditorUiState.Loaded ?: return
            _uiState.value = loaded.copy(model = loaded.model.change())
        }

        companion object { const val SUBJECT_ID_ARGUMENT = "subjectId" }
    }

private fun SemanticEntityEditorModel.withRelations(
    catalog: List<CanonicalEntitySummary>,
    refs: List<AspectOrientationRefEntity>,
    bindings: List<WorkspaceBindingEntity>,
    graphRelations: List<OrientationRelationEntity>,
): SemanticEntityEditorModel {
    val byRef = catalog.associateBy(CanonicalEntitySummary::ref)
    return copy(
        orientationRelations = graphRelations.map { relation ->
            val otherId =
                when (id) {
                    relation.fromOrientationId -> relation.toOrientationId
                    relation.toOrientationId -> relation.fromOrientationId
                    else -> error("Orientation relation does not include editor subject")
                }
            val title = requireNotNull(byRef[CanonicalEntityRef.Orientation(otherId)]) {
                "Orientation relation points to an unavailable Orientation"
            }.title
            OrientationGraphRelationItem(
                relationId = relation.id,
                otherOrientationId = otherId,
                title = title,
                relationType = OrientationRelationType.valueOf(relation.relationType),
                direction = if (relation.fromOrientationId == id) {
                    OrientationRelationEditDirection.OUTGOING
                } else {
                    OrientationRelationEditDirection.INCOMING
                },
                version = relation.version,
            )
        },
        aspectAssociations = refs.map { ref ->
            val title = requireNotNull(byRef[CanonicalEntityRef.Aspect(ref.aspectId)]) {
                "Aspect association points to an unavailable Aspect"
            }.title
            AspectAssociationItem(
                ref.id, ref.aspectId, title,
                AspectOrientationRelationType.valueOf(ref.relationType), ref.isPrimary,
            )
        },
        workspaceBindings = bindings.map { binding ->
            val title = requireNotNull(byRef[CanonicalEntityRef.Workspace(binding.workspaceId)]) {
                "Workspace binding points to an unavailable Workspace"
            }.title
            WorkspaceBindingItem(
                binding.id, binding.workspaceId, title,
                WorkspaceBindingType.valueOf(binding.bindingType), binding.isPrimary,
            )
        },
    )
}

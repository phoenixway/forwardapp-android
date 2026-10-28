package com.romankozak.forwardappmobile.features.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityPickerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun CanonicalEntityRef.toPlanningTarget() = PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(entityClass.name), id)

fun PlanningReferenceTarget.toCanonicalRef(): CanonicalEntityRef = when (kind) {
    PlanningReferenceTargetKind.ORIENTATION -> CanonicalEntityRef.Orientation(id)
    PlanningReferenceTargetKind.ASPECT -> CanonicalEntityRef.Aspect(id)
    PlanningReferenceTargetKind.WORKSPACE -> CanonicalEntityRef.Workspace(id)
}

fun CanonicalEntityRef.planningNavigationTarget(): NavTarget = when (this) {
    is CanonicalEntityRef.Workspace -> NavTarget.Workspace(id)
    is CanonicalEntityRef.Orientation, is CanonicalEntityRef.Aspect -> NavTarget.SemanticEntityEditor(id)
}

data class PlanningReferencePresentation(val row: PlanningReferenceEntity, val entity: CanonicalEntitySummary)

fun projectPlanningReferences(rows: List<PlanningReferenceEntity>, catalog: List<CanonicalEntitySummary>, owner: PlanningReferenceOwner): List<PlanningReferencePresentation> {
    val entities = catalog.associateBy { it.ref }
    return rows.filter { !it.isDeleted && it.ownerKind == owner.kind.name && it.ownerId == owner.id &&
        it.role in PlanningReferenceRole.entries.map { role -> role.name } }
        .mapNotNull { row ->
            val target = runCatching { PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(row.targetKind), row.targetId).toCanonicalRef() }.getOrNull()
            entities[target]?.let { PlanningReferencePresentation(row, it) }
        }.sortedWith(compareBy({ it.row.role }, { it.entity.title.lowercase(java.util.Locale.ROOT) }, { it.row.targetKind }, { it.row.targetId }))
}

@HiltViewModel
class PlanningReferencesViewModel @Inject constructor(
    private val references: CanonicalPlanningReferenceRepository,
    private val catalog: CanonicalEntityCatalogRepository,
    private val navigation: DefaultNavigationDispatcher,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val owner = MutableStateFlow<PlanningReferenceOwner?>(null)
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val links = combine(references.observeAll(), catalog.observeCatalog(), owner) { rows, entities, selected ->
        selected?.let { projectPlanningReferences(rows, entities, it) }.orEmpty()
    }.catch { error.value = it.message; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectOwner(value: PlanningReferenceOwner) { owner.value = value }
    suspend fun attachRelated(owner: PlanningReferenceOwner, entity: CanonicalEntitySummary) {
        references.attach(owner, entity.ref.toPlanningTarget(), PlanningReferenceRole.RELATED)
    }
    fun open(ref: CanonicalEntityRef) = navigation.navigate(ref.planningNavigationTarget())
    fun attach(ref: CanonicalEntityRef) {
        val selected = owner.value ?: return
        command { references.attach(selected, ref.toPlanningTarget()) }
    }
    fun remove(row: PlanningReferenceEntity) = command { references.remove(row.id, row.version) }

    private fun command(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try { action() } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error.value = failure.message ?: "Не вдалося змінити зв’язок" }
            finally { busy.value = false }
        }
    }

    /** Reuse the actual canonical picker engine, not a second query/selection stack. */
    fun newPicker(allowedClasses: Set<CanonicalEntityClass> = CanonicalEntityClass.entries.toSet()) = CanonicalEntityPickerViewModel(catalog, SavedStateHandle(mapOf(
        "allowedClasses" to allowedClasses.joinToString(",") { it.name },
        "selectionMode" to "SINGLE",
    )), dispatcher)
}

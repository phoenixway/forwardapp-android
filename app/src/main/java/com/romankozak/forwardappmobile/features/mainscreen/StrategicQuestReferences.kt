package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.features.entitybrowser.resolveOperationalEmbodiments
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel
import com.romankozak.forwardappmobile.features.planning.PlanningReferencePresentation
import com.romankozak.forwardappmobile.features.planning.planningNavigationTarget
import com.romankozak.forwardappmobile.features.planning.projectPlanningReferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

/** One observable snapshot for the whole list, never a repository lookup per card. */
internal fun projectStrategicQuestReferences(
    rows: List<PlanningReferenceEntity>,
    catalog: List<CanonicalEntitySummary>,
): Map<String, List<PlanningReferencePresentation>> =
    rows.filter { it.ownerKind == PlanningReferenceOwnerKind.ARC_QUEST.name && it.ownerId.isNotBlank() }
        .groupBy { it.ownerId }
        .mapValues { (id, references) ->
            projectPlanningReferences(references, catalog, PlanningReferenceOwner(PlanningReferenceOwnerKind.ARC_QUEST, id))
        }.filterValues { it.isNotEmpty() }

internal fun PlanningReferencePresentation.questBadgeLabel(): String =
    "${if (row.role == PlanningReferenceRole.SOURCE.name) "На основі" else "Пов’язано"}: ${entity.title} · ${entity.ref.entityClass.displayLabel()}"

private data class QuestReferenceSnapshot(
    val links: Map<String, List<PlanningReferencePresentation>> = emptyMap(),
    val embodiments: Map<CanonicalEntityRef, CanonicalEntityRef.Workspace> = emptyMap(),
)

@HiltViewModel
class StrategicQuestReferencesViewModel @Inject constructor(
    references: CanonicalPlanningReferenceRepository,
    catalog: CanonicalEntityCatalogRepository,
    orientationDao: OrientationDao,
    private val navigation: DefaultNavigationDispatcher,
) : ViewModel() {
    private val snapshot = combine(
        references.observeAll(), catalog.observeCatalog(), orientationDao.observeWorkspaceBindings(),
    ) { rows, entities, bindings ->
        QuestReferenceSnapshot(projectStrategicQuestReferences(rows, entities), resolveOperationalEmbodiments(bindings, entities))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestReferenceSnapshot())

    val links = snapshot.map { it.links }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun open(ref: CanonicalEntityRef) = navigation.navigate((snapshot.value.embodiments[ref] ?: ref).planningNavigationTarget())
}

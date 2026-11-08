package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind
import com.romankozak.forwardappmobile.data.planning.PlanningCommitmentCompositionProjection
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentRelationType
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind

/** Overview only. A parent in another scope must not hide this Arc's work. */
internal fun strategicArcComponentQuestIds(quests: List<ArcQuestEntity>, frame: PlanningCommitmentCompositionProjection): Set<String> {
    val participants = quests.filterNot { it.isDeleted }.mapNotNull { quest ->
        frame.forOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.ARC_QUEST, quest.id))
            ?.takeIf { !it.commitment.isDeleted && !it.scope.isDeleted && it.scope.kind == PlanningScopeKind.STRATEGIC_ARC }
            ?.let { it.commitment.id to (quest.id to it) }
    }.toMap()
    return frame.relations.asSequence().filter { !it.isDeleted && it.relationType == PlanningCommitmentRelationType.COMPONENT_OF }
        .mapNotNull { edge ->
            val source = participants[edge.sourceCommitmentId] ?: return@mapNotNull null
            val target = participants[edge.targetCommitmentId] ?: return@mapNotNull null
            source.first.takeIf { source.second.scope.id == target.second.scope.id && edge.sourceCommitmentId != edge.targetCommitmentId }
        }.toSet()
}

/** Reorder only visible slots; hidden component slots and the full mutation input survive. */
internal fun mergeStrategicArcOverviewOrder(all: List<ArcQuestEntity>, reordered: List<ArcQuestEntity>): List<ArcQuestEntity> {
    val original = all.associateBy { it.id }
    val ids = reordered.map { it.id }
    require(original.size == all.size && ids.distinct().size == ids.size && ids.all { it in original })
    val selected = ids.toSet()
    val next = ids.iterator()
    return all.map { if (it.id in selected) original.getValue(next.next()) else it }
}

/** One presentation selection shared by the list and the contextual dock menu. */
internal class StrategicArcOverviewSelection(private val handle: SavedStateHandle,
    private val arc: StateFlow<String>, scope: CoroutineScope) {
    private val key = "strategic_arc_all_participants"
    private val selectedArc = handle.getStateFlow<String?>(key, null)
    val showAll = combine(arc, selectedArc) { current, selected -> current == selected }
        .stateIn(scope, SharingStarted.Eagerly, selectedArc.value == arc.value)

    fun select(showAll: Boolean) {
        handle[key] = if (showAll) arc.value else null
    }
}

package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

data class PlanningParticipantSummary(val commitment: PlanningCommitment, val scope: PlanningScope, val entity: CanonicalEntitySummary,
    val horizonLabel: String? = null, val dayTaskId: String? = null) {
    val contextLabel: String get() {
        val kind = when (scope.kind) { PlanningScopeKind.DAY -> "День"; PlanningScopeKind.TACTICAL_CYCLE -> "Тактична ітерація"; PlanningScopeKind.STRATEGIC_ARC -> "Стратегічна арка" }
        val horizon = horizonLabel ?: scope.title ?: scope.startsAt?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        }
        val role = when (commitment.role.value) {
            "MISSION" -> "Місія"; "FOCUS" -> "Фокус"; "DAY.RESPONSIBILITY" -> "Відповідальність"
            "DAY.TASK" -> "Завдання"; "DAY.THEME" -> "Тема"; "PRIORITY" -> "Пріоритет"; else -> "Участь"
        }
        return listOfNotNull(kind, horizon?.takeIf { it.isNotBlank() }, role).joinToString(" · ")
    }
}
data class PlanningCompositionEdge(val relation: PlanningCommitmentRelation, val participant: PlanningParticipantSummary)
data class PlanningCommitmentCompositionProjection(
    val participants: List<PlanningParticipantSummary>, val relations: List<PlanningCommitmentRelation>,
    val orientations: List<CanonicalEntitySummary> = participants.map { it.entity }.distinctBy { it.ref },
) {
    val byOrientation by lazy { participants.groupBy { it.commitment.orientationId } }
    val participantIndex by lazy { participants.associateBy { it.commitment.id } }
    private val incomingIndex by lazy { relations.groupBy { it.targetCommitmentId } }
    private val outgoingIndex by lazy { relations.groupBy { it.sourceCommitmentId } }
    private val provenanceIndex by lazy {
        participants.groupBy { it.commitment.provenance.sourceType to it.commitment.provenance.sourceId }
    }
    fun incoming(id: PlanningCommitmentId): List<PlanningCompositionEdge> {
        return incomingIndex[id].orEmpty().mapNotNull { edge ->
            participantIndex[edge.sourceCommitmentId]?.let { PlanningCompositionEdge(edge, it) }
        }
    }
    fun outgoing(id: PlanningCommitmentId): List<PlanningCompositionEdge> {
        return outgoingIndex[id].orEmpty().mapNotNull { edge ->
            participantIndex[edge.targetCommitmentId]?.let { PlanningCompositionEdge(edge, it) }
        }
    }
    fun forOwner(owner: PlanningReferenceOwner): PlanningParticipantSummary? {
        val expectedType = when (owner.kind) {
            PlanningReferenceOwnerKind.ARC_QUEST -> "ARC_QUEST"
            PlanningReferenceOwnerKind.TACTICAL_MISSION -> "TACTICAL_MISSION"
            PlanningReferenceOwnerKind.DAY_TASK -> "DAY_TASK"
            PlanningReferenceOwnerKind.DAY_FOCUS -> "DAY_FOCUS"
            PlanningReferenceOwnerKind.SCOPE -> return null
        }
        val matches = provenanceIndex[expectedType to owner.id].orEmpty()
        val adoptedTask = if (owner.kind == PlanningReferenceOwnerKind.DAY_TASK) participantIndex[
            com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId(owner.id)] else null
        return (matches + listOfNotNull(adoptedTask)).distinctBy { it.commitment.id }.singleOrNull()
    }
}

fun projectPlanningComposition(scopes: List<PlanningScope>, commitments: List<PlanningCommitment>,
    relations: List<PlanningCommitmentRelation>, catalog: List<CanonicalEntitySummary>,
    horizonLabels: Map<String, String> = emptyMap()): PlanningCommitmentCompositionProjection {
    val scopeIndex = scopes.filterNot { it.isDeleted }.associateBy { it.id }
    val entities = catalog.associateBy { it.ref }
    val participants = commitments.filterNot { it.isDeleted }.mapNotNull { commitment ->
        val scope = scopeIndex[commitment.scopeId] ?: return@mapNotNull null
        val entity = entities[CanonicalEntityRef.Orientation(commitment.orientationId)] ?: return@mapNotNull null
        PlanningParticipantSummary(commitment, scope, entity, horizonLabels[scope.id.value])
    }.sortedWith(compareBy({ it.scope.kind.name }, { it.scope.startsAt ?: Long.MAX_VALUE }, { it.scope.id.value },
        { it.commitment.order }, { it.commitment.id.value }))
    val ids = participants.mapTo(hashSetOf()) { it.commitment.id }
    val live = relations.filter { !it.isDeleted && it.sourceCommitmentId in ids && it.targetCommitmentId in ids }
        .sortedWith(compareBy<PlanningCommitmentRelation> { it.order }.thenBy { it.id.value })
    return PlanningCommitmentCompositionProjection(participants, live, catalog.filter { it.ref is CanonicalEntityRef.Orientation })
}

/** Bulk subscriptions only; catalog owns titles and target lifecycle, extensions supply Arc labels. */
class PlanningCompositionReadRepository @Inject constructor(db: AppDatabase, catalog: CanonicalEntityCatalogRepository) {
    val projection = combine(db.planningDao().observeAllScopes(), db.planningDao().observeAllCommitments(),
        db.planningCommitmentRelationDao().observeAll(), catalog.observeCatalog(), db.strategicPlanningScopeExtensionDao().observeAll()) { scopes, commitments, relations, entities, extensions ->
        projectPlanningComposition(scopes.map { it.toPlanningScopeStrict() },
            commitments.map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }, relations.map { it.toDomain() }, entities,
            extensions.filterNot { it.isDeleted }.associate { it.scopeId to it.legacyArcKey })
    }.combine(db.dayTaskDao().getAllVisibleTasksFlow()) { frame, tasks ->
        // Existing deterministic Day adoption identity, not raw-ID or title inference.
        val taskIds = tasks.associate { com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId(it.id) to it.id }
        frame.copy(participants = frame.participants.map { it.copy(dayTaskId = taskIds[it.commitment.id]) })
    }.flowOn(Dispatchers.Default)
}

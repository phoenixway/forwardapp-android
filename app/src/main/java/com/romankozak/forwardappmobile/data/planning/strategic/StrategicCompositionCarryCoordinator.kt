package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import java.util.UUID
import javax.inject.Inject

data class StrategicCarryComponent(val id: PlanningCommitmentId, val title: String, val completed: Boolean)

fun defaultStrategicCarrySelection(components: List<StrategicCarryComponent>): Set<PlanningCommitmentId> =
    components.filterNot { it.completed }.mapTo(linkedSetOf()) { it.id }

/** Selected same-Arc components are admitted first; edges are remapped once with the complete map. */
class StrategicCompositionCarryCoordinator @Inject constructor(
    private val db: AppDatabase, private val strategic: CanonicalStrategicPlanningAdapter,
    private val planning: CanonicalPlanningRepository, private val relations: CanonicalPlanningCommitmentRelationRepository,
) {
    suspend fun components(questId: String): List<StrategicCarryComponent> = db.withTransaction {
        val root = root(questId)
        val incoming = db.planningCommitmentRelationDao().getAll().filter { !it.isDeleted && it.targetCommitmentId == root.id.value }
        val participants = db.planningDao().getAllCommitments().filter { !it.isDeleted && it.scopeId == root.scopeId.value }.associateBy { it.id }
        val subjects = db.orientationDao().getAllManagedSubjects().filterNot { it.isDeleted }.associateBy { it.id }
        incoming.mapNotNull { edge ->
            val participant = participants[edge.sourceCommitmentId] ?: return@mapNotNull null
            val subject = subjects[participant.orientationId] ?: return@mapNotNull null
            StrategicCarryComponent(PlanningCommitmentId(participant.id), subject.title, participant.status == "COMPLETED")
        }
    }

    suspend fun carry(questId: String, targetArcKey: String, selected: Set<PlanningCommitmentId>): StrategicQuestCarryForwardResult = db.withTransaction {
        val source = root(questId)
        val allowed = components(questId).mapTo(hashSetOf()) { it.id }
        require(allowed.containsAll(selected)) { "Selected composition changed; reopen carry-forward" }
        val result = strategic.carryQuest(questId, targetArcKey, preserveOutgoing = false)
        val target = root(result.quest.id)
        val mapping = mutableMapOf(source.id to target.id)
        selected.sortedBy { it.value }.forEach { id ->
            val previous = requireNotNull(planning.getCommitment(id))
            require(!previous.isDeleted && previous.scopeId == source.scopeId)
            val existing = planning.getLiveCommitments(target.scopeId, previous.role).singleOrNull { it.orientationId == previous.orientationId }
            val next = existing ?: if (previous.role == PlanningRoleCode.FOCUS && previous.provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE) {
                val carried = strategic.carryQuest(requireNotNull(previous.provenance.sourceId), targetArcKey, preserveOutgoing = false)
                root(carried.quest.id)
            } else {
                val key = "planning-carry:v1:${previous.id.value.length}:${previous.id.value}:${target.scopeId.value}"
                val newId = PlanningCommitmentId(UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString())
                require(planning.getCommitment(newId) == null) { "Carried participation was retired; no implicit resurrection" }
                val now = System.currentTimeMillis()
                previous.copy(id = newId, scopeId = target.scopeId, status = PlanningCommitmentStatus.PLANNED,
                    provenance = PlanningProvenance(PlanningProvenanceKind.CARRIED_FORWARD, "PLANNING_COMMITMENT", previous.id.value),
                    createdAt = now, updatedAt = now, syncedAt = null, isDeleted = false, version = 1).also { planning.saveCommitment(it) }
            }
            mapping[id] = next.id
        }
        relations.carryRelations(mapping)
        result
    }

    private suspend fun root(questId: String): PlanningCommitment {
        val quest = requireNotNull(strategic.getById(questId)) { "Quest unavailable" }
        require(!quest.isDeleted)
        return db.planningDao().getLiveCommitmentsForRole(canonicalStrategicArcScopeId(quest.arcKey).value, PlanningRoleCode.FOCUS.value)
            .single { it.provenanceSourceType == ARC_QUEST_PROVENANCE_SOURCE && it.provenanceSourceId == questId }
            .toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
    }
}

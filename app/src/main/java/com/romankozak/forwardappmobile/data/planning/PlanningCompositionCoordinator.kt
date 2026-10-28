package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningAdapter
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import javax.inject.Inject

/** Compose participation admission and relation creation without duplicating semantic identity. */
class PlanningCompositionCoordinator @Inject constructor(
    private val db: AppDatabase, private val planning: CanonicalPlanningRepository,
    private val relations: CanonicalPlanningCommitmentRelationRepository,
    private val strategic: CanonicalStrategicPlanningAdapter,
) {
    suspend fun addStrategicComponent(target: PlanningCommitmentId, orientationId: String) = db.withTransaction {
        val parent = requireNotNull(planning.getCommitment(target))
        require(!parent.isDeleted && planning.getScope(parent.scopeId)?.let { !it.isDeleted && it.kind == PlanningScopeKind.STRATEGIC_ARC } == true)
        val existing = planning.getLiveCommitments(parent.scopeId, PlanningRoleCode.FOCUS).singleOrNull { it.orientationId == orientationId }
        val source = existing ?: run {
            val extension = requireNotNull(db.strategicPlanningScopeExtensionDao().get(parent.scopeId.value))
            require(!extension.isDeleted)
            val subject = requireNotNull(db.orientationDao().getManagedSubject(orientationId))
            val quest = ArcQuestEntity(arcKey = extension.legacyArcKey, title = subject.title, description = subject.description)
            strategic.addQuest(quest, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, orientationId))
            planning.getLiveCommitments(parent.scopeId, PlanningRoleCode.FOCUS).single { it.orientationId == orientationId }
        }
        relations.addRelation(source.id, parent.id)
    }
}

package com.romankozak.forwardappmobile.data.planning.strategic

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import java.time.YearMonth

data class StrategicQuestCarryForwardResult(val quest: ArcQuestEntity, val created: Boolean)

internal fun isHistoricalStrategicArc(sourceArcKey: String, targetArcKey: String): Boolean =
    runCatching { YearMonth.parse(sourceArcKey).isBefore(YearMonth.parse(targetArcKey)) }.getOrDefault(false)

/** New Strategic participation in an existing subject; never a semantic identity clone. */
internal class StrategicQuestCarryForward(
    private val database: AppDatabase,
    private val adapter: CanonicalStrategicPlanningAdapter,
    private val orientations: CanonicalOrientationRepository,
) {
    suspend fun carry(questId: String, targetArcKey: String): StrategicQuestCarryForwardResult = database.withTransaction {
        require(targetArcKey.isNotBlank()) { "Target Strategic arc is missing" }
        val previous = requireNotNull(adapter.getById(questId)) { "Source quest is unavailable" }
        require(!previous.isDeleted) { "Source quest is deleted" }
        require(isHistoricalStrategicArc(previous.arcKey, targetArcKey)) { "Only previous arcs can carry into the current arc" }
        val commitments = database.planningDao().getAllCommitments()
            .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
        val source = requireNotNull(commitments.singleOrNull {
            !it.isDeleted && it.role == PlanningRoleCode.FOCUS &&
                it.scopeId == canonicalStrategicArcScopeId(previous.arcKey) &&
                it.provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE && it.provenance.sourceId == previous.id
        }) { "Source quest has no unique live canonical FOCUS participation" }
        orientations.requireCompleteActiveOrientationAggregate(source.orientationId)

        val existing = commitments.singleOrNull {
            !it.isDeleted && it.role == PlanningRoleCode.FOCUS &&
                it.scopeId == canonicalStrategicArcScopeId(targetArcKey) && it.orientationId == source.orientationId
        }
        if (existing != null) {
            require(existing.provenance.sourceType == ARC_QUEST_PROVENANCE_SOURCE) { "Existing FOCUS has no quest destination" }
            val target = requireNotNull(existing.provenance.sourceId?.let { adapter.getById(it) }) {
                "Existing Strategic quest is unavailable"
            }
            require(!target.isDeleted && target.arcKey == targetArcKey)
            return@withTransaction StrategicQuestCarryForwardResult(target, created = false)
        }

        val created = ArcQuestEntity(
            arcKey = targetArcKey,
            title = previous.title,
            description = previous.description,
            linkedContextId = previous.linkedContextId,
        )
        adapter.addQuest(created, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, source.orientationId))
        StrategicQuestCarryForwardResult(requireNotNull(adapter.getById(created.id)), created = true)
    }
}

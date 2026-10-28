package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.planning.strategic.*
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalMissionAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.TacticalIterationRepository
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Narrow Planning orchestration. Each domain adapter retains carrier/participation authority. */
class PlanningContextCoordinator @Inject constructor(
    private val db: AppDatabase,
    private val planning: CanonicalPlanningRepository,
    private val relations: CanonicalPlanningCommitmentRelationRepository,
    private val orientations: CanonicalOrientationRepository,
    private val strategic: CanonicalStrategicPlanningAdapter,
    private val tactical: CanonicalTacticalMissionAdapter,
    private val iterations: TacticalIterationRepository,
    private val days: DayManagementRepository,
) {
    suspend fun add(orientationId: String, horizon: PlanningScopeKind, now: Long = System.currentTimeMillis()): PlanningCommitment = db.withTransaction {
        orientations.requireCompleteActiveOrientationAggregate(orientationId)
        val subject = requireNotNull(db.orientationDao().getManagedSubject(orientationId))
        val source = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, orientationId)
        val (scopeId, role) = when (horizon) {
            PlanningScopeKind.STRATEGIC_ARC -> {
                val arc = YearMonth.from(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())).toString()
                val scope = canonicalStrategicArcScopeId(arc)
                if (existing(scope, orientationId, PlanningRoleCode.FOCUS) == null) {
                    strategic.addQuest(ArcQuestEntity(arcKey = arc, title = subject.title, description = subject.description), source)
                }
                scope to PlanningRoleCode.FOCUS
            }
            PlanningScopeKind.TACTICAL_CYCLE -> {
                val currentScopes = db.planningDao().getAllScopes().map { it.toPlanningScopeStrict() }.filter {
                    !it.isDeleted && it.kind == PlanningScopeKind.TACTICAL_CYCLE &&
                        it.lifecycle in setOf(PlanningScopeLifecycle.ACTIVE, PlanningScopeLifecycle.DRAFT)
                }
                require(currentScopes.size <= 1) { "Кілька відкритих Tactical cycles. Спочатку визначте поточний цикл." }
                val cycle = requireNotNull(iterations.getCurrentIteration()) { "Спочатку створіть Tactical cycle." }
                val scope = PlanningScopeId(cycle.id)
                if (existing(scope, orientationId, PlanningRoleCode.MISSION) == null) {
                    tactical.insertMissionWithAutoOrder(TacticalMission(title = subject.title, description = subject.description, projectId = null,
                        weekKey = cycle.weekKey.orEmpty(), iterationId = cycle.id, missionStreamId = GENERAL_MISSION_STREAM_ID,
                        sourceType = MissionSourceType.MANUAL, status = MissionStatus.ACTIVE, priority = MissionPriority.MEDIUM,
                        deadline = NO_DEADLINE), source)
                }
                scope to PlanningRoleCode.MISSION
            }
            PlanningScopeKind.DAY -> {
                val plan = days.createOrUpdateDayPlan(now)
                val scope = PlanningScopeId(plan.id)
                if (existing(scope, orientationId, PlanningRoleCode.DAY_TASK) == null) {
                    days.addTaskToDayPlan(NewTaskParameters(dayPlanId = plan.id, title = subject.title,
                        description = subject.description, planningSource = source))
                }
                scope to PlanningRoleCode.DAY_TASK
            }
        }
        requireNotNull(existing(scopeId, orientationId, role)) { "Planning writer did not admit the requested participation" }
    }

    suspend fun promote(sourceId: PlanningCommitmentId, horizon: PlanningScopeKind, now: Long = System.currentTimeMillis()): PlanningCommitment = db.withTransaction {
        val source = live(sourceId)
        val target = add(source.orientationId, horizon, now)
        relations.addRelation(target.id, source.id, now = now)
        target
    }

    suspend fun questToTactical(questId: String) = promote(owned("ARC_QUEST", questId).id, PlanningScopeKind.TACTICAL_CYCLE)
    suspend fun missionToToday(missionId: Long) = promote(owned("TACTICAL_MISSION", missionId.toString()).id, PlanningScopeKind.DAY)
    suspend fun missionToArc(missionId: Long) = db.withTransaction {
        val mission = live(owned("TACTICAL_MISSION", missionId.toString()).id)
        val focus = add(mission.orientationId, PlanningScopeKind.STRATEGIC_ARC)
        relations.addRelation(mission.id, focus.id)
        focus
    }

    suspend fun remove(id: PlanningCommitmentId, expectedVersion: Long) = db.withTransaction {
        val item = live(id)
        require(item.version == expectedVersion) { "Участь змінилася. Повторіть дію." }
        when (item.provenance.sourceType) {
            "ARC_QUEST" -> strategic.deleteQuest(requireNotNull(strategic.getById(requireNotNull(item.provenance.sourceId))))
            "TACTICAL_MISSION" -> tactical.deleteMissionById(requireNotNull(item.provenance.sourceId?.toLongOrNull()))
            "DAY_TASK" -> days.deleteTask(requireNotNull(item.provenance.sourceId))
            else -> {
                val task = db.dayTaskDao().getAllVisibleTasksFlow().first()
                    .singleOrNull { com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId(it.id) == id }
                if (task != null) days.deleteTask(task.id) else {
                    require(item.provenance.sourceType == null) { "Use the owning Day editor to remove this participation" }
                    planning.tombstoneCommitment(id, System.currentTimeMillis())
                }
            }
        }
    }

    /** Carrier-owned role is fixed. Only statuses admitted by these existing horizon projections are exposed. */
    suspend fun status(id: PlanningCommitmentId, expectedVersion: Long, status: PlanningCommitmentStatus) = db.withTransaction {
        val item = live(id)
        require(item.version == expectedVersion)
        require(item.provenance.sourceType in setOf("ARC_QUEST", "TACTICAL_MISSION"))
        require(status in setOf(PlanningCommitmentStatus.ACTIVE, PlanningCommitmentStatus.PAUSED, PlanningCommitmentStatus.COMPLETED))
        if (item.status != status) planning.saveCommitment(item.copy(status = status, version = item.version + 1,
            updatedAt = System.currentTimeMillis(), syncedAt = null))
    }

    private suspend fun owned(type: String, id: String) = requireNotNull(db.planningDao().getLiveForProvenance(type, id)
        .singleOrNull()?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)) { "Canonical Planning participation is unavailable" }
    private suspend fun live(id: PlanningCommitmentId) = requireNotNull(planning.getCommitment(id)?.takeUnless { it.isDeleted })
    private suspend fun existing(scope: PlanningScopeId, orientation: String, role: PlanningRoleCode) =
        planning.getLiveCommitments(scope, role).singleOrNull { it.orientationId == orientation }
}

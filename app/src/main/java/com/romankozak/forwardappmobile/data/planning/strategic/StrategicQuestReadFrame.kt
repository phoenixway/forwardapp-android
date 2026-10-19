package com.romankozak.forwardappmobile.data.planning.strategic

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalMissionOrientationResolutionSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

/** Immutable snapshot; graph validation/projection runs after releasing Room's transaction executor. */
internal data class StrategicQuestReadFrame(
    val rows: List<ArcQuestEntity>,
    val commitments: List<PlanningCommitment>,
    val scopes: List<PlanningScope>,
    val extensions: List<StrategicPlanningScopeExtension>,
    val resolution: StrategicPlanningResolutionSnapshot,
) {
    companion object {
        suspend fun load(db: AppDatabase): StrategicQuestReadFrame {
            val dao = db.orientationDao()
            val rows = db.arcQuestDao().getAllSync()
            val mappings = dao.getAllLegacyMappings()
            val subjects = dao.getAllManagedSubjects()
            val orientations = dao.getAllOrientations()
            val resolution = StrategicPlanningResolutionSnapshot(mappings, subjects, orientations,
                TacticalMissionOrientationResolutionSnapshot(db.tacticalMissionDao().getAllMissionsSync(),
                    db.workspaceBacklogEntryDao().getAll(), rows, mappings, subjects, orientations))
            return StrategicQuestReadFrame(rows,
                db.planningDao().getAllCommitments().map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) },
                db.planningDao().getAllScopes().map { it.toPlanningScopeStrict() },
                db.strategicPlanningScopeExtensionDao().getAll().map { it.toDomainStrict() }, resolution)
        }
    }
}

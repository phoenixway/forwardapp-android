package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.data.planning.day.PlanningDaySourceWriter
import com.romankozak.forwardappmobile.data.planning.strategic.*
import com.romankozak.forwardappmobile.data.planning.tactical.*
import io.mockk.mockk

internal class PlanningSourceRoomFixture : AutoCloseable {
    val base = SemanticDeletionRoomFixture()
    val db = base.db
    val planning = CanonicalPlanningRepository(db, db.planningDao(), db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao())
    val references = CanonicalPlanningReferenceRepository(db)
    val tacticalAuthority = CanonicalTacticalPlanningAuthorityActivator(db, mockk(relaxed = true), mockk(relaxed = true))
    val tactical = CanonicalTacticalMissionAdapter(db, tacticalAuthority, TacticalMissionOrientationResolver(db), base.orientations,
        planning, CanonicalTacticalPlanningAdapter(db, tacticalAuthority))
    private val themes = CanonicalDayThemeBootstrapper(db, db.dayThemeDocumentDao(), db.canonicalDayThemeDao())
    private val bootstrapper = CanonicalOrientationBootstrapper(db, db.orientationDao(), db.goalDao(), db.mainBeaconDao(),
        db.arcQuestDao(), db.canonicalDayThemeDao(), themes)
    val bridge = QuestOrientationBridge(db.orientationDao(), bootstrapper)
    val strategicAuthority = CanonicalStrategicPlanningAuthorityActivator(db, bridge, tacticalAuthority)
    val strategic = CanonicalStrategicPlanningAdapter(db, strategicAuthority, planning, base.orientations, bridge)
    val day = PlanningDaySourceWriter(db, references, planning)

    suspend fun seed() {
        val cycle = TacticalIteration(id = "cycle", title = "Cycle", startedAt = 10, plannedEndAt = 20, weekKey = "2026-W43", createdAt = 1, version = 1)
        db.planningDao().upsertScope(cycle.toCanonicalTacticalPlanningScope().toPlanningScopeEntity())
        db.tacticalPlanningScopeExtensionDao().upsert(cycle.toCanonicalTacticalPlanningScopeExtension().toEntity())
        db.tacticalPlanningAuthorityStateDao().upsert(TacticalPlanningAuthorityStateEntity(version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION, activatedAt = 1))
        db.strategicPlanningAuthorityStateDao().upsert(StrategicPlanningAuthorityStateEntity(version = CanonicalStrategicPlanningAuthorityActivator.CURRENT_VERSION, activatedAt = 1))
        db.planningDao().upsertScope(PlanningScopeEntity("day", "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false))
        db.dayPlanDao().insert(DayPlan(id = "day", date = 0))
        db.dayPlanningScopeExtensionDao().upsert(com.romankozak.forwardappmobile.core.data.models.entities.planning.DayPlanningScopeExtensionEntity(
            "day", 0, null, null, 1, 1, null, 1, false,
        ))
        base.workspace()
    }

    override fun close() = base.close()
}

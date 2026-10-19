package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.day.PlanningDaySourceWriter

internal fun recurrencePlanningSources(db: AppDatabase): RecurrencePlanningSourceBridge {
    val references = CanonicalPlanningReferenceRepository(db)
    val planning = CanonicalPlanningRepository(db, db.planningDao(), db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao())
    return RecurrencePlanningSourceBridge(db, references, PlanningDaySourceWriter(db, references, planning))
}

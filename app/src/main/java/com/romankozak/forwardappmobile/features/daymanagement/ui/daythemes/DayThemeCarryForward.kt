package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.orientation.ThemeOrientationBridge
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayThemePlanningAdapter
import com.romankozak.forwardappmobile.data.planning.day.canonicalDayThemeCommitmentId
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import java.util.Calendar

/** Materialize only into an opened current/future DAY; browsing history never backfills it. */
internal suspend fun carryForwardDayThemes(
    db: AppDatabase,
    dao: CanonicalDayThemeDao,
    bridge: ThemeOrientationBridge,
    planning: CanonicalDayThemePlanningAdapter,
    dayPlanId: String,
    now: Long = System.currentTimeMillis(),
): Int = db.withTransaction {
    val scopes = db.planningDao().getAllScopes().associate { it.id to it.toPlanningScopeStrict() }
    val target = scopes[dayPlanId] ?: return@withTransaction 0
    if (target.isDeleted || target.kind != PlanningScopeKind.DAY ||
        target.lifecycle !in setOf(PlanningScopeLifecycle.DRAFT, PlanningScopeLifecycle.ACTIVE)) return@withTransaction 0
    val extensions = db.dayPlanningScopeExtensionDao().getAll().filterNot { it.isDeleted }
    val targetDay = extensions.singleOrNull { it.scopeId == dayPlanId } ?: return@withTransaction 0
    val today = Calendar.getInstance().apply {
        timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    if (targetDay.calendarDayStartMillis < today) return@withTransaction 0
    // Closest prior DAY, not the last day with themes: an intentionally empty day stops the chain.
    val previous = extensions.filter { extension ->
        extension.calendarDayStartMillis < targetDay.calendarDayStartMillis &&
            scopes[extension.scopeId]?.let { !it.isDeleted && it.kind == PlanningScopeKind.DAY } == true
    }.maxWithOrNull(compareBy({ it.calendarDayStartMillis }, { it.scopeId })) ?: return@withTransaction 0
    val definitions = bridge.projectCanonicalDefinitions(dao.getAllThemeDefinitionsSync()).associateBy { it.id }
    val origins = planning.getDayThemeCommitments(previous.scopeId).associateBy { it.id }
    val previousRows = planning.applyCanonicalProjection(
        dao.getDayThemesForDay(previous.scopeId).map { it.toCanonicalSnapshot() }, origins.values.toList(),
    )
    val existing = dao.getDayThemesForDay(dayPlanId).map { it.toCanonicalSnapshot() }
    val existingIds = existing.mapTo(hashSetOf()) { it.id }
    var order = planning.applyCanonicalProjection(existing, planning.getDayThemeCommitments(dayPlanId))
        .filterNot { it.isDeleted }.maxOfOrNull { it.order } ?: -1L
    val lineage = linkedMapOf<String, PlanningCommitmentId>()
    val additions = previousRows.filter { row ->
        !row.isDeleted && row.isActive && definitions[row.themeId]?.let { !it.isDeleted && !it.archived && it.carryForward } == true
    }.sortedWith(compareBy({ it.order }, { it.id })).mapNotNull { source ->
        val id = canonicalDayThemeId(dayPlanId, source.themeId)
        if (id in existingIds) return@mapNotNull null // Including tombstones: never resurrect.
        require(db.planningDao().getCommitment(canonicalDayThemeCommitmentId(id).value) == null) {
            "Day theme participation exists without its domain row"
        }
        lineage[id] = origins.getValue(canonicalDayThemeCommitmentId(source.id)).id
        DayThemeSnapshot(id, source.themeId, dayPlanId, source.budgetPercent, ++order, true, now, now, null, 1, false)
    }
    if (additions.isNotEmpty()) {
        planning.applyProjectionChanges(additions, carriedFrom = lineage)
        dao.upsertDayThemes(additions.map { it.toCanonicalEntity() })
    }
    additions.size
}

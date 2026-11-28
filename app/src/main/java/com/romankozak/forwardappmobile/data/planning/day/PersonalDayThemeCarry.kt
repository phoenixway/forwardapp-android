package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId

private suspend fun AppDatabase.hasPersonalCalendar(scopeId: String): Boolean {
    val row = dayPlanningScopeExtensionDao().get(scopeId) ?: return false
    return verifyPersonalPlanningDay(row.scopeId, row.calendarDayStartMillis, row.localDate, row.timeZoneId) != null
}

internal suspend fun AppDatabase.requireStablePersonalThemeCarryOrigin(local: PlanningCommitment, incoming: PlanningCommitment) {
    if (local.role == PlanningRoleCode.DAY_THEME && local.createdAt == 0L && local.provenance != incoming.provenance &&
        (local.provenance.kind == PlanningProvenanceKind.CARRIED_FORWARD || incoming.provenance.kind == PlanningProvenanceKind.CARRIED_FORWARD) &&
        hasPersonalCalendar(local.scopeId.value)) {
        throw IllegalArgumentException("A personal Day Theme cannot change its carry origin")
    }
}

/** Called only after an exact comparison of all commitment facts except the first update clock. */
internal suspend fun AppDatabase.isInitialPersonalDayThemeCarry(row: PlanningCommitment): Boolean {
    if (row.version != 1L || row.createdAt != 0L || row.isDeleted || row.role != PlanningRoleCode.DAY_THEME ||
        row.status != PlanningCommitmentStatus.ACTIVE || row.provenance.kind != PlanningProvenanceKind.CARRIED_FORWARD ||
        row.provenance.sourceType != "PLANNING_COMMITMENT" || !hasPersonalCalendar(row.scopeId.value)) return false
    val previousId = row.provenance.sourceId ?: return false
    val origin = planningDao().getCommitment(previousId) ?: return false
    if (origin.scopeId == row.scopeId.value || origin.orientationId != row.orientationId || origin.role != row.role.value) return false
    return canonicalDayThemeDao().getDayThemesForDay(row.scopeId.value).any { theme ->
        theme.id == canonicalDayThemeId(row.scopeId.value, theme.themeId) &&
            canonicalDayThemeCommitmentId(theme.id) == row.id &&
            canonicalDayThemeCommitmentId(canonicalDayThemeId(origin.scopeId, theme.themeId)).value == previousId
    }
}

/** DAY.THEME does not own the occurrence budget. Verify its domain facts before normal freshness selection. */
internal suspend fun AppDatabase.requireCompatibleInitialThemeCarries(
    incoming: List<DayThemeSnapshot>,
    local: List<DayThemeSnapshot>,
) {
    val indexed = local.associateBy { it.id }
    for (candidate in incoming) {
        val existing = indexed[candidate.id] ?: continue
        if (existing.version != 1L || candidate.version != 1L || existing.createdAt != 0L || candidate.createdAt != 0L) continue
        if (candidate.copy(updatedAt = existing.updatedAt, syncedAt = existing.syncedAt) != existing && hasPersonalCalendar(existing.dayPlanId)) {
            throw IllegalArgumentException("Equal-version carried Day Theme content differs between devices")
        }
    }
}

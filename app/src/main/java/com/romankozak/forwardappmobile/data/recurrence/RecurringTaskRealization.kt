package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesOverride
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesTargetOverrides
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.data.planning.day.canonicalDayThemeCommitmentId
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.realizesOn
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskRealizesDefaults
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries

internal data class RealizationEndpoints(
    val themes: Map<String, String>,
    val priorities: Map<String, String>,
)

internal suspend fun AppDatabase.realizationEndpoints(planId: String): RealizationEndpoints {
    val commitments = planningDao().getLiveCommitmentsForRole(planId, PlanningRoleCode.DAY_THEME.value)
        .filter { it.status == PlanningCommitmentStatus.ACTIVE.name }.mapTo(hashSetOf()) { it.id }
    val themes = canonicalDayThemeDao().getDayThemesForDay(planId).filter {
        !it.isDeleted && canonicalDayThemeCommitmentId(it.id).value in commitments &&
            canonicalDayThemeDao().getThemeDefinitionById(it.themeId)?.isDeleted == false
    }.associate { it.themeId to it.id }
    val priorities = canonicalRecurringSeriesDao().getRealizablePriorityOccurrences(planId)
        .mapNotNull { item -> item.recurrenceSeriesId?.let { it to item.id } }.toMap()
    return RealizationEndpoints(themes, priorities)
}

internal suspend fun AppDatabase.conversionDefaults(task: DayTask): RecurringTaskRealizesDefaults? {
    val dao = canonicalDayThemeDao()
    val selectedThemes = dao.getAssignmentDocumentByDayPlanId(task.dayPlanId)?.takeUnless { it.isDeleted }
        ?.toCanonicalSnapshot()?.assignments.orEmpty().filter { it.entityId == task.id }.flatMap { it.dayThemeIds }.toSet()
    if (selectedThemes.isEmpty() && task.realizedDayFocusItemIds.isEmpty()) return null
    val endpoints = realizationEndpoints(task.dayPlanId)
    return RecurringTaskRealizesDefaults(
        endpoints.themes.filterValues { it in selectedThemes }.keys,
        endpoints.priorities.filterValues { it in task.realizedDayFocusItemIds }.keys,
    )
}

internal data class ResolvedRealization(val links: Set<String>, val overrides: TaskRealizesTargetOverrides)

internal fun resolveRealization(
    concrete: Set<String>,
    defaults: Set<String>,
    overrides: TaskRealizesTargetOverrides,
    endpoints: Map<String, String>,
): ResolvedRealization {
    // Existing concrete links without inheritance provenance belong to the occurrence.
    val local = concrete - overrides.inheritedLinks.values.toSet()
    val desired = ((defaults + overrides.explicit) - overrides.excluded)
        .mapNotNull { target -> endpoints[target]?.let { target to it } }.toMap()
    val inherited = desired.filterValues { it !in local }
    return ResolvedRealization(local + desired.values, overrides.copy(inheritedLinks = inherited))
}

/** Called inside the transaction that changes the task, series or Day endpoints. */
internal suspend fun AppDatabase.reconcileRecurringTaskLinks(
    planId: String,
    now: Long,
) {
    val tasks = dayTaskDao().getTasksForDayIncludingDeletedSync(planId)
        .filter { !it.isDeleted && it.recurrenceSeriesId != null && it.recurrenceOccurrenceDayKey != null }
    if (tasks.isEmpty()) return
    val series = tasks.mapNotNull { it.recurrenceSeriesId }.distinct().associateWith { id ->
        canonicalRecurringSeriesDao().getById(id)?.toCanonicalSeries() as? RecurringTaskSeries
    }
    val candidates = tasks.mapNotNull { task ->
        val rule = series[task.recurrenceSeriesId]?.takeUnless { it.isDeleted } ?: return@mapNotNull null
        val defaults = rule.template.realizesOn(requireNotNull(task.recurrenceOccurrenceDayKey))
        if (defaults == null && task.realizesOverride == null) null else task to defaults
    }
    if (candidates.isEmpty()) return
    val endpoints = realizationEndpoints(planId)
    val dao = canonicalDayThemeDao()
    val document = dao.getAssignmentDocumentByDayPlanId(planId)?.toCanonicalSnapshot()
    val original = document?.takeUnless { it.isDeleted }?.assignments.orEmpty()
        .associate { it.entityId to it.dayThemeIds.toSet() }
    val assignments = original.toMutableMap()
    for ((task, defaults) in candidates) {
        val current = task.realizesOverride ?: TaskRealizesOverride()
        val themes = resolveRealization(assignments[task.id].orEmpty(), defaults?.themeDefinitionIds.orEmpty(),
            current.themes, if (document?.isDeleted == true) emptyMap() else endpoints.themes)
        val priorities = resolveRealization(task.realizedDayFocusItemIds.toSet(), defaults?.prioritySeriesIds.orEmpty(),
            current.priorities, endpoints.priorities)
        if (themes.links.isEmpty()) assignments.remove(task.id) else assignments[task.id] = themes.links
        val overrides = TaskRealizesOverride(themes.overrides, priorities.overrides)
        if (priorities.links != task.realizedDayFocusItemIds.toSet() || overrides != current) {
            dayTaskDao().update(task.copy(realizedDayFocusItemIds = priorities.links.sorted(), realizesOverride = overrides,
                updatedAt = now, syncedAt = null, version = task.version + 1))
        }
    }
    if (original != assignments) {
        val updated = DayThemeAssignmentDocumentSnapshot(
            dayPlanId = planId, assignments = assignments.toSortedMap().map { (id, themes) ->
                DayThemeAssignmentSnapshot(id, themes.sorted())
            }, createdAt = document?.createdAt ?: now, updatedAt = now, syncedAt = null,
            version = (document?.version ?: 0L) + 1, isDeleted = false,
        )
        dao.upsertAssignmentDocuments(listOf(updated.toCanonicalEntity()))
    }
}

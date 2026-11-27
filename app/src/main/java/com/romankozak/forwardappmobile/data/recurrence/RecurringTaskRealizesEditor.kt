package com.romankozak.forwardappmobile.data.recurrence

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesOverride
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.realizesOn
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskRealizesDefaults
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import javax.inject.Inject

enum class TaskRealizesKind { THEMES, PRIORITIES }

class RecurringTaskRealizesEditor @Inject constructor(private val db: AppDatabase) {
    suspend fun set(
        taskId: String, kind: TaskRealizesKind, concreteIds: Set<String>, selected: Boolean, forward: Boolean,
    ) = db.withTransaction {
        require(concreteIds.isNotEmpty())
        val task = requireNotNull(db.dayTaskDao().getTaskById(taskId)?.takeUnless { it.isDeleted })
        val seriesDao = db.canonicalRecurringSeriesDao()
        val id = requireNotNull(task.recurrenceSeriesId)
        val series = requireNotNull(seriesDao.getById(id)?.toCanonicalSeries() as? RecurringTaskSeries)
        require(!series.isDeleted)
        val day = requireNotNull(task.recurrenceOccurrenceDayKey)
        val endpoints = db.realizationEndpoints(task.dayPlanId)
        val isTheme = kind == TaskRealizesKind.THEMES
        val stable = (if (isTheme) endpoints.themes else endpoints.priorities).filterValues { it in concreteIds }.keys
        require(stable.size == concreteIds.size) { "Для цього зв’язку доступне лише редагування окремого дня" }
        val now = System.currentTimeMillis()
        val current = task.realizesOverride ?: TaskRealizesOverride()
        val override = if (isTheme) current.themes else current.priorities
        val next = when {
            forward -> override.copy(explicit = override.explicit - stable, excluded = override.excluded - stable)
            selected -> override.copy(explicit = override.explicit + stable, excluded = override.excluded - stable)
            else -> override.copy(explicit = override.explicit - stable, excluded = override.excluded + stable)
        }
        if (forward) {
            fun change(previous: RecurringTaskRealizesDefaults): RecurringTaskRealizesDefaults {
                val ids = if (isTheme) previous.themeDefinitionIds else previous.prioritySeriesIds
                val updated = if (selected) ids + stable else ids - stable
                return if (isTheme) previous.copy(themeDefinitionIds = updated) else previous.copy(prioritySeriesIds = updated)
            }
            val template = series.template
            val atDay = template.realizesOn(day) ?: RecurringTaskRealizesDefaults()
            val after = template.realizesChanges.mapValues { (key, value) -> if (key >= day) change(value) else value } +
                (day to change(atDay))
            if (after != template.realizesChanges) seriesDao.update(series.copy(
                template = template.copy(realizesChanges = after), updatedAt = now, syncedAt = null, version = series.version + 1,
            ).toAndroidEntity())
        }

        // Explicit removal also releases a pre-feature concrete membership owned by this occurrence.
        val priorityIds = if (!selected && !isTheme) task.realizedDayFocusItemIds - concreteIds else task.realizedDayFocusItemIds
        if (!selected && isTheme) {
            val dao = db.canonicalDayThemeDao()
            val doc = dao.getAssignmentDocumentByDayPlanId(task.dayPlanId)?.takeUnless { it.isDeleted }?.toCanonicalSnapshot()
            if (doc != null) {
                val assignments = doc.assignments.mapNotNull {
                    if (it.entityId != taskId) it else (it.dayThemeIds - concreteIds).takeIf { remaining -> remaining.isNotEmpty() }
                        ?.let { remaining -> DayThemeAssignmentSnapshot(taskId, remaining) }
                }
                if (assignments != doc.assignments) dao.upsertAssignmentDocuments(listOf(doc.copy(
                    assignments = assignments, version = doc.version + 1, updatedAt = now, syncedAt = null,
                ).toCanonicalEntity()))
            }
        }
        val nextOverride = if (isTheme) current.copy(themes = next) else current.copy(priorities = next)
        if (nextOverride != current || priorityIds != task.realizedDayFocusItemIds) {
            db.dayTaskDao().update(task.copy(realizesOverride = nextOverride, realizedDayFocusItemIds = priorityIds,
                updatedAt = now, syncedAt = null, version = task.version + 1))
        }
        val plans = if (forward) seriesDao.getTaskOccurrencesForSeries(id).filter {
            !it.isDeleted && (it.recurrenceOccurrenceDayKey ?: "") >= day
        }.map { it.dayPlanId }.distinct() else listOf(task.dayPlanId)
        plans.forEach { db.reconcileRecurringTaskLinks(it, now) }
    }
}

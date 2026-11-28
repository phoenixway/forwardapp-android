package com.romankozak.forwardappmobile.data.recurrence

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.planning.day.PersonalPlanningDay
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.RecurrenceMaterializationPlan
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.planRecurringSeriesForDay
import com.romankozak.forwardappmobile.shared.core.models.day.CanonicalDayDatabase
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android persistence adapter for the canonical shared recurrence engine.
 *
 * This class deliberately does not read RecurringTask, recurringTaskId,
 * recurringKey or nextOccurrenceTime. Those fields belong to the quarantined
 * legacy recurrence implementation.
 */
@Singleton
class CanonicalRecurrenceMaterializationAdapter
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val planningSources: RecurrencePlanningSourceBridge,
        private val focusSources: FocusRecurrenceSourceBridge =
            FocusRecurrenceSourceBridge(appDatabase, CanonicalPlanningReferenceRepository(appDatabase)),
    ) {
        /** Rule edits/imports can prepare an open Day without treating task/metric writes as rule edits. */
        fun observeRuleChanges(): Flow<Unit> = appDatabase.invalidationTracker
            .createFlow("canonical_recurring_series", emitInitialState = false).map { Unit }

        suspend fun materializeForDate(
            date: Long,
            now: Long = System.currentTimeMillis(),
        ): RecurrenceMaterializationPlan =
            withContext(ioDispatcher) {
                appDatabase.withTransaction {
                    materializeInTransaction(date = date, now = now)
                }
            }

        internal suspend fun materializeForPlan(planId: String, now: Long): RecurrenceMaterializationPlan =
            withContext(ioDispatcher) {
                appDatabase.withTransaction {
                    val plan = checkNotNull(appDatabase.dayPlanDao().getPlanById(planId)?.takeUnless { it.isDeleted }) {
                        "No live DayPlan for recurrence materialization: $planId"
                    }
                    val day = appDatabase.verifiedRecurrenceDay(planId)
                    if (day == null) {
                        materializeInTransaction(date = plan.date, now = now)
                    } else {
                        check(plan.date == day.start) { "DayPlan and its keyed recurrence date disagree: $planId" }
                        materializeInTransaction(listOf(plan), day.date.toString(), now)
                    }
                }
            }

        private suspend fun materializeInTransaction(
            date: Long,
            now: Long,
        ): RecurrenceMaterializationPlan {
            val calendar = PersonalPlanningDay.at(date)
            val plans = appDatabase.dayPlanDao().getActivePersonalDayPlans(calendar.start, calendar.scopeId)
            return materializeInTransaction(plans, calendar.date.toString(), now)
        }

        private suspend fun materializeInTransaction(
            plans: List<DayPlan>,
            dayKey: String,
            now: Long,
        ): RecurrenceMaterializationPlan {
            val dayTaskDao = appDatabase.dayTaskDao()
            val dayFocusItemDao = appDatabase.dayFocusItemDao()
            val recurringSeriesDao = appDatabase.canonicalRecurringSeriesDao()

            val planIds = plans.map { it.id }
            val keyedDayStart = planIds.singleOrNull()?.let { appDatabase.verifiedRecurrenceDayStart(it, dayKey) }

            val tasks =
                planIds.flatMap { planId ->
                    dayTaskDao.getTasksForDayIncludingDeletedSync(planId)
                }

            val focusItems =
                planIds.flatMap { planId ->
                    dayFocusItemDao.getItemsForDayPlanSync(planId)
                }

            val recurringSeries =
                recurringSeriesDao
                    .getActiveCandidatesForDay(dayKey)
                    .map { it.toCanonicalSeries() }
                    .filter { series ->
                        when (series) {
                            is RecurringTaskSeries -> planningSources.available(series.template.planningSource)
                            is RecurringFocusSeries -> focusSources.available(series.template.sourceOrientationId)
                            is RecurringResponsibilitySeries -> focusSources.available(series.template.sourceOrientationId)
                        }
                    }

            val recurringSeriesById = recurringSeries.associateBy { it.id }

            val repairedTasks =
                tasks.map { task ->
                    val hasAnyProvenance =
                        task.recurrenceSeriesId != null ||
                            task.recurrenceOccurrenceDayKey != null ||
                            task.recurrenceSourceSeriesVersion != null
                    val hasCompleteProvenance =
                        task.recurrenceSeriesId != null &&
                            task.recurrenceOccurrenceDayKey != null &&
                            task.recurrenceSourceSeriesVersion != null

                    check(!hasAnyProvenance || hasCompleteProvenance) {
                        "Partial canonical recurrence provenance on task ${task.id}"
                    }

                    if (hasCompleteProvenance) {
                        task
                    } else {
                        val match =
                            Regex("""^recurrence:TASK:(.+):(\d{4}-\d{2}-\d{2})$""")
                                .matchEntire(task.id)

                        if (match == null) {
                            task
                        } else {
                            val seriesId = match.groupValues[1]
                            val occurrenceDayKey = match.groupValues[2]
                            val series = recurringSeriesById[seriesId]

                            if (series == null || occurrenceDayKey != dayKey) {
                                task
                            } else {
                                task.copy(
                                    recurrenceSeriesId = seriesId,
                                    recurrenceOccurrenceDayKey = occurrenceDayKey,
                                    recurrenceSourceSeriesVersion = series.version,
                                )
                            }
                        }
                    }
                }

            val repairedFocusItems =
                focusItems.map { item ->
                    val hasAnyProvenance =
                        item.recurrenceSeriesId != null ||
                            item.recurrenceOccurrenceDayKey != null ||
                            item.recurrenceSourceSeriesVersion != null
                    val hasCompleteProvenance =
                        item.recurrenceSeriesId != null &&
                            item.recurrenceOccurrenceDayKey != null &&
                            item.recurrenceSourceSeriesVersion != null

                    check(!hasAnyProvenance || hasCompleteProvenance) {
                        "Partial canonical recurrence provenance on focus item ${item.id}"
                    }

                    if (hasCompleteProvenance) {
                        item
                    } else {
                        val match =
                            Regex("""^recurrence:(FOCUS|RESPONSIBILITY):(.+):(\d{4}-\d{2}-\d{2})$""")
                                .matchEntire(item.id)

                        if (match == null) {
                            item
                        } else {
                            val kind = match.groupValues[1]
                            val seriesId = match.groupValues[2]
                            val occurrenceDayKey = match.groupValues[3]
                            val series = recurringSeriesById[seriesId]

                            if (
                                series == null ||
                                occurrenceDayKey != dayKey ||
                                item.type.name != kind
                            ) {
                                item
                            } else {
                                item.copy(
                                    recurrenceSeriesId = seriesId,
                                    recurrenceOccurrenceDayKey = occurrenceDayKey,
                                    recurrenceSourceSeriesVersion = series.version,
                                )
                            }
                        }
                    }
                }

            val taskRepairs =
                repairedTasks.filterIndexed { index, repaired ->
                    repaired != tasks[index]
                }
            val focusRepairs =
                repairedFocusItems.filterIndexed { index, repaired ->
                    repaired != focusItems[index]
                }

            if (taskRepairs.isNotEmpty()) {
                dayTaskDao.updateAll(taskRepairs)
            }

            focusRepairs.forEach { repaired ->
                dayFocusItemDao.update(repaired)
            }

            if (taskRepairs.isNotEmpty() || focusRepairs.isNotEmpty()) {
                android.util.Log.i(
                    "CanonicalRecurrence",
                    "Repaired canonical occurrence provenance: " +
                        "dayKey=$dayKey, tasks=${taskRepairs.size}, focusItems=${focusRepairs.size}",
                )
            }

            val canonicalDatabase =
                CanonicalDayDatabase(
                    dayPlans = plans.map { it.toCanonicalDayPlan(dayKey) },
                    dayTasks = repairedTasks.map { it.toCanonicalDayTask() },
                    dayFocusItems = repairedFocusItems.map { it.toCanonicalDayFocusItem() },
                    recurringSeries = recurringSeries,
                    dayManagementRuntimeState = null,
                )

            val result =
                planRecurringSeriesForDay(
                    database = canonicalDatabase,
                    dayKey = dayKey,
                    now = now,
                    keyedDayStart = keyedDayStart,
                )

            val skippedExistingOccurrenceKeys =
                result.skippedExistingOccurrenceKeys.toMutableList()

            val taskEntitiesToCreate =
                result.tasksToCreate.map { it.toAndroidDayTask() }
            val safeTaskEntities =
                taskEntitiesToCreate.filter { candidate ->
                    val seriesId =
                        requireNotNull(candidate.recurrenceSeriesId) {
                            "Canonical recurring task ${candidate.id} has no recurrenceSeriesId"
                        }
                    val occurrenceDayKey =
                        requireNotNull(candidate.recurrenceOccurrenceDayKey) {
                            "Canonical recurring task ${candidate.id} has no recurrenceOccurrenceDayKey"
                        }
                    val existing =
                        dayTaskDao.getByIdForCanonicalRecurrenceSync(candidate.id)

                    if (existing == null) {
                        true
                    } else {
                        val sameLogicalOccurrence =
                            existing.recurrenceSeriesId == seriesId &&
                                existing.recurrenceOccurrenceDayKey == occurrenceDayKey

                        check(sameLogicalOccurrence) {
                            "Canonical recurring task physical ID collision: " +
                                "id=${candidate.id}, " +
                                "planned=$seriesId@$occurrenceDayKey, " +
                                "existing=${existing.recurrenceSeriesId}@${existing.recurrenceOccurrenceDayKey}, " +
                                "existingDayPlanId=${existing.dayPlanId}, " +
                                "plannedDayPlanId=${candidate.dayPlanId}"
                        }
                        check(existing.dayPlanId == candidate.dayPlanId ||
                            (keyedDayStart == null && appDatabase.verifiedRecurrenceDayStart(existing.dayPlanId, occurrenceDayKey) == null)) {
                            "Canonical recurring task identity belongs to a different keyed DayPlan: ${candidate.id}"
                        }

                        skippedExistingOccurrenceKeys +=
                            "$seriesId@$occurrenceDayKey"
                        false
                    }
                }

            val focusEntitiesToCreate =
                result.focusItemsToCreate.map { it.toAndroidDayFocusItem() }
            val safeFocusEntities =
                focusEntitiesToCreate.filter { candidate ->
                    val seriesId =
                        requireNotNull(candidate.recurrenceSeriesId) {
                            "Canonical recurring focus item ${candidate.id} has no recurrenceSeriesId"
                        }
                    val occurrenceDayKey =
                        requireNotNull(candidate.recurrenceOccurrenceDayKey) {
                            "Canonical recurring focus item ${candidate.id} has no recurrenceOccurrenceDayKey"
                        }
                    val existing =
                        dayFocusItemDao.getByIdForCanonicalRecurrenceSync(candidate.id)

                    if (existing == null) {
                        true
                    } else {
                        val sameLogicalOccurrence =
                            existing.recurrenceSeriesId == seriesId &&
                                existing.recurrenceOccurrenceDayKey == occurrenceDayKey

                        check(sameLogicalOccurrence) {
                            "Canonical recurring focus physical ID collision: " +
                                "id=${candidate.id}, " +
                                "planned=$seriesId@$occurrenceDayKey, " +
                                "existing=${existing.recurrenceSeriesId}@${existing.recurrenceOccurrenceDayKey}, " +
                                "existingDayPlanId=${existing.dayPlanId}, " +
                                "plannedDayPlanId=${candidate.dayPlanId}"
                        }
                        check(existing.dayPlanId == candidate.dayPlanId ||
                            (keyedDayStart == null && appDatabase.verifiedRecurrenceDayStart(existing.dayPlanId, occurrenceDayKey) == null)) {
                            "Canonical recurring priority identity belongs to a different keyed DayPlan: ${candidate.id}"
                        }

                        skippedExistingOccurrenceKeys +=
                            "$seriesId@$occurrenceDayKey"
                        false
                    }
                }

            if (safeTaskEntities.isNotEmpty()) {
                dayTaskDao.insertAll(safeTaskEntities)
                safeTaskEntities.forEach { task ->
                    val series = recurringSeriesById[task.recurrenceSeriesId] as com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
                    planningSources.materialize(task, series.template.planningSource, now, keyedDayStart ?: now)
                }
            }

            if (safeFocusEntities.isNotEmpty()) {
                dayFocusItemDao.insertAll(safeFocusEntities)
                safeFocusEntities.forEach { item ->
                    val series = requireNotNull(recurringSeriesById[item.recurrenceSeriesId])
                    focusSources.materialize(item.id, series.focusSourceOrientationId(), now, keyedDayStart ?: now)
                }
            }

            repairedTasks.filterNot { it.isDeleted }.forEach { task ->
                val series = recurringSeriesById[task.recurrenceSeriesId] as? com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
                if (series != null) {
                    val initialCreatedAt = keyedDayStart?.takeIf { task.createdAt == it } ?: now
                    planningSources.repairMissing(task, series.template.planningSource, now, initialCreatedAt)
                }
            }

            repairedFocusItems.filterNot { it.isDeleted }.forEach { item ->
                val series = recurringSeriesById[item.recurrenceSeriesId]
                if (series is RecurringFocusSeries || series is RecurringResponsibilitySeries) {
                    val initialCreatedAt = keyedDayStart?.takeIf { item.createdAt == it } ?: now
                    focusSources.repairMissing(item.id, series.focusSourceOrientationId(), now, initialCreatedAt)
                }
            }

            planIds.forEach { appDatabase.reconcileRecurringTaskLinks(it, now) }

            val safeTaskIds = safeTaskEntities.mapTo(mutableSetOf()) { it.id }
            val safeFocusIds = safeFocusEntities.mapTo(mutableSetOf()) { it.id }

            return result.copy(
                tasksToCreate =
                    result.tasksToCreate.filter { it.id in safeTaskIds },
                focusItemsToCreate =
                    result.focusItemsToCreate.filter { it.id in safeFocusIds },
                skippedExistingOccurrenceKeys =
                    skippedExistingOccurrenceKeys.distinct(),
            )
        }

        private fun RecurringSeries.focusSourceOrientationId(): String? =
            when (this) {
                is RecurringFocusSeries -> template.sourceOrientationId
                is RecurringResponsibilitySeries -> template.sourceOrientationId
                is RecurringTaskSeries -> null
        }
    }

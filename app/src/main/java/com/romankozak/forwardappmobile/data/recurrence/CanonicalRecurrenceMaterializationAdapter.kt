package com.romankozak.forwardappmobile.data.recurrence

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.RecurrenceMaterializationPlan
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyOf
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.planRecurringSeriesForDay
import com.romankozak.forwardappmobile.shared.core.models.day.CanonicalDayDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Calendar
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
    ) {
        suspend fun materializeForDate(
            date: Long,
            now: Long = System.currentTimeMillis(),
        ): RecurrenceMaterializationPlan =
            withContext(ioDispatcher) {
                appDatabase.withTransaction {
                    materializeInTransaction(date = date, now = now)
                }
            }

        private suspend fun materializeInTransaction(
            date: Long,
            now: Long,
        ): RecurrenceMaterializationPlan {
            val dayStart = localDayStart(date)
            val dayKey = localDayKey(dayStart)

            val dayPlanDao = appDatabase.dayPlanDao()
            val dayTaskDao = appDatabase.dayTaskDao()
            val dayFocusItemDao = appDatabase.dayFocusItemDao()
            val recurringSeriesDao = appDatabase.canonicalRecurringSeriesDao()

            val plans = dayPlanDao.getActivePlansForDateSync(dayStart)
            val planIds = plans.map { it.id }

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

                        skippedExistingOccurrenceKeys +=
                            "$seriesId@$occurrenceDayKey"
                        false
                    }
                }

            if (safeTaskEntities.isNotEmpty()) {
                dayTaskDao.insertAll(safeTaskEntities)
            }

            if (safeFocusEntities.isNotEmpty()) {
                dayFocusItemDao.insertAll(safeFocusEntities)
            }

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

        private fun localDayStart(timestamp: Long): Long {
            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            return calendar.timeInMillis
        }

        private fun localDayKey(timestamp: Long): String {
            val calendar =
                Calendar.getInstance().apply {
                    timeInMillis = timestamp
                }

            return localDayKeyOf(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
            )
        }
    }

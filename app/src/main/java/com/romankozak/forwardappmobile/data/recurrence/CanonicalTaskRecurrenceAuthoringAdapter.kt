package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority as AndroidTaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.data.dao.CanonicalTaskSplitSourceVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.compareLocalDayKeys
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyDayOfWeek
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyOf
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.previousLocalDayKey
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceRuleMatchesDay
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority as CanonicalTaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android authoring boundary for canonical recurrence-v2 TASK series.
 *
 * This adapter never creates RecurringTask, recurringTaskId or nextOccurrenceTime
 * state. Series scheduling lives in canonical_recurring_series and concrete
 * occurrences carry only canonical recurrence provenance.
 */
@Singleton
class CanonicalTaskRecurrenceAuthoringAdapter
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val materializationAdapter: CanonicalRecurrenceMaterializationAdapter,
    ) {
        suspend fun createSeriesForPlan(
            dayPlanId: String,
            title: String,
            description: String?,
            goalId: String?,
            projectId: String?,
            taskType: String?,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
            priority: AndroidTaskPriority,
            estimatedDurationMinutes: Long?,
            points: Int,
            executionStrictness: TaskExecutionStrictness,
            rule: RecurrenceRule,
        ): DayTask {
            require(title.isNotBlank()) { "Canonical recurring task title must not be blank" }

            val dayPlan =
                checkNotNull(appDatabase.dayPlanDao().getPlanById(dayPlanId)) {
                    "Cannot create canonical recurring task: DayPlan not found: $dayPlanId"
                }

            val now = System.currentTimeMillis()
            val seriesId = UUID.randomUUID().toString()
            val startDayKey = canonicalLocalDayKey(dayPlan.date)
            val canonicalRule = rule.normalizedForSeriesStart(startDayKey)
            val template =
                buildTemplate(
                    title = title,
                    description = description,
                    goalId = goalId,
                    projectId = projectId,
                    taskType = taskType,
                    linkedProjectIds = linkedProjectIds,
                    linkedAttachmentIds = linkedAttachmentIds,
                    priority = priority,
                    estimatedDurationMinutes = estimatedDurationMinutes,
                    points = points,
                    executionStrictness = executionStrictness,
                )
            val series =
                RecurringTaskSeries(
                    id = seriesId,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    rule = canonicalRule,
                    startDayKey = startDayKey,
                    endDayKey = null,
                    template = template,
                )

            appDatabase.canonicalRecurringSeriesDao().insert(series.toAndroidEntity())

            val materialization =
                materializationAdapter.materializeForDate(
                    date = dayPlan.date,
                    now = now,
                )
            val occurrenceId =
                recurrenceOccurrenceId(
                    kind = RecurringSeriesKind.TASK,
                    seriesId = seriesId,
                    dayKey = materialization.dayKey,
                )

            return checkNotNull(
                appDatabase.dayTaskDao().getByIdForCanonicalRecurrenceSync(occurrenceId),
            ) {
                "Canonical recurring task occurrence was not materialized: " +
                    "$seriesId@${materialization.dayKey}"
            }
        }

        suspend fun convertOneOffToSeries(
            task: DayTask,
            title: String,
            description: String?,
            goalId: String?,
            projectId: String?,
            taskType: String?,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
            priority: AndroidTaskPriority,
            estimatedDurationMinutes: Long?,
            points: Int,
            executionStrictness: TaskExecutionStrictness,
            rule: RecurrenceRule,
        ): DayTask {
            require(task.recurrenceSeriesId == null) {
                "Cannot convert already-canonical recurring task ${task.id}"
            }
            require(task.recurringTaskId == null) {
                "Cannot treat legacy recurring task as one-off during canonical conversion: ${task.id}"
            }
            require(!task.isDeleted) {
                "Cannot convert deleted task ${task.id}"
            }
            require(title.isNotBlank()) {
                "Canonical recurring task title must not be blank"
            }

            val dayPlan =
                checkNotNull(appDatabase.dayPlanDao().getPlanById(task.dayPlanId)) {
                    "Cannot convert task to canonical recurrence: DayPlan not found: ${task.dayPlanId}"
                }

            val now = System.currentTimeMillis()
            val seriesId = UUID.randomUUID().toString()
            val startDayKey = canonicalLocalDayKey(dayPlan.date)
            val canonicalRule = rule.normalizedForSeriesStart(startDayKey)
            val template =
                buildTemplate(
                    title = title,
                    description = description,
                    goalId = goalId,
                    projectId = projectId,
                    taskType = taskType,
                    linkedProjectIds = linkedProjectIds,
                    linkedAttachmentIds = linkedAttachmentIds,
                    priority = priority,
                    estimatedDurationMinutes = estimatedDurationMinutes,
                    points = points,
                    executionStrictness = executionStrictness,
                )
            val series =
                RecurringTaskSeries(
                    id = seriesId,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    rule = canonicalRule,
                    startDayKey = startDayKey,
                    endDayKey = null,
                    template = template,
                )

            val occurrence =
                task.copy(
                    id =
                        recurrenceOccurrenceId(
                            kind = RecurringSeriesKind.TASK,
                            seriesId = seriesId,
                            dayKey = startDayKey,
                        ),
                    title = template.title,
                    description = template.description,
                    goalId = template.goalId,
                    projectId = template.projectId,
                    linkedProjectIds = template.linkedProjectIds,
                    linkedAttachmentIds = template.linkedAttachmentIds,
                    recurringTaskId = null,
                    recurrenceSeriesId = seriesId,
                    recurrenceOccurrenceDayKey = startDayKey,
                    recurrenceSourceSeriesVersion = series.version,
                    taskType = template.taskType ?: if (template.goalId != null) "GOAL" else null,
                    priority = priority,
                    estimatedDurationMinutes = template.estimatedDurationMinutes,
                    executionStrictness = executionStrictness,
                    points = template.points,
                    nextOccurrenceTime = null,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                )

            appDatabase.canonicalRecurringSeriesDao().convertTaskOneOffToCanonicalSeries(
                series = series.toAndroidEntity(),
                occurrence = occurrence,
                sourceTaskId = task.id,
                sourceExpectedVersion = task.version,
                updatedAt = now,
            )

            return occurrence
        }

        suspend fun splitSeriesFromOccurrence(
            task: DayTask,
            title: String,
            description: String?,
            goalId: String?,
            projectId: String?,
            taskType: String?,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
            priority: AndroidTaskPriority,
            estimatedDurationMinutes: Long?,
            points: Int,
            executionStrictness: TaskExecutionStrictness,
            rule: RecurrenceRule,
        ): DayTask {
            val oldSeriesId =
                requireNotNull(task.recurrenceSeriesId) {
                    "Cannot split recurrence for non-recurring task ${task.id}"
                }
            val splitDayKey =
                requireNotNull(task.recurrenceOccurrenceDayKey) {
                    "Cannot split canonical task without occurrence day key: ${task.id}"
                }
            require(!task.isDeleted) {
                "Cannot split recurrence from deleted task ${task.id}"
            }

            val now = System.currentTimeMillis()
            val seriesDao = appDatabase.canonicalRecurringSeriesDao()
            val storedSeries =
                checkNotNull(seriesDao.getById(oldSeriesId)) {
                    "Canonical recurring task series not found: $oldSeriesId"
                }.toCanonicalSeries()

            check(storedSeries is RecurringTaskSeries) {
                "Canonical series $oldSeriesId is ${storedSeries.kind}, not TASK"
            }
            check(!storedSeries.isDeleted) {
                "Cannot split deleted canonical recurring task series: $oldSeriesId"
            }
            check(compareLocalDayKeys(splitDayKey, storedSeries.startDayKey) >= 0L) {
                "Split day $splitDayKey precedes series start ${storedSeries.startDayKey}"
            }
            storedSeries.endDayKey?.let { endDayKey ->
                check(compareLocalDayKeys(splitDayKey, endDayKey) <= 0L) {
                    "Split day $splitDayKey is after series end $endDayKey"
                }
            }

            val oldTemplate = storedSeries.template
            val occurrences = seriesDao.getTaskOccurrencesForSeries(oldSeriesId)
            val selectedStoredOccurrence =
                checkNotNull(
                    occurrences.firstOrNull { occurrence ->
                        occurrence.id == task.id && !occurrence.isDeleted
                    },
                ) {
                    "Selected canonical task occurrence not found or deleted: ${task.id}"
                }
            check(selectedStoredOccurrence.recurrenceOccurrenceDayKey == splitDayKey) {
                "Selected task occurrence day changed while preparing split: ${task.id}"
            }

            val newSeriesId = UUID.randomUUID().toString()
            val canonicalRule = rule.normalizedForSeriesStart(splitDayKey)
            val nextTemplate =
                buildTemplate(
                    title = title,
                    description = description,
                    goalId = goalId,
                    projectId = projectId,
                    taskType = taskType,
                    linkedProjectIds = linkedProjectIds,
                    linkedAttachmentIds = linkedAttachmentIds,
                    priority = priority,
                    estimatedDurationMinutes = estimatedDurationMinutes,
                    points = points,
                    executionStrictness = executionStrictness,
                )
            val newSeries =
                RecurringTaskSeries(
                    id = newSeriesId,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1,
                    rule = canonicalRule,
                    startDayKey = splitDayKey,
                    endDayKey = null,
                    template = nextTemplate,
                )

            val liveSourceOccurrences = mutableListOf<CanonicalTaskSplitSourceVersion>()
            val replacementOccurrences = mutableListOf<DayTask>()
            var selectedReplacement: DayTask? = null

            occurrences.forEach { source ->
                val occurrenceDayKey =
                    requireNotNull(source.recurrenceOccurrenceDayKey) {
                        "Canonical task occurrence has no day key: ${source.id}"
                    }
                if (compareLocalDayKeys(occurrenceDayKey, splitDayKey) < 0L) {
                    return@forEach
                }

                val matchesNewRule =
                    recurrenceRuleMatchesDay(
                        rule = canonicalRule,
                        startDayKey = splitDayKey,
                        targetDayKey = occurrenceDayKey,
                    )
                val isSelected = source.id == task.id
                val isClean =
                    source.recurrenceSourceSeriesVersion == storedSeries.version &&
                        source.matchesTemplate(oldTemplate)

                if (!source.isDeleted) {
                    liveSourceOccurrences +=
                        CanonicalTaskSplitSourceVersion(
                            taskId = source.id,
                            expectedVersion = source.version,
                        )
                }

                when {
                    source.isDeleted && matchesNewRule -> {
                        replacementOccurrences +=
                            source.copy(
                                id =
                                    recurrenceOccurrenceId(
                                        kind = RecurringSeriesKind.TASK,
                                        seriesId = newSeriesId,
                                        dayKey = occurrenceDayKey,
                                    ),
                                title = nextTemplate.title,
                                description = nextTemplate.description,
                                goalId = nextTemplate.goalId,
                                projectId = nextTemplate.projectId,
                                linkedProjectIds = nextTemplate.linkedProjectIds,
                                linkedAttachmentIds = nextTemplate.linkedAttachmentIds,
                                recurringTaskId = null,
                                recurrenceSeriesId = newSeriesId,
                                recurrenceOccurrenceDayKey = occurrenceDayKey,
                                recurrenceSourceSeriesVersion = newSeries.version,
                                taskType = effectiveTaskType(nextTemplate),
                                priority = AndroidTaskPriority.valueOf(nextTemplate.priority.name),
                                estimatedDurationMinutes = nextTemplate.estimatedDurationMinutes,
                                executionStrictness = effectiveExecutionStrictness(nextTemplate),
                                nextOccurrenceTime = null,
                                points = nextTemplate.points,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = true,
                                version = 1,
                            )
                    }

                    source.isDeleted -> Unit

                    matchesNewRule -> {
                        val useNewTemplate = isSelected || isClean
                        val replacement =
                            source.copy(
                                id =
                                    recurrenceOccurrenceId(
                                        kind = RecurringSeriesKind.TASK,
                                        seriesId = newSeriesId,
                                        dayKey = occurrenceDayKey,
                                    ),
                                title = if (useNewTemplate) nextTemplate.title else source.title,
                                description =
                                    if (useNewTemplate) nextTemplate.description else source.description,
                                goalId = if (useNewTemplate) nextTemplate.goalId else source.goalId,
                                projectId =
                                    if (useNewTemplate) nextTemplate.projectId else source.projectId,
                                linkedProjectIds =
                                    if (useNewTemplate) {
                                        nextTemplate.linkedProjectIds
                                    } else {
                                        source.linkedProjectIds
                                    },
                                linkedAttachmentIds =
                                    if (useNewTemplate) {
                                        nextTemplate.linkedAttachmentIds
                                    } else {
                                        source.linkedAttachmentIds
                                    },
                                recurringTaskId = null,
                                recurrenceSeriesId = newSeriesId,
                                recurrenceOccurrenceDayKey = occurrenceDayKey,
                                recurrenceSourceSeriesVersion = newSeries.version,
                                taskType =
                                    if (useNewTemplate) effectiveTaskType(nextTemplate) else source.taskType,
                                priority =
                                    if (useNewTemplate) {
                                        AndroidTaskPriority.valueOf(nextTemplate.priority.name)
                                    } else {
                                        source.priority
                                    },
                                estimatedDurationMinutes =
                                    if (useNewTemplate) {
                                        nextTemplate.estimatedDurationMinutes
                                    } else {
                                        source.estimatedDurationMinutes
                                    },
                                executionStrictness =
                                    if (useNewTemplate) {
                                        effectiveExecutionStrictness(nextTemplate)
                                    } else {
                                        source.executionStrictness
                                    },
                                nextOccurrenceTime = null,
                                points = if (useNewTemplate) nextTemplate.points else source.points,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1,
                            )
                        replacementOccurrences += replacement
                        if (isSelected) {
                            selectedReplacement = replacement
                        }
                    }

                    !isClean -> {
                        replacementOccurrences +=
                            source.copy(
                                id = UUID.randomUUID().toString(),
                                recurringTaskId = null,
                                recurrenceSeriesId = null,
                                recurrenceOccurrenceDayKey = null,
                                recurrenceSourceSeriesVersion = null,
                                nextOccurrenceTime = null,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1,
                            )
                    }
                }
            }

            val returnedOccurrence =
                checkNotNull(selectedReplacement) {
                    "New recurrence rule did not preserve selected split task occurrence: ${task.id}"
                }

            seriesDao.splitCanonicalTaskSeries(
                oldSeriesId = oldSeriesId,
                oldSeriesExpectedVersion = storedSeries.version,
                oldSeriesEndDayKey = previousLocalDayKey(splitDayKey),
                newSeries = newSeries.toAndroidEntity(),
                liveSourceOccurrences = liveSourceOccurrences,
                replacementOccurrences = replacementOccurrences,
                updatedAt = now,
            )

            return returnedOccurrence
        }

        suspend fun updateCurrentOccurrence(
            task: DayTask,
            title: String,
            description: String?,
            goalId: String?,
            projectId: String?,
            taskType: String?,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
            priority: AndroidTaskPriority,
            estimatedDurationMinutes: Long?,
            points: Int,
            executionStrictness: TaskExecutionStrictness,
        ): DayTask {
            require(task.recurrenceSeriesId != null) {
                "Cannot edit canonical occurrence for non-recurring task ${task.id}"
            }
            require(task.recurrenceOccurrenceDayKey != null) {
                "Canonical task has no occurrence day key: ${task.id}"
            }
            require(!task.isDeleted) {
                "Cannot edit deleted canonical task occurrence ${task.id}"
            }

            val template =
                buildTemplate(
                    title = title,
                    description = description,
                    goalId = goalId,
                    projectId = projectId,
                    taskType = taskType,
                    linkedProjectIds = linkedProjectIds,
                    linkedAttachmentIds = linkedAttachmentIds,
                    priority = priority,
                    estimatedDurationMinutes = estimatedDurationMinutes,
                    points = points,
                    executionStrictness = executionStrictness,
                )
            val now = System.currentTimeMillis()
            val updated =
                task.copy(
                    title = template.title,
                    description = template.description,
                    goalId = template.goalId,
                    projectId = template.projectId,
                    linkedProjectIds = template.linkedProjectIds,
                    linkedAttachmentIds = template.linkedAttachmentIds,
                    recurringTaskId = null,
                    taskType = effectiveTaskType(template),
                    priority = priority,
                    estimatedDurationMinutes = template.estimatedDurationMinutes,
                    executionStrictness = effectiveExecutionStrictness(template),
                    points = template.points,
                    nextOccurrenceTime = null,
                    updatedAt = now,
                    syncedAt = null,
                    version = task.version + 1,
                )

            appDatabase.dayTaskDao().update(updated)
            return updated
        }

        suspend fun updateSeriesTemplate(
            task: DayTask,
            title: String,
            description: String?,
            goalId: String?,
            projectId: String?,
            taskType: String?,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
            priority: AndroidTaskPriority,
            estimatedDurationMinutes: Long?,
            points: Int,
            executionStrictness: TaskExecutionStrictness,
        ): DayTask {
            val seriesId =
                requireNotNull(task.recurrenceSeriesId) {
                    "Cannot update canonical task series for non-recurring task ${task.id}"
                }
            require(!task.isDeleted) {
                "Cannot update canonical task series from deleted occurrence ${task.id}"
            }

            val now = System.currentTimeMillis()
            val seriesDao = appDatabase.canonicalRecurringSeriesDao()
            val storedSeries =
                checkNotNull(seriesDao.getById(seriesId)) {
                    "Canonical recurring task series not found: $seriesId"
                }.toCanonicalSeries()

            check(storedSeries is RecurringTaskSeries) {
                "Canonical series $seriesId is ${storedSeries.kind}, not TASK"
            }
            check(!storedSeries.isDeleted) {
                "Cannot update deleted canonical recurring task series: $seriesId"
            }

            val oldTemplate = storedSeries.template
            val nextTemplate =
                buildTemplate(
                    title = title,
                    description = description,
                    goalId = goalId,
                    projectId = projectId,
                    taskType = taskType,
                    linkedProjectIds = linkedProjectIds,
                    linkedAttachmentIds = linkedAttachmentIds,
                    priority = priority,
                    estimatedDurationMinutes = estimatedDurationMinutes,
                    points = points,
                    executionStrictness = executionStrictness,
                )
            val nextVersion = storedSeries.version + 1
            val updatedSeries =
                storedSeries.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = nextVersion,
                    template = nextTemplate,
                )

            val occurrences = seriesDao.getTaskOccurrencesForSeries(seriesId)
            check(occurrences.any { occurrence -> occurrence.id == task.id && !occurrence.isDeleted }) {
                "Selected canonical task occurrence not found or deleted: ${task.id}"
            }

            val occurrencesToUpdate =
                occurrences.mapNotNull { occurrence ->
                    if (occurrence.isDeleted) {
                        return@mapNotNull null
                    }

                    val isSelectedOccurrence = occurrence.id == task.id
                    val isCleanOccurrence =
                        occurrence.recurrenceSourceSeriesVersion == storedSeries.version &&
                            occurrence.matchesTemplate(oldTemplate)

                    if (!isSelectedOccurrence && !isCleanOccurrence) {
                        return@mapNotNull null
                    }

                    occurrence.copy(
                        title = nextTemplate.title,
                        description = nextTemplate.description,
                        goalId = nextTemplate.goalId,
                        projectId = nextTemplate.projectId,
                        linkedProjectIds = nextTemplate.linkedProjectIds,
                        linkedAttachmentIds = nextTemplate.linkedAttachmentIds,
                        recurringTaskId = null,
                        taskType = effectiveTaskType(nextTemplate),
                        priority = AndroidTaskPriority.valueOf(nextTemplate.priority.name),
                        estimatedDurationMinutes = nextTemplate.estimatedDurationMinutes,
                        executionStrictness = effectiveExecutionStrictness(nextTemplate),
                        points = nextTemplate.points,
                        nextOccurrenceTime = null,
                        recurrenceSourceSeriesVersion = nextVersion,
                        updatedAt = now,
                        syncedAt = null,
                        version = occurrence.version + 1,
                    )
                }

            seriesDao.updateSeriesAndTaskOccurrences(
                series = updatedSeries.toAndroidEntity(),
                occurrences = occurrencesToUpdate,
            )

            return checkNotNull(
                occurrencesToUpdate.firstOrNull { occurrence -> occurrence.id == task.id },
            ) {
                "Selected canonical task occurrence was not updated: ${task.id}"
            }
        }

        /**
         * Deletes only this logical occurrence.
         *
         * The row remains as a canonical tombstone with its recurrence provenance,
         * so deterministic materialization cannot recreate the same series/day key.
         */
        suspend fun deleteCurrentOccurrence(task: DayTask): DayTask {
            val seriesId =
                requireNotNull(task.recurrenceSeriesId) {
                    "Cannot delete canonical occurrence for non-recurring task ${task.id}"
                }
            requireNotNull(task.recurrenceOccurrenceDayKey) {
                "Canonical task has no occurrence day key: ${task.id}"
            }
            require(task.recurringTaskId == null) {
                "Cannot delete legacy recurring task through canonical recurrence API: ${task.id}"
            }
            require(!task.isDeleted) {
                "Cannot delete already-deleted canonical task occurrence ${task.id}"
            }

            val now = System.currentTimeMillis()
            val deletedCount =
                appDatabase.canonicalRecurringSeriesDao().softDeleteCanonicalTaskOccurrence(
                    taskId = task.id,
                    expectedVersion = task.version,
                    seriesId = seriesId,
                    updatedAt = now,
                )
            check(deletedCount == 1) {
                "Cannot delete stale or detached canonical task occurrence: ${task.id}"
            }

            return task.copy(
                isDeleted = true,
                updatedAt = now,
                syncedAt = null,
                version = task.version + 1,
            )
        }

        /**
         * Stops this series starting with the selected occurrence day.
         *
         * Historical occurrences before the selected day remain intact. The series
         * ends on D-1, while the selected and already-materialized future occurrences
         * are retained as tombstones so sync/materialization cannot resurrect them.
         */
        suspend fun stopSeriesFromOccurrence(task: DayTask) {
            val seriesId =
                requireNotNull(task.recurrenceSeriesId) {
                    "Cannot stop recurrence for non-recurring task ${task.id}"
                }
            val stopDayKey =
                requireNotNull(task.recurrenceOccurrenceDayKey) {
                    "Canonical task has no occurrence day key: ${task.id}"
                }
            require(task.recurringTaskId == null) {
                "Cannot stop legacy recurring task through canonical recurrence API: ${task.id}"
            }
            require(!task.isDeleted) {
                "Cannot stop canonical task series from deleted occurrence ${task.id}"
            }

            val seriesDao = appDatabase.canonicalRecurringSeriesDao()
            val storedSeries =
                checkNotNull(seriesDao.getById(seriesId)) {
                    "Canonical recurring task series not found: $seriesId"
                }.toCanonicalSeries()

            check(storedSeries is RecurringTaskSeries) {
                "Canonical series $seriesId is ${storedSeries.kind}, not TASK"
            }
            check(!storedSeries.isDeleted) {
                "Cannot stop deleted canonical recurring task series: $seriesId"
            }
            check(compareLocalDayKeys(stopDayKey, storedSeries.startDayKey) >= 0L) {
                "Stop day $stopDayKey precedes series start ${storedSeries.startDayKey}"
            }
            storedSeries.endDayKey?.let { endDayKey ->
                check(compareLocalDayKeys(stopDayKey, endDayKey) <= 0L) {
                    "Stop day $stopDayKey is after series end $endDayKey"
                }
            }

            val now = System.currentTimeMillis()
            seriesDao.stopCanonicalTaskSeriesFromDay(
                seriesId = seriesId,
                expectedSeriesVersion = storedSeries.version,
                fromDayKey = stopDayKey,
                endDayKey = previousLocalDayKey(stopDayKey),
                updatedAt = now,
            )
        }

        private fun DayTask.matchesTemplate(template: RecurringTaskTemplate): Boolean =
            title == template.title &&
                description?.trim()?.takeIf { it.isNotEmpty() } ==
                template.description?.trim()?.takeIf { it.isNotEmpty() } &&
                goalId == template.goalId &&
                projectId == template.projectId &&
                linkedProjectIds.orEmpty() == template.linkedProjectIds &&
                linkedAttachmentIds.orEmpty() == template.linkedAttachmentIds &&
                taskType == effectiveTaskType(template) &&
                priority.name == template.priority.name &&
                estimatedDurationMinutes == template.estimatedDurationMinutes &&
                executionStrictness == effectiveExecutionStrictness(template) &&
                points == template.points

        private fun effectiveTaskType(template: RecurringTaskTemplate): String? =
            template.taskType ?: if (template.goalId != null) "GOAL" else null

        private fun effectiveExecutionStrictness(
            template: RecurringTaskTemplate,
        ): TaskExecutionStrictness =
            TaskExecutionStrictness.valueOf(template.executionStrictness ?: "NORMAL")

        private fun buildTemplate(
            title: String,
            description: String?,
            goalId: String?,
            projectId: String?,
            taskType: String?,
            linkedProjectIds: List<String>,
            linkedAttachmentIds: List<String>,
            priority: AndroidTaskPriority,
            estimatedDurationMinutes: Long?,
            points: Int,
            executionStrictness: TaskExecutionStrictness,
        ): RecurringTaskTemplate =
            RecurringTaskTemplate(
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                goalId = goalId,
                linkedProjectIds = linkedProjectIds.filter { it.isNotBlank() }.distinct(),
                linkedAttachmentIds = linkedAttachmentIds.filter { it.isNotBlank() }.distinct(),
                priority = CanonicalTaskPriority.valueOf(priority.name),
                estimatedDurationMinutes = estimatedDurationMinutes,
                points = points,
                projectId = projectId,
                taskType = taskType,
                executionStrictness = executionStrictness.name,
            )

        private fun RecurrenceRule.normalizedForSeriesStart(startDayKey: String): RecurrenceRule {
            require(interval >= 1) { "Invalid recurrence interval: $interval" }

            return when (frequency) {
                RecurrenceFrequency.WEEKLY -> {
                    val selectedDays = daysOfWeek.orEmpty()
                    if (selectedDays.isEmpty()) {
                        copy(daysOfWeek = null)
                    } else {
                        val startDay = localDayKeyDayOfWeek(startDayKey)
                        copy(
                            daysOfWeek =
                                (selectedDays + startDay)
                                    .distinct()
                                    .sortedBy { it.ordinal },
                        )
                    }
                }

                else -> copy(daysOfWeek = null)
            }
        }

        private fun canonicalLocalDayKey(timestamp: Long): String {
            val calendar =
                java.util.Calendar.getInstance().apply {
                    timeInMillis = timestamp
                }
            return localDayKeyOf(
                year = calendar.get(java.util.Calendar.YEAR),
                month = calendar.get(java.util.Calendar.MONTH) + 1,
                day = calendar.get(java.util.Calendar.DAY_OF_MONTH),
            )
        }
    }

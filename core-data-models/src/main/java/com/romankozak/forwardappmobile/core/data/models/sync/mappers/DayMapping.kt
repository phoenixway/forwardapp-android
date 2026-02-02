package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DailyMetricSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayPlanSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurrenceRuleSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.RecurringTaskSnapshot
import java.time.DayOfWeek

// Day Management Mappings
fun DayPlan.toSnapshot(): DayPlanSnapshot = DayPlanSnapshot(
    id,
    date,
    name,
    status.name,
    reflection,
    energyLevel,
    mood,
    weatherConditions,
    totalPlannedMinutes,
    totalCompletedMinutes,
    completionPercentage,
    createdAt,
    updatedAt ?: createdAt,
    isDeleted,
    version
)
fun DayPlanSnapshot.toEntity(): DayPlan = DayPlan(
    id,
    date,
    name,
    enumValueOf(status),
    reflection,
    energyLevel,
    mood,
    weatherConditions,
    totalPlannedMinutes,
    totalCompletedMinutes,
    completionPercentage,
    createdAt,
    updatedAt,
    isDeleted = isDeleted,
    version = version
)

fun DayTask.toSnapshot(): DayTaskSnapshot = DayTaskSnapshot(
    id,
    dayPlanId,
    title,
    description,
    goalId,
    projectId,
    activityRecordId,
    recurringTaskId,
    taskType,
    entityId,
    order,
    priority.name,
    status.name,
    completed,
    scheduledTime,
    estimatedDurationMinutes,
    actualDurationMinutes,
    dueTime,
    valueImportance,
    valueImpact,
    effort,
    cost,
    risk,
    location,
    tags,
    notes,
    createdAt,
    updatedAt ?: createdAt,
    isDeleted,
    version,
    completedAt,
    nextOccurrenceTime,
    points
)
fun DayTaskSnapshot.toEntity(): DayTask = DayTask(
    id,
    dayPlanId,
    title,
    description,
    goalId,
    projectId,
    activityRecordId,
    recurringTaskId,
    taskType,
    entityId,
    order,
    enumValueOf(priority),
    enumValueOf(status),
    completed,
    scheduledTime,
    estimatedDurationMinutes,
    actualDurationMinutes,
    dueTime,
    valueImportance,
    valueImpact,
    effort,
    cost,
    risk,
    location,
    tags,
    notes,
    createdAt,
    updatedAt,
    isDeleted = isDeleted,
    version = version,
    completedAt = completedAt,
    nextOccurrenceTime = nextOccurrenceTime,
    points = points
)

fun DailyMetric.toSnapshot(): DailyMetricSnapshot = DailyMetricSnapshot(
    id,
    dayPlanId,
    date,
    tasksPlanned,
    tasksCompleted,
    completionRate,
    totalPlannedTime,
    totalActiveTime,
    completedPoints,
    totalBreakTime,
    morningEnergyLevel,
    eveningEnergyLevel,
    overallMood,
    stressLevel,
    customMetrics,
    createdAt,
    updatedAt ?: createdAt,
    isDeleted,
    version
)
fun DailyMetricSnapshot.toEntity(): DailyMetric = DailyMetric(
    id,
    dayPlanId,
    date,
    tasksPlanned,
    tasksCompleted,
    completionRate,
    totalPlannedTime,
    totalActiveTime,
    completedPoints,
    totalBreakTime,
    morningEnergyLevel,
    eveningEnergyLevel,
    overallMood,
    stressLevel,
    customMetrics,
    createdAt,
    updatedAt,
    isDeleted = isDeleted,
    version = version
)

fun RecurrenceRule.toSnapshot(): RecurrenceRuleSnapshot =
    RecurrenceRuleSnapshot(frequency.name, interval, daysOfWeek?.map { it.name })
// Безпечний мапінг
fun RecurrenceRuleSnapshot.toEntity(): RecurrenceRule = RecurrenceRule(
    frequency = RecurrenceFrequency.values().find { it.name == frequency }
        ?: RecurrenceFrequency.DAILY,
    interval = interval,
    daysOfWeek = daysOfWeek?.mapNotNull { dayString ->
        DayOfWeek.values().find { it.name == dayString }
    }
)
fun RecurringTask.toSnapshot(): RecurringTaskSnapshot = RecurringTaskSnapshot(
    id,
    title,
    description,
    goalId,
    duration,
    priority.name,
    points,
    recurrenceRule.toSnapshot(),
    startDate,
    endDate
)
fun RecurringTaskSnapshot.toEntity(): RecurringTask = RecurringTask(
    id,
    title,
    description,
    goalId,
    duration,
    enumValueOf(priority),
    points,
    recurrenceRule.toEntity(),
    startDate,
    endDate
)
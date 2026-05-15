package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.*
import java.time.DayOfWeek


// --- DayPlan Mappings ---
fun DayPlan.toSnapshot(): DayPlanSnapshot = DayPlanSnapshot(
    id = id,
    date = date,
    name = name,
    linkedProjectIds = linkedProjectIds,
    linkedAttachmentIds = linkedAttachmentIds,
    status = status.name,
    reflection = reflection,
    energyLevel = energyLevel,
    mood = mood,
    weatherConditions = weatherConditions,
    totalPlannedMinutes = totalPlannedMinutes,
    totalCompletedMinutes = totalCompletedMinutes,
    completionPercentage = completionPercentage,
    createdAt = createdAt,
    // Використовуємо логіку updatedAt ?: createdAt для надійної синхронізації
    updatedAt = updatedAt ?: createdAt,
    isDeleted = isDeleted,
    version = version,
)

fun DayPlanSnapshot.toEntity(): DayPlan = DayPlan(
    id = id,
    date = date,
    name = name,
    linkedProjectIds = linkedProjectIds,
    linkedAttachmentIds = linkedAttachmentIds,
    status = enumValueOf<DayStatus>(status),
    reflection = reflection,
    energyLevel = energyLevel,
    mood = mood,
    weatherConditions = weatherConditions,
    totalPlannedMinutes = totalPlannedMinutes,
    totalCompletedMinutes = totalCompletedMinutes,
    completionPercentage = completionPercentage,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version,
)

// --- DayTask Mappings ---
fun DayTask.toSnapshot(): DayTaskSnapshot = DayTaskSnapshot(
    id = id,
    dayPlanId = dayPlanId,
    title = title,
    description = description,
    goalId = goalId,
    projectId = projectId,
    linkedProjectIds = linkedProjectIds,
    linkedAttachmentIds = linkedAttachmentIds,
    activityRecordId = activityRecordId,
    recurringTaskId = recurringTaskId,
    taskType = taskType,
    entityId = entityId,
    order = order,
    priority = priority.name,
    status = status.name,
    completed = completed,
    scheduledTime = scheduledTime,
    estimatedDurationMinutes = estimatedDurationMinutes,
    actualDurationMinutes = actualDurationMinutes,
    dueTime = dueTime,
    executionStrictness = executionStrictness.name,
    valueImportance = valueImportance,
    valueImpact = valueImpact,
    effort = effort,
    cost = cost,
    risk = risk,
    location = location,
    tags = tags,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt ?: createdAt,
    isDeleted = isDeleted,
    version = version,
    completedAt = completedAt,
    nextOccurrenceTime = nextOccurrenceTime,
    points = points,
)

fun DayTaskSnapshot.toEntity(): DayTask = DayTask(
    id = id,
    dayPlanId = dayPlanId,
    title = title,
    description = description,
    goalId = goalId,
    projectId = projectId,
    linkedProjectIds = linkedProjectIds,
    linkedAttachmentIds = linkedAttachmentIds,
    activityRecordId = activityRecordId,
    recurringTaskId = recurringTaskId,
    taskType = taskType,
    entityId = entityId,
    order = order,
    priority = enumValueOf<TaskPriority>(priority),
    status = enumValueOf<TaskStatus>(status),
    completed = completed,
    scheduledTime = scheduledTime,
    estimatedDurationMinutes = estimatedDurationMinutes,
    actualDurationMinutes = actualDurationMinutes,
    dueTime = dueTime,
    executionStrictness = enumValueOf<TaskExecutionStrictness>(executionStrictness),
    valueImportance = valueImportance,
    valueImpact = valueImpact,
    effort = effort,
    cost = cost,
    risk = risk,
    location = location,
    tags = tags,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version,
    completedAt = completedAt,
    nextOccurrenceTime = nextOccurrenceTime,
    points = points,
)

// --- DailyMetric Mappings ---
fun DailyMetric.toSnapshot(): DailyMetricSnapshot = DailyMetricSnapshot(
    id = id,
    dayPlanId = dayPlanId,
    date = date,
    tasksPlanned = tasksPlanned,
    tasksCompleted = tasksCompleted,
    completionRate = completionRate,
    totalPlannedTime = totalPlannedTime,
    totalActiveTime = totalActiveTime,
    completedPoints = completedPoints,
    totalBreakTime = totalBreakTime,
    morningEnergyLevel = morningEnergyLevel,
    eveningEnergyLevel = eveningEnergyLevel,
    overallMood = overallMood,
    stressLevel = stressLevel,
    customMetrics = customMetrics,
    createdAt = createdAt,
    updatedAt = updatedAt ?: createdAt,
    isDeleted = isDeleted,
    version = version,
)

fun DailyMetricSnapshot.toEntity(): DailyMetric = DailyMetric(
    id = id,
    dayPlanId = dayPlanId,
    date = date,
    tasksPlanned = tasksPlanned,
    tasksCompleted = tasksCompleted,
    completionRate = completionRate,
    totalPlannedTime = totalPlannedTime,
    totalActiveTime = totalActiveTime,
    completedPoints = completedPoints,
    totalBreakTime = totalBreakTime,
    morningEnergyLevel = morningEnergyLevel,
    eveningEnergyLevel = eveningEnergyLevel,
    overallMood = overallMood,
    stressLevel = stressLevel,
    customMetrics = customMetrics,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    version = version,
)

// --- RecurrenceRule Mapping (Виправлено values() -> entries) ---
fun RecurrenceRule.toSnapshot(): RecurrenceRuleSnapshot = RecurrenceRuleSnapshot(
    frequency = frequency.name,
    interval = interval,
    daysOfWeek = daysOfWeek?.map { it.name },
)

fun RecurrenceRuleSnapshot.toEntity(): RecurrenceRule = RecurrenceRule(
    frequency = RecurrenceFrequency.entries.find { it.name == frequency }
        ?: RecurrenceFrequency.DAILY,
    interval = interval,
    daysOfWeek = daysOfWeek?.mapNotNull { dayString ->
        DayOfWeek.entries.find { it.name == dayString }
    },
)

// --- RecurringTask Mappings ---
fun RecurringTask.toSnapshot(): RecurringTaskSnapshot = RecurringTaskSnapshot(
    id = id,
    title = title,
    description = description,
    goalId = goalId,
    duration = duration,
    priority = priority.name,
    points = points,
    recurrenceRule = recurrenceRule.toSnapshot(),
    startDate = startDate,
    endDate = endDate,
)

fun RecurringTaskSnapshot.toEntity(): RecurringTask = RecurringTask(
    id = id,
    title = title,
    description = description,
    goalId = goalId,
    duration = duration,
    priority = enumValueOf<TaskPriority>(priority),
    points = points,
    recurrenceRule = recurrenceRule.toEntity(),
    startDate = startDate,
    endDate = endDate,
)

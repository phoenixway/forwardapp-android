@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusItem
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusType
import com.romankozak.forwardappmobile.shared.core.models.day.DayPlan
import com.romankozak.forwardappmobile.shared.core.models.day.DayStatus
import com.romankozak.forwardappmobile.shared.core.models.day.DayTask
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.day.TaskStatus
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalLinkType
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalRelatedLink
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
@JsName("createCanonicalRelatedLink")
fun createCanonicalRelatedLinkForJs(
    type: String?,
    target: String,
    displayName: String?,
    vault: String?,
): CanonicalRelatedLink =
    CanonicalRelatedLink(
        type = type?.let { enumValueOf<CanonicalLinkType>(it.trim().uppercase()) },
        target = target,
        displayName = displayName,
        vault = vault,
    )

@JsExport
@JsName("createDayPlan")
fun createDayPlanForJs(
    id: String,
    createdAt: Double,
    updatedAt: Double,
    syncedAt: Double?,
    isDeleted: Boolean,
    version: Double,
    dayKey: String,
    name: String?,
    linkedProjectIds: Array<String>,
    linkedAttachmentIds: Array<String>,
    status: String,
    reflection: String?,
    energyLevel: Int?,
    mood: String?,
    weatherConditions: String?,
    predictedDurationMinutes: Double?,
    totalPlannedMinutes: Double,
    totalCompletedMinutes: Double,
    completionPercentage: Float,
): DayPlan =
    DayPlan(
        id = id,
        createdAt = requireJsSafeIntegerLong(createdAt, "DayPlan.createdAt"),
        updatedAt = requireJsSafeIntegerLong(updatedAt, "DayPlan.updatedAt"),
        syncedAt = requireJsSafeIntegerLongOrNull(syncedAt, "DayPlan.syncedAt"),
        isDeleted = isDeleted,
        version = requireJsSafeIntegerLong(version, "DayPlan.version"),
        dayKey = requireLocalDayKey(dayKey),
        name = name,
        linkedProjectIds = linkedProjectIds.toList(),
        linkedAttachmentIds = linkedAttachmentIds.toList(),
        status = enumValueOf<DayStatus>(status.trim().uppercase()),
        reflection = reflection,
        energyLevel = energyLevel,
        mood = mood,
        weatherConditions = weatherConditions,
        predictedDurationMinutes =
            requireJsSafeIntegerLongOrNull(
                predictedDurationMinutes,
                "DayPlan.predictedDurationMinutes",
            ),
        totalPlannedMinutes =
            requireJsSafeIntegerLong(
                totalPlannedMinutes,
                "DayPlan.totalPlannedMinutes",
            ),
        totalCompletedMinutes =
            requireJsSafeIntegerLong(
                totalCompletedMinutes,
                "DayPlan.totalCompletedMinutes",
            ),
        completionPercentage = completionPercentage,
    )

@JsExport
@JsName("createDayTask")
fun createDayTaskForJs(
    id: String,
    createdAt: Double,
    updatedAt: Double,
    syncedAt: Double?,
    isDeleted: Boolean,
    version: Double,
    dayPlanId: String,
    recurrence: RecurrenceOrigin?,
    title: String,
    description: String?,
    goalId: String?,
    projectId: String?,
    linkedProjectIds: Array<String>,
    linkedAttachmentIds: Array<String>,
    activityRecordId: String?,
    taskType: String?,
    entityId: String?,
    order: Double,
    priority: String,
    status: String,
    completed: Boolean,
    scheduledTime: Double?,
    estimatedDurationMinutes: Double?,
    actualDurationMinutes: Double?,
    dueTime: Double?,
    executionStrictness: String?,
    valueImportance: Float,
    valueImpact: Float,
    effort: Float,
    cost: Float,
    risk: Float,
    location: String?,
    tags: Array<String>,
    notes: String?,
    completedAt: Double?,
    points: Int,
): DayTask =
    DayTask(
        id = id,
        createdAt = requireJsSafeIntegerLong(createdAt, "DayTask.createdAt"),
        updatedAt = requireJsSafeIntegerLong(updatedAt, "DayTask.updatedAt"),
        syncedAt = requireJsSafeIntegerLongOrNull(syncedAt, "DayTask.syncedAt"),
        isDeleted = isDeleted,
        version = requireJsSafeIntegerLong(version, "DayTask.version"),
        dayPlanId = dayPlanId,
        recurrence = recurrence,
        title = title,
        description = description,
        goalId = goalId,
        projectId = projectId,
        linkedProjectIds = linkedProjectIds.toList(),
        linkedAttachmentIds = linkedAttachmentIds.toList(),
        activityRecordId = activityRecordId,
        taskType = taskType,
        entityId = entityId,
        order = requireJsSafeIntegerLong(order, "DayTask.order"),
        priority = enumValueOf<TaskPriority>(priority.trim().uppercase()),
        status = enumValueOf<TaskStatus>(status.trim().uppercase()),
        completed = completed,
        scheduledTime =
            requireJsSafeIntegerLongOrNull(
                scheduledTime,
                "DayTask.scheduledTime",
            ),
        estimatedDurationMinutes =
            requireJsSafeIntegerLongOrNull(
                estimatedDurationMinutes,
                "DayTask.estimatedDurationMinutes",
            ),
        actualDurationMinutes =
            requireJsSafeIntegerLongOrNull(
                actualDurationMinutes,
                "DayTask.actualDurationMinutes",
            ),
        dueTime =
            requireJsSafeIntegerLongOrNull(
                dueTime,
                "DayTask.dueTime",
            ),
        executionStrictness = executionStrictness,
        valueImportance = valueImportance,
        valueImpact = valueImpact,
        effort = effort,
        cost = cost,
        risk = risk,
        location = location,
        tags = tags.toList(),
        notes = notes,
        completedAt =
            requireJsSafeIntegerLongOrNull(
                completedAt,
                "DayTask.completedAt",
            ),
        points = points,
    )

@JsExport
@JsName("createDayFocusItem")
fun createDayFocusItemForJs(
    id: String,
    createdAt: Double,
    updatedAt: Double,
    syncedAt: Double?,
    isDeleted: Boolean,
    version: Double,
    dayPlanId: String,
    recurrence: RecurrenceOrigin?,
    title: String,
    notes: String?,
    relatedLinks: Array<CanonicalRelatedLink>,
    type: String,
    budgetPercent: Int?,
    order: Double,
): DayFocusItem =
    DayFocusItem(
        id = id,
        createdAt =
            requireJsSafeIntegerLong(
                createdAt,
                "DayFocusItem.createdAt",
            ),
        updatedAt =
            requireJsSafeIntegerLong(
                updatedAt,
                "DayFocusItem.updatedAt",
            ),
        syncedAt =
            requireJsSafeIntegerLongOrNull(
                syncedAt,
                "DayFocusItem.syncedAt",
            ),
        isDeleted = isDeleted,
        version =
            requireJsSafeIntegerLong(
                version,
                "DayFocusItem.version",
            ),
        dayPlanId = dayPlanId,
        recurrence = recurrence,
        title = title,
        notes = notes,
        relatedLinks = relatedLinks.toList(),
        type = enumValueOf<DayFocusType>(type.trim().uppercase()),
        budgetPercent = budgetPercent,
        order =
            requireJsSafeIntegerLong(
                order,
                "DayFocusItem.order",
            ),
    )


@JsExport
@JsName("createRecurringTaskTemplate")
fun createRecurringTaskTemplateForJs(
    title: String,
    description: String?,
    goalId: String?,
    linkedProjectIds: Array<String>,
    linkedAttachmentIds: Array<String>,
    priority: String,
    estimatedDurationMinutes: Double?,
    points: Int,
    projectId: String?,
    taskType: String?,
    executionStrictness: String?,
): RecurringTaskTemplate =
    RecurringTaskTemplate(
        title = title,
        description = description,
        goalId = goalId,
        linkedProjectIds = linkedProjectIds.toList(),
        linkedAttachmentIds = linkedAttachmentIds.toList(),
        priority = enumValueOf<TaskPriority>(priority.trim().uppercase()),
        estimatedDurationMinutes =
            requireJsSafeIntegerLongOrNull(
                estimatedDurationMinutes,
                "RecurringTaskTemplate.estimatedDurationMinutes",
            ),
        points = points,
        projectId = projectId,
        taskType = taskType,
        executionStrictness = executionStrictness,
    )

@JsExport
@JsName("createRecurringFocusTemplate")
fun createRecurringFocusTemplateForJs(
    title: String,
    notes: String?,
    relatedLinks: Array<CanonicalRelatedLink>,
    budgetPercent: Int?,
): RecurringFocusTemplate =
    RecurringFocusTemplate(
        title = title,
        notes = notes,
        relatedLinks = relatedLinks.toList(),
        budgetPercent = budgetPercent,
    )

@JsExport
@JsName("createRecurringTaskSeries")
fun createRecurringTaskSeriesForJs(
    id: String,
    createdAt: Double,
    updatedAt: Double,
    syncedAt: Double?,
    isDeleted: Boolean,
    version: Double,
    rule: RecurrenceRule,
    startDayKey: String,
    endDayKey: String?,
    template: RecurringTaskTemplate,
): RecurringTaskSeries =
    RecurringTaskSeries(
        id = id,
        createdAt = requireJsSafeIntegerLong(createdAt, "RecurringTaskSeries.createdAt"),
        updatedAt = requireJsSafeIntegerLong(updatedAt, "RecurringTaskSeries.updatedAt"),
        syncedAt =
            requireJsSafeIntegerLongOrNull(
                syncedAt,
                "RecurringTaskSeries.syncedAt",
            ),
        isDeleted = isDeleted,
        version = requireJsSafeIntegerLong(version, "RecurringTaskSeries.version"),
        rule = rule,
        startDayKey = requireLocalDayKey(startDayKey),
        endDayKey = endDayKey?.let(::requireLocalDayKey),
        template = template,
    )

@JsExport
@JsName("createRecurringFocusSeries")
fun createRecurringFocusSeriesForJs(
    id: String,
    createdAt: Double,
    updatedAt: Double,
    syncedAt: Double?,
    isDeleted: Boolean,
    version: Double,
    rule: RecurrenceRule,
    startDayKey: String,
    endDayKey: String?,
    template: RecurringFocusTemplate,
): RecurringFocusSeries =
    RecurringFocusSeries(
        id = id,
        createdAt = requireJsSafeIntegerLong(createdAt, "RecurringFocusSeries.createdAt"),
        updatedAt = requireJsSafeIntegerLong(updatedAt, "RecurringFocusSeries.updatedAt"),
        syncedAt =
            requireJsSafeIntegerLongOrNull(
                syncedAt,
                "RecurringFocusSeries.syncedAt",
            ),
        isDeleted = isDeleted,
        version = requireJsSafeIntegerLong(version, "RecurringFocusSeries.version"),
        rule = rule,
        startDayKey = requireLocalDayKey(startDayKey),
        endDayKey = endDayKey?.let(::requireLocalDayKey),
        template = template,
    )

@JsExport
@JsName("createRecurringResponsibilitySeries")
fun createRecurringResponsibilitySeriesForJs(
    id: String,
    createdAt: Double,
    updatedAt: Double,
    syncedAt: Double?,
    isDeleted: Boolean,
    version: Double,
    rule: RecurrenceRule,
    startDayKey: String,
    endDayKey: String?,
    template: RecurringFocusTemplate,
): RecurringResponsibilitySeries =
    RecurringResponsibilitySeries(
        id = id,
        createdAt =
            requireJsSafeIntegerLong(
                createdAt,
                "RecurringResponsibilitySeries.createdAt",
            ),
        updatedAt =
            requireJsSafeIntegerLong(
                updatedAt,
                "RecurringResponsibilitySeries.updatedAt",
            ),
        syncedAt =
            requireJsSafeIntegerLongOrNull(
                syncedAt,
                "RecurringResponsibilitySeries.syncedAt",
            ),
        isDeleted = isDeleted,
        version =
            requireJsSafeIntegerLong(
                version,
                "RecurringResponsibilitySeries.version",
            ),
        rule = rule,
        startDayKey = requireLocalDayKey(startDayKey),
        endDayKey = endDayKey?.let(::requireLocalDayKey),
        template = template,
    )


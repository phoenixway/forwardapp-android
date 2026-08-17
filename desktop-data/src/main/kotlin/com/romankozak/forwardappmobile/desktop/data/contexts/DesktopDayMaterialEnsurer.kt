package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayFocusItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedRecurringTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata
import java.util.Calendar

class DesktopDayMaterialEnsurer {
    fun ensureToday(
        snapshot: DesktopWorkspaceSnapshot,
        now: Long = System.currentTimeMillis(),
    ): DesktopWorkspaceSnapshot {
        val todayStart = localDayStart(now)
        val todayPlans = snapshot.dayPlans.filter { plan -> localDayStart(plan.date) == todayStart }
        val todayPlan = todayPlans.firstOrNull() ?: createTodayPlan(todayStart = todayStart, now = now)
        val todayPlanIds = (todayPlans.map { plan -> plan.id } + todayPlan.id).toSet()
        val focusResult = ensureEverydayFocusItems(snapshot, todayPlan, todayPlanIds, now)
        val taskResult = ensureRecurringTasks(snapshot, todayPlan, todayPlanIds, todayStart, now)
        val dayPlans =
            if (snapshot.dayPlans.any { plan -> plan.id == todayPlan.id }) {
                snapshot.dayPlans
            } else {
                snapshot.dayPlans + todayPlan
            }
        return snapshot.copy(
            dayPlans = dayPlans,
            dayFocusItems = focusResult,
            dayTasks = taskResult,
        )
    }

    private fun ensureEverydayFocusItems(
        snapshot: DesktopWorkspaceSnapshot,
        todayPlan: SharedDayPlan,
        todayPlanIds: Set<String>,
        now: Long,
    ): List<SharedDayFocusItem> {
        val existingTodayKeys =
            snapshot.dayFocusItems
                .filter { item -> !item.isDeleted && item.dayPlanId in todayPlanIds }
                .mapTo(hashSetOf()) { item -> item.recurringKey ?: item.id }
        val sourceItems =
            snapshot.dayFocusItems
                .filter { item -> !item.isDeleted && item.isEveryday }
                .groupBy { item -> item.recurringKey ?: item.id }
                .mapValues { (_, items) -> items.maxBy { item -> item.sync.updatedAt.takeIf { it > 0L } ?: item.sync.createdAt } }
        val additions =
            sourceItems
                .filterKeys { key -> key !in existingTodayKeys }
                .values
                .mapIndexed { index, source ->
                    val recurringKey = source.recurringKey ?: source.id
                    source.copy(
                        id = "day-focus-${todayPlan.id}-$recurringKey",
                        dayPlanId = todayPlan.id,
                        recurringKey = recurringKey,
                        order = snapshot.dayFocusItems.count { item -> item.dayPlanId in todayPlanIds }.toLong() + index,
                        sync = SharedSyncMetadata(createdAt = now, updatedAt = now, version = 1),
                    )
                }
        return snapshot.dayFocusItems + additions
    }

    private fun ensureRecurringTasks(
        snapshot: DesktopWorkspaceSnapshot,
        todayPlan: SharedDayPlan,
        todayPlanIds: Set<String>,
        todayStart: Long,
        now: Long,
    ): List<SharedDayTask> {
        val allPlanIds = snapshot.dayPlans.map { plan -> plan.id }
        val existingTodayKeys =
            snapshot.dayTasks
                .filter { task -> !task.isDeleted && task.dayPlanId in todayPlanIds }
                .flatMapTo(hashSetOf()) { task ->
                    listOfNotNull(task.recurringKey(allPlanIds), normalizeRecurringTitle(task.title))
                }
        val baseOrder = minOrderForToday(snapshot.dayTasks, todayPlanIds)
        val masterAdditions =
            snapshot.recurringTasks
                .filter { task ->
                    task.matchesDate(todayStart) &&
                        task.id !in existingTodayKeys &&
                        normalizeRecurringTitle(task.title) !in existingTodayKeys
                }
                .mapIndexed { index, recurringTask ->
                    SharedDayTask(
                        id = recurringTaskInstanceId(todayPlan.id, recurringTask.id),
                        dayPlanId = todayPlan.id,
                        title = recurringTask.title,
                        description = recurringTask.description,
                        projectId = null,
                        linkedProjectIds = recurringTask.linkedProjectIds,
                        recurringTaskId = recurringTask.id,
                        taskType = "GOAL",
                        isDone = false,
                        priority = recurringTask.priority,
                        order = baseOrder - additionsOffset(index),
                        estimatedDurationMinutes = recurringTask.duration?.toLong(),
                        points = recurringTask.points,
                        sync = SharedSyncMetadata(createdAt = now, updatedAt = now, version = 1),
                    )
                }
        val fallbackAdditions =
            buildFallbackRecurringTaskAdditions(
                snapshot = snapshot,
                todayPlan = todayPlan,
                allPlanIds = allPlanIds,
                blockedKeys = existingTodayKeys + masterAdditions.mapNotNull { task -> task.recurringTaskId },
                baseOrder = baseOrder,
                offset = masterAdditions.size,
                now = now,
            )
        return snapshot.dayTasks + masterAdditions + fallbackAdditions
    }

    private fun buildFallbackRecurringTaskAdditions(
        snapshot: DesktopWorkspaceSnapshot,
        todayPlan: SharedDayPlan,
        allPlanIds: List<String>,
        blockedKeys: Set<String>,
        baseOrder: Long,
        offset: Int,
        now: Long,
    ): List<SharedDayTask> {
        val planDatesById = snapshot.dayPlans.associate { plan -> plan.id to plan.date }
        val masterIds = snapshot.recurringTasks.mapTo(hashSetOf()) { task -> task.id }
        return snapshot.dayTasks
            .asSequence()
            .filter { task -> !task.isDeleted && task.dayPlanId != todayPlan.id }
            .mapNotNull { task ->
                val sourceDay = planDatesById[task.dayPlanId] ?: return@mapNotNull null
                if (sourceDay >= todayPlan.date) return@mapNotNull null
                val key = task.recurringKey(allPlanIds) ?: return@mapNotNull null
                key to (sourceDay to task)
            }
            .groupBy({ item -> item.first }, { item -> item.second })
            .entries
            .mapIndexedNotNull { index, (key, sources) ->
                if (key in blockedKeys) return@mapIndexedNotNull null
                val source =
                    sources.maxWithOrNull(
                        compareBy<Pair<Long, SharedDayTask>> { item -> item.first }
                            .thenBy { item -> item.second.sync.updatedAt.takeIf { it > 0L } ?: item.second.sync.createdAt },
                    )?.second ?: return@mapIndexedNotNull null
                source.copy(
                    id = recurringTaskInstanceId(todayPlan.id, key),
                    dayPlanId = todayPlan.id,
                    recurringTaskId = key.takeIf { it in masterIds },
                    isDone = false,
                    isDeleted = false,
                    order = baseOrder - additionsOffset(offset + index),
                    sync = SharedSyncMetadata(createdAt = now, updatedAt = now, version = 1),
                )
            }
    }

    private fun minOrderForToday(
        tasks: List<SharedDayTask>,
        todayPlanIds: Set<String>,
    ): Long = tasks.filter { task -> task.dayPlanId in todayPlanIds }.minOfOrNull { task -> task.order } ?: 0L

    private fun additionsOffset(index: Int): Long = (index + 1).toLong()

    private fun SharedDayTask.recurringKey(allPlanIds: Collection<String>): String? {
        recurringTaskId?.let { return it }
        return allPlanIds.firstNotNullOfOrNull { planId ->
            val prefix = "recurring-task-instance-$planId-"
            id.removePrefix(prefix).takeIf { value -> value != id && value.isNotBlank() }
        }
    }

    private fun normalizeRecurringTitle(title: String): String =
        title
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    private fun createTodayPlan(
        todayStart: Long,
        now: Long,
    ): SharedDayPlan =
        SharedDayPlan(
            id = "desktop-day-plan-$todayStart",
            date = todayStart,
            name = null,
            status = "PLANNED",
            sync = SharedSyncMetadata(createdAt = now, updatedAt = now, version = 1),
        )

    private fun SharedRecurringTask.matchesDate(dayStart: Long): Boolean {
        if (dayStart < localDayStart(startDate)) return false
        val end = endDate
        if (end != null && dayStart > localDayStart(end)) return false
        val calendar = Calendar.getInstance().apply { timeInMillis = dayStart }
        return when (frequency.uppercase()) {
            "HOURLY", "DAILY" -> true
            "WEEKLY" -> daysOfWeek.contains(calendarDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))
            "MONTHLY" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startDate }
                calendar.get(Calendar.DAY_OF_MONTH) == start.get(Calendar.DAY_OF_MONTH)
            }
            "YEARLY" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startDate }
                calendar.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)
            }
            else -> false
        }
    }

    private fun localDayStart(timestamp: Long): Long =
        Calendar
            .getInstance()
            .apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

    private fun calendarDayOfWeek(dayOfWeek: Int): String =
        when (dayOfWeek) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> ""
        }

    private fun recurringTaskInstanceId(
        dayPlanId: String,
        recurringTaskId: String,
    ): String = "recurring-task-instance-$dayPlanId-$recurringTaskId"
}

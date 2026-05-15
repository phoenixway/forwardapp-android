package com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.features.daymanagement.utils.formatDayTime
import javax.inject.Inject
import javax.inject.Singleton

data class TaskExecutionReminderSpec(
    val requestCode: Int,
    val triggerAt: Long,
    val title: String,
    val message: String,
    val extraInfo: String?,
)

@Singleton
class TaskExecutionReminderPolicy
    @Inject
    constructor() {
        private companion object {
            const val FIFTEEN_MINUTES = 15 * 60 * 1000L
            const val TEN_MINUTES = 10 * 60 * 1000L
            const val HARD_ALERT_COUNT = 4
        }

        fun build(task: DayTask, now: Long = System.currentTimeMillis()): List<TaskExecutionReminderSpec> {
            val dueTime = task.dueTime ?: return emptyList()
            if (task.completed) return emptyList()

            val triggerTimes =
                when (task.executionStrictness) {
                    TaskExecutionStrictness.SOFT -> listOf(dueTime)
                    TaskExecutionStrictness.NORMAL -> listOf(dueTime + FIFTEEN_MINUTES)
                    TaskExecutionStrictness.HARD -> List(HARD_ALERT_COUNT) { index -> dueTime + (index * TEN_MINUTES) }
                }.filter { it > now }

            return triggerTimes.mapIndexed { index, triggerAt ->
                TaskExecutionReminderSpec(
                    requestCode = reminderRequestCode(task.id, index),
                    triggerAt = triggerAt,
                    title = task.title,
                    message = buildMessage(task.executionStrictness, dueTime),
                    extraInfo = "TaskDeadline:${task.id}",
                )
            }
        }

        fun requestCodes(taskId: String): List<Int> =
            List(HARD_ALERT_COUNT) { index -> reminderRequestCode(taskId, index) }

        private fun reminderRequestCode(taskId: String, index: Int): Int =
            ((taskId.hashCode() * 31) + index).and(Int.MAX_VALUE)

        private fun buildMessage(strictness: TaskExecutionStrictness, dueTime: Long): String =
            when (strictness) {
                TaskExecutionStrictness.SOFT -> "М'яке нагадування: дедлайн ${formatDayTime(dueTime)}."
                TaskExecutionStrictness.NORMAL -> "Час завершувати. Пільгові 15 хв після дедлайну вже минули."
                TaskExecutionStrictness.HARD -> "Жорсткий ліміт: час завершувати зараз."
            }
    }

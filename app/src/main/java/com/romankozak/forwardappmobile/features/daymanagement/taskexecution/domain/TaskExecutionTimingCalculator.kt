package com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain

import javax.inject.Inject
import javax.inject.Singleton

data class TaskExecutionTimingRequest(
    val scheduledTime: Long?,
    val dueTime: Long?,
    val durationMinutes: Long?,
    val actualStartTime: Long? = null,
)

data class TaskExecutionTimingResolution(
    val scheduledTime: Long?,
    val dueTime: Long?,
)

@Singleton
class TaskExecutionTimingCalculator
    @Inject
    constructor() {
        private companion object {
            const val MILLIS_PER_MINUTE = 60_000L
        }

        fun resolve(request: TaskExecutionTimingRequest): TaskExecutionTimingResolution {
            val durationMillis = request.durationMinutes?.takeIf { it > 0 }?.times(MILLIS_PER_MINUTE)
            val resolvedScheduledTime =
                request.scheduledTime
                    ?: if (request.dueTime != null && durationMillis != null) {
                        request.dueTime - durationMillis
                    } else {
                        null
                    }
            val resolvedDueTime =
                request.dueTime
                    ?: when {
                        resolvedScheduledTime != null && durationMillis != null -> resolvedScheduledTime + durationMillis
                        request.actualStartTime != null && durationMillis != null -> request.actualStartTime + durationMillis
                        else -> null
                    }

            return TaskExecutionTimingResolution(
                scheduledTime = resolvedScheduledTime,
                dueTime = resolvedDueTime,
            )
        }
    }

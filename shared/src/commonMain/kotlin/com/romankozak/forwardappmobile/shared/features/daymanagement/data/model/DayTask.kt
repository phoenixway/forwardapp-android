package com.romankozak.forwardappmobile.shared.features.daymanagement.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TaskPriority { LOW, MEDIUM, HIGH, CRITICAL, NONE }

@Serializable
enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, DEFERRED }

@Serializable
data class DayTask(
    val id: String = UUID.randomUUID().toString(),
    val dayPlanId: String,
    val title: String,
    val description: String? = null,

    val goalId: String? = null,
    val projectId: String? = null,
    val activityRecordId: String? = null,
    val recurringTaskId: String? = null,

    val taskType: String? = null,
    val entityId: String? = null,

    val order: Long = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val completed: Boolean = false,

    val scheduledTime: Long? = null,
    val estimatedDurationMinutes: Long? = null,
    val actualDurationMinutes: Long? = null,
    val dueTime: Long? = null,

    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,

    val location: String? = null,
    val tags: List<String>? = null,
    val notes: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val completedAt: Long? = null,
    val nextOccurrenceTime: Long? = null,
    val points: Int = 0,
)

package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import com.google.gson.annotations.SerializedName
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import java.util.UUID

/**
 * Recurrence-v1 parser DTO retained temporarily for old wire fields.
 * It is no longer a Room entity or recurrence source of truth.
 */
data class RecurringTask(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("goalId")
    val goalId: String? = null,
    @SerializedName("linkedProjectIds")
    val linkedProjectIds: List<String>? = emptyList(),
    @SerializedName("linkedAttachmentIds")
    val linkedAttachmentIds: List<String>? = emptyList(),
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("priority")
    val priority: TaskPriority,
    @SerializedName("points")
    val points: Int = 0,
    @SerializedName("recurrenceRule")
    val recurrenceRule: RecurrenceRule,
    @SerializedName("startDate")
    val startDate: Long,
    @SerializedName("endDate")
    val endDate: Long? = null,
)

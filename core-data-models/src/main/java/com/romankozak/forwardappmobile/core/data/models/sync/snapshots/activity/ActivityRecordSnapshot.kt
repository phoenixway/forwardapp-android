// File: ActivityRecordSnapshot.kt

package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.activity

import com.google.gson.annotations.SerializedName

data class ActivityRecordSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("startTime") val startTime: Long?,
    @SerializedName("endTime") val endTime: Long?,
    @SerializedName("text") val text: String,
    @SerializedName("rawNoteText") val rawNoteText: String? = null,
    @SerializedName("noteText") val noteText: String? = null,
    @SerializedName("stateEventType") val stateEventType: String? = null,
    @SerializedName("stateEventCrisisLevel") val stateEventCrisisLevel: Int? = null,
    @SerializedName("stateEventLabel") val stateEventLabel: String? = null,
    @SerializedName("stateEventApplied") val stateEventApplied: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("version") val version: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,

    @SerializedName("targetId") val targetId: String?,
    @SerializedName("targetType") val targetType: String?,
    @SerializedName("goalId") val goalId: String?,
    @SerializedName("contextId") val contextId: String?,
    @SerializedName("reminderTime") val reminderTime: Long?,

    @SerializedName("xpGained") val xpGained: Int, // Залишаємо Int, але в мапері додаємо ?: 0
    @SerializedName("antyXp") val antyXp: Int?    // Дозволяємо null, як ти і хотів
)

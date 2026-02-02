package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

import com.google.gson.annotations.SerializedName

data class DayPlanSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("date") val date: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("status") val status: String,
    @SerializedName("reflection") val reflection: String?,
    @SerializedName("energyLevel") val energyLevel: Int?,
    @SerializedName("mood") val mood: String?,
    @SerializedName("weatherConditions") val weatherConditions: String?,
    @SerializedName("totalPlannedMinutes") val totalPlannedMinutes: Long,
    @SerializedName("totalCompletedMinutes") val totalCompletedMinutes: Long,
    @SerializedName("completionPercentage") val completionPercentage: Float,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long,
)
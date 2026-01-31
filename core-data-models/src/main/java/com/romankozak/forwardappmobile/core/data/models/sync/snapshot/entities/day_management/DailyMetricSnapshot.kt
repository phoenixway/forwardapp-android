package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.day_management

import com.google.gson.annotations.SerializedName

data class DailyMetricSnapshot(
    @SerializedName("id") val id: String,
    @SerializedName("dayPlanId") val dayPlanId: String,
    @SerializedName("date") val date: Long,
    @SerializedName("tasksPlanned") val tasksPlanned: Int,
    @SerializedName("tasksCompleted") val tasksCompleted: Int,
    @SerializedName("completionRate") val completionRate: Float,
    @SerializedName("totalPlannedTime") val totalPlannedTime: Long,
    @SerializedName("totalActiveTime") val totalActiveTime: Long,
    @SerializedName("completedPoints") val completedPoints: Int,
    @SerializedName("totalBreakTime") val totalBreakTime: Long,
    @SerializedName("morningEnergyLevel") val morningEnergyLevel: Int?,
    @SerializedName("eveningEnergyLevel") val eveningEnergyLevel: Int?,
    @SerializedName("overallMood") val overallMood: String?,
    @SerializedName("stressLevel") val stressLevel: Int?,
    @SerializedName("customMetrics") val customMetrics: Map<String, Float>?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isDeleted") val isDeleted: Boolean,
    @SerializedName("version") val version: Long
)

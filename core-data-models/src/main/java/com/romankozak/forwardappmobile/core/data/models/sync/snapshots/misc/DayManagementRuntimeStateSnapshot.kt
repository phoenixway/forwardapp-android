package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc

import com.google.gson.annotations.SerializedName

data class DayManagementRuntimeStateSnapshot(
    @SerializedName("sessionId") val sessionId: String?,
    @SerializedName("calendarAnchorDate") val calendarAnchorDate: Long?,
    @SerializedName("wokeAt") val wokeAt: Long?,
    @SerializedName("sleepAt") val sleepAt: Long?,
    @SerializedName("currentPhase") val currentPhase: String,
    @SerializedName("phaseStartedAt") val phaseStartedAt: Long?,
    @SerializedName("dayFocusFinalizedAt") val dayFocusFinalizedAt: Long?,
    @SerializedName("dayPlanFinalizedAt") val dayPlanFinalizedAt: Long?,
    @SerializedName("implementationStartedAt") val implementationStartedAt: Long?,
    @SerializedName("finalizationStartedAt") val finalizationStartedAt: Long?,
    @SerializedName("activeAlarmIds") val activeAlarmIds: Set<String>,
    @SerializedName("riskFlags") val riskFlags: Set<String>,
    @SerializedName("updatedAt") val updatedAt: Long?,
)

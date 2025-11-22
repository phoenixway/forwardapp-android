package com.romankozak.forwardappmobile.domain.lifestate.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LifeStateTrackerEntry(
    val id: String,
    @SerialName("timestampStart") val timestampStart: Long?,
    @SerialName("timestampEnd") val timestampEnd: Long?,
    val category: String? = null,
    @SerialName("label") val label: String? = null,
    val notes: String? = null,
    val energy: Float? = null,
    val stress: Float? = null,
    val importance: Float? = null,
    val satisfaction: Float? = null,
    val projectId: String? = null,
    val goalId: String? = null,
    val targetId: String? = null,
    val targetType: String? = null,
)

@Serializable
data class LifeStatePromptPayload(
    @SerialName("TRACKER_ENTRIES_JSON") val trackerEntries: List<LifeStateTrackerEntry>,
    @SerialName("SYSTEM_APP_NOTE_TEXT") val systemAppNoteText: String = "",
)

@Serializable
data class AiAnalysis(
    val summary: String = "",
    @SerialName("key_processes") val keyProcesses: List<String> = emptyList(),
    val signals: AiSignals = AiSignals(),
    val risks: List<AiRisk> = emptyList(),
    val opportunities: List<AiOpportunity> = emptyList(),
    val recommendations: List<AiRecommendation> = emptyList(),
)

@Serializable
data class AiSignals(
    val positive: List<String> = emptyList(),
    val negative: List<String> = emptyList(),
)

@Serializable
data class AiRisk(
    val name: String = "",
    val description: String = "",
    val severity: String = "medium",
)

@Serializable
data class AiOpportunity(
    val name: String = "",
    val description: String = "",
)

@Serializable
data class AiRecommendation(
    val title: String = "",
    val message: String = "",
    val actions: List<AiAction> = emptyList(),
)

@Serializable
data class AiAction(
    val id: String = "",
    val label: String = "",
    val payload: Map<String, String?> = emptyMap(),
)

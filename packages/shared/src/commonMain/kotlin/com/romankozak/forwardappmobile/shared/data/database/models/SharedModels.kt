package com.romankozak.forwardappmobile.shared.data.database.models

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val tags: List<String>? = null,
    val relatedLinks: List<RelatedLink>? = null,
    val isExpanded: Boolean = true,
    val order: Long = 0,
    val isAttachmentsExpanded: Boolean = false,
    val defaultViewModeName: String? = null,
    val isCompleted: Boolean = false,
    val isProjectManagementEnabled: Boolean? = false,
    val projectStatus: String? = ProjectStatusValues.NO_PLAN,
    val projectStatusText: String? = null,
    val projectLogLevel: String? = ProjectLogLevelValues.NORMAL,
    val totalTimeSpentMinutes: Long? = 0,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val showCheckboxes: Boolean = false,
    val projectType: ProjectType = ProjectType.DEFAULT,
    val reservedGroup: String? = null
)

@Serializable
enum class ProjectType {
    DEFAULT,
    RESERVED,
    SYSTEM;

    companion object {
        fun fromString(value: String?): ProjectType {
            return try {
                if (value == null) ProjectType.DEFAULT else valueOf(value)
            } catch (e: IllegalArgumentException) {
                ProjectType.DEFAULT
            }
        }
    }
}

object ProjectStatusValues {
    const val NO_PLAN = "NO_PLAN"
    const val PLANNING = "PLANNING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val ON_HOLD = "ON_HOLD"
    const val PAUSED = "PAUSED"

    fun getDisplayName(status: String?): String {
        return when (status) {
            NO_PLAN -> "Без плану"
            PLANNING -> "Планується"
            IN_PROGRESS -> "В реалізації"
            COMPLETED -> "Завершено"
            ON_HOLD -> "Відкладено"
            PAUSED -> "На паузі"
            else -> "Без плану"
        }
    }
}

object ProjectLogLevelValues {
    const val DETAILED = "DETAILED"
    const val NORMAL = "NORMAL"
}

object ScoringStatusValues {
    const val NOT_ASSESSED = "NOT_ASSESSED"
    const val IMPOSSIBLE_TO_ASSESS = "IMPOSSIBLE_TO_ASSESS"
    const val ASSESSED = "ASSESSED"
}
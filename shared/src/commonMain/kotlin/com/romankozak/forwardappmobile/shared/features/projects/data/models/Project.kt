package com.romankozak.forwardappmobile.shared.features.projects.data.models

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.data.models.ProjectStatusValues
import com.romankozak.forwardappmobile.shared.data.models.ProjectLogLevelValues
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues

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
    val goalOrder: Long = 0,
    val isAttachmentsExpanded: Boolean = false,
    val defaultViewMode: String? = null,
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
    val reservedGroup: ReservedGroup? = null
)

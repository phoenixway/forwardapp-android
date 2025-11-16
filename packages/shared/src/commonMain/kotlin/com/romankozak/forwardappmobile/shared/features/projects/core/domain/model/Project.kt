package com.romankozak.forwardappmobile.shared.features.projects.core.domain.model

import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.data.models.ProjectStatusValues

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
    val projectLogLevel: Long? = 0L,
    val totalTimeSpentMinutes: Long? = 0,
    val valueImportance: Double = 0.0,
    val valueImpact: Double = 0.0,
    val effort: Double = 0.0,
    val cost: Double = 0.0,
    val risk: Double = 0.0,
    val weightEffort: Double = 1.0,
    val weightCost: Double = 1.0,
    val weightRisk: Double = 1.0,
    val rawScore: Double = 0.0,
    val displayScore: Long = 0L,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val showCheckboxes: Boolean = false,
    val projectType: ProjectType = ProjectType.DEFAULT,
    val reservedGroup: ReservedGroup? = null
)

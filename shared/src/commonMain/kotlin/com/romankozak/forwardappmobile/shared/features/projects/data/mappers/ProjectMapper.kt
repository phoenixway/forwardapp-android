package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.features.projects.domain.model.Project
import com.romankozak.forwardappmobile.shared.database.booleanAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.features.projects.domain.model.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.data.models.ScoringStatusValues

fun Projects.toDomain(): Project {
    return Project(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags,
        relatedLinks = relatedLinks,
        isExpanded = isExpanded,
        goalOrder = goalOrder,
        isAttachmentsExpanded = isAttachmentsExpanded,
        defaultViewMode = defaultViewMode,
        isCompleted = isCompleted,
        isProjectManagementEnabled = isProjectManagementEnabled,
        projectStatus = projectStatus,
        projectStatusText = projectStatusText,
        projectLogLevel = projectLogLevel?.toString(),
        totalTimeSpentMinutes = totalTimeSpentMinutes ?: 0L,
        valueImportance = valueImportance ?: 0.0,
        valueImpact = valueImpact ?: 0.0,
        effort = effort ?: 0.0,
        cost = cost ?: 0.0,
        risk = risk ?: 0.0,
        weightEffort = weightEffort ?: 1.0,
        weightCost = weightCost ?: 1.0,
        weightRisk = weightRisk ?: 1.0,
        rawScore = rawScore ?: 0.0,
        displayScore = displayScore ?: 0L,
        scoringStatus = scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
        showCheckboxes = showCheckboxes,
        projectType = projectType ?: ProjectType.DEFAULT,
        reservedGroup = reservedGroup
    )
}

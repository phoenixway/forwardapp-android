package com.romankozak.forwardappmobile.shared.features.projects.data.mappers

import com.romankozak.forwardappmobile.shared.database.Projects
import com.romankozak.forwardappmobile.shared.features.projects.data.models.Project
import com.romankozak.forwardappmobile.shared.database.booleanAdapter
import com.romankozak.forwardappmobile.shared.database.stringListAdapter
import com.romankozak.forwardappmobile.shared.database.relatedLinksListAdapter
import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup

fun Projects.toDomain(): Project {
    return Project(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags?.let { stringListAdapter.decode(it) },
        relatedLinks = relatedLinks?.let { relatedLinksListAdapter.decode(it) },
        isExpanded = booleanAdapter.decode(isExpanded),
        goalOrder = goalOrder,
        isAttachmentsExpanded = booleanAdapter.decode(isAttachmentsExpanded),
        defaultViewMode = defaultViewMode,
        isCompleted = booleanAdapter.decode(isCompleted),
        isProjectManagementEnabled = isProjectManagementEnabled?.let { booleanAdapter.decode(it) },
        projectStatus = projectStatus,
        projectStatusText = projectStatusText,
        projectLogLevel = projectLogLevel,
        totalTimeSpentMinutes = totalTimeSpentMinutes,
        valueImportance = valueImportance.toFloat(),
        valueImpact = valueImpact.toFloat(),
        effort = effort.toFloat(),
        cost = cost.toFloat(),
        risk = risk.toFloat(),
        weightEffort = weightEffort.toFloat(),
        weightCost = weightCost.toFloat(),
        weightRisk = weightRisk.toFloat(),
        rawScore = rawScore.toFloat(),
        displayScore = displayScore.toInt(),
        scoringStatus = scoringStatus,
        showCheckboxes = booleanAdapter.decode(showCheckboxes),
        projectType = ProjectType.fromString(projectType),
        reservedGroup = reservedGroup?.let { ReservedGroup.fromString(it) }
    )
}

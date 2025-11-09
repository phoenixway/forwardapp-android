package com.romankozak.forwardappmobile.shared.features.projects.data

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.database.Projects

fun Projects.toModel(): Project {
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
        order = goalOrder,
        isAttachmentsExpanded = isAttachmentsExpanded,
        defaultViewModeName = defaultViewMode,
        isCompleted = isCompleted,
        isProjectManagementEnabled = isProjectManagementEnabled,
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
        showCheckboxes = showCheckboxes,
        projectType = projectType,
        reservedGroup = reservedGroup?.groupName
    )
}

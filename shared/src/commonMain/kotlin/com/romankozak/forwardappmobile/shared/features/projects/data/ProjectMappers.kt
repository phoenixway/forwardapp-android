package com.romankozak.forwardappmobile.shared.features.projects.data

import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.database.ProjectQueriesQueries
import com.romankozak.forwardappmobile.shared.database.Projects
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json =
    Json {
        ignoreUnknownKeys = true
    }

fun Projects.toModel(): Project =
    Project(
        id = id,
        name = name,
        description = description,
        parentId = parentId?.normalizeParentIdFromDb(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags?.toTagList(),
        relatedLinks = relatedLinks?.toRelatedLinkList(),
        isExpanded = is_expanded.toBoolean(),
        order = goal_order,
        isAttachmentsExpanded = is_attachments_expanded.toBoolean(),
        defaultViewModeName = default_view_mode,
        isCompleted = is_completed.toBoolean(),
        isProjectManagementEnabled = is_project_management_enabled?.toBoolean(),
        projectStatus = project_status,
        projectStatusText = project_status_text,
        projectLogLevel = project_log_level,
        totalTimeSpentMinutes = total_time_spent_minutes,
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
        scoringStatus = scoring_status,
        showCheckboxes = show_checkboxes.toBoolean(),
        projectType = ProjectType.fromString(project_type),
        reservedGroup = reserved_group,
    )

fun ProjectQueriesQueries.insertOrReplace(project: Project) {
    insertProject(
        id = project.id,
        name = project.name,
        description = project.description,
        parentId = project.parentId?.normalizeParentIdForStorage(),
        createdAt = project.createdAt,
        updatedAt = project.updatedAt,
        tags = project.tags.toTagString(),
        relatedLinks = project.relatedLinks.toRelatedLinkString(),
        is_expanded = project.isExpanded.toLong(),
        goal_order = project.order,
        is_attachments_expanded = project.isAttachmentsExpanded.toLong(),
        default_view_mode = project.defaultViewModeName,
        is_completed = project.isCompleted.toLong(),
        is_project_management_enabled = project.isProjectManagementEnabled?.toLong(),
        project_status = project.projectStatus,
        project_status_text = project.projectStatusText,
        project_log_level = project.projectLogLevel,
        total_time_spent_minutes = project.totalTimeSpentMinutes,
        valueImportance = project.valueImportance.toDouble(),
        valueImpact = project.valueImpact.toDouble(),
        effort = project.effort.toDouble(),
        cost = project.cost.toDouble(),
        risk = project.risk.toDouble(),
        weightEffort = project.weightEffort.toDouble(),
        weightCost = project.weightCost.toDouble(),
        weightRisk = project.weightRisk.toDouble(),
        rawScore = project.rawScore.toDouble(),
        displayScore = project.displayScore.toLong(),
        scoring_status = project.scoringStatus,
        show_checkboxes = project.showCheckboxes.toLong(),
        project_type = project.projectType.name,
        reserved_group = project.reservedGroup,
    )
}

private fun String.normalizeParentIdFromDb(): String? {
    val cleaned = trim()
    if (cleaned.isEmpty() || cleaned.equals("null", ignoreCase = true)) return null
    return cleaned
}

private fun String.normalizeParentIdForStorage(): String? =
    trim()
        .takeIf { it.isNotEmpty() && !equals("null", ignoreCase = true) }

private fun Long.toBoolean(): Boolean = this != 0L

private fun Long?.toBoolean(): Boolean? = this?.let { it != 0L }

private fun Boolean.toLong(): Long = if (this) 1L else 0L

private fun Boolean?.toLong(): Long? = this?.let { if (it) 1L else 0L }

private fun String?.toTagList(): List<String>? =
    this
        ?.split(" / ")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.takeIf { it.isNotEmpty() }

private fun List<String>?.toTagString(): String? =
    this
        ?.filter { it.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(" / ")

private fun String?.toRelatedLinkList(): List<RelatedLink>? =
    this
        ?.takeIf { it.isNotBlank() }
        ?.let { json.decodeFromString<List<RelatedLink>>(it) }

private fun List<RelatedLink>?.toRelatedLinkString(): String? =
    this
        ?.takeIf { it.isNotEmpty() }
        ?.let { json.encodeToString(it) }

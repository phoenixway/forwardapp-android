package com.romankozak.forwardappmobile.shared.data.database

import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Projects
import kotlinx.datetime.Clock

private data class SpecialProject(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: (() -> String)?,
    val projectType: ProjectType,
    val reservedGroup: ReservedGroup?,
    val order: Long
)

class DatabaseInitializer(
    private val database: ForwardAppDatabase
) {

    private val specialProjects by lazy {
        listOf(
            SpecialProject("special-project-id", "special", null, null, ProjectType.SYSTEM, null, 0),
            SpecialProject("inbox-project-id", "inbox", "Default inbox for new items", { "special-project-id" }, ProjectType.RESERVED, ReservedGroup.Inbox, 0),
            SpecialProject("strategic-group-id", "strategic", null, { "special-project-id" }, ProjectType.RESERVED, ReservedGroup.StrategicGroup, 1),
            SpecialProject("main-beacon-realization-id", "main-beacon-realization", null, { "special-project-id" }, ProjectType.RESERVED, ReservedGroup.MainBeaconsGroup, 2),
            SpecialProject("main-beacon-list-id", "list", null, { "main-beacon-realization-id" }, ProjectType.RESERVED, null, 0),
            SpecialProject("mission-project-id", "mission", "Mission project", { "main-beacon-list-id" }, ProjectType.RESERVED, ReservedGroup.MainBeacons, 0)
        )
    }

    suspend fun initialize() {
        database.projectsQueries.transaction {
            specialProjects.forEach { projectInfo ->
                val existingProject = if (projectInfo.reservedGroup != null) {
                    database.projectsQueries.getProjectsByReservedGroup(projectInfo.reservedGroup).executeAsOneOrNull()
                } else {
                    database.projectsQueries.getProjectById(projectInfo.id).executeAsOneOrNull()
                }

                if (existingProject == null) {
                    insertProject(projectInfo)
                } else {
                    if (existingProject.parentId != projectInfo.parentId?.invoke()) {
                        database.projectsQueries.updateParent(projectInfo.parentId?.invoke(), existingProject.id)
                    }
                }
            }
        }
    }

    private fun insertProject(projectInfo: SpecialProject) {
        database.projectsQueries.insertProject(
            id = projectInfo.id,
            name = projectInfo.name,
            description = projectInfo.description,
            parentId = projectInfo.parentId?.invoke(),
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = null,
            tags = emptyList(),
            relatedLinks = emptyList(),
            isExpanded = false,
            goalOrder = projectInfo.order,
            isAttachmentsExpanded = false,
            defaultViewMode = null,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = null,
            projectStatusText = null,
            projectLogLevel = null,
            totalTimeSpentMinutes = 0,
            valueImportance = 0.0,
            valueImpact = 0.0,
            effort = 0.0,
            cost = 0.0,
            risk = 0.0,
            weightEffort = 1.0,
            weightCost = 1.0,
            weightRisk = 1.0,
            rawScore = 0.0,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            projectType = projectInfo.projectType,
            reservedGroup = projectInfo.reservedGroup
        )
    }
}
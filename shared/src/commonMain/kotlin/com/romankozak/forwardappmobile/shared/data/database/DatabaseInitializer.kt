package com.romankozak.forwardappmobile.shared.data.database

import com.romankozak.forwardappmobile.shared.data.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.datetime.Clock

class DatabaseInitializer(
    private val database: ForwardAppDatabase
) {

    suspend fun initialize() {
        val specialProject = database.projectsQueries.getProjectsByType(ProjectType.SYSTEM).executeAsOneOrNull()
        if (specialProject == null) {
            prePopulateProjects()
        }
    }

    private fun prePopulateProjects() {
        database.projectsQueries.transaction {
            val specialProjectId = "special-project-id"
            database.projectsQueries.insertProject(
                id = specialProjectId,
                name = "special",
                description = null,
                parentId = null,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = null,
                tags = null,
                relatedLinks = null,
                isExpanded = false,
                goalOrder = 0,
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
                projectType = ProjectType.SYSTEM,
                reservedGroup = null
            )

            // TODO: ініціалізувати тут інбокси
            val inboxProjectId = "inbox-project-id"
            database.projectsQueries.insertProject(
                id = inboxProjectId,
                name = "inbox",
                description = "Default inbox for new items",
                parentId = specialProjectId,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                updatedAt = null,
                tags = null,
                relatedLinks = null,
                isExpanded = false,
                goalOrder = 0,
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
                projectType = ProjectType.RESERVED,
                reservedGroup = ReservedGroup.Inbox
            )

            createStrategicProjects(specialProjectId)
            createMainBeaconsProjects(specialProjectId)
        }
    }

    private fun createStrategicProjects(specialProjectId: String) {
        val strategicGroupId = "strategic-group-id"
        database.projectsQueries.insertProject(
            id = strategicGroupId,
            name = "strategic",
            description = null,
            parentId = specialProjectId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = null,
            tags = null,
            relatedLinks = null,
                            isExpanded = false,
                            goalOrder = 1,
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
                            projectType = ProjectType.RESERVED,
                            reservedGroup = ReservedGroup.StrategicGroup)

        // TODO: ініціалізувати тут стратегічні проекти
    }

    private fun createMainBeaconsProjects(specialProjectId: String) {
        val mainBeaconsGroupId = "main-beacon-realization-id"
        database.projectsQueries.insertProject(
            id = mainBeaconsGroupId,
            name = "main-beacon-realization",
            description = null,
            parentId = specialProjectId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = null,
            tags = null,
            relatedLinks = null,
                            isExpanded = false,
                            goalOrder = 2,
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
                            projectType = ProjectType.RESERVED,
                            reservedGroup = ReservedGroup.MainBeaconsGroup)

        val listId = "main-beacon-list-id"
        database.projectsQueries.insertProject(
            id = listId,
            name = "list",
            description = null,
            parentId = mainBeaconsGroupId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = null,
            tags = null,
            relatedLinks = null,
                            isExpanded = false,
                            goalOrder = 0,
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
                            projectType = ProjectType.RESERVED,
                            reservedGroup = null)

        database.projectsQueries.insertProject(
            id = "mission-project-id",
            name = "mission",
            description = "Mission project",
            parentId = listId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = null,
            tags = null,
            relatedLinks = null,
                            isExpanded = false,
                            goalOrder = 0,
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
                            projectType = ProjectType.RESERVED,
                            reservedGroup = ReservedGroup.MainBeacons)
    }
}
package com.romankozak.forwardappmobile.data.database

import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectLogLevelValues
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectStatusValues
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val projectLocalDataSource: ProjectLocalDataSource,
) {
    suspend fun prePopulate() {
        val specialProject =
            projectLocalDataSource.getByType(ProjectType.SYSTEM.name).firstOrNull()
        if (specialProject == null) {
            prePopulateProjects()
        } else {
            ensureReservedHierarchy(specialProject)
        }
    }

    private suspend fun prePopulateProjects() {
        val specialProject =
            createReservedProject(
                name = "special",
                parentId = null,
                projectType = ProjectType.SYSTEM,
                reservedGroup = null,
            )

        val strategicGroupProject =
            createReservedProject(
                name = "strategic",
                parentId = specialProject.id,
                projectType = ProjectType.RESERVED,
                reservedGroup = ReservedGroup.StrategicGroup,
            )

        val mainBeaconsGroupProject =
            createReservedProject(
                name = "main-beacons",
                parentId = specialProject.id,
                projectType = ProjectType.RESERVED,
                reservedGroup = ReservedGroup.MainBeaconsGroup,
            )

        val projects =
            mutableListOf(
                specialProject,
                strategicGroupProject,
                mainBeaconsGroupProject,
                createReservedProject(
                    name = "mission",
                    parentId = mainBeaconsGroupProject.id,
                    projectType = ProjectType.RESERVED,
                    reservedGroup = ReservedGroup.MainBeacons,
                ),
                createReservedProject(
                    name = "long-term-strategy",
                    parentId = strategicGroupProject.id,
                    projectType = ProjectType.RESERVED,
                    reservedGroup = ReservedGroup.Strategic,
                ),
                createReservedProject(
                    name = "medium-term-program",
                    parentId = strategicGroupProject.id,
                    projectType = ProjectType.RESERVED,
                    reservedGroup = ReservedGroup.Strategic,
                ),
                createReservedProject(
                    name = "active-quests",
                    parentId = strategicGroupProject.id,
                    projectType = ProjectType.RESERVED,
                    reservedGroup = ReservedGroup.Strategic,
                ),
                createReservedProject(
                    name = "strategic-inbox",
                    parentId = strategicGroupProject.id,
                    projectType = ProjectType.RESERVED,
                    reservedGroup = ReservedGroup.Strategic,
                ),
                createReservedProject(
                    name = "strategic-review",
                    parentId = strategicGroupProject.id,
                    projectType = ProjectType.RESERVED,
                    reservedGroup = ReservedGroup.Strategic,
                ),
            )

        projects +=
            createReservedProject(
                name = "inbox",
                parentId = specialProject.id,
                projectType = ProjectType.RESERVED,
                reservedGroup = ReservedGroup.Inbox,
            )

        projectLocalDataSource.upsert(projects)
    }

    private suspend fun ensureReservedHierarchy(specialProject: Project) {
        val specialId = specialProject.id

        val strategicGroupId =
            ensureChildWithReservedGroup(
                parentId = specialId,
                group = ReservedGroup.StrategicGroup,
                defaultName = "strategic",
            )

        val mainBeaconsGroupId =
            ensureChildWithReservedGroup(
                parentId = specialId,
                group = ReservedGroup.MainBeaconsGroup,
                defaultName = "main-beacons",
            )

        ensureChildWithReservedGroup(
            parentId = mainBeaconsGroupId,
            group = ReservedGroup.MainBeacons,
            defaultName = "mission",
        )

        listOf(
            "long-term-strategy",
            "medium-term-program",
            "active-quests",
            "strategic-inbox",
            "strategic-review",
        ).forEach { name ->
            ensureChildWithReservedGroup(
                parentId = strategicGroupId,
                group = ReservedGroup.Strategic,
                defaultName = name,
            )
        }

        ensureChildWithReservedGroup(
            parentId = specialId,
            group = ReservedGroup.Inbox,
            defaultName = "inbox",
        )
    }

    private suspend fun ensureChildWithReservedGroup(
        parentId: String,
        group: ReservedGroup,
        defaultName: String,
    ): String {
        val existing =
            projectLocalDataSource.getByParentAndReservedGroup(parentId, group.groupName)
                ?: projectLocalDataSource.getByReservedGroup(group.groupName).firstOrNull()

        if (existing != null) {
            if (existing.parentId != parentId) {
                projectLocalDataSource.upsert(existing.copy(parentId = parentId))
            }
            return existing.id
        }

        val newProject =
            createReservedProject(
                name = defaultName,
                parentId = parentId,
                projectType = ProjectType.RESERVED,
                reservedGroup = group,
            )
        projectLocalDataSource.upsert(newProject)
        return newProject.id
    }

    private fun createReservedProject(
        name: String,
        parentId: String?,
        projectType: ProjectType,
        reservedGroup: ReservedGroup?,
    ): Project =
        Project(
            id = UUID.randomUUID().toString(),
            name = name,
            description = null,
            parentId = parentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null,
            relatedLinks = null,
            isExpanded = false,
            order = 0,
            isAttachmentsExpanded = false,
            defaultViewModeName = ProjectViewMode.BACKLOG.name,
            isCompleted = false,
            isProjectManagementEnabled = false,
            projectStatus = ProjectStatusValues.NO_PLAN,
            projectStatusText = "",
            projectLogLevel = ProjectLogLevelValues.NORMAL,
            totalTimeSpentMinutes = 0,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = ScoringStatusValues.NOT_ASSESSED,
            showCheckboxes = false,
            projectType = projectType,
            reservedGroup = reservedGroup?.groupName,
        )
}

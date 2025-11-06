package com.romankozak.forwardappmobile.data.database

import android.content.Context
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.ProjectEntity
import com.romankozak.forwardappmobile.data.database.models.ProjectLogLevelValues
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues
import com.romankozak.forwardappmobile.data.database.models.ProjectType
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val projectDao: ProjectDao,
    private val context: Context,
) {
    suspend fun prePopulate() {
        val specialProject = projectDao.getProjectsByType(ProjectType.SYSTEM.name).firstOrNull()
        if (specialProject == null) {
            prePopulateProjects(projectDao)
        } else {
            ensureReservedHierarchy(specialProject)
        }
    }

    private suspend fun prePopulateProjects(projectDao: ProjectDao) {
        val specialProjectId = UUID.randomUUID().toString()
        val specialProject = ProjectEntity(
            id = specialProjectId,
            name = "special",
            isExpanded = false,
            projectType = ProjectType.SYSTEM,
            parentId = null,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null,
        )

        val strategicGroupId = UUID.randomUUID().toString()
        val strategicGroupProject = ProjectEntity(
            id = strategicGroupId,
            name = "strategic",
            parentId = specialProjectId,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.StrategicGroup,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null,
        )

        val mainBeaconsGroupId = UUID.randomUUID().toString()
        val mainBeaconsGroupProject = ProjectEntity(
            id = mainBeaconsGroupId,
            name = "main-beacons",
            parentId = specialProjectId,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.MainBeaconsGroup,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null,
        )

        val projects = mutableListOf(
            specialProject,
            strategicGroupProject,
            mainBeaconsGroupProject,
            ProjectEntity(id = UUID.randomUUID().toString(), name = "mission", parentId = mainBeaconsGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.MainBeacons, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            ProjectEntity(id = UUID.randomUUID().toString(), name = "long-term-strategy", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            ProjectEntity(id = UUID.randomUUID().toString(), name = "medium-term-program", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            ProjectEntity(id = UUID.randomUUID().toString(), name = "active-quests", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            ProjectEntity(id = UUID.randomUUID().toString(), name = "strategic-inbox", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            ProjectEntity(id = UUID.randomUUID().toString(), name = "strategic-review", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
        )

        val inboxProjectId = UUID.randomUUID().toString()
        val inboxProject = ProjectEntity(
            id = inboxProjectId,
            name = "inbox",
            parentId = specialProjectId,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.Inbox,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null,
        )
        projects.add(inboxProject)

        projectDao.insertProjects(projects)
    }

    private suspend fun ensureReservedHierarchy(specialProject: ProjectEntity) {
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
            projectDao.getProjectByParentAndReservedGroup(parentId, group.groupName)
                ?: projectDao.getProjectsByReservedGroup(group.groupName).firstOrNull()

        if (existing != null) {
            if (existing.parentId != parentId) {
                projectDao.update(existing.copy(parentId = parentId))
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
        projectDao.insert(newProject)
        return newProject.id
    }

    private fun createReservedProject(
        name: String,
        parentId: String?,
        projectType: ProjectType,
        reservedGroup: ReservedGroup,
    ): ProjectEntity =
        ProjectEntity(
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
            reservedGroup = reservedGroup,
        )
}

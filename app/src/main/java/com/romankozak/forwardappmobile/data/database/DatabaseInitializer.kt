package com.romankozak.forwardappmobile.data.database

import android.content.Context
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectType
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import com.romankozak.forwardappmobile.data.database.models.ReservedProjectKeys
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val projectDao: ProjectDao,
    private val context: Context
) {
    suspend fun prePopulate() {
        val specialProject = projectDao.getProjectsByType(ProjectType.SYSTEM.name).firstOrNull()
        if (specialProject == null) {
            prePopulateProjects(projectDao)
        }
    }

    private suspend fun prePopulateProjects(projectDao: ProjectDao) {
        val personalManagementProjectId = UUID.randomUUID().toString()
        val personalManagementProject = Project(
            id = personalManagementProjectId,
            name = "personal-management",
            isExpanded = false,
            projectType = ProjectType.SYSTEM,
            parentId = null,
            description = null,
            systemKey = ReservedProjectKeys.PERSONAL_MANAGEMENT,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )

        val strategicGroupId = UUID.randomUUID().toString()
        val strategicGroupProject = Project(
            id = strategicGroupId,
            name = "strategic",
            parentId = personalManagementProjectId,
            systemKey = ReservedProjectKeys.STRATEGIC,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.StrategicGroup,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )

        val strategicBeaconsGroupId = UUID.randomUUID().toString()
        val strategicBeaconsGroupProject = Project(
            id = strategicBeaconsGroupId,
            name = "strategic-beacons",
            parentId = strategicGroupId,
            systemKey = ReservedProjectKeys.STRATEGIC_BEACONS,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.MainBeaconsGroup,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )

        val weekProjectId = UUID.randomUUID().toString()
        val weekProject = Project(
            id = weekProjectId,
            name = "week",
            parentId = personalManagementProjectId,
            systemKey = ReservedProjectKeys.WEEK,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.Strategic,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null,
        )

        val projects = mutableListOf(
            personalManagementProject,
            strategicGroupProject,
            strategicBeaconsGroupProject,
            weekProject,
            Project(id = UUID.randomUUID().toString(), name = "mission", parentId = strategicBeaconsGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.MainBeacons, description = null, systemKey = ReservedProjectKeys.MISSION, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "long-term-strategy", parentId = strategicBeaconsGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.LONG_TERM_STRATEGY, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "strategic-programs", parentId = strategicBeaconsGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.STRATEGIC_PROGRAMS, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "main-beacons-realization", parentId = strategicBeaconsGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.MAIN_BEACONS_REALIZATION, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "medium-term-strategy", parentId = personalManagementProjectId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.MEDIUM_TERM_STRATEGY, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "active-quests", parentId = weekProjectId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.ACTIVE_QUESTS, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "strategic-inbox", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.STRATEGIC_INBOX, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "strategic-review", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, systemKey = ReservedProjectKeys.STRATEGIC_REVIEW, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
        )

        val inboxProjectId = UUID.randomUUID().toString()
        val inboxProject = Project(
            id = inboxProjectId,
            name = "inbox",
            parentId = personalManagementProjectId,
            systemKey = ReservedProjectKeys.INBOX,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.Inbox,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )
        projects.add(inboxProject)

        projectDao.insertProjects(projects)
    }
}

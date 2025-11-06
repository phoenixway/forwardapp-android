package com.romankozak.forwardappmobile.data.database

import android.content.Context
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.ProjectEntity
import com.romankozak.forwardappmobile.shared.data.database.models.ProjectType
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
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
            tags = null
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
            tags = null
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
            tags = null
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
            tags = null
        )
        projects.add(inboxProject)

        projectDao.insertProjects(projects)
    }
}

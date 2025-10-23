package com.romankozak.forwardappmobile.data.database

import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val projectDao: ProjectDao
) {
    suspend fun prePopulate() {
        val specialProject = projectDao.getProjectsByType(ProjectType.SYSTEM.name).firstOrNull()
        if (specialProject == null) {
            prePopulateProjects(projectDao)
        }
    }

    private suspend fun prePopulateProjects(projectDao: ProjectDao) {
        val specialProjectId = UUID.randomUUID().toString()
        val specialProject = Project(
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
        val strategicGroupProject = Project(
            id = strategicGroupId,
            name = "strategic",
            parentId = specialProjectId,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = "strategic_group",
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )

        val projects = mutableListOf(
            specialProject,
            strategicGroupProject,
            Project(id = UUID.randomUUID().toString(), name = "mission", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = "strategic", description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "long-term-strategy", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = "strategic", description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "medium-term-program", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = "strategic", description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "active-quests", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = "strategic", description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "strategic-goals", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = "strategic", description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = "strategic-review", parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = "strategic", description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
        )

        val inboxProjectId = UUID.randomUUID().toString()
        val inboxProject = Project(
            id = inboxProjectId,
            name = "inbox",
            parentId = specialProjectId,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = "inbox",
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )
        projects.add(inboxProject)

        projectDao.insertProjects(projects)
    }
}

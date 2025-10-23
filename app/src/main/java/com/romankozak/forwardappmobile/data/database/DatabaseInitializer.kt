package com.romankozak.forwardappmobile.data.database

import android.content.Context
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectType
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
        val specialProject = Project(
            id = specialProjectId,
            name = context.getString(R.string.special_project_name),
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
            name = context.getString(R.string.strategic_group_name),
            parentId = specialProjectId,
            isExpanded = false,
            projectType = ProjectType.RESERVED,
            reservedGroup = ReservedGroup.StrategicGroup,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )

        val projects = mutableListOf(
            specialProject,
            strategicGroupProject,
            Project(id = UUID.randomUUID().toString(), name = context.getString(R.string.mission_project_name), parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = context.getString(R.string.long_term_strategy_project_name), parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = context.getString(R.string.medium_term_program_project_name), parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = context.getString(R.string.active_quests_project_name), parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = context.getString(R.string.strategic_goals_project_name), parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
            Project(id = UUID.randomUUID().toString(), name = context.getString(R.string.strategic_review_project_name), parentId = strategicGroupId, isExpanded = false, projectType = ProjectType.RESERVED, reservedGroup = ReservedGroup.Strategic, description = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null),
        )

        val inboxProjectId = UUID.randomUUID().toString()
        val inboxProject = Project(
            id = inboxProjectId,
            name = context.getString(R.string.inbox_project_name),
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

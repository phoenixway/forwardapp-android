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
        prePopulateProjects(projectDao)
    }

    private suspend fun prePopulateProjects(projectDao: ProjectDao) {
        val personalManagementProjectId = ensureProjectExists(projectDao, ReservedProjectKeys.PERSONAL_MANAGEMENT, "personal-management", null, ProjectType.SYSTEM, null)
        val strategicGroupId = ensureProjectExists(projectDao, ReservedProjectKeys.STRATEGIC, "strategic", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.StrategicGroup)
        val strategicBeaconsGroupId = ensureProjectExists(projectDao, ReservedProjectKeys.STRATEGIC_BEACONS, "strategic-beacons", strategicGroupId, ProjectType.RESERVED, ReservedGroup.MainBeaconsGroup)
        val weekProjectId = ensureProjectExists(projectDao, ReservedProjectKeys.WEEK, "week", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.Strategic)
        val todayProjectId = ensureProjectExists(projectDao, ReservedProjectKeys.TODAY, "today", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.Inbox)
        ensureProjectExists(projectDao, ReservedProjectKeys.MAIN_BEACONS, "main-beacons", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(projectDao, ReservedProjectKeys.MISSION, "mission", strategicBeaconsGroupId, ProjectType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(projectDao, ReservedProjectKeys.LONG_TERM_STRATEGY, "long-term-strategy", strategicBeaconsGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedProjectKeys.STRATEGIC_PROGRAMS, "strategic-programs", strategicBeaconsGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedProjectKeys.MEDIUM_TERM_STRATEGY, "medium-term-strategy", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedProjectKeys.ACTIVE_QUESTS, "active-quests", weekProjectId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedProjectKeys.STRATEGIC_INBOX, "strategic-inbox", strategicGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedProjectKeys.STRATEGIC_REVIEW, "strategic-review", strategicGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedProjectKeys.INBOX, "inbox", todayProjectId, ProjectType.RESERVED, ReservedGroup.Inbox)
    }

    private suspend fun ensureProjectExists(
        projectDao: ProjectDao,
        systemKey: String,
        name: String,
        parentId: String?,
        projectType: ProjectType,
        reservedGroup: ReservedGroup?
    ): String {
        val existingProject = projectDao.getProjectBySystemKey(systemKey)
        if (existingProject != null) {
            return existingProject.id
        }

        val newProject = Project(
            id = UUID.randomUUID().toString(),
            systemKey = systemKey,
            name = name,
            parentId = parentId,
            projectType = projectType,
            reservedGroup = reservedGroup,
            isExpanded = false,
            description = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = null,
            tags = null
        )
        projectDao.insert(newProject)
        return newProject.id
    }
}

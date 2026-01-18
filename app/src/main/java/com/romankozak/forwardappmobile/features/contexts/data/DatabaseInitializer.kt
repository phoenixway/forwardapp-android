package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectType
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedGroup
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedContextKeys
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedSystemAppKeys
import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val projectDao: ProjectDao,
    private val systemAppRepository: SystemAppRepository,
) {
    suspend fun prePopulate() {
        prePopulateProjects(projectDao)
        prePopulateSystemApps()
    }

    private suspend fun prePopulateProjects(projectDao: ProjectDao) {
        val personalManagementProjectId = ensureProjectExists(projectDao, ReservedContextKeys.PERSONAL_MANAGEMENT, "personal-management", null, ProjectType.SYSTEM, null)
        val strategicGroupId = ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC, "strategic", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.StrategicGroup)
        val strategicBeaconsGroupId = ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_BEACONS, "strategic-beacons", strategicGroupId, ProjectType.RESERVED, ReservedGroup.MainBeaconsGroup)
        val weekProjectId = ensureProjectExists(projectDao, ReservedContextKeys.WEEK, "week", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.Strategic)
        val todayProjectId = ensureProjectExists(projectDao, ReservedContextKeys.TODAY, "today", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.Inbox)
        ensureProjectExists(projectDao, ReservedContextKeys.MAIN_BEACONS, "main-beacons", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(projectDao, ReservedContextKeys.MISSION, "mission", strategicBeaconsGroupId, ProjectType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(projectDao, ReservedContextKeys.LONG_TERM_STRATEGY, "long-term-strategy", strategicBeaconsGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_PROGRAMS, "strategic-programs", strategicBeaconsGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.MEDIUM_TERM_STRATEGY, "medium-term-strategy", personalManagementProjectId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.ACTIVE_QUESTS, "active-quests", weekProjectId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_INBOX, "strategic-inbox", strategicGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_REVIEW, "strategic-review", strategicGroupId, ProjectType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.INBOX, "inbox", todayProjectId, ProjectType.RESERVED, ReservedGroup.Inbox)
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

    private suspend fun prePopulateSystemApps() {
        val lifeStateApp = systemAppRepository.ensureNoteApp(
            systemKey = ReservedSystemAppKeys.MY_LIFE_CURRENT_STATE,
            projectSystemKey = ReservedContextKeys.STRATEGIC,
            documentName = "my-life-current-state",
        )
        systemAppRepository.linkSystemNoteToProject(
            systemKey = ReservedSystemAppKeys.MY_LIFE_CURRENT_STATE,
            targetProjectSystemKey = ReservedContextKeys.TODAY,
        )
    }
}

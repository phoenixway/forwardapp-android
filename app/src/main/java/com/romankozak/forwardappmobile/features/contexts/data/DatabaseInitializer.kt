package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextType
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
        val personalManagementProjectId = ensureProjectExists(projectDao, ReservedContextKeys.PERSONAL_MANAGEMENT, "personal-management", null, ContextType.SYSTEM, null)
        val strategicGroupId = ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC, "strategic", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.StrategicGroup)
        val strategicBeaconsGroupId = ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_BEACONS, "strategic-beacons", strategicGroupId, ContextType.RESERVED, ReservedGroup.MainBeaconsGroup)
        val weekProjectId = ensureProjectExists(projectDao, ReservedContextKeys.WEEK, "week", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.Strategic)
        val todayProjectId = ensureProjectExists(projectDao, ReservedContextKeys.TODAY, "today", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.Inbox)
        ensureProjectExists(projectDao, ReservedContextKeys.MAIN_BEACONS, "main-beacons", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(projectDao, ReservedContextKeys.MISSION, "mission", strategicBeaconsGroupId, ContextType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(projectDao, ReservedContextKeys.LONG_TERM_STRATEGY, "long-term-strategy", strategicBeaconsGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_PROGRAMS, "strategic-programs", strategicBeaconsGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.MEDIUM_TERM_STRATEGY, "medium-term-strategy", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.ACTIVE_QUESTS, "active-quests", weekProjectId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_INBOX, "strategic-inbox", strategicGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.STRATEGIC_REVIEW, "strategic-review", strategicGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(projectDao, ReservedContextKeys.INBOX, "inbox", todayProjectId, ContextType.RESERVED, ReservedGroup.Inbox)
    }

    private suspend fun ensureProjectExists(
        projectDao: ProjectDao,
        systemKey: String,
        name: String,
        parentId: String?,
        projectType: ContextType,
        reservedGroup: ReservedGroup?
    ): String {
        val existingProject = projectDao.getProjectBySystemKey(systemKey)
        if (existingProject != null) {
            return existingProject.id
        }

        val newProject = Context(
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

package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
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
    private val contextDao: ContextDao,
    private val systemAppRepository: SystemAppRepository,
) {
    suspend fun prePopulate() {
        prePopulateProjects(contextDao)
        prePopulateSystemApps()
    }

    private suspend fun prePopulateProjects(contextDao: ContextDao) {
        val personalManagementProjectId = ensureProjectExists(contextDao, ReservedContextKeys.PERSONAL_MANAGEMENT, "personal-management", null, ContextType.SYSTEM, null)
        val strategicGroupId = ensureProjectExists(contextDao, ReservedContextKeys.STRATEGIC, "strategic", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.StrategicGroup)
        val strategicBeaconsGroupId = ensureProjectExists(contextDao, ReservedContextKeys.STRATEGIC_BEACONS, "strategic-beacons", strategicGroupId, ContextType.RESERVED, ReservedGroup.MainBeaconsGroup)
        val weekProjectId = ensureProjectExists(contextDao, ReservedContextKeys.WEEK, "week", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.Strategic)
        val todayProjectId = ensureProjectExists(contextDao, ReservedContextKeys.TODAY, "today", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.Inbox)
        ensureProjectExists(contextDao, ReservedContextKeys.MAIN_BEACONS, "main-beacons", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(contextDao, ReservedContextKeys.MISSION, "mission", strategicBeaconsGroupId, ContextType.RESERVED, ReservedGroup.MainBeacons)
        ensureProjectExists(contextDao, ReservedContextKeys.LONG_TERM_STRATEGY, "long-term-strategy", strategicBeaconsGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(contextDao, ReservedContextKeys.STRATEGIC_PROGRAMS, "strategic-programs", strategicBeaconsGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(contextDao, ReservedContextKeys.MEDIUM_TERM_STRATEGY, "medium-term-strategy", personalManagementProjectId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(contextDao, ReservedContextKeys.ACTIVE_QUESTS, "active-quests", weekProjectId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(contextDao, ReservedContextKeys.STRATEGIC_INBOX, "strategic-inbox", strategicGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(contextDao, ReservedContextKeys.STRATEGIC_REVIEW, "strategic-review", strategicGroupId, ContextType.RESERVED, ReservedGroup.Strategic)
        ensureProjectExists(contextDao, ReservedContextKeys.INBOX, "inbox", todayProjectId, ContextType.RESERVED, ReservedGroup.Inbox)
    }

    private suspend fun ensureProjectExists(
        contextDao: ContextDao,
        systemKey: String,
        name: String,
        parentId: String?,
        projectType: ContextType,
        reservedGroup: ReservedGroup?
    ): String {
        val existingProject = contextDao.getProjectBySystemKey(systemKey)
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
        contextDao.insert(newProject)
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

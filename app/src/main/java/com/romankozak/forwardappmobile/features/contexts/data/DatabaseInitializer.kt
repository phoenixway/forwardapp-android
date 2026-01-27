package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedContextKeys
import com.romankozak.forwardappmobile.features.contexts.data.models.ReservedSystemAppKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val systemAppRepository: SystemAppRepository,
    ) {
        suspend fun prePopulate() {
            prePopulateProjects(contextDao)
            prePopulateSystemApps()
        }

        private suspend fun prePopulateProjects(contextDao: ContextDao) {
            val personalManagementProjectId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.PERSONAL_MANAGEMENT.id,
                    "personal-management",
                    null,
                )
            val strategicGroupId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.STRATEGIC.id,
                    "strategic",
                    personalManagementProjectId,
                )
            val strategicBeaconsGroupId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.STRATEGIC_BEACONS.id,
                    "strategic-beacons",
                    strategicGroupId,
                )
            val weekProjectId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.WEEK.id,
                    "week",
                    personalManagementProjectId,
                )
            val todayProjectId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.TODAY.id,
                    "today",
                    personalManagementProjectId,
                )
            ensureProjectExists(
                contextDao,
                SystemContexts.MAIN_BEACONS.id,
                "main-beacons",
                personalManagementProjectId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.MISSION.id,
                "mission",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.LONG_TERM_STRATEGY.id,
                "long-term-strategy",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.STRATEGIC_PROGRAMS.id,
                "strategic-programs",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.MEDIUM_TERM_STRATEGY.id,
                "medium-term-strategy",
                personalManagementProjectId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.ACTIVE_QUESTS.id,
                "active-quests",
                weekProjectId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.STRATEGIC_INBOX.id,
                "strategic-inbox",
                strategicGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.STRATEGIC_REVIEW.id,
                "strategic-review",
                strategicGroupId,
            )
            ensureProjectExists(contextDao, SystemContexts.INBOX.id, "inbox", todayProjectId)

        }

        private suspend fun ensureProjectExists(
            contextDao: ContextDao,
            id: String,
            name: String,
            parentId: String?,
        ): String {
            val existingProject = contextDao.getContextById(id)
            if (existingProject != null) {
                return existingProject.id
            }

            val newProject =
                Context(
                    id = id,
                    name = name,
                    parentId = parentId,
                    isExpanded = false,
                    description = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = null,
                    tags = null,
                )
            contextDao.insert(newProject)
            return newProject.id
        }

        private suspend fun prePopulateSystemApps() {
            val lifeStateApp =
                systemAppRepository.ensureNoteApp(
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

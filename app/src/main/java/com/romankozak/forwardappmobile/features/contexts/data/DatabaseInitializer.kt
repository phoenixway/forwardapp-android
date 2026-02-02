package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer
    @Inject
    constructor(
        private val contextDao: ContextDao,
        // private val systemAppRepository: SystemAppRepository, // Removed to break cycle
    ) : SystemContextEnsurer { // Implement SystemContextEnsurer
        override suspend fun ensureAllSystemContextsExist() { // Renamed from prePopulate
            prePopulateProjects(contextDao)
            // prePopulateSystemApps() // Removed to break cycle
        }

        private suspend fun prePopulateProjects(contextDao: ContextDao) {
            val personalManagementProjectId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.PERSONAL_MANAGEMENT.raw,
                    "personal-management",
                    null,
                )
            val strategicGroupId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.STRATEGIC.raw,
                    "strategic",
                    personalManagementProjectId,
                )
            val strategicBeaconsGroupId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.STRATEGIC_BEACONS.raw,
                    "strategic-beacons",
                    strategicGroupId,
                )
            val weekProjectId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.WEEK.raw,
                    "week",
                    personalManagementProjectId,
                )
            val todayProjectId =
                ensureProjectExists(
                    contextDao,
                    SystemContexts.TODAY.raw,
                    "today",
                    personalManagementProjectId,
                )
            ensureProjectExists(
                contextDao,
                SystemContexts.MAIN_BEACONS.raw,
                "main-beacons",
                personalManagementProjectId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.MISSION.raw,
                "mission",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.LONG_TERM_STRATEGY.raw,
                "long-term-strategy",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.STRATEGIC_PROGRAMS.raw,
                "strategic-programs",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.MEDIUM_TERM_STRATEGY.raw,
                "medium-term-strategy",
                personalManagementProjectId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.ACTIVE_QUESTS.raw,
                "active-quests",
                weekProjectId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.STRATEGIC_INBOX.raw,
                "strategic-inbox",
                strategicGroupId,
            )
            ensureProjectExists(
                contextDao,
                SystemContexts.STRATEGIC_REVIEW.raw,
                "strategic-review",
                strategicGroupId,
            )
            ensureProjectExists(contextDao, SystemContexts.INBOX.raw, "inbox", todayProjectId)
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
    }

package com.romankozak.forwardappmobile.features.contexts.data

import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
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
                    ReservedContextKeys.PERSONAL_MANAGEMENT,
                    "personal-management",
                    null,
                )
            val strategicGroupId =
                ensureProjectExists(
                    contextDao,
                    ReservedContextKeys.STRATEGIC,
                    "strategic",
                    personalManagementProjectId,
                )
            val strategicBeaconsGroupId =
                ensureProjectExists(
                    contextDao,
                    ReservedContextKeys.STRATEGIC_BEACONS,
                    "strategic-beacons",
                    strategicGroupId,
                )
            val weekProjectId =
                ensureProjectExists(
                    contextDao,
                    ReservedContextKeys.WEEK,
                    "week",
                    personalManagementProjectId,
                )
            val todayProjectId =
                ensureProjectExists(
                    contextDao,
                    ReservedContextKeys.TODAY,
                    "today",
                    personalManagementProjectId,
                )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.MAIN_BEACONS,
                "main-beacons",
                personalManagementProjectId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.MISSION,
                "mission",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.LONG_TERM_STRATEGY,
                "long-term-strategy",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.STRATEGIC_PROGRAMS,
                "strategic-programs",
                strategicBeaconsGroupId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.MEDIUM_TERM_STRATEGY,
                "medium-term-strategy",
                personalManagementProjectId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.ACTIVE_QUESTS,
                "active-quests",
                weekProjectId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.STRATEGIC_INBOX,
                "strategic-inbox",
                strategicGroupId,
            )
            ensureProjectExists(
                contextDao,
                ReservedContextKeys.STRATEGIC_REVIEW,
                "strategic-review",
                strategicGroupId,
            )
            ensureProjectExists(contextDao, ReservedContextKeys.INBOX, "inbox", todayProjectId)
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
                    systemKey = id,
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

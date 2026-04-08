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
                    contextDao = contextDao,
                    id = SystemContexts.PERSONAL_MANAGEMENT.raw,
                    name = "personal-management",
                    parentId = null,
                )
            val strategicGroupId =
                ensureProjectExists(
                    contextDao = contextDao,
                    id = SystemContexts.STRATEGIC.raw,
                    name = "strategic",
                    parentId = personalManagementProjectId,
                )
            val weekProjectId =
                ensureProjectExists(
                    contextDao = contextDao,
                    id = SystemContexts.WEEK.raw,
                    name = "week",
                    parentId = personalManagementProjectId,
                )
            val todayProjectId =
                ensureProjectExists(
                    contextDao = contextDao,
                    id = SystemContexts.TODAY.raw,
                    name = "today",
                    parentId = personalManagementProjectId,
                )

            createSystemProjects(
                contextDao = contextDao,
                projectDefinitions =
                    buildSystemProjectDefinitions(
                        personalManagementProjectId = personalManagementProjectId,
                        strategicGroupId = strategicGroupId,
                        weekProjectId = weekProjectId,
                        todayProjectId = todayProjectId,
                    ),
            )
        }

        private fun buildSystemProjectDefinitions(
            personalManagementProjectId: String,
            strategicGroupId: String,
            weekProjectId: String,
            todayProjectId: String,
        ): List<SystemProjectDefinition> =
            listOf(
                SystemProjectDefinition(
                    id = SystemContexts.ABOUT_MODES.raw,
                    name = "mode-about",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.SESSION_IMPROVE.raw,
                    name = "mode-improve",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.SESSION_EXECUTION.raw,
                    name = "mode-execution",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.SESSION_CONTROL.raw,
                    name = "mode-control",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.SESSION_RECOVERY.raw,
                    name = "mode-recovery",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.SESSION_EMERGENCY.raw,
                    name = "mode-emergency",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.MAIN_BEACONS.raw,
                    name = "main-beacons",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.MISSION.raw,
                    name = "mission",
                    parentId = strategicGroupId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.LONG_TERM_STRATEGY.raw,
                    name = "long-term-strategy",
                    parentId = strategicGroupId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.STRATEGIC_PROGRAMS.raw,
                    name = "strategic-programs",
                    parentId = strategicGroupId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.MEDIUM_TERM_STRATEGY.raw,
                    name = "medium-term-strategy",
                    parentId = personalManagementProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.ACTIVE_QUESTS.raw,
                    name = "active-quests",
                    parentId = weekProjectId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.STRATEGIC_INBOX.raw,
                    name = "strategic-inbox",
                    parentId = strategicGroupId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.STRATEGIC_REVIEW.raw,
                    name = "strategic-review",
                    parentId = strategicGroupId,
                ),
                SystemProjectDefinition(
                    id = SystemContexts.INBOX.raw,
                    name = "inbox",
                    parentId = todayProjectId,
                ),
            )

        private suspend fun createSystemProjects(
            contextDao: ContextDao,
            projectDefinitions: List<SystemProjectDefinition>,
        ) {
            projectDefinitions.forEach { projectDefinition ->
                ensureProjectExists(
                    contextDao = contextDao,
                    id = projectDefinition.id,
                    name = projectDefinition.name,
                    parentId = projectDefinition.parentId,
                )
            }
        }

        private suspend fun ensureProjectExists(
            contextDao: ContextDao,
            id: String,
            name: String,
            parentId: String?,
        ): String {
            val existingProject = contextDao.getContextById(id)
            if (existingProject != null) {
                if (existingProject.name != name || existingProject.parentId != parentId) {
                    contextDao.update(
                        existingProject.copy(
                            name = name,
                            parentId = parentId,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
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

private data class SystemProjectDefinition(
    val id: String,
    val name: String,
    val parentId: String?,
)

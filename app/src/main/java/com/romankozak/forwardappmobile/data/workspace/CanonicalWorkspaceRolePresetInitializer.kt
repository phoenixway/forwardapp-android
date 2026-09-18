package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalKeyProblemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies create-time role/preset capability defaults directly to canonical
 * Workspace capability instances.
 *
 * ContextRoleProfile remains template input only. This boundary never creates
 * legacy aggregate rows, preset structure items, attachments, or structural
 * child entities.
 */
@Singleton
class CanonicalWorkspaceRolePresetInitializer
    @Inject
    constructor(
        private val database: AppDatabase,
        private val structurePresetDao: StructurePresetDao,
        private val inboxRepository: CanonicalInboxRepository,
        private val backlogRepository: CanonicalBacklogRepository,
        private val inboxSortingRepository: CanonicalInboxSortingRepository,
        private val keyProblemsRepository: CanonicalKeyProblemsRepository,
        private val directionRepository: CanonicalDirectionRepository,
        private val dashboardRepository: CanonicalDashboardCapabilityRepository,
        private val executionLogRepository: CanonicalExecutionLogRepository,
        private val connectionsRepository: CanonicalConnectionsRepository,
    ) {
        suspend fun apply(
            workspaceId: String,
            roleCode: String?,
            now: Long = System.currentTimeMillis(),
        ) {
            val normalizedRoleCode = roleCode?.trim()?.takeIf { it.isNotEmpty() } ?: return

            database.withTransaction {
                val preset = structurePresetDao.getByCode(normalizedRoleCode)
                val roleCapabilities = ContextRoleRegistry.getCapabilitiesForRole(normalizedRoleCode)

                fun roleHas(raw: String): Boolean =
                    CapabilityId(raw) in roleCapabilities

                // Persisted preset booleans are explicit create-time overrides
                // for the legacy boolean capabilities. If absent, the role
                // registry supplies the default.
                val inboxEnabled = preset?.enableInbox ?: roleHas("inbox")
                val backlogEnabled = preset?.enableBacklog ?: roleHas("backlog")
                val dashboardEnabled = preset?.enableDashboard ?: roleHas("dashboard")
                val executionLogEnabled = preset?.enableLog ?: roleHas("log")
                val connectionsEnabled =
                    preset?.enableAttachments
                        ?: (roleHas("connections") || roleHas("attachments"))

                // These capabilities never had dedicated preset boolean columns.
                val inboxSortingEnabled = roleHas("inbox_sorting")
                val keyProblemsEnabled = roleHas("key_problems")
                val directionEnabled = roleHas("direction")

                inboxRepository.setEnabled(workspaceId, inboxEnabled, now)
                backlogRepository.setEnabled(workspaceId, backlogEnabled, now)
                inboxSortingRepository.setEnabled(workspaceId, inboxSortingEnabled, now)
                keyProblemsRepository.setEnabled(workspaceId, keyProblemsEnabled, now)
                directionRepository.setEnabled(workspaceId, directionEnabled, now)
                dashboardRepository.setEnabled(workspaceId, dashboardEnabled, now)
                executionLogRepository.setEnabled(workspaceId, executionLogEnabled, now)
                connectionsRepository.setEnabled(workspaceId, connectionsEnabled, now)

                if (directionEnabled) {
                    directionRepository.updateConfiguration(
                        workspaceId = workspaceId,
                        configuration =
                            DirectionCapabilityConfigurationV1(
                                autoLinkChildWorkspaces =
                                    preset?.enableAutoLinkSubprojects ?: true,
                            ),
                        now = now,
                    )
                }
            }
        }
    }

package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogConfigurationAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDashboardCapabilityRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalExecutionLogRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureWithItems
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class ContextStructureRepository
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val contextStructureDao: ContextStructureDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
        private val workspaceWriteThrough: ContextWorkspaceWriteThrough,
        private val systemContextCanonicalInboxDirectionAccess:
            SystemContextCanonicalInboxDirectionAccess,
        private val systemContextCanonicalRemainingCapabilityLifecycleAccess:
            SystemContextCanonicalRemainingCapabilityLifecycleAccess,
        private val systemContextCanonicalBacklogConfigurationAccess:
            SystemContextCanonicalBacklogConfigurationAccess,
        private val systemContextCanonicalBacklogLifecycleAccess:
            SystemContextCanonicalBacklogLifecycleAccess,
        private val canonicalDashboardCapabilityRepository:
            CanonicalDashboardCapabilityRepository,
        private val canonicalExecutionLogRepository:
            CanonicalExecutionLogRepository,
    ) {
        private suspend fun isLiveContext(contextId: String): Boolean =
            contextDao.getContextById(contextId)?.isDeleted == false

        private suspend fun isLiveStructureOwner(structureId: String): Boolean {
            val structure = contextStructureDao.getStructureById(structureId) ?: return false
            return isLiveContext(structure.contextId)
        }

        suspend fun ensureReservedBaseRolePresets() {
            val now = System.currentTimeMillis()
            ContextRoleRegistry.getReservedBaseRoleDefinitions().forEach { definition ->
                val existing = structurePresetDao.getByCode(definition.code)
                val capabilities = definition.capabilities.map { it.raw }.toSet()

                val preset =
                    ContextRoleProfile(
                        id = existing?.id ?: "reserved_preset_${definition.code}",
                        code = definition.code,
                        label = definition.label,
                        description = definition.description,
                        enableInbox = capabilities.contains("inbox"),
                        enableLog = capabilities.contains("log"),
                        enableAdvanced = existing?.enableAdvanced,
                        enableDashboard = true,
                        enableBacklog = capabilities.contains("backlog"),
                        enableAttachments = capabilities.contains("attachments") || capabilities.contains("connections"),
                        enableAutoLinkSubprojects = existing?.enableAutoLinkSubprojects ?: true,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                        version = existing?.version ?: 0,
                        isDeleted = false,
                    )
                structurePresetDao.insertPreset(preset)
            }
        }

        suspend fun ensureStructure(
            contextId: String,
            basePresetCode: String? = null,
        ): ContextConfiguration {
            val existing = contextStructureDao.getStructureByContext(contextId)
            if (existing != null) return existing

            check(isLiveContext(contextId)) {
                "Cannot create Context structure for retired or missing Context: $contextId"
            }

            val structure =
                ContextConfiguration(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    basePresetCode = basePresetCode,
                    enableAutoLinkSubprojects = true,
                    removeInboxEntryAfterTagAutocopy = false,
                    removeBacklogEntryAfterTagAutocopy = false,
                )
            val now = System.currentTimeMillis()
            return workspaceWriteThrough.mutate(
                now = now,
                mutation = {
                    val reconciled = reconcileSystemCapabilityCompatibility(structure)
                    contextStructureDao.insertStructure(reconciled)
                    reconciled
                },
            )
        }

        suspend fun getStructureByContext(contextId: String): ContextConfiguration? = contextStructureDao.getStructureByContext(contextId)

        fun observeStructure(contextId: String): Flow<ContextStructureWithItems?> =
            combine(
                contextStructureDao.observeStructureByContext(contextId),
                contextStructureDao.observeItemsForContext(contextId),
            ) { structure, items ->
                if (structure == null) null else ContextStructureWithItems(structure, items)
            }

        fun observeStructureOnly(contextId: String): Flow<ContextConfiguration?> = contextStructureDao.observeStructureByContext(contextId)

        suspend fun updateStructure(structure: ContextConfiguration) {
            if (!isLiveContext(structure.contextId)) return

            workspaceWriteThrough.mutate(
                now = structure.updatedAt,
                mutation = {
                    val reconciled = reconcileSystemCapabilityCompatibility(structure)
                    contextStructureDao.updateStructure(reconciled)
                    reconciled
                },
            )
        }

        suspend fun upsertStructure(structure: ContextConfiguration) {
            if (!isLiveContext(structure.contextId)) return

            workspaceWriteThrough.mutate(
                now = structure.updatedAt,
                mutation = {
                    val reconciled = reconcileSystemCapabilityCompatibility(structure)
                    contextStructureDao.insertStructure(reconciled)
                    reconciled
                },
            )
        }

        suspend fun applyPresetToContext(
            contextId: String,
            presetCode: String,
        ) {
            if (!isLiveContext(contextId)) return

            ensureReservedBaseRolePresets()
            val preset = structurePresetDao.getByCode(presetCode) ?: return
            val presetCapabilities = ContextRoleRegistry.getCapabilitiesForRole(preset.code)
            val knownLegacyCaps =
                setOf(
                    "inbox",
                    "log",
                    "dashboard",
                    "backlog",
                    "attachments",
                    "connections",
                )
            val experimentalIdsFromPreset = presetCapabilities.filter { it.raw !in knownLegacyCaps }
            val structure = ensureStructure(contextId, basePresetCode = preset.code)
            val experimentalCapabilityIds =
                if (systemContextCanonicalInboxDirectionAccess.handles(contextId)) {
                    (
                        structure.experimentalCapabilityIds.filterNot {
                            it.raw in SYSTEM_CANONICAL_TARGET_CAPABILITY_IDS
                        } + experimentalIdsFromPreset
                    ).distinctBy { it.raw }
                } else {
                    experimentalIdsFromPreset
                }
            val updatedStructure =
                structure.copy(
                    basePresetCode = preset.code,
                    applyMode = "ADDITIVE",
                    enableInbox = presetCapabilities.contains(CapabilityId("inbox")),
                    enableLog = presetCapabilities.contains(CapabilityId("log")),
                    enableAdvanced = structure.enableAdvanced,
                    enableDashboard = preset.enableDashboard,
                    enableBacklog = presetCapabilities.contains(CapabilityId("backlog")),
                    enableAttachments =
                        presetCapabilities.contains(CapabilityId("connections")) ||
                            presetCapabilities.contains(CapabilityId("attachments")),
                    enableAutoLinkSubprojects = preset.enableAutoLinkSubprojects ?: true,
                    removeInboxEntryAfterTagAutocopy = structure.removeInboxEntryAfterTagAutocopy ?: false,
                    removeBacklogEntryAfterTagAutocopy = structure.removeBacklogEntryAfterTagAutocopy ?: false,
                    experimentalCapabilityIds = experimentalCapabilityIds,
                )
            val presetItems = structurePresetItemDao.getItemsByPresetOnce(preset.id)
            val projectItems = presetItems.map { it.toContextStructureItem(structure.id) }
            workspaceWriteThrough.mutateAndAfterWorkspaceRefresh(
                now = updatedStructure.updatedAt,
                mutation = {
                    val reconciled = reconcileSystemCapabilityCompatibility(updatedStructure)
                    contextStructureDao.updateStructure(reconciled)
                    contextStructureDao.replaceItems(structure.id, projectItems)
                    reconciled
                },
                afterRefresh = { persisted ->
                    if (systemContextCanonicalInboxDirectionAccess.handles(contextId)) {
                        val inboxEnabled = CapabilityId("inbox") in presetCapabilities
                        val directionEnabled = CapabilityId("direction") in presetCapabilities
                        systemContextCanonicalInboxDirectionAccess.setInboxEnabled(
                            contextId,
                            inboxEnabled,
                            updatedStructure.updatedAt,
                        )
                        systemContextCanonicalInboxDirectionAccess.setDirectionEnabled(
                            contextId,
                            directionEnabled,
                            updatedStructure.updatedAt,
                        )
                        if (directionEnabled) {
                            systemContextCanonicalInboxDirectionAccess.updateDirectionConfiguration(
                                contextId,
                                com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1(
                                    updatedStructure.enableAutoLinkSubprojects ?: true,
                                ),
                                updatedStructure.updatedAt,
                            )
                        }
                    }
                    if (systemContextCanonicalRemainingCapabilityLifecycleAccess.handles(contextId)) {
                        systemContextCanonicalRemainingCapabilityLifecycleAccess.setConnectionsEnabled(
                            contextId,
                            CapabilityId("connections") in presetCapabilities ||
                                CapabilityId("attachments") in presetCapabilities,
                            updatedStructure.updatedAt,
                        )
                        systemContextCanonicalRemainingCapabilityLifecycleAccess.setInboxSortingEnabled(
                            contextId,
                            CapabilityId("inbox_sorting") in presetCapabilities,
                            updatedStructure.updatedAt,
                        )
                        systemContextCanonicalRemainingCapabilityLifecycleAccess.setKeyProblemsEnabled(
                            contextId,
                            CapabilityId("key_problems") in presetCapabilities,
                            updatedStructure.updatedAt,
                        )
                    }
                    if (systemContextCanonicalBacklogLifecycleAccess.handles(contextId)) {
                        systemContextCanonicalBacklogLifecycleAccess.setEnabled(
                            contextId,
                            CapabilityId("backlog") in presetCapabilities,
                            updatedStructure.updatedAt,
                        )
                    }
                    canonicalDashboardCapabilityRepository.setEnabled(
                        workspaceId = contextId,
                        enabled = persisted.enableDashboard == true,
                        now = updatedStructure.updatedAt,
                    )
                    canonicalExecutionLogRepository.setEnabled(
                        workspaceId = contextId,
                        enabled = CapabilityId("log") in presetCapabilities,
                        now = updatedStructure.updatedAt,
                    )
                },
            )
        }

        private suspend fun reconcileSystemCapabilityCompatibility(
            structure: ContextConfiguration,
        ): ContextConfiguration =
            systemContextCanonicalBacklogConfigurationAccess.reconcileCompatibilityOutput(
                systemContextCanonicalBacklogLifecycleAccess.reconcileCompatibilityOutput(
                    systemContextCanonicalRemainingCapabilityLifecycleAccess.reconcileCompatibilityOutput(
                        systemContextCanonicalInboxDirectionAccess.reconcileCompatibilityOutput(structure),
                    ),
                ),
            )

        suspend fun addOrUpdateItem(
            structureId: String,
            item: ContextStructureItem,
        ) {
            if (item.contextStructureId != structureId) return
            if (!isLiveStructureOwner(structureId)) return
            contextStructureDao.insertItems(listOf(item))
        }

        suspend fun setItemEnabled(
            item: ContextStructureItem,
            enabled: Boolean,
        ) {
            if (!isLiveStructureOwner(item.contextStructureId)) return
            contextStructureDao.updateItem(item.copy(isEnabled = enabled))
        }

        suspend fun getStructureWithItems(contextId: String): ContextStructureWithItems {
            val structure = ensureStructure(contextId)
            val items = contextStructureDao.getItems(structure.id)
            return ContextStructureWithItems(structure, items)
        }

        private fun ContextRoleProfileItem.toContextStructureItem(structureId: String): ContextStructureItem =
            ContextStructureItem(
                id = UUID.randomUUID().toString(),
                contextStructureId = structureId,
                entityType = entityType,
                roleCode = roleCode,
                containerType = containerType,
                title = title,
                mandatory = mandatory,
                isEnabled = true,
            )

        private companion object {
            val SYSTEM_CANONICAL_TARGET_CAPABILITY_IDS =
                setOf(
                    "backlog",
                    "inbox",
                    "inbox_sorting",
                    "key_problems",
                    "direction",
                    "dashboard",
                    "log",
                    "execution_log",
                    "connections",
                    "attachments",
                )
        }
    }

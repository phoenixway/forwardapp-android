package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfile
import com.romankozak.forwardappmobile.core.data.models.entities.ContextRoleProfileItem
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.gate.ContextRoleRegistry
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureWithItems
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextStructureRepository
    @Inject
    constructor(
        private val contextStructureDao: ContextStructureDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
    ) {
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
                        enableArtifact = capabilities.contains("advanced"),
                        enableAdvanced = capabilities.contains("advanced"),
                        enableDashboard = true,
                        enableBacklog = capabilities.contains("backlog"),
                        enableAttachments = capabilities.contains("attachments"),
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
            val structure =
                ContextConfiguration(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    basePresetCode = basePresetCode,
                    enableAutoLinkSubprojects = true,
                )
            contextStructureDao.insertStructure(structure)
            return structure
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
            contextStructureDao.updateStructure(structure)
        }

        suspend fun applyPresetToContext(
            contextId: String,
            presetCode: String,
        ) {
            ensureReservedBaseRolePresets()
            val preset = structurePresetDao.getByCode(presetCode) ?: return
            val presetCapabilities = ContextRoleRegistry.getCapabilitiesForRole(preset.code)
            val knownLegacyCaps =
                setOf(
                    "inbox",
                    "log",
                    "advanced",
                    "dashboard",
                    "backlog",
                    "attachments",
                )
            val experimentalIdsFromPreset = presetCapabilities.filter { it.raw !in knownLegacyCaps }
            val structure = ensureStructure(contextId, basePresetCode = preset.code)
            val updatedStructure =
                structure.copy(
                    basePresetCode = preset.code,
                    enableInbox = presetCapabilities.contains(CapabilityId("inbox")),
                    enableLog = presetCapabilities.contains(CapabilityId("log")),
                    enableArtifact = presetCapabilities.contains(CapabilityId("advanced")),
                    enableAdvanced = presetCapabilities.contains(CapabilityId("advanced")),
                    enableDashboard = preset.enableDashboard,
                    enableBacklog = presetCapabilities.contains(CapabilityId("backlog")),
                    enableAttachments = presetCapabilities.contains(CapabilityId("attachments")),
                    enableAutoLinkSubprojects = preset.enableAutoLinkSubprojects ?: true,
                    experimentalCapabilityIds = experimentalIdsFromPreset,
                )
            contextStructureDao.updateStructure(updatedStructure)
            val presetItems = structurePresetItemDao.getItemsByPresetOnce(preset.id)
            val projectItems = presetItems.map { it.toContextStructureItem(structure.id) }
            contextStructureDao.replaceItems(structure.id, projectItems)
        }

        suspend fun addOrUpdateItem(
            structureId: String,
            item: ContextStructureItem,
        ) {
            contextStructureDao.insertItems(listOf(item))
        }

        suspend fun setItemEnabled(
            item: ContextStructureItem,
            enabled: Boolean,
        ) {
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
    }

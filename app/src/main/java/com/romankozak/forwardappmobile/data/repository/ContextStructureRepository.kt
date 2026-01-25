package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureWithItems
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextConfiguration
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfileItem
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextStructureItem
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
            val preset = structurePresetDao.getByCode(presetCode) ?: return
            val structure = ensureStructure(contextId, basePresetCode = preset.code)
            val updatedStructure =
                structure.copy(
                    basePresetCode = preset.code,
                    enableInbox = preset.enableInbox,
                    enableLog = preset.enableLog,
                    enableArtifact = preset.enableArtifact,
                    enableAdvanced = preset.enableAdvanced,
                    enableDashboard = preset.enableDashboard,
                    enableBacklog = preset.enableBacklog,
                    enableAttachments = preset.enableAttachments,
                    enableAutoLinkSubprojects = preset.enableAutoLinkSubprojects,
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

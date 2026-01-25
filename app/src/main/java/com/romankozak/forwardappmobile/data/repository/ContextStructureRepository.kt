package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectStructureWithItems
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextConfiguration
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextRoleProfileItem
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectStructureItem
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
            projectId: String,
            basePresetCode: String? = null,
        ): ContextConfiguration {
            val existing = contextStructureDao.getStructureByProject(projectId)
            if (existing != null) return existing
            val structure =
                ContextConfiguration(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    basePresetCode = basePresetCode,
                    enableAutoLinkSubprojects = true,
                )
            contextStructureDao.insertStructure(structure)
            return structure
        }

        suspend fun getStructureByProject(projectId: String): ContextConfiguration? = contextStructureDao.getStructureByProject(projectId)

        fun observeStructure(projectId: String): Flow<ProjectStructureWithItems?> =
            combine(
                contextStructureDao.observeStructureByProject(projectId),
                contextStructureDao.observeItemsForProject(projectId),
            ) { structure, items ->
                if (structure == null) null else ProjectStructureWithItems(structure, items)
            }

        fun observeStructureOnly(projectId: String): Flow<ContextConfiguration?> = contextStructureDao.observeStructureByProject(projectId)

        suspend fun updateStructure(structure: ContextConfiguration) {
            contextStructureDao.updateStructure(structure)
        }

        suspend fun applyPresetToProject(
            projectId: String,
            presetCode: String,
        ) {
            val preset = structurePresetDao.getByCode(presetCode) ?: return
            val structure = ensureStructure(projectId, basePresetCode = preset.code)
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
            val projectItems = presetItems.map { it.toProjectStructureItem(structure.id) }
            contextStructureDao.replaceItems(structure.id, projectItems)
        }

        suspend fun addOrUpdateItem(
            structureId: String,
            item: ProjectStructureItem,
        ) {
            contextStructureDao.insertItems(listOf(item))
        }

        suspend fun setItemEnabled(
            item: ProjectStructureItem,
            enabled: Boolean,
        ) {
            contextStructureDao.updateItem(item.copy(isEnabled = enabled))
        }

        suspend fun getStructureWithItems(projectId: String): ProjectStructureWithItems {
            val structure = ensureStructure(projectId)
            val items = contextStructureDao.getItems(structure.id)
            return ProjectStructureWithItems(structure, items)
        }

        private fun ContextRoleProfileItem.toProjectStructureItem(structureId: String): ProjectStructureItem =
            ProjectStructureItem(
                id = UUID.randomUUID().toString(),
                projectStructureId = structureId,
                entityType = entityType,
                roleCode = roleCode,
                containerType = containerType,
                title = title,
                mandatory = mandatory,
                isEnabled = true,
            )
    }

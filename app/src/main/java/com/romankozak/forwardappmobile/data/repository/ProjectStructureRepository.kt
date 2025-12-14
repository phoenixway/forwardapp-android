package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ProjectStructureDao
import com.romankozak.forwardappmobile.data.dao.ProjectStructureWithItems
import com.romankozak.forwardappmobile.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.data.database.models.ProjectStructure
import com.romankozak.forwardappmobile.data.database.models.ProjectStructureItem
import com.romankozak.forwardappmobile.data.database.models.StructurePresetItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectStructureRepository @Inject constructor(
    private val projectStructureDao: ProjectStructureDao,
    private val structurePresetDao: StructurePresetDao,
    private val structurePresetItemDao: StructurePresetItemDao,
) {

    suspend fun ensureStructure(projectId: String, basePresetCode: String? = null): ProjectStructure {
        val existing = projectStructureDao.getStructureByProject(projectId)
        if (existing != null) return existing
        val structure = ProjectStructure(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            basePresetCode = basePresetCode,
        )
        projectStructureDao.insertStructure(structure)
        return structure
    }

    suspend fun getStructureByProject(projectId: String): ProjectStructure? =
        projectStructureDao.getStructureByProject(projectId)

    fun observeStructure(projectId: String): Flow<ProjectStructureWithItems?> =
        combine(
            projectStructureDao.observeStructureByProject(projectId),
            projectStructureDao.observeItemsForProject(projectId)
        ) { structure, items ->
            if (structure == null) null else ProjectStructureWithItems(structure, items)
        }

    fun observeStructureOnly(projectId: String): Flow<ProjectStructure?> =
        projectStructureDao.observeStructureByProject(projectId)

    suspend fun updateStructure(structure: ProjectStructure) {
        projectStructureDao.updateStructure(structure)
    }

    suspend fun applyPresetToProject(projectId: String, presetCode: String) {
        val preset = structurePresetDao.getByCode(presetCode) ?: return
        val structure = ensureStructure(projectId, basePresetCode = preset.code)
        val updatedStructure = structure.copy(
            basePresetCode = preset.code,
            enableInbox = preset.enableInbox,
            enableLog = preset.enableLog,
            enableArtifact = preset.enableArtifact,
            enableAdvanced = preset.enableAdvanced,
            enableDashboard = preset.enableDashboard,
            enableBacklog = preset.enableBacklog,
            enableAttachments = preset.enableAttachments,
        )
        projectStructureDao.updateStructure(updatedStructure)
        val presetItems = structurePresetItemDao.getItemsByPresetOnce(preset.id)
        val projectItems = presetItems.map { it.toProjectStructureItem(structure.id) }
        projectStructureDao.replaceItems(structure.id, projectItems)
    }

    suspend fun addOrUpdateItem(structureId: String, item: ProjectStructureItem) {
        projectStructureDao.insertItems(listOf(item))
    }

    suspend fun setItemEnabled(item: ProjectStructureItem, enabled: Boolean) {
        projectStructureDao.updateItem(item.copy(isEnabled = enabled))
    }

    suspend fun getStructureWithItems(projectId: String): ProjectStructureWithItems {
        val structure = ensureStructure(projectId)
        val items = projectStructureDao.getItems(structure.id)
        return ProjectStructureWithItems(structure, items)
    }

    private fun StructurePresetItem.toProjectStructureItem(structureId: String): ProjectStructureItem =
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

package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItemTypeValues
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecklistRepository @Inject constructor(
    private val checklistDao: ChecklistDao,
    private val attachmentRepository: AttachmentRepository,
    private val recentItemsRepository: RecentItemsRepository,
) {

    fun getChecklistsForProject(projectId: String): Flow<List<ChecklistEntity>> =
        checklistDao.getChecklistsForProject(projectId, ListItemTypeValues.CHECKLIST)

    fun getAllChecklistsAsFlow(): Flow<List<ChecklistEntity>> = checklistDao.getAllChecklistsAsFlow()

    suspend fun getChecklistById(id: String): ChecklistEntity? = checklistDao.getChecklistById(id)

    fun observeChecklistById(id: String): Flow<ChecklistEntity?> = checklistDao.observeChecklistById(id)

    suspend fun createChecklist(
        name: String,
        projectId: String,
        roleCode: String? = null,
        isSystem: Boolean = false
    ): String {
        val now = System.currentTimeMillis()
        val checklist = ChecklistEntity(projectId = projectId, name = name, updatedAt = now)
        checklistDao.insertChecklist(checklist)
        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = ListItemTypeValues.CHECKLIST,
            entityId = checklist.id,
            projectId = projectId,
            ownerProjectId = projectId,
            createdAt = System.currentTimeMillis(),
            roleCode = roleCode,
            isSystem = isSystem,
        )
        recentItemsRepository.logChecklistAccess(checklist)
        return checklist.id
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        val bumped = checklist.bumpSync()
        checklistDao.updateChecklist(bumped)
        recentItemsRepository.logChecklistAccess(checklist)
    }

    suspend fun deleteChecklist(checklistId: String) {
        val now = System.currentTimeMillis()
        val entity = checklistDao.getChecklistById(checklistId)
        if (entity != null) {
            checklistDao.insertChecklists(
                listOf(entity.softDelete(now)),
            )
        } else {
            checklistDao.deleteChecklistById(checklistId)
        }
        attachmentRepository.findAttachmentByEntity(ListItemTypeValues.CHECKLIST, checklistId)?.let {
            attachmentRepository.deleteAttachment(it.id)
        }
    }

    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItemEntity>> =
        checklistDao.getItemsForChecklist(checklistId)

    suspend fun addItem(
        checklistId: String,
        content: String = "",
        isChecked: Boolean = false,
        order: Long = System.currentTimeMillis(),
    ): String {
        val now = System.currentTimeMillis()
        val item =
            ChecklistItemEntity(
                checklistId = checklistId,
                content = content,
                isChecked = isChecked,
                itemOrder = order,
                updatedAt = now,
                syncedAt = null,
                version = 1,
            )
        checklistDao.insertItem(item)
        return item.id
    }

    suspend fun addItems(items: List<ChecklistItemEntity>) {
        if (items.isEmpty()) return
        checklistDao.insertItems(items)
    }

    suspend fun updateItem(item: ChecklistItemEntity) {
        val bumped = item.bumpSync()
        checklistDao.updateItem(bumped)
    }

    suspend fun updateItems(items: List<ChecklistItemEntity>) {
        if (items.isEmpty()) return
        checklistDao.updateItems(items.map { it.bumpSync() })
    }

    suspend fun deleteItem(itemId: String) {
        val entity = checklistDao.getItemById(itemId)
        val now = System.currentTimeMillis()
        if (entity != null) {
            checklistDao.insertItems(
                listOf(entity.softDelete(now)),
            )
        } else {
            checklistDao.deleteItemById(itemId)
        }
    }

    suspend fun deleteItemsByChecklist(checklistId: String) {
        val now = System.currentTimeMillis()
        val items = checklistDao.getItemsForChecklistSync(checklistId)
        if (items.isNotEmpty()) {
            checklistDao.insertItems(
                items.map { it.softDelete(now) },
            )
        } else {
            checklistDao.deleteItemsByChecklistId(checklistId)
        }
    }

    suspend fun getItemsSnapshot(checklistId: String): List<ChecklistItemEntity> =
        checklistDao.getItemsForChecklistSync(checklistId)

    suspend fun getAllItems(): List<ChecklistItemEntity> = checklistDao.getAllChecklistItems()
}

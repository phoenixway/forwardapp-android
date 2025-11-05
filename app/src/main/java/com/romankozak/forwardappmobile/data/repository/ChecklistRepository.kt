package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
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

    suspend fun createChecklist(name: String, projectId: String): String {
        val checklist = ChecklistEntity(projectId = projectId, name = name)
        checklistDao.insertChecklist(checklist)
        attachmentRepository.ensureAttachmentLinkedToProject(
            attachmentType = ListItemTypeValues.CHECKLIST,
            entityId = checklist.id,
            projectId = projectId,
            ownerProjectId = projectId,
            createdAt = System.currentTimeMillis(),
        )
        recentItemsRepository.logChecklistAccess(checklist)
        return checklist.id
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.updateChecklist(checklist)
        recentItemsRepository.logChecklistAccess(checklist)
    }

    suspend fun deleteChecklist(checklistId: String) {
        checklistDao.deleteChecklistById(checklistId)
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
        val item =
            ChecklistItemEntity(
                checklistId = checklistId,
                content = content,
                isChecked = isChecked,
                itemOrder = order,
            )
        checklistDao.insertItem(item)
        return item.id
    }

    suspend fun addItems(items: List<ChecklistItemEntity>) {
        if (items.isEmpty()) return
        checklistDao.insertItems(items)
    }

    suspend fun updateItem(item: ChecklistItemEntity) {
        checklistDao.updateItem(item)
    }

    suspend fun updateItems(items: List<ChecklistItemEntity>) {
        if (items.isEmpty()) return
        checklistDao.updateItems(items)
    }

    suspend fun deleteItem(itemId: String) {
        checklistDao.deleteItemById(itemId)
    }

    suspend fun deleteItemsByChecklist(checklistId: String) {
        checklistDao.deleteItemsByChecklistId(checklistId)
    }

    suspend fun getItemsSnapshot(checklistId: String): List<ChecklistItemEntity> =
        checklistDao.getItemsForChecklistSync(checklistId)

    suspend fun getAllItems(): List<ChecklistItemEntity> = checklistDao.getAllChecklistItems()
}

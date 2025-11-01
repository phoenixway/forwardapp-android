package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecklistRepository @Inject constructor(
    private val checklistDao: ChecklistDao,
    private val listItemDao: ListItemDao,
    private val recentItemsRepository: RecentItemsRepository,
) {

    fun getChecklistsForProject(projectId: String): Flow<List<ChecklistEntity>> =
        checklistDao.getChecklistsForProject(projectId)

    fun getAllChecklistsAsFlow(): Flow<List<ChecklistEntity>> = checklistDao.getAllChecklistsAsFlow()

    suspend fun getChecklistById(id: String): ChecklistEntity? = checklistDao.getChecklistById(id)

    fun observeChecklistById(id: String): Flow<ChecklistEntity?> = checklistDao.observeChecklistById(id)

    suspend fun createChecklist(name: String, projectId: String): String {
        val checklist = ChecklistEntity(projectId = projectId, name = name)
        checklistDao.insertChecklist(checklist)

        val listItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                itemType = ListItemTypeValues.CHECKLIST,
                entityId = checklist.id,
                order = -System.currentTimeMillis(),
            )
        listItemDao.insertItem(listItem)
        recentItemsRepository.logChecklistAccess(checklist)
        return checklist.id
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.updateChecklist(checklist)
        recentItemsRepository.logChecklistAccess(checklist)
    }

    suspend fun deleteChecklist(checklistId: String) {
        checklistDao.deleteChecklistById(checklistId)
        listItemDao.deleteItemByEntityId(checklistId)
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

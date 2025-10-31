package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
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
    companion object {
        private const val TAG = "ChecklistRepository"
    }

    fun getChecklistsForProject(projectId: String): Flow<List<ChecklistEntity>> =
        checklistDao.getChecklistsForProject(projectId)

    fun getChecklistItems(checklistId: String): Flow<List<ChecklistItemEntity>> =
        checklistDao.getChecklistItems(checklistId)

    fun getAllChecklistsAsFlow(): Flow<List<ChecklistEntity>> = checklistDao.getAllChecklistsAsFlow()

    suspend fun getChecklistById(id: String): ChecklistEntity? = checklistDao.getChecklistById(id)

    @Transaction
    suspend fun createChecklist(name: String, projectId: String): String {
        val id = UUID.randomUUID().toString()
        val checklist = ChecklistEntity(
            id = id,
            projectId = projectId,
            name = name,
        )
        Log.d(TAG, "Creating checklist: $checklist")
        checklistDao.insertChecklist(checklist)
        listItemDao.insertItem(
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                itemType = ListItemTypeValues.CHECKLIST,
                entityId = id,
                order = -System.currentTimeMillis(),
            ),
        )
        recentItemsRepository.logChecklistAccess(checklist)
        return id
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.updateChecklist(checklist)
        recentItemsRepository.updateRecentItemDisplayName(checklist.id, checklist.name)
    }

    suspend fun deleteChecklist(checklistId: String) {
        checklistDao.deleteChecklist(checklistId)
        listItemDao.deleteItemByEntityId(checklistId)
    }

    suspend fun addChecklistItem(checklistId: String, content: String, order: Long): String {
        val item = ChecklistItemEntity(
            id = UUID.randomUUID().toString(),
            checklistId = checklistId,
            content = content,
            isChecked = false,
            itemOrder = order,
        )
        checklistDao.insertChecklistItem(item)
        return item.id
    }

    suspend fun updateChecklistItem(item: ChecklistItemEntity) {
        checklistDao.updateChecklistItem(item)
    }

    suspend fun updateChecklistItems(items: List<ChecklistItemEntity>) {
        checklistDao.updateChecklistItems(items)
    }

    suspend fun deleteChecklistItem(itemId: String) {
        checklistDao.deleteChecklistItem(itemId)
    }
}

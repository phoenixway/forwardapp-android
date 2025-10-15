package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.CustomListDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.CustomListEntity
import com.romankozak.forwardappmobile.data.database.models.CustomListItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomListRepository @Inject constructor(
    private val customListDao: CustomListDao,
    private val listItemDao: ListItemDao,
    private val recentItemsRepository: RecentItemsRepository,
) {
    private val TAG = "CustomListRepository"

    fun getCustomListsForProject(projectId: String): Flow<List<CustomListEntity>> = customListDao.getCustomListsForProject(projectId)

    fun getAllCustomListsAsFlow(): Flow<List<CustomListEntity>> = customListDao.getAllCustomListsAsFlow()

    @Transaction
    suspend fun createCustomList(
        name: String,
        projectId: String,
        content: String? = null
    ): String {
        Log.d(TAG, "createCustomList called with name: $name, projectId: $projectId, content: $content")
        val newList = CustomListEntity(name = name, projectId = projectId, content = content)
        Log.d(TAG, "Inserting new custom list: $newList")
        customListDao.insertCustomList(newList)
        
        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                itemType = ListItemTypeValues.CUSTOM_LIST,
                entityId = newList.id,
                order = -System.currentTimeMillis(),
            )
        Log.d(TAG, "Inserting new list item: $newListItem")
        listItemDao.insertItem(newListItem)
        Log.d(TAG, "createCustomList finished")
        return newList.id
    }

    @Transaction
    suspend fun deleteCustomList(listId: String) {
        customListDao.deleteCustomListById(listId)
        listItemDao.deleteItemByEntityId(listId)
    }

    fun getCustomListItems(listId: String): Flow<List<CustomListItemEntity>> = customListDao.getListItemsForList(listId)

    suspend fun saveCustomListItem(item: CustomListItemEntity) {
        val existingItem = customListDao.getListItemById(item.id)
        if (existingItem == null) {
            customListDao.insertListItem(item)
        } else {
            customListDao.updateListItem(item.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteCustomListItem(itemId: String) {
        customListDao.deleteListItemById(itemId)
    }

    suspend fun updateCustomListItems(items: List<CustomListItemEntity>) {
        customListDao.updateListItems(items)
    }

    suspend fun getCustomListById(listId: String): CustomListEntity? {
        Log.d(TAG, "getCustomListById called with listId: $listId")
        val list = customListDao.getCustomListById(listId)
        Log.d(TAG, "getCustomListById returned: $list")
        return list
    }

    suspend fun updateCustomList(list: CustomListEntity) {
        android.util.Log.d("CursorDebug", "Repository updating custom list. lastCursorPosition: ${list.lastCursorPosition}")
        Log.d(TAG, "updateCustomList called with list: $list")
        customListDao.updateCustomList(list)
        recentItemsRepository.updateRecentItemDisplayName(list.id, list.name)
        Log.d(TAG, "updateCustomList finished")
    }
}

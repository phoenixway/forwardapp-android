package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDao
import com.romankozak.forwardappmobile.data.dao.RecentListDao
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ListItemType
import com.romankozak.forwardappmobile.data.database.models.Note
import com.romankozak.forwardappmobile.data.database.models.RecentListEntry
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal enum class ContextTextAction { ADD, REMOVE }

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    private val recentListDao: RecentListDao,
    private val noteDao: NoteDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val contextHandlerProvider: Provider<ContextHandler>
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }
    private val TAG = "AddSublistDebug" // Тег для логування

    // --- ОСНОВНІ МЕТОДИ ДЛЯ РОБОТИ З ВМІСТОМ СПИСКУ ---

    /**
     * Отримує потік з повним, типізованим вмістом списку (цілі, нотатки, посилання).
     */
    fun getListContentStream(listId: String): Flow<List<ListItemContent>> {
        return listItemDao.getItemsForListStream(listId).map { items ->
            items.mapNotNull { item ->
                when (item.itemType) {
                    ListItemType.GOAL -> goalDao.getGoalById(item.entityId)?.let { ListItemContent.GoalItem(it, item) }
                    ListItemType.NOTE -> noteDao.getNoteById(item.entityId)?.let { ListItemContent.NoteItem(it, item) }
                    ListItemType.SUBLIST -> goalListDao.getGoalListById(item.entityId)?.let { ListItemContent.SublistItem(it, item) }
                    ListItemType.LINK_ITEM -> linkItemDao.getLinkItemById(item.entityId)?.let { ListItemContent.LinkItem(it, item) }
                }
            }
        }
    }

    /**
     * Створює нову ціль і додає посилання на неї у вказаний список.
     */
    suspend fun addGoalToList(title: String, listId: String): String {
        val currentTime = System.currentTimeMillis()
        val newGoal = Goal(
            id = UUID.randomUUID().toString(),
            text = title,
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        goalDao.insertGoal(newGoal)
        syncContextMarker(newGoal.id, listId, ContextTextAction.ADD)

        val newListItem = ListItem(
            id = UUID.randomUUID().toString(),
            listId = listId,
            itemType = ListItemType.GOAL,
            entityId = newGoal.id,
            order = -currentTime
        )
        listItemDao.insertItem(newListItem)

        val finalGoalState = goalDao.getGoalById(newGoal.id)!!
        contextHandler.handleContextsOnCreate(finalGoalState)
        return newListItem.id
    }

    /**
     * Створює посилання на існуючий список і додає його як елемент у поточний список.
     */
    @Transaction
    suspend fun addListLinkToList(targetListId: String, currentListId: String): String {
        Log.d(TAG, "addListLinkToList: targetListId=$targetListId, currentListId=$currentListId")
        val newListItem = ListItem(
            id = UUID.randomUUID().toString(),
            listId = currentListId,
            itemType = ListItemType.SUBLIST,
            entityId = targetListId,
            order = -System.currentTimeMillis()
        )
        Log.d(TAG, "Constructed ListItem to insert: $newListItem")
        try {
            Log.d(TAG, "Attempting to insert via listItemDao.insertItems...")
            listItemDao.insertItems(listOf(newListItem))
            Log.d(TAG, "Insertion successful for ListItem ID: ${newListItem.id}")
        } catch (e: Exception) {
            Log.e(TAG, "DATABASE INSERTION FAILED for ListItem: $newListItem", e)
            throw e // Перекидаємо помилку, щоб її було видно вище
        }
        return newListItem.id
    }


    /**
     * Створює посилання на існуючі цілі у вказаному списку.
     */
    suspend fun createGoalLinks(goalIds: List<String>, targetListId: String) {
        if (goalIds.isNotEmpty()) {
            val newItems = goalIds.map { goalId ->
                ListItem(
                    id = UUID.randomUUID().toString(),
                    listId = targetListId,
                    itemType = ListItemType.GOAL,
                    entityId = goalId,
                    order = -System.currentTimeMillis()
                )
            }
            listItemDao.insertItems(newItems)
        }
    }

    /**
     * Копіює цілі (створює нові екземпляри) і додає їх у вказаний список.
     */
    suspend fun copyGoalsToList(goalIds: List<String>, targetListId: String) {
        if (goalIds.isNotEmpty()) {
            val originalGoals = goalDao.getGoalsByIdsSuspend(goalIds)
            val newGoals = mutableListOf<Goal>()
            val newItems = mutableListOf<ListItem>()

            originalGoals.forEach { goal ->
                val newGoal = goal.copy(id = UUID.randomUUID().toString())
                newGoals.add(newGoal)
                newItems.add(
                    ListItem(
                        id = UUID.randomUUID().toString(),
                        listId = targetListId,
                        itemType = ListItemType.GOAL,
                        entityId = newGoal.id,
                        order = -System.currentTimeMillis()
                    )
                )
            }
            goalDao.insertGoals(newGoals)
            listItemDao.insertItems(newItems)
        }
    }

    /**
     * Переміщує елементи (посилання) до іншого списку.
     */
    suspend fun moveListItems(itemIds: List<String>, targetListId: String) {
        if (itemIds.isNotEmpty()) {
            listItemDao.updateListItemListIds(itemIds, targetListId)
        }
    }

    /**
     * Видаляє елементи (посилання) зі списків. Не видаляє самі сутності (цілі, нотатки).
     */
    suspend fun deleteListItems(itemIds: List<String>) {
        if (itemIds.isNotEmpty()) {
            listItemDao.deleteItemsByIds(itemIds)
        }
    }

    /**
     * Оновлює порядок елементів у списку.
     */
    suspend fun updateListItemsOrder(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            listItemDao.updateItems(items)
        }
    }

    // --- ДОПОМІЖНІ МЕТОДИ ---

    private suspend fun syncContextMarker(goalId: String, listId: String, action: ContextTextAction) {
        val list = goalListDao.getGoalListById(listId) ?: return
        val listTags = list.tags.orEmpty()
        if (listTags.isEmpty()) return

        val tagMap = contextHandler.tagToContextNameMap.value
        val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in listTags }?.value ?: return
        val marker = contextHandler.getContextMarker(contextName) ?: return
        val goal = goalDao.getGoalById(goalId) ?: return

        var newText = goal.text
        val hasMarker = goal.text.contains(marker)

        if (action == ContextTextAction.ADD && !hasMarker) {
            newText = "${goal.text} $marker".trim()
        } else if (action == ContextTextAction.REMOVE && hasMarker) {
            newText = goal.text.replace(Regex("\\s*${Regex.escape(marker)}\\s*"), " ").trim()
        }

        if (newText != goal.text) {
            goalDao.updateGoal(goal.copy(text = newText, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun doesLinkExist(entityId: String, listId: String): Boolean = listItemDao.getLinkCount(entityId, listId) > 0

    suspend fun deleteLinkByEntityIdAndListId(entityId: String, listId: String) = listItemDao.deleteLinkByEntityAndList(entityId, listId)

    // --- МЕТОДИ ДЛЯ РОБОТИ З GOALLIST ---

    fun getAllGoalListsFlow(): Flow<List<GoalList>> = goalListDao.getAllLists()

    suspend fun getGoalListById(id: String): GoalList? = goalListDao.getGoalListById(id)

    fun getGoalListByIdFlow(id: String): Flow<GoalList?> = goalListDao.getGoalListByIdStream(id)

    suspend fun updateGoalList(list: GoalList) {
        goalListDao.update(list)
    }

    suspend fun updateGoalLists(lists: List<GoalList>): Int {
        return if (lists.isNotEmpty()) goalListDao.update(lists) else 0
    }

    @Transaction
    suspend fun deleteListsAndSubLists(listsToDelete: List<GoalList>) {
        if (listsToDelete.isEmpty()) return
        val listIds = listsToDelete.map { it.id }
        listItemDao.deleteItemsForLists(listIds)
        listsToDelete.forEach { goalListDao.delete(it) }
    }

    suspend fun createGoalListWithId(id: String, name: String, parentId: String?) {
        val newList = GoalList(
            id = id, name = name, parentId = parentId, description = "",
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        goalListDao.insert(newList)
    }

    // --- МЕТОДИ ДЛЯ РОБОТИ З GOAL ---

    suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)

    suspend fun updateGoals(goals: List<Goal>) = goalDao.updateGoals(goals)

    fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()

    @Transaction
    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResultItem> {
        val goalResults = goalDao.searchGoalsGlobal(query).map {
            GlobalSearchResultItem.GoalItem(it)
        }
        val linkResults = linkItemDao.searchLinksGlobal(query).map {
            GlobalSearchResultItem.LinkItem(it)
        }
        val sublistResults = goalListDao.searchSublistsGlobal(query).map {
            GlobalSearchResultItem.SublistItem(it)
        }
        return goalResults + linkResults + sublistResults
    }

    suspend fun logListAccess(listId: String) {
        recentListDao.logAccess(RecentListEntry(listId = listId, lastAccessed = System.currentTimeMillis()))
    }

    fun getRecentLists(limit: Int = 20): Flow<List<GoalList>> = recentListDao.getRecentLists(limit)

    @Transaction
    suspend fun moveGoalList(listToMove: GoalList, newParentId: String?) {
        val listFromDb = goalListDao.getGoalListById(listToMove.id) ?: return
        val oldParentId = listFromDb.parentId

        if (oldParentId != newParentId) {
            val oldSiblings = (if (oldParentId != null) {
                goalListDao.getListsByParentId(oldParentId)
            } else {
                goalListDao.getTopLevelLists()
            }).filter { it.id != listToMove.id }

            if (oldSiblings.isNotEmpty()) {
                goalListDao.update(oldSiblings.mapIndexed { index, list -> list.copy(order = index.toLong()) })
            }
        }

        val newSiblings = (if (newParentId != null) {
            goalListDao.getListsByParentId(newParentId)
        } else {
            goalListDao.getTopLevelLists()
        }).filter { it.id != listToMove.id }

        val finalListToMove = listToMove.copy(
            parentId = newParentId,
            order = newSiblings.size.toLong()
        )
        goalListDao.update(finalListToMove)
    }

    /**
     * Отримує одну нотатку за її ID.
     */
    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    /**
     * Оновлює існуючу нотатку в базі даних.
     */
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun addNoteToList(content: String, listId: String): String {
        val currentTime = System.currentTimeMillis()

        val title: String?
        val noteContent: String
        if (content.length <= 60 && !content.contains('\n')) {
            title = content
            noteContent = ""
        } else {
            title = "Нотатка"
            noteContent = content
        }

        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            content = noteContent,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        return addNoteToList(newNote, listId)
    }

    @Transaction
    suspend fun addNoteToList(note: Note, listId: String): String {
        noteDao.insertNote(note)
        val newListItem = ListItem(
            id = UUID.randomUUID().toString(),
            listId = listId,
            itemType = ListItemType.NOTE,
            entityId = note.id,
            order = -System.currentTimeMillis()
        )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    @Transaction
    suspend fun addLinkItemToList(listId: String, link: RelatedLink): String {
        val newLinkEntity = LinkItemEntity(
            id = UUID.randomUUID().toString(),
            linkData = link
        )
        linkItemDao.insert(newLinkEntity)

        val newListItem = ListItem(
            id = UUID.randomUUID().toString(),
            listId = listId,
            itemType = ListItemType.LINK_ITEM,
            entityId = newLinkEntity.id,
            order = -System.currentTimeMillis()
        )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    suspend fun findListIdsByTag(tag: String): List<String> {
        return goalListDao.getListIdsByTag(tag)
    }
    suspend fun getAllGoalLists(): List<GoalList> = goalListDao.getAll()
    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()
    suspend fun getAllNotes(): List<Note> = noteDao.getAll()
    suspend fun getAllListItems(): List<ListItem> = listItemDao.getAll()

    suspend fun logCurrentDbOrderForDebug(listId: String) {
        val itemsFromDb = listItemDao.getItemsForListSyncForDebug(listId)
        val orderLog = itemsFromDb.joinToString(separator = "\n") {
            "  - DB_ORDER=${it.order}, id=${it.id}"
        }
        Log.d("DND_DEBUG", "[DEBUG_QUERY] СИРИЙ ПОРЯДОК З БАЗИ ДАНИХ:\n$orderLog")
    }

}
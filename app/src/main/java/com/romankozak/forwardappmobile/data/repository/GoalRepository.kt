// --- File: app/src/main/java/com/romankozak/forwardappmobile/data/repository/GoalRepository.kt ---
package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDao
import com.romankozak.forwardappmobile.data.dao.RecentListDao
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResult
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ListItemType
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.Note
import com.romankozak.forwardappmobile.data.database.models.RecentListEntry
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    private val contextHandlerProvider: Provider<ContextHandler>
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }

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
                    ListItemType.LIST_LINK -> goalListDao.getGoalListById(item.entityId)?.let { ListItemContent.SublistItem(it, item) }
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
     * Створює нову нотатку і додає посилання на неї у вказаний список.
     */
    suspend fun addNoteToList(content: String, listId: String): String {
        val currentTime = System.currentTimeMillis()
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = null,
            content = content,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        noteDao.insertNote(newNote)

        val newListItem = ListItem(
            id = UUID.randomUUID().toString(),
            listId = listId,
            itemType = ListItemType.NOTE,
            entityId = newNote.id,
            order = -currentTime
        )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    /**
     * Створює посилання на існуючий список і додає його як елемент у поточний список.
     */
    suspend fun addListLinkToList(targetListId: String, currentListId: String): String {
        val newListItem = ListItem(
            id = UUID.randomUUID().toString(),
            listId = currentListId,
            itemType = ListItemType.LIST_LINK,
            entityId = targetListId,
            order = -System.currentTimeMillis()
        )
        listItemDao.insertItem(newListItem)
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

    suspend fun updateMarkdown(goalId: String, markdown: String) {
        goalDao.updateMarkdown(goalId, markdown)
    }

    /**
     * Отримує дані про списки, на які є посилання в полі `relatedLinks` для заданих цілей.
     */
    fun getAssociatedListsForGoals(goalIds: List<String>): Flow<Map<String, List<GoalList>>> {
        if (goalIds.isEmpty()) return flowOf(emptyMap())
        val goalsFlow = goalDao.getGoalsByIds(goalIds)
        val allListsFlow = goalListDao.getAllLists()
        return combine(goalsFlow, allListsFlow) { goals, allLists ->
            val listLookup = allLists.associateBy { it.id }
            goals.associate { goal ->
                val associatedLists = goal.relatedLinks
                    ?.filter { it.type == LinkType.GOAL_LIST }
                    ?.mapNotNull { listLookup[it.target] } ?: emptyList()
                goal.id to associatedLists
            }
        }
    }

    // --- ІНШІ ПУБЛІЧНІ МЕТОДИ ---

    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResult> = goalDao.searchGoalsGlobal(query)

    suspend fun logListAccess(listId: String) {
        recentListDao.logAccess(RecentListEntry(listId = listId, lastAccessed = System.currentTimeMillis()))
    }

    fun getRecentLists(limit: Int = 20): Flow<List<GoalList>> = recentListDao.getRecentLists(limit)

    // --- Методи для повної синхронізації/бекапу (можуть знадобитись) ---
    suspend fun getAllGoalLists(): List<GoalList> = goalListDao.getAll()
    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()
    suspend fun getAllNotes(): List<Note> = noteDao.getAll()
    suspend fun getAllListItems(): List<ListItem> = listItemDao.getAll()
    suspend fun insertGoalLists(lists: List<GoalList>) = goalListDao.insertLists(lists)
    suspend fun insertGoals(goals: List<Goal>) = goalDao.insertGoals(goals)
    suspend fun insertNotes(notes: List<Note>) = noteDao.insertNotes(notes)
    suspend fun insertListItems(items: List<ListItem>) = listItemDao.insertItems(items)

    suspend fun findListIdsByTag(tag: String): List<String> {
        return goalListDao.getListsByTag(tag).map { it.id }
    }

    // --- Додайте цей метод у файл GoalRepository.kt ---

    @Transaction
    suspend fun moveGoalList(listToMove: GoalList, newParentId: String?) {
        // Отримуємо актуальний стан списку з БД, щоб знати старого батька
        val listFromDb = goalListDao.getGoalListById(listToMove.id) ?: return
        val oldParentId = listFromDb.parentId

        // Якщо батько змінився, потрібно пересортувати старий список "сусідів"
        if (oldParentId != newParentId) {
            val oldSiblings = (if (oldParentId != null) {
                goalListDao.getListsByParentId(oldParentId)
            } else {
                goalListDao.getTopLevelLists()
            }).filter { it.id != listToMove.id } // Виключаємо сам елемент, що переміщується

            // Оновлюємо порядок у старому списку
            if (oldSiblings.isNotEmpty()) {
                goalListDao.update(oldSiblings.mapIndexed { index, list -> list.copy(order = index.toLong()) })
            }
        }

        // Отримуємо новий список "сусідів", щоб визначити порядок
        val newSiblings = (if (newParentId != null) {
            goalListDao.getListsByParentId(newParentId)
        } else {
            goalListDao.getTopLevelLists()
        }).filter { it.id != listToMove.id }

        // Оновлюємо сам список: нового батька та новий порядок (в кінець списку)
        val finalListToMove = listToMove.copy(
            parentId = newParentId,
            order = newSiblings.size.toLong()
        )
        goalListDao.update(finalListToMove)
    }

}
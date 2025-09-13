package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentListDao
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal enum class ContextTextAction { ADD, REMOVE }

@Singleton
class GoalRepository
@Inject
constructor(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    private val recentListDao: RecentListDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val contextHandlerProvider: Provider<ContextHandler>,
    private val inboxRecordDao: InboxRecordDao,
    private val activityRepository: ActivityRepository,
    private val projectManagementDao: ProjectManagementDao,
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }
    private val TAG = "AddSublistDebug"

    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        projectManagementDao.getLogsForProjectStream(projectId)

    suspend fun toggleProjectManagement(
        listId: String,
        isEnabled: Boolean,
    ) {
        val list = getGoalListById(listId) ?: return
        if (list.isProjectManagementEnabled == isEnabled) return

        updateGoalList(list.copy(isProjectManagementEnabled = isEnabled))

        val status = if (isEnabled) "активовано" else "деактивовано"
        addProjectLogEntry(
            projectId = listId,
            type = ProjectLogEntryType.AUTOMATIC,
            description = "Управління проектом було $status.",
        )
    }

    suspend fun updateProjectStatus(
        listId: String,
        newStatus: ProjectStatus,
        statusText: String?,
    ) {
        val list = getGoalListById(listId) ?: return
        if (list.projectStatus == newStatus && list.projectStatusText == statusText) return

        updateGoalList(
            list.copy(
                projectStatus = newStatus,
                projectStatusText = statusText,
                updatedAt = System.currentTimeMillis(),
            ),
        )

        val logDescription =
            "Статус змінено на '$newStatus'." +
                    (statusText?.let { "\nКоментар: $it" } ?: "")

        addProjectLogEntry(
            projectId = listId,
            type = ProjectLogEntryType.STATUS_CHANGE,
            description = logDescription,
        )
    }

    suspend fun addProjectComment(
        projectId: String,
        comment: String,
    ) {
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryType.COMMENT,
            description = comment,
        )
    }

    private suspend fun addProjectLogEntry(
        projectId: String,
        type: ProjectLogEntryType,
        description: String,
        details: String? = null,
    ) {
        val logEntry =
            ProjectExecutionLog(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                timestamp = System.currentTimeMillis(),
                type = type,
                description = description,
                details = details,
            )
        projectManagementDao.insertLog(logEntry)
    }

    suspend fun updateGoalListViewMode(
        listId: String,
        viewMode: ProjectViewMode,
    ) {
        val list = getGoalListById(listId)
        if (list != null) {
            updateGoalList(list.copy(defaultViewModeName = viewMode.name))
        }
    }

    fun getListContentStream(listId: String): Flow<List<ListItemContent>> =
        listItemDao.getItemsForListStream(listId).map { items ->
            items.mapNotNull { item ->
                when (item.itemType) {
                    ListItemType.GOAL -> goalDao.getGoalById(item.entityId)?.let { ListItemContent.GoalItem(it, item) }
                    ListItemType.SUBLIST -> goalListDao.getGoalListById(item.entityId)?.let { ListItemContent.SublistItem(it, item) }
                    ListItemType.LINK_ITEM -> linkItemDao.getLinkItemById(item.entityId)?.let { ListItemContent.LinkItem(it, item) }
                }
            }
        }

    suspend fun addGoalToList(
        title: String,
        listId: String,
        completed: Boolean = false,
    ): String {
        val currentTime = System.currentTimeMillis()
        val newGoal =
            Goal(
                id = UUID.randomUUID().toString(),
                text = title,
                completed = completed,
                createdAt = currentTime,
                updatedAt = currentTime,
            )
        goalDao.insertGoal(newGoal)
        syncContextMarker(newGoal.id, listId, ContextTextAction.ADD)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                listId = listId,
                itemType = ListItemType.GOAL,
                entityId = newGoal.id,
                order = -currentTime,
            )
        listItemDao.insertItem(newListItem)

        val finalGoalState = goalDao.getGoalById(newGoal.id)!!
        contextHandler.handleContextsOnCreate(finalGoalState)
        return newListItem.id
    }

    @Transaction
    suspend fun addListLinkToList(
        targetListId: String,
        currentListId: String,
    ): String {
        Log.d(TAG, "addListLinkToList: targetListId=$targetListId, currentListId=$currentListId")
        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                listId = currentListId,
                itemType = ListItemType.SUBLIST,
                entityId = targetListId,
                order = -System.currentTimeMillis(),
            )
        Log.d(TAG, "Constructed ListItem to insert: $newListItem")
        try {
            Log.d(TAG, "Attempting to insert via listItemDao.insertItems...")
            listItemDao.insertItems(listOf(newListItem))
            Log.d(TAG, "Insertion successful for ListItem ID: ${newListItem.id}")
        } catch (e: Exception) {
            Log.e(TAG, "DATABASE INSERTION FAILED for ListItem: $newListItem", e)
            throw e
        }
        return newListItem.id
    }

    suspend fun createGoalLinks(
        goalIds: List<String>,
        targetListId: String,
    ) {
        if (goalIds.isNotEmpty()) {
            val newItems =
                goalIds.map { goalId ->
                    ListItem(
                        id = UUID.randomUUID().toString(),
                        listId = targetListId,
                        itemType = ListItemType.GOAL,
                        entityId = goalId,
                        order = -System.currentTimeMillis(),
                    )
                }
            listItemDao.insertItems(newItems)
        }
    }

    suspend fun copyGoalsToList(
        goalIds: List<String>,
        targetListId: String,
    ) {
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
                        order = -System.currentTimeMillis(),
                    ),
                )
            }
            goalDao.insertGoals(newGoals)
            listItemDao.insertItems(newItems)
        }
    }

    suspend fun moveListItems(
        itemIds: List<String>,
        targetListId: String,
    ) {
        if (itemIds.isNotEmpty()) {
            listItemDao.updateListItemListIds(itemIds, targetListId)
        }
    }

    suspend fun deleteListItems(itemIds: List<String>) {
        if (itemIds.isNotEmpty()) {
            listItemDao.deleteItemsByIds(itemIds)
        }
    }

    suspend fun updateListItemsOrder(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            listItemDao.updateItems(items)
        }
    }

    private suspend fun syncContextMarker(
        goalId: String,
        listId: String,
        action: ContextTextAction,
    ) {
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

    suspend fun doesLinkExist(
        entityId: String,
        listId: String,
    ): Boolean = listItemDao.getLinkCount(entityId, listId) > 0

    suspend fun deleteLinkByEntityIdAndListId(
        entityId: String,
        listId: String,
    ) = listItemDao.deleteLinkByEntityAndList(entityId, listId)

    fun getAllGoalListsFlow(): Flow<List<GoalList>> = goalListDao.getAllLists()

    suspend fun getGoalListById(id: String): GoalList? = goalListDao.getGoalListById(id)

    fun getGoalListByIdFlow(id: String): Flow<GoalList?> = goalListDao.getGoalListByIdStream(id)

    suspend fun updateGoalList(list: GoalList) {
        goalListDao.update(list)
    }

    suspend fun updateGoalLists(lists: List<GoalList>): Int = if (lists.isNotEmpty()) goalListDao.update(lists) else 0

    @Transaction
    suspend fun deleteListsAndSubLists(listsToDelete: List<GoalList>) {
        if (listsToDelete.isEmpty()) return
        val listIds = listsToDelete.map { it.id }
        listItemDao.deleteItemsForLists(listIds)
        listsToDelete.forEach { goalListDao.delete(it) }
    }

    suspend fun createGoalListWithId(
        id: String,
        name: String,
        parentId: String?,
    ) {
        val newList =
            GoalList(
                id = id,
                name = name,
                parentId = parentId,
                description = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        goalListDao.insert(newList)
    }

    suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)

    suspend fun updateGoals(goals: List<Goal>) = goalDao.updateGoals(goals)

    fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()

    @Transaction
    suspend fun searchGoalsGlobal(query: String): List<GlobalSearchResultItem> {
        val goalResults =
            goalDao.searchGoalsGlobal(query).map {
                GlobalSearchResultItem.GoalItem(it)
            }
        val linkResults =
            linkItemDao.searchLinksGlobal(query).map {
                GlobalSearchResultItem.LinkItem(it)
            }
        val sublistResults =
            goalListDao.searchSublistsGlobal(query).map {
                GlobalSearchResultItem.SublistItem(it)
            }
        val listResults =
            goalListDao.searchListsGlobal(query).map {
                GlobalSearchResultItem.ListItem(it)
            }
        val activityResults =
            activityRepository.searchActivities(query).map {
                GlobalSearchResultItem.ActivityItem(it)
            }
        val inboxResults =
            inboxRecordDao.searchInboxRecordsGlobal(query).map {
                GlobalSearchResultItem.InboxItem(it)
            }

        return (goalResults + linkResults + sublistResults + listResults + activityResults + inboxResults)
            .sortedByDescending { it.timestamp }
    }

    suspend fun logListAccess(listId: String) {
        recentListDao.logAccess(RecentListEntry(listId = listId, lastAccessed = System.currentTimeMillis()))
    }

    fun getRecentLists(limit: Int = 20): Flow<List<GoalList>> = recentListDao.getRecentLists(limit)

    @Transaction
    suspend fun moveGoalList(
        listToMove: GoalList,
        newParentId: String?,
    ) {
        val listFromDb = goalListDao.getGoalListById(listToMove.id) ?: return
        val oldParentId = listFromDb.parentId

        if (oldParentId != newParentId) {
            val oldSiblings =
                (
                        if (oldParentId != null) {
                            goalListDao.getListsByParentId(oldParentId)
                        } else {
                            goalListDao.getTopLevelLists()
                        }
                        ).filter { it.id != listToMove.id }

            if (oldSiblings.isNotEmpty()) {
                goalListDao.update(oldSiblings.mapIndexed { index, list -> list.copy(order = index.toLong()) })
            }
        }

        val newSiblings =
            (
                    if (newParentId != null) {
                        goalListDao.getListsByParentId(newParentId)
                    } else {
                        goalListDao.getTopLevelLists()
                    }
                    ).filter { it.id != listToMove.id }

        val finalListToMove =
            listToMove.copy(
                parentId = newParentId,
                order = newSiblings.size.toLong(),
            )
        goalListDao.update(finalListToMove)
    }

    @Transaction
    suspend fun addLinkItemToList(
        listId: String,
        link: RelatedLink,
    ): String {
        val newLinkEntity =
            LinkItemEntity(
                id = UUID.randomUUID().toString(),
                linkData = link,
                createdAt = System.currentTimeMillis(),
            )
        linkItemDao.insert(newLinkEntity)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                listId = listId,
                itemType = ListItemType.LINK_ITEM,
                entityId = newLinkEntity.id,
                order = -System.currentTimeMillis(),
            )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    suspend fun findListIdsByTag(tag: String): List<String> = goalListDao.getListIdsByTag(tag)

    suspend fun getAllGoalLists(): List<GoalList> = goalListDao.getAll()

    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

    suspend fun getAllListItems(): List<ListItem> = listItemDao.getAll()

    suspend fun logCurrentDbOrderForDebug(listId: String) {
        val itemsFromDb = listItemDao.getItemsForListSyncForDebug(listId)
        val orderLog =
            itemsFromDb.joinToString(separator = "\n") {
                "  - DB_ORDER=${it.order}, id=${it.id}"
            }
        Log.d("DND_DEBUG", "[DEBUG_QUERY] СИРИЙ ПОРЯДОК З БАЗИ ДАНИХ:\n$orderLog")
    }

    suspend fun deleteGoalList(listId: String) {
        goalDao.deleteGoalListById(listId)
    }

    fun getInboxRecordsStream(projectId: String): Flow<List<InboxRecord>> = inboxRecordDao.getRecordsForProjectStream(projectId)

    suspend fun addInboxRecord(
        text: String,
        projectId: String,
    ) {
        val currentTime = System.currentTimeMillis()
        val newRecord =
            InboxRecord(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                text = text,
                createdAt = currentTime,
                order = -currentTime,
            )
        inboxRecordDao.insert(newRecord)
    }

    suspend fun updateInboxRecord(record: InboxRecord) {
        inboxRecordDao.update(record)
    }

    suspend fun deleteInboxRecordById(recordId: String) {
        inboxRecordDao.deleteById(recordId)
    }

    @Transaction
    suspend fun promoteInboxRecordToGoal(record: InboxRecord) {
        addGoalToList(record.text, record.projectId)
        inboxRecordDao.deleteById(record.id)
    }

    @Transaction
    suspend fun promoteInboxRecordToGoal(
        record: InboxRecord,
        targetListId: String,
    ) {
        addGoalToList(record.text, targetListId)
        inboxRecordDao.deleteById(record.id)
    }

    suspend fun addGoalWithReminder(
        title: String,
        listId: String,
        reminderTime: Long,
    ): Goal {
        val currentTime = System.currentTimeMillis()
        val newGoal =
            Goal(
                id = UUID.randomUUID().toString(),
                text = title,
                completed = false,
                createdAt = currentTime,
                updatedAt = currentTime,
                reminderTime = reminderTime,
            )
        goalDao.insertGoal(newGoal)
        syncContextMarker(newGoal.id, listId, ContextTextAction.ADD)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                listId = listId,
                itemType = ListItemType.GOAL,
                entityId = newGoal.id,
                order = -currentTime,
            )
        listItemDao.insertItem(newListItem)

        val finalGoalState = goalDao.getGoalById(newGoal.id)!!
        contextHandler.handleContextsOnCreate(finalGoalState)
        return newGoal
    }

    suspend fun logProjectTimeSummaryForDate(
        projectId: String,
        dayToLog: Calendar,
    ) {
        val calendar = dayToLog.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = calendar.timeInMillis - 1

        val goalIds = listItemDao.getGoalIdsForList(projectId)

        val activities =
            activityRepository.getCompletedActivitiesForProject(
                listId = projectId,
                goalIds = goalIds,
                startTime = startTime,
                endTime = endTime,
            )

        if (activities.isEmpty()) {
            return
        }

        var totalDurationMillis: Long = 0
        val activitiesByText = activities.groupBy { it.text }

        val detailsBuilder = StringBuilder()
        detailsBuilder.append("### Деталізація за день:\n\n")

        activitiesByText.forEach { (text, records) ->
            val durationForText = records.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }
            if (durationForText > 0) {
                totalDurationMillis += durationForText
                val formattedDuration = formatDuration(durationForText)
                detailsBuilder.append("- **$text**: $formattedDuration\n")
            }
        }

        if (totalDurationMillis <= 0) {
            return
        }

        val totalFormattedDuration = formatDuration(totalDurationMillis)
        val description = "Загальний час за день: $totalFormattedDuration."
        val details = detailsBuilder.toString()

        addProjectLogEntry(projectId = projectId, type = ProjectLogEntryType.AUTOMATIC, description = description, details = details)
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format(Locale.ROOT, "%d год %02d хв %02d с", hours, minutes, seconds)
        } else if (minutes > 0) {
            String.format(Locale.ROOT, "%d хв %02d с", minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d с", seconds)
        }
    }

    private suspend fun logTotalProjectTimeSummary(projectId: String) {
        val goalIds = listItemDao.getGoalIdsForList(projectId)
        val activities = activityRepository.getAllCompletedActivitiesForProject(projectId, goalIds)

        if (activities.isEmpty()) return

        val totalDurationMillis = activities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        if (totalDurationMillis <= 0) return

        val totalFormattedDuration = formatDuration(totalDurationMillis)
        val description = "Загальний час по проекту: $totalFormattedDuration."

        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryType.AUTOMATIC,
            description = description,
            details = "Розраховано на запит користувача.",
        )
    }

    suspend fun recalculateAndLogProjectTime(projectId: String) {
        logProjectTimeSummaryForDate(projectId, Calendar.getInstance())
        logTotalProjectTimeSummary(projectId)
    }

    suspend fun calculateProjectTimeMetrics(projectId: String): ProjectTimeMetrics {
        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        val startTime = todayCalendar.timeInMillis
        todayCalendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = todayCalendar.timeInMillis - 1

        val goalIds = listItemDao.getGoalIdsForList(projectId)
        val todayActivities = activityRepository.getCompletedActivitiesForProject(projectId, goalIds, startTime, endTime)
        val timeToday = todayActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        val allActivities = activityRepository.getAllCompletedActivitiesForProject(projectId, goalIds)
        val timeTotal = allActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        return ProjectTimeMetrics(timeToday = timeToday, timeTotal = timeTotal)
    }
}
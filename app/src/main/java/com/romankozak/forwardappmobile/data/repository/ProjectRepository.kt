package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentProjectDao
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

private val GlobalSearchResultItem.typeOrder: Int
    get() = when (this) {
        is GlobalSearchResultItem.ProjectItem,
        is GlobalSearchResultItem.SublistItem -> 0
        is GlobalSearchResultItem.GoalItem -> 1
        else -> 2
    }

@Singleton
class ProjectRepository
@Inject
constructor(
    private val goalDao: GoalDao,
    private val projectDao: ProjectDao,
    private val recentProjectDao: RecentProjectDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val contextHandlerProvider: Provider<ContextHandler>,
    private val inboxRecordDao: InboxRecordDao,
    private val activityRepository: ActivityRepository,
    private val projectManagementDao: ProjectManagementDao,
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }
    private val TAG = "AddSubprojectDebug"

    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        projectManagementDao.getLogsForProjectStream(projectId)

    suspend fun toggleProjectManagement(
        projectId: String,
        isEnabled: Boolean,
    ) {
        val project = getProjectById(projectId) ?: return
        if (project.isProjectManagementEnabled == isEnabled) return

        updateProject(project.copy(isProjectManagementEnabled = isEnabled))

        val status = if (isEnabled) "активовано" else "деактивовано"
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryType.AUTOMATIC,
            description = "Управління проектом було $status.",
        )
    }

    suspend fun updateProjectStatus(
        projectId: String,
        newStatus: ProjectStatus,
        statusText: String?,
    ) {
        val project = getProjectById(projectId) ?: return
        if (project.projectStatus == newStatus && project.projectStatusText == statusText) return

        updateProject(
            project.copy(
                projectStatus = newStatus,
                projectStatusText = statusText,
                updatedAt = System.currentTimeMillis(),
            ),
        )

        val logDescription =
            "Статус змінено на '$newStatus'." +
                    (statusText?.let { "\nКоментар: $it" } ?: "")

        addProjectLogEntry(
            projectId = projectId,
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

    suspend fun updateProjectViewMode(
        projectId: String,
        viewMode: ProjectViewMode,
    ) {
        projectDao.updateViewMode(projectId, viewMode.name)
    }

    fun getProjectContentStream(projectId: String): Flow<List<ListItemContent>> =
        listItemDao.getItemsForProjectStream(projectId).map { items ->
            items.mapNotNull { item ->
                when (item.itemType) {
                    ListItemType.GOAL -> goalDao.getGoalById(item.entityId)?.let { ListItemContent.GoalItem(it, item) }
                    ListItemType.SUBLIST -> projectDao.getProjectById(item.entityId)?.let { ListItemContent.SublistItem(it, item) }
                    ListItemType.LINK_ITEM -> linkItemDao.getLinkItemById(item.entityId)?.let { ListItemContent.LinkItem(it, item) }
                }
            }
        }

    suspend fun addGoalToProject(
        title: String,
        projectId: String,
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
        syncContextMarker(newGoal.id, projectId, ContextTextAction.ADD)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
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
    suspend fun addProjectLinkToProject(
        targetProjectId: String,
        currentProjectId: String,
    ): String {
        Log.d(TAG, "addProjectLinkToProject: targetProjectId=$targetProjectId, currentProjectId=$currentProjectId")
        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = currentProjectId,
                itemType = ListItemType.SUBLIST,
                entityId = targetProjectId,
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
        targetProjectId: String,
    ) {
        if (goalIds.isNotEmpty()) {
            val newItems =
                goalIds.map { goalId ->
                    ListItem(
                        id = UUID.randomUUID().toString(),
                        projectId = targetProjectId,
                        itemType = ListItemType.GOAL,
                        entityId = goalId,
                        order = -System.currentTimeMillis(),
                    )
                }
            listItemDao.insertItems(newItems)
        }
    }

    suspend fun copyGoalsToProject(
        goalIds: List<String>,
        targetProjectId: String,
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
                        projectId = targetProjectId,
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
        targetProjectId: String,
    ) {
        if (itemIds.isNotEmpty()) {
            listItemDao.updateListItemProjectIds(itemIds, targetProjectId)
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
        projectId: String,
        action: ContextTextAction,
    ) {
        val project = projectDao.getProjectById(projectId) ?: return
        val projectTags = project.tags.orEmpty()
        if (projectTags.isEmpty()) return

        val tagMap = contextHandler.tagToContextNameMap.value
        val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in projectTags }?.value ?: return
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
        projectId: String,
    ): Boolean = listItemDao.getLinkCount(entityId, projectId) > 0

    suspend fun deleteLinkByEntityIdAndProjectId(
        entityId: String,
        projectId: String,
    ) = listItemDao.deleteLinkByEntityAndProject(entityId, projectId)

    fun getAllProjectsFlow(): Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: String): Project? = projectDao.getProjectById(id)

    fun getProjectByIdFlow(id: String): Flow<Project?> = projectDao.getProjectByIdStream(id)

    suspend fun updateProject(project: Project) {
        projectDao.update(project)
    }

    suspend fun updateProjects(projects: List<Project>): Int = if (projects.isNotEmpty()) projectDao.update(projects) else 0

    @Transaction
    suspend fun deleteProjectsAndSubProjects(projectsToDelete: List<Project>) {
        if (projectsToDelete.isEmpty()) return
        val projectIds = projectsToDelete.map { it.id }
        listItemDao.deleteItemsForProjects(projectIds)
        projectsToDelete.forEach { projectDao.delete(it) }
    }

    suspend fun createProjectWithId(
        id: String,
        name: String,
        parentId: String?,
    ) {
        val newProject =
            Project(
                id = id,
                name = name,
                parentId = parentId,
                description = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        projectDao.insert(newProject)
    }

    suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)

    suspend fun updateGoals(goals: List<Goal>) = goalDao.updateGoals(goals)

    fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()

    @Transaction
    suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
        val goalResults =
            goalDao.searchGoalsGlobal(query).map {
                GlobalSearchResultItem.GoalItem(it)
            }
        val linkResults =
            linkItemDao.searchLinksGlobal(query).map {
                GlobalSearchResultItem.LinkItem(it)
            }
        val subprojectResults =
            projectDao.searchSubprojectsGlobal(query).map {
                GlobalSearchResultItem.SublistItem(it)
            }
        val projectResults =
            projectDao.searchProjectsGlobal(query).map {
                GlobalSearchResultItem.ProjectItem(it)
            }
        val activityResults =
            activityRepository.searchActivities(query).map {
                GlobalSearchResultItem.ActivityItem(it)
            }
        val inboxResults =
            inboxRecordDao.searchInboxRecordsGlobal(query).map {
                GlobalSearchResultItem.InboxItem(it)
            }

        val combinedResults = (goalResults + linkResults + subprojectResults + projectResults + activityResults + inboxResults)

        return combinedResults.sortedWith(
            compareBy<GlobalSearchResultItem> { it.typeOrder }
                .thenByDescending { it.timestamp }
        )
    }
    suspend fun logProjectAccess(projectId: String) {
        recentProjectDao.logAccess(RecentProjectEntry(projectId = projectId, lastAccessed = System.currentTimeMillis()))
    }

    fun getRecentProjects(limit: Int = 20): Flow<List<Project>> = recentProjectDao.getRecentProjects(limit)

    @Transaction
    suspend fun moveProject(
        projectToMove: Project,
        newParentId: String?,
    ) {
        val projectFromDb = projectDao.getProjectById(projectToMove.id) ?: return
        val oldParentId = projectFromDb.parentId

        if (oldParentId != newParentId) {
            val oldSiblings =
                (
                        if (oldParentId != null) {
                            projectDao.getProjectsByParentId(oldParentId)
                        } else {
                            projectDao.getTopLevelProjects()
                        }
                        ).filter { it.id != projectToMove.id }

            if (oldSiblings.isNotEmpty()) {
                projectDao.update(oldSiblings.mapIndexed { index, project -> project.copy(order = index.toLong()) })
            }
        }

        val newSiblings =
            (
                    if (newParentId != null) {
                        projectDao.getProjectsByParentId(newParentId)
                    } else {
                        projectDao.getTopLevelProjects()
                    }
                    ).filter { it.id != projectToMove.id }

        val finalProjectToMove =
            projectToMove.copy(
                parentId = newParentId,
                order = newSiblings.size.toLong(),
            )
        projectDao.update(finalProjectToMove)
    }

    @Transaction
    suspend fun addLinkItemToProject(
        projectId: String,
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
                projectId = projectId,
                itemType = ListItemType.LINK_ITEM,
                entityId = newLinkEntity.id,
                order = -System.currentTimeMillis(),
            )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    suspend fun findProjectIdsByTag(tag: String): List<String> = projectDao.getProjectIdsByTag(tag)

    suspend fun getAllProjects(): List<Project> = projectDao.getAll()

    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

    suspend fun getAllListItems(): List<ListItem> = listItemDao.getAll()

    suspend fun logCurrentDbOrderForDebug(projectId: String) {
        val itemsFromDb = listItemDao.getItemsForProjectSyncForDebug(projectId)
        val orderLog =
            itemsFromDb.joinToString(separator = "\n") {
                "  - DB_ORDER=${it.order}, id=${it.id}"
            }
        Log.d("DND_DEBUG", "[DEBUG_QUERY] СИРИЙ ПОРЯДОК З БАЗИ ДАНИХ:\n$orderLog")
    }
    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
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
        addGoalToProject(record.text, record.projectId)
        inboxRecordDao.deleteById(record.id)
    }

    @Transaction
    suspend fun promoteInboxRecordToGoal(
        record: InboxRecord,
        targetProjectId: String,
    ) {
        addGoalToProject(record.text, targetProjectId)
        inboxRecordDao.deleteById(record.id)
    }

    suspend fun addGoalWithReminder(
        title: String,
        projectId: String,
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
        syncContextMarker(newGoal.id, projectId, ContextTextAction.ADD)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
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

        val goalIds = listItemDao.getGoalIdsForProject(projectId)

        val activities =
            activityRepository.getCompletedActivitiesForProject(
                projectId = projectId,
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
        val goalIds = listItemDao.getGoalIdsForProject(projectId)
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

        val goalIds = listItemDao.getGoalIdsForProject(projectId)
        val todayActivities = activityRepository.getCompletedActivitiesForProject(projectId, goalIds, startTime, endTime)
        val timeToday = todayActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        val allActivities = activityRepository.getAllCompletedActivitiesForProject(projectId, goalIds)
        val timeTotal = allActivities.sumOf { (it.endTime ?: 0) - (it.startTime ?: 0) }

        return ProjectTimeMetrics(timeToday = timeToday, timeTotal = timeTotal)
    }
}
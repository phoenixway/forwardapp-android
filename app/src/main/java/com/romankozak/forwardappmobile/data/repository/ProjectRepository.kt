package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import androidx.room.Transaction
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.ProjectLogEntryTypeValues
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues
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
    get() =
        when (this) {
            is GlobalSearchResultItem.ProjectItem,
            is GlobalSearchResultItem.SublistItem,
            -> 0
            is GlobalSearchResultItem.GoalItem -> 1
            else -> 2
        }

@Singleton
class ProjectRepository
@Inject
constructor(
    private val goalDao: GoalDao,
    private val projectDao: ProjectDao,
    private val listItemDao: ListItemDao,
    private val linkItemDao: LinkItemDao,
    private val noteDao: NoteDao,
    private val customListDao: CustomListDao,
    private val contextHandlerProvider: Provider<ContextHandler>,
    private val inboxRecordDao: InboxRecordDao,
    private val activityRepository: ActivityRepository,
    private val projectManagementDao: ProjectManagementDao,
    private val recentItemDao: RecentItemDao,
    val reminderDao: ReminderDao,
) {
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }
    private val TAG = "CUSTOM_LIST_DEBUG"

    fun getProjectLogsStream(projectId: String): Flow<List<ProjectExecutionLog>> =
        projectManagementDao.getLogsForProjectStream(
            projectId,
        )

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
            type = ProjectLogEntryTypeValues.AUTOMATIC,
            description = "Управління проектом було $status.",
        )
    }

    suspend fun updateProjectStatus(
        projectId: String,
        newStatus: String,
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
            type = ProjectLogEntryTypeValues.STATUS_CHANGE,
            description = logDescription,
        )
    }

    suspend fun addProjectComment(
        projectId: String,
        comment: String,
    ) {
        addProjectLogEntry(
            projectId = projectId,
            type = ProjectLogEntryTypeValues.COMMENT,
            description = comment,
        )
    }

    private suspend fun addProjectLogEntry(
        projectId: String,
        type: String,
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
                    ListItemTypeValues.GOAL ->
                        goalDao.getGoalById(item.entityId)?.let { goal ->
                            ListItemContent.GoalItem(goal, item)
                        }
                    ListItemTypeValues.SUBLIST ->
                        projectDao.getProjectById(item.entityId)?.let { project ->
                            ListItemContent.SublistItem(project, item)
                        }
                    ListItemTypeValues.LINK_ITEM ->
                        linkItemDao.getLinkItemById(item.entityId)?.let { link ->
                            ListItemContent.LinkItem(link, item)
                        }
                    ListItemTypeValues.NOTE ->
                        noteDao.getNoteById(item.entityId)?.let { note ->
                            ListItemContent.NoteItem(note, item)
                        }
                    ListItemTypeValues.CUSTOM_LIST ->
                        customListDao.getCustomListById(item.entityId)?.let { customList ->
                            ListItemContent.CustomListItem(customList, item)
                        }
                    else -> null
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
                itemType = ListItemTypeValues.GOAL,
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
                itemType = ListItemTypeValues.SUBLIST,
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
                        itemType = ListItemTypeValues.GOAL,
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
                        itemType = ListItemTypeValues.GOAL,
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

    suspend fun restoreListItems(items: List<ListItem>) {
        if (items.isNotEmpty()) {
            listItemDao.insertItems(items)
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
        val recentItem = recentItemDao.getRecentItemById(project.id)
        if (recentItem != null) {
            recentItemDao.logAccess(recentItem.copy(displayName = project.name))
        }
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

    @Transaction
    suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoalById(goalId)
        listItemDao.deleteItemByEntityId(goalId)
    }

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)

    suspend fun updateGoals(goals: List<Goal>) = goalDao.updateGoals(goals)

    fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()

    @Transaction
    suspend fun searchGlobal(query: String): List<GlobalSearchResultItem> {
        val goalResults =
            goalDao.searchGoalsGlobal(query).mapNotNull { searchResult ->
                val listItem = listItemDao.getListItemByEntityId(searchResult.goal.id)
                listItem?.let {
                    GlobalSearchResultItem.GoalItem(
                        goal = searchResult.goal,
                        listItem = it,
                        projectName = searchResult.projectName,
                        pathSegments = searchResult.pathSegments
                    )
                }
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
                .thenByDescending { it.timestamp },
        )
    }

    suspend fun logProjectAccess(projectId: String) {
        val project = getProjectById(projectId)
        if (project != null) {
            val existingItem = recentItemDao.getRecentItemById(project.id)
            val recentItem = if (existingItem != null) {
                existingItem.copy(lastAccessed = System.currentTimeMillis())
            } else {
                RecentItem(
                    id = project.id,
                    type = RecentItemType.PROJECT,
                    lastAccessed = System.currentTimeMillis(),
                    displayName = project.name,
                    target = project.id
                )
            }
            Log.d("Recents_Debug", "Logging project access: $recentItem")
            recentItemDao.logAccess(recentItem)
        }
    }

    fun getRecentItems(limit: Int = 20): Flow<List<RecentItem>> = recentItemDao.getRecentItems(limit)

    suspend fun logNoteAccess(note: NoteEntity) {
        val existingItem = recentItemDao.getRecentItemById(note.id)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = note.id,
                type = RecentItemType.NOTE,
                lastAccessed = System.currentTimeMillis(),
                displayName = note.title,
                target = note.id
            )
        }
        Log.d("Recents_Debug", "Logging note access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun logCustomListAccess(customList: CustomListEntity) {
        val existingItem = recentItemDao.getRecentItemById(customList.id)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = customList.id,
                type = RecentItemType.CUSTOM_LIST,
                lastAccessed = System.currentTimeMillis(),
                displayName = customList.name,
                target = customList.id
            )
        }
        Log.d("Recents_Debug", "Logging custom list access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun logObsidianLinkAccess(link: RelatedLink) {
        val existingItem = recentItemDao.getRecentItemById(link.target)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = link.target,
                type = RecentItemType.OBSIDIAN_LINK,
                lastAccessed = System.currentTimeMillis(),
                displayName = link.displayName ?: link.target,
                target = link.target
            )
        }
        Log.d("Recents_Debug", "Logging obsidian link access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun updateRecentItem(item: RecentItem) {
        recentItemDao.logAccess(item)
    }

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
                itemType = ListItemTypeValues.LINK_ITEM,
                entityId = newLinkEntity.id,
                order = -System.currentTimeMillis(),
            )
        listItemDao.insertItem(newListItem)
        return newListItem.id
    }

    suspend fun findProjectIdsByTag(tag: String): List<String> = projectDao.getProjectIdsByTag(tag)

    suspend fun getAllProjects(): List<Project> = projectDao.getAll()

    fun getAllGoalsFlow(): Flow<List<Goal>> = goalDao.getAllGoalsFlow()

    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

    suspend fun deleteItemByEntityId(entityId: String) {
        listItemDao.deleteItemByEntityId(entityId)
    }

    suspend fun findProjectIdForGoal(goalId: String): String? {
        return listItemDao.findProjectIdForGoal(goalId)
    }

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

    suspend fun getInboxRecordById(id: String): InboxRecord? = inboxRecordDao.getRecordById(id)

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
                itemType = ListItemTypeValues.GOAL,
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

        addProjectLogEntry(projectId = projectId, type = ProjectLogEntryTypeValues.AUTOMATIC, description = description, details = details)
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
            type = ProjectLogEntryTypeValues.AUTOMATIC,
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

    
    suspend fun getNoteById(noteId: String): NoteEntity? = noteDao.getNoteById(noteId)

    fun getNotesForProject(projectId: String): Flow<List<NoteEntity>> = noteDao.getNotesForProject(projectId)

    @Transaction
    suspend fun saveNote(note: NoteEntity) {
        val existingNote = noteDao.getNoteById(note.id)
        if (existingNote == null) {
            noteDao.insert(note)
            
            val newListItem =
                ListItem(
                    id = UUID.randomUUID().toString(),
                    projectId = note.projectId,
                    itemType = ListItemTypeValues.NOTE,
                    entityId = note.id,
                    order = -System.currentTimeMillis(),
                )
            listItemDao.insertItem(newListItem)
        } else {
            noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
            val recentItem = recentItemDao.getRecentItemById(note.id)
            if (recentItem != null) {
                recentItemDao.logAccess(recentItem.copy(displayName = note.title))
            }
        }
    }

    @Transaction
    suspend fun deleteNote(noteId: String) {
        noteDao.deleteNoteById(noteId)
        listItemDao.deleteItemByEntityId(noteId)
    }

    
    fun getCustomListsForProject(projectId: String): Flow<List<CustomListEntity>> = customListDao.getCustomListsForProject(projectId)

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
        val recentItem = recentItemDao.getRecentItemById(list.id)
        if (recentItem != null) {
            recentItemDao.logAccess(recentItem.copy(displayName = list.name))
        }
        Log.d(TAG, "updateCustomList finished")
    }

    suspend fun cleanupDanglingListItems() {
        val allListItems = listItemDao.getAll()
        val itemsToDelete = mutableListOf<String>()

        allListItems.forEach { item ->
            val entityExists = when (item.itemType) {
                ListItemTypeValues.GOAL -> goalDao.getGoalById(item.entityId) != null
                ListItemTypeValues.SUBLIST -> projectDao.getProjectById(item.entityId) != null
                ListItemTypeValues.LINK_ITEM -> linkItemDao.getLinkItemById(item.entityId) != null
                ListItemTypeValues.NOTE -> noteDao.getNoteById(item.entityId) != null
                ListItemTypeValues.CUSTOM_LIST -> customListDao.getCustomListById(item.entityId) != null
                else -> true // Assume unknown types are valid to avoid deleting them
            }
            if (!entityExists) {
                itemsToDelete.add(item.id)
            }
        }

        if (itemsToDelete.isNotEmpty()) {
            listItemDao.deleteItemsByIds(itemsToDelete)
            Log.d("DB_CLEANUP", "Deleted ${itemsToDelete.size} dangling ListItem records.")
        }
    }


}

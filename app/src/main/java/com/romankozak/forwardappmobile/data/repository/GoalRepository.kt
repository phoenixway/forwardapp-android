package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import com.romankozak.forwardappmobile.core.database.models.Goal
import com.romankozak.forwardappmobile.core.database.models.ListItem
import com.romankozak.forwardappmobile.core.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.features.projects.data.ContextTextAction
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val listItemQueries: ListItemQueries,
    private val reminderRepository: ReminderRepository,
    private val contextHandlerProvider: Provider<ContextHandler>,
    private val projectLocalDataSource: ProjectLocalDataSource,
)
{
    private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }

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
        listItemQueries.insert(
            id = newListItem.id,
            project_id = newListItem.projectId,
            item_type = newListItem.itemType,
            entity_id = newListItem.entityId,
            item_order = newListItem.order
        )
        contextHandler.handleContextsOnCreate(finalGoalState)
        return newListItem.id
    }

    @androidx.room.Transaction
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
            )
        goalDao.insertGoal(newGoal)

        val newListItem =
            ListItem(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                itemType = ListItemTypeValues.GOAL,
                entityId = newGoal.id,
                order = -currentTime,
            )
        listItemDao.insertItem(newListItem)

        reminderRepository.createReminder(newGoal.id, "GOAL", reminderTime)

        syncContextMarker(newGoal.id, projectId, ContextTextAction.ADD)
        contextHandler.handleContextsOnCreate(newGoal)
        return newGoal
    }

    suspend fun createGoalLinks(
        goalIds: List<String>,
        targetProjectId: String,
    ) {
        if (goalIds.isNotEmpty()) {
            val newItems =
                goalIds.map {
                    ListItem(
                        id = UUID.randomUUID().toString(),
                        projectId = targetProjectId,
                        itemType = ListItemTypeValues.GOAL,
                        entityId = it,
                        order = -System.currentTimeMillis(),
                    )
        newItems.forEach { listItem ->
            listItemQueries.insert(
                id = listItem.id,
                project_id = listItem.projectId,
                item_type = listItem.itemType,
                entity_id = listItem.entityId,
                item_order = listItem.order
            )
        }
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

            originalGoals.forEach {
                val newGoal = it.copy(id = UUID.randomUUID().toString())
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

    suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)

    @androidx.room.Transaction
    suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoalById(goalId)
        listItemQueries.deleteByEntityId(goalId)
    }

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)

    suspend fun updateGoals(goals: List<Goal>) = goalDao.updateGoals(goals)

    fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()

    fun getAllGoalsFlow(): Flow<List<Goal>> = goalDao.getAllGoalsFlow()

    suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

    suspend fun findProjectIdForGoal(goalId: String): String? {
        return listItemQueries.findProjectIdForGoal(goalId).executeAsOneOrNull()
    }

    private suspend fun syncContextMarker(
        goalId: String,
        projectId: String,
        action: ContextTextAction,
    ) {
        val project = projectLocalDataSource.getById(projectId) ?: return
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
            newText = goal.text.replace(Regex("""\s*${Regex.escape(marker)}\s*"""), " ").trim()
        }

        if (newText != goal.text) {
            goalDao.updateGoal(goal.copy(text = newText, updatedAt = System.currentTimeMillis()))
        }
    }
}

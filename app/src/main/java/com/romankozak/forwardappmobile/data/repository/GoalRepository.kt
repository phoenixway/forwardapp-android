package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val listItemDao: ListItemDao,
    private val contextHandler: ContextHandler,
    private val reminderRepository: ReminderRepository,
    private val projectDao: ProjectDao,
) {

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
}
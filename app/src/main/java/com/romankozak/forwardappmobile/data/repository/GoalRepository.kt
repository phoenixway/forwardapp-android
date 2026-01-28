package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val SYNC_LOG_TAG = "FWD_SYNC_TEST"

@Singleton
class GoalRepository
    @Inject
    constructor(
        private val goalDao: GoalDao,
        private val listItemDao: ListItemDao,
        private val reminderRepository: ReminderRepository,
        private val contextHandlerProvider: Provider<ContextHandler>,
        private val contextDao: ContextDao,
    ) {
        private val contextHandler: ContextHandler by lazy { contextHandlerProvider.get() }

        suspend fun addGoalToContext(
            title: String,
            contextId: String,
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
            syncContextMarker(newGoal.id, contextId, ContextTextAction.ADD)

            val newBacklogItem =
                BacklogItem(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    itemType = BacklogItemTypeValues.GOAL,
                    entityId = newGoal.id,
                    order = -currentTime,
                )
            listItemDao.insertItem(newBacklogItem)

            val finalGoalState = goalDao.getGoalById(newGoal.id)!!
            contextHandler.handleContextsOnCreate(finalGoalState)
            return newBacklogItem.id
        }

        @androidx.room.Transaction
        suspend fun addGoalWithReminder(
            title: String,
            contextId: String,
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

            val newBacklogItem =
                BacklogItem(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    itemType = BacklogItemTypeValues.GOAL,
                    entityId = newGoal.id,
                    order = -currentTime,
                )
            listItemDao.insertItem(newBacklogItem)

            reminderRepository.createReminder(newGoal.id, "GOAL", reminderTime)

            syncContextMarker(newGoal.id, contextId, ContextTextAction.ADD)
            contextHandler.handleContextsOnCreate(newGoal)
            return newGoal
        }

        suspend fun createGoalLinks(
            goalIds: List<String>,
            targetContextId: String,
        ) {
            if (goalIds.isNotEmpty()) {
                val newItems =
                    goalIds.map {
                        BacklogItem(
                            id = UUID.randomUUID().toString(),
                            contextId = targetContextId,
                            itemType = BacklogItemTypeValues.GOAL,
                            entityId = it,
                            order = -System.currentTimeMillis(),
                        )
                    }
                listItemDao.insertItems(newItems)
            }
        }

        suspend fun copyGoalsToContext(
            goalIds: List<String>,
            targetContextId: String,
        ) {
            if (goalIds.isNotEmpty()) {
                val originalGoals = goalDao.getGoalsByIdsSuspend(goalIds)
                val newGoals = mutableListOf<Goal>()
                val newItems = mutableListOf<BacklogItem>()

                originalGoals.forEach {
                    val newGoal = it.copy(id = UUID.randomUUID().toString())
                    newGoals.add(newGoal)
                    newItems.add(
                        BacklogItem(
                            id = UUID.randomUUID().toString(),
                            contextId = targetContextId,
                            itemType = BacklogItemTypeValues.GOAL,
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
            val now = System.currentTimeMillis()
            goalDao.getGoalById(goalId)?.let { goal ->
                Log.d(SYNC_LOG_TAG, "[GoalRepo] Deleting Goal. Original: $goal")
                val tombstone = goal.softDelete(now)
                Log.d(SYNC_LOG_TAG, "[GoalRepo] Deleting Goal. Tombstone: $tombstone")
                goalDao.insertGoal(tombstone)
            }
            listItemDao.getListItemByEntityId(goalId)?.let { listItem ->
                Log.d(SYNC_LOG_TAG, "[GoalRepo] Deleting ListItem. Original: $listItem")
                val tombstone = listItem.softDelete(now)
                Log.d(SYNC_LOG_TAG, "[GoalRepo] Deleting ListItem. Tombstone: $tombstone")
                listItemDao.insertItem(tombstone)
            }
        }

        suspend fun updateGoal(goal: Goal) {
            goalDao.updateGoal(goal.bumpSync())
        }

        suspend fun updateGoals(goals: List<Goal>) {
            if (goals.isNotEmpty()) {
                goalDao.updateGoals(goals.map { it.bumpSync() })
            }
        }

        fun getAllGoalsCountFlow(): Flow<Int> = goalDao.getAllGoalsCountFlow()

        fun getAllGoalsFlow(): Flow<List<Goal>> = goalDao.getAllVisibleGoalsFlow()

        suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

        suspend fun findContextIdForGoal(goalId: String): String? {
            return listItemDao.findContextIdForGoal(goalId)
        }

        private suspend fun syncContextMarker(
            goalId: String,
            contextId: String,
            action: ContextTextAction,
        ) {
            val context = contextDao.getContextById(contextId) ?: return
            val contextTags = context.tags.orEmpty()
            if (contextTags.isEmpty()) return

            val tagMap = contextHandler.tagToContextNameMap.value
            val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in contextTags }?.value ?: return
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

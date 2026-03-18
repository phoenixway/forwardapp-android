package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
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
        private val contextMarkerHandlerProvider: Provider<ContextMarkerHandler>,
        private val contextDao: ContextDao,
    ) {
        private val contextMarkerHandler: ContextMarkerHandler by lazy { contextMarkerHandlerProvider.get() }

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

            // Тепер ContextTextAction буде знайдено
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
            contextMarkerHandler.handleContextsOnCreate(finalGoalState)
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
            contextMarkerHandler.handleContextsOnCreate(newGoal)
            return newGoal
        }

        suspend fun updateGoal(goal: Goal) {
            val now = System.currentTimeMillis()
            goalDao.updateGoal(goal.bumpSync(now))
        }

        suspend fun updateGoals(goals: List<Goal>) {
            if (goals.isNotEmpty()) {
                val now = System.currentTimeMillis()
                goalDao.updateGoals(goals.map { it.bumpSync(now) })
            }
        }

        private suspend fun syncContextMarker(
            goalId: String,
            contextId: String,
            action: ContextTextAction,
        ) {
            val context = contextDao.getContextById(contextId) ?: return
            val contextTags = context.tags.orEmpty()
            if (contextTags.isEmpty()) return

            val tagMap = contextMarkerHandler.tagToContextMarkerNameMap.value
            val contextName = tagMap.entries.find { (tagKey, _) -> tagKey in contextTags }?.value ?: return
            val marker = contextMarkerHandler.getContextMarker(contextName) ?: return
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

        // Решта методів залишаються без змін...
        suspend fun getGoalById(id: String): Goal? = goalDao.getGoalById(id)

        fun getAllGoalsFlow(): Flow<List<Goal>> = goalDao.getAllVisibleGoalsFlow()

        // У GoalRepository.kt перевірте модифікатор доступу
        suspend fun createGoalLinks(
            goalIds: List<String>,
            targetContextId: String,
            sourceContextId: String? = null,
        ) {
            if (goalIds.isNotEmpty()) {
                val sourceContextIdNormalized = sourceContextId?.takeIf { it.isNotBlank() && it != targetContextId }
                val sourceContextLink =
                    sourceContextIdNormalized?.let { sourceId ->
                        val sourceName = contextDao.getContextById(sourceId)?.name?.trim().orEmpty()
                        RelatedLink(
                            type = LinkType.CONTEXT,
                            target = sourceId,
                            displayName = sourceName.ifBlank { "Контекст" },
                        )
                    }

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

                if (sourceContextLink != null) {
                    goalIds.forEach { goalId ->
                        val goal = goalDao.getGoalById(goalId) ?: return@forEach
                        val existingLinks = goal.relatedLinks.orEmpty()
                        if (existingLinks.any { it.type == LinkType.CONTEXT && it.target == sourceContextLink.target }) {
                            return@forEach
                        }
                        goalDao.updateGoal(
                            goal.copy(
                                relatedLinks = existingLinks + sourceContextLink,
                                updatedAt = System.currentTimeMillis(),
                            ).bumpSync(System.currentTimeMillis()),
                        )
                    }
                }
            }
        }

        suspend fun copyGoalsToContext(
            goalIds: List<String>,
            targetContextId: String,
        ) {
            if (goalIds.isEmpty()) return
            val originalGoals = goalDao.getGoalsByIdsSuspend(goalIds)
            val now = System.currentTimeMillis()

            originalGoals.forEach { original ->
                val newGoal = original.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now, syncedAt = null)
                goalDao.insertGoal(newGoal)

                val newItem =
                    BacklogItem(
                        id = UUID.randomUUID().toString(),
                        contextId = targetContextId,
                        itemType = BacklogItemTypeValues.GOAL,
                        entityId = newGoal.id,
                        order = -now,
                    )
                listItemDao.insertItem(newItem)
            }
        }

        suspend fun deleteGoal(goalId: String) {
            val now = System.currentTimeMillis()
            goalDao.getGoalById(goalId)?.let { goal ->
                goalDao.insertGoal(goal.softDelete(now))
            }
            listItemDao.getListItemByEntityId(goalId)?.let { item ->
                listItemDao.insertItem(item.softDelete(now))
            }
        }

        suspend fun findContextIdForGoal(goalId: String): String? = listItemDao.findContextIdForGoal(goalId)

        suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

        fun getGoalsByContextIdFlow(contextId: String): Flow<List<Goal>> {
            return goalDao.getGoalsByContextIdFlow(contextId)
        }
    }

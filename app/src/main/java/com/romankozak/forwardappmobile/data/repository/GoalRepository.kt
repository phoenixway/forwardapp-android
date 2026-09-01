package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
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
        private val reminderRepository: ReminderRepository,
        private val contextMarkerHandlerProvider: Provider<ContextMarkerHandler>,
        private val contextDao: ContextDao,
        private val tagAssociationHandler: TagAssociationHandler,
        private val contextStructureRepository: ContextStructureRepository,
        private val backlogPlacementCommands: BacklogPlacementCommands,
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
                    goalStatus = if (completed) GoalStatusValues.DONE else GoalStatusValues.ACTIVE,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                )
            return addGoalToContext(newGoal, contextId)
        }

        suspend fun addGoalToContext(
            goal: Goal,
            contextId: String,
        ): String {
            val goalToInsert =
                normalizeGoalState(goal).copy(
                    updatedAt = goal.updatedAt ?: System.currentTimeMillis(),
                )
            goalDao.insertGoal(goalToInsert)

            // Тепер ContextTextAction буде знайдено
            syncContextMarker(goalToInsert.id, contextId, ContextTextAction.ADD)

            val backlogItemId =
                backlogPlacementCommands.addToContextBacked(
                    contextId = contextId,
                    itemType = BacklogItemTypeValues.GOAL,
                    entityId = goalToInsert.id,
                )

            val finalGoalState = goalDao.getGoalById(goalToInsert.id)!!
            contextMarkerHandler.handleContextsOnCreate(finalGoalState)
            val associatedContexts = tagAssociationHandler.syncGoalAssociations(finalGoalState, contextId)
            applyBacklogAutoMovePolicy(
                goalId = finalGoalState.id,
                sourceContextId = contextId,
                associatedContextIds = associatedContexts.keys,
            )
            return backlogItemId
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
                    goalStatus = GoalStatusValues.ACTIVE,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                )
            goalDao.insertGoal(newGoal)

            backlogPlacementCommands.addToContextBacked(
                contextId = contextId,
                itemType = BacklogItemTypeValues.GOAL,
                entityId = newGoal.id,
            )

            reminderRepository.createReminder(newGoal.id, "GOAL", reminderTime)

            syncContextMarker(newGoal.id, contextId, ContextTextAction.ADD)
            contextMarkerHandler.handleContextsOnCreate(newGoal)
            val associatedContexts = tagAssociationHandler.syncGoalAssociations(newGoal, contextId)
            applyBacklogAutoMovePolicy(
                goalId = newGoal.id,
                sourceContextId = contextId,
                associatedContextIds = associatedContexts.keys,
            )
            return newGoal
        }

        suspend fun updateGoal(
            goal: Goal,
            sourceContextId: String? = null,
        ) {
            val now = System.currentTimeMillis()
            val updatedGoal = normalizeGoalState(goal).bumpSync(now)
            goalDao.updateGoal(updatedGoal)
            val resolvedSourceContextId =
                sourceContextId?.takeIf { it.isNotBlank() }
                    ?: backlogPlacementCommands.findFirstContextBackedWorkspaceId(
                        itemType = BacklogItemTypeValues.GOAL,
                        entityId = updatedGoal.id,
                    )
                    ?: tagAssociationHandler.findGoalAssociationOwnerContextId(updatedGoal.id)
            val associatedContexts = tagAssociationHandler.syncGoalAssociations(updatedGoal, resolvedSourceContextId)
            if (resolvedSourceContextId != null) {
                applyBacklogAutoMovePolicy(
                    goalId = updatedGoal.id,
                    sourceContextId = resolvedSourceContextId,
                    associatedContextIds = associatedContexts.keys,
                )
            }
        }

        suspend fun updateGoals(goals: List<Goal>) {
            if (goals.isNotEmpty()) {
                val now = System.currentTimeMillis()
                goalDao.updateGoals(goals.map { normalizeGoalState(it).bumpSync(now) })
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

                backlogPlacementCommands.addManyToContextBacked(
                    contextId = targetContextId,
                    entries = goalIds.map { BacklogItemTypeValues.GOAL to it },
                )

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

                backlogPlacementCommands.addToContextBacked(
                    contextId = targetContextId,
                    itemType = BacklogItemTypeValues.GOAL,
                    entityId = newGoal.id,
                )
            }
        }

        suspend fun deleteGoal(goalId: String) {
            val now = System.currentTimeMillis()
            goalDao.getGoalById(goalId)?.let { goal ->
                goalDao.insertGoal(goal.softDelete(now))
            }
            tagAssociationHandler.removeGoalAssociations(goalId)
            backlogPlacementCommands.tombstoneContextBackedTarget(
                itemType = BacklogItemTypeValues.GOAL,
                entityId = goalId,
                now = now,
            )
        }

        suspend fun findContextIdForGoal(goalId: String): String? =
            backlogPlacementCommands.findFirstContextBackedWorkspaceId(
                itemType = BacklogItemTypeValues.GOAL,
                entityId = goalId,
            )

        suspend fun getAllGoals(): List<Goal> = goalDao.getAll()

        fun getGoalsByContextIdFlow(contextId: String): Flow<List<Goal>> {
            return goalDao.getGoalsByContextIdFlow(contextId)
        }

        private suspend fun applyBacklogAutoMovePolicy(
            goalId: String,
            sourceContextId: String,
            associatedContextIds: Set<String>,
        ) {
            if (
                !backlogPlacementCommands.hasContextBackedPlacementHistory(
                    contextId = sourceContextId,
                    itemType = BacklogItemTypeValues.GOAL,
                    entityId = goalId,
                )
            ) {
                return
            }
            val shouldHide =
                shouldRemoveBacklogAfterTagAutocopy(sourceContextId) &&
                    associatedContextIds.isNotEmpty()
            backlogPlacementCommands.setContextBackedPlacementVisible(
                contextId = sourceContextId,
                itemType = BacklogItemTypeValues.GOAL,
                entityId = goalId,
                visible = !shouldHide,
            )
        }

        private suspend fun shouldRemoveBacklogAfterTagAutocopy(contextId: String): Boolean =
            contextStructureRepository.getStructureByContext(contextId)?.removeBacklogEntryAfterTagAutocopy == true

        private fun normalizeGoalState(goal: Goal): Goal {
            val normalizedStatus =
                when {
                    goal.completed -> GoalStatusValues.DONE
                    GoalStatusValues.isTerminal(goal.goalStatus) -> GoalStatusValues.ACTIVE
                    else -> GoalStatusValues.normalize(goal.goalStatus)
                }

            return goal.copy(
                completed = normalizedStatus == GoalStatusValues.DONE,
                goalStatus = normalizedStatus,
            )
        }
    }

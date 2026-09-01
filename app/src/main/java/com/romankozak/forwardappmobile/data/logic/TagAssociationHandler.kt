package com.romankozak.forwardappmobile.data.logic

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogGoalAssociationLink
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTagRef
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextTagRefDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogGoalAssociationLinkDao
import com.romankozak.forwardappmobile.data.repository.BacklogPlacementCommands
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagAssociationHandler
    @Inject
    constructor(
        private val contextTagRefDao: ContextTagRefDao,
        private val contextDao: ContextDao,
        private val backlogPlacementCommands: BacklogPlacementCommands,
        private val backlogGoalAssociationLinkDao: BacklogGoalAssociationLinkDao,
        private val inboxAssociationCache: InboxAssociationCache,
        private val goalDao: GoalDao,
        private val inboxRecordDao: InboxRecordDao,
    ) {
        fun normalizeTags(tags: List<String>?): List<String> =
            tags.orEmpty()
                .mapNotNull { tag ->
                    tag.trim().removePrefix("#").lowercase().takeIf { it.isNotBlank() }
                }.distinct()
                .sorted()

        fun extractHashtags(text: String): List<String> =
            TAG_REGEX.findAll(text)
                .map { match -> match.groupValues[1].trim().lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()

        @Transaction
        suspend fun syncContextTags(
            context: Context,
            previousTags: List<String>? = null,
        ) {
            val normalizedTags = normalizeTags(context.tags)
            contextTagRefDao.deleteForContext(context.id)
            if (normalizedTags.isNotEmpty()) {
                contextTagRefDao.insertAll(normalizedTags.map { normalized -> ContextTagRef(context.id, normalized) })
            }

            val changedTags =
                ((previousTags ?: emptyList()).let(::normalizeTags) + normalizedTags)
                    .distinct()
                    .filter { tag ->
                        tag in normalizeTags(previousTags) || tag in normalizedTags
                    }
            if (changedTags.isNotEmpty()) {
                reconcileChangedTags(changedTags)
            }
        }

        @Transaction
        suspend fun syncGoalAssociations(
            goal: Goal,
            sourceContextId: String?,
        ): Map<String, String> {
            val directContextIds =
                backlogPlacementCommands
                    .findLiveWorkspaceIds(
                        itemType = BacklogItemTypeValues.GOAL,
                        entityId = goal.id,
                    )
                    .filter { workspaceId ->
                        contextDao.getContextById(workspaceId) != null
                    }
                    .toSet()

            val ownerContextId =
                sourceContextId?.takeIf { it.isNotBlank() }
                    ?: directContextIds.sorted().firstOrNull()
                    ?: return emptyMap()
            val desiredContexts = resolveDesiredContexts(goal.text).filterKeys { it !in directContextIds }
            val existingAutoItems =
                backlogGoalAssociationLinkDao
                    .getLinksForGoal(goal.id)
                    .associateBy { it.contextId }

            val staleContextIds = existingAutoItems.keys - desiredContexts.keys
            if (staleContextIds.isNotEmpty()) {
                backlogGoalAssociationLinkDao.deleteForGoalAndContexts(
                    goalId = goal.id,
                    contextIds = staleContextIds.toList(),
                )
            }

            val now = System.currentTimeMillis()
            val desiredOrder = -(goal.updatedAt ?: goal.createdAt)
            val itemsToUpsert =
                desiredContexts.mapNotNull { (contextId, matchedTag) ->
                    val existing = existingAutoItems[contextId]
                    if (
                        existing != null &&
                        existing.ownerContextId == ownerContextId &&
                        existing.associationTag == matchedTag &&
                        existing.order == desiredOrder
                    ) {
                        null
                    } else {
                        BacklogGoalAssociationLink(
                            projectionId = stableGoalAssociationProjectionId(goal.id, contextId),
                            goalId = goal.id,
                            contextId = contextId,
                            ownerContextId = ownerContextId,
                            associationTag = matchedTag,
                            order = desiredOrder,
                            linkedAt = existing?.linkedAt ?: now,
                        )
                    }
                }
            if (itemsToUpsert.isNotEmpty()) {
                backlogGoalAssociationLinkDao.insertAll(itemsToUpsert)
            }
            return desiredContexts
        }

        /**
         * A projection owner is retained while auto-move policy hides the
         * source placement. It is the safe recovery hint for a later edit:
         * unlike arbitrary placement history, it cannot resurrect an item
         * that the user removed explicitly.
         */
        suspend fun findGoalAssociationOwnerContextId(goalId: String): String? =
            backlogGoalAssociationLinkDao
                .getLinksForGoal(goalId)
                .map { it.ownerContextId }
                .distinct()
                .singleOrNull()

        @Transaction
        suspend fun reconcileChangedTags(changedTags: List<String>) {
            if (changedTags.isEmpty()) return
            val changedSet = changedTags.map { it.lowercase() }.toSet()

            goalDao.getAllVisible().asSequence()
                .filter { goal -> extractHashtags(goal.text).any { it in changedSet } }
                .forEach { goal -> syncGoalAssociations(goal, sourceContextId = null) }

            inboxRecordDao.getAll().asSequence()
                .filter { record -> !record.isDeleted && extractHashtags(record.text).any { it in changedSet } }
                .forEach { record -> inboxAssociationCache.refresh(record) }
        }

        @Transaction
        suspend fun removeGoalAssociations(goalId: String) {
            backlogGoalAssociationLinkDao.deleteForGoal(goalId)
        }

        @Transaction
        suspend fun repairAllAssociations() {
            backlogGoalAssociationLinkDao.deleteAll()

            contextDao.getAll()
                .filterNot { it.isDeleted }
                .forEach { context -> syncContextTags(context, previousTags = null) }

            goalDao.getAllVisible().forEach { goal ->
                syncGoalAssociations(goal, sourceContextId = null)
            }

            inboxAssociationCache.rebuild()
        }

        private suspend fun resolveDesiredContexts(text: String): Map<String, String> {
            val tags = extractHashtags(text)
            if (tags.isEmpty()) return emptyMap()
            return contextTagRefDao.findContextsByTags(tags)
                .groupBy { it.contextId }
                .mapValues { (_, lookups) -> lookups.first().normalizedTag }
        }

        private companion object {
            val TAG_REGEX = Regex("#(\\p{L}[\\p{L}0-9_-]*)")
        }
        private fun stableGoalAssociationProjectionId(
            goalId: String,
            contextId: String,
        ): String = "goal_association:$goalId:$contextId"

    }

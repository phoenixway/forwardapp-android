package com.romankozak.forwardappmobile.data.logic

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTagRef
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecordLink
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextTagRefDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagAssociationHandler
    @Inject
    constructor(
        private val contextTagRefDao: ContextTagRefDao,
        private val contextDao: ContextDao,
        private val listItemDao: ListItemDao,
        private val inboxRecordLinkDao: InboxRecordLinkDao,
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
            val ownerContextId =
                sourceContextId?.takeIf { it.isNotBlank() }
                    ?: listItemDao.getDirectContextIdsForEntity(goal.id, BacklogItemTypeValues.GOAL).firstOrNull()
                    ?: return emptyMap()
            val directContextIds = listItemDao.getDirectContextIdsForEntity(goal.id, BacklogItemTypeValues.GOAL).toSet()
            val desiredContexts = resolveDesiredContexts(goal.text).filterKeys { it !in directContextIds }
            val existingAutoItems =
                listItemDao
                    .getAssociatedItemsForOwner(goal.id, BacklogItemTypeValues.GOAL, ownerContextId)
                    .associateBy { it.contextId }

            val staleContextIds = existingAutoItems.keys - desiredContexts.keys
            if (staleContextIds.isNotEmpty()) {
                listItemDao.deleteItemsByIds(staleContextIds.mapNotNull { existingAutoItems[it]?.id })
            }

            val now = System.currentTimeMillis()
            val itemsToUpsert =
                desiredContexts.mapNotNull { (contextId, matchedTag) ->
                    if (contextId in existingAutoItems.keys) {
                        val existing = existingAutoItems.getValue(contextId)
                        if (existing.associationTag == matchedTag) {
                            null
                        } else {
                            existing.copy(
                                associationTag = matchedTag,
                                updatedAt = now,
                                syncedAt = null,
                                version = existing.version + 1,
                            )
                        }
                    } else {
                        BacklogItem(
                            id = UUID.randomUUID().toString(),
                            contextId = contextId,
                            itemType = BacklogItemTypeValues.GOAL,
                            entityId = goal.id,
                            associationOwnerContextId = ownerContextId,
                            associationTag = matchedTag,
                            order = -(goal.updatedAt ?: goal.createdAt),
                            updatedAt = now,
                            syncedAt = null,
                            version = 1,
                        )
                    }
                }
            if (itemsToUpsert.isNotEmpty()) {
                listItemDao.insertItems(itemsToUpsert)
            }
            return desiredContexts
        }

        @Transaction
        suspend fun syncInboxRecordAssociations(record: InboxRecord): Map<String, String> {
            val desiredContexts = resolveDesiredContexts(record.text).filterKeys { it != record.contextId }
            val existingLinks = inboxRecordLinkDao.getLinksForRecord(record.id).associateBy { it.contextId }

            val staleContextIds = existingLinks.keys - desiredContexts.keys
            if (staleContextIds.isNotEmpty()) {
                inboxRecordLinkDao.deleteForRecordAndContexts(record.id, staleContextIds.toList())
            }

            val linksToUpsert =
                desiredContexts.mapNotNull { (contextId, matchedTag) ->
                    val existing = existingLinks[contextId]
                    if (existing != null && existing.associationTag == matchedTag) {
                        null
                    } else {
                        InboxRecordLink(
                            recordId = record.id,
                            contextId = contextId,
                            ownerContextId = record.contextId,
                            associationTag = matchedTag,
                            linkedAt = existing?.linkedAt ?: System.currentTimeMillis(),
                        )
                    }
                }
            if (linksToUpsert.isNotEmpty()) {
                inboxRecordLinkDao.insertAll(linksToUpsert)
            }
            return desiredContexts
        }

        @Transaction
        suspend fun reconcileChangedTags(changedTags: List<String>) {
            if (changedTags.isEmpty()) return
            val changedSet = changedTags.map { it.lowercase() }.toSet()

            goalDao.getAllVisible().asSequence()
                .filter { goal -> extractHashtags(goal.text).any { it in changedSet } }
                .forEach { goal -> syncGoalAssociations(goal, sourceContextId = null) }

            inboxRecordDao.getAll().asSequence()
                .filter { record -> !record.isDeleted && extractHashtags(record.text).any { it in changedSet } }
                .forEach { record -> syncInboxRecordAssociations(record) }
        }

        @Transaction
        suspend fun repairAllAssociations() {
            contextDao.getAll()
                .filterNot { it.isDeleted }
                .forEach { context -> syncContextTags(context, previousTags = null) }

            goalDao.getAllVisible().forEach { goal ->
                syncGoalAssociations(goal, sourceContextId = null)
            }

            inboxRecordDao.getAll()
                .filterNot { it.isDeleted }
                .forEach { record ->
                    syncInboxRecordAssociations(record)
                }
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
    }

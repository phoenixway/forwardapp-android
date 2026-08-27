package com.romankozak.forwardappmobile.data.logic

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecordLink
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import com.romankozak.forwardappmobile.shared.core.domain.inbox.firstMatchingInboxAssociationTag
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local materialized cache for Inbox hashtag routing.
 *
 * Canonical authority is InboxRecord + Context.tags. These links are never
 * synchronization or backup authority and may be deleted and rebuilt at any time.
 */
@Singleton
class InboxAssociationCache
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val inboxRecordDao: InboxRecordDao,
        private val inboxRecordLinkDao: InboxRecordLinkDao,
    ) {
        @Transaction
        suspend fun refresh(record: InboxRecord): Map<String, String> {
            if (record.isDeleted) {
                inboxRecordLinkDao.deleteForRecord(record.id)
                return emptyMap()
            }

            val desired =
                contextDao.getAll()
                    .asSequence()
                    .filter { context -> !context.isDeleted && context.id != record.contextId }
                    .mapNotNull { context ->
                        firstMatchingInboxAssociationTag(record.text, context.tags.orEmpty())
                            ?.let { tag -> context.id to tag }
                    }
                    .toMap()

            val existing = inboxRecordLinkDao.getLinksForRecord(record.id).associateBy { it.contextId }
            val staleContextIds = existing.keys - desired.keys
            if (staleContextIds.isNotEmpty()) {
                inboxRecordLinkDao.deleteForRecordAndContexts(record.id, staleContextIds.toList())
            }

            val linksToUpsert =
                desired.mapNotNull { (contextId, matchedTag) ->
                    val current = existing[contextId]
                    if (
                        current != null &&
                        current.ownerContextId == record.contextId &&
                        current.associationTag == matchedTag
                    ) {
                        null
                    } else {
                        InboxRecordLink(
                            recordId = record.id,
                            contextId = contextId,
                            ownerContextId = record.contextId,
                            associationTag = matchedTag,
                            linkedAt = current?.linkedAt ?: System.currentTimeMillis(),
                        )
                    }
                }

            if (linksToUpsert.isNotEmpty()) {
                inboxRecordLinkDao.insertAll(linksToUpsert)
            }

            return desired
        }

        @Transaction
        suspend fun remove(recordId: String) {
            inboxRecordLinkDao.deleteForRecord(recordId)
        }

        @Transaction
        suspend fun rebuild() {
            inboxRecordLinkDao.deleteAll()
            inboxRecordDao.getAll()
                .filterNot { it.isDeleted }
                .forEach { record -> refresh(record) }
        }
    }

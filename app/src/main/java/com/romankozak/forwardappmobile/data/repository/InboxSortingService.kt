package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxSortingService
    @Inject
    constructor(
        private val listItemRepository: ListItemRepository,
        private val inboxRepository: InboxRepository,
        private val contextRepository: ContextRepository,
    ) {
        enum class SortTarget {
            BACKLOG,
            INBOX_RECORDS,
            ATTACHMENTS,
        }

        suspend fun applySorting(
            contextId: String,
            rulesText: String,
            target: SortTarget,
        ): Int =
            when (target) {
                SortTarget.BACKLOG -> sortBacklog(contextId, rulesText)
                SortTarget.INBOX_RECORDS -> sortInboxRecords(contextId, rulesText)
                SortTarget.ATTACHMENTS -> sortAttachments(contextId, rulesText)
            }

        private suspend fun sortBacklog(
            contextId: String,
            rulesText: String,
        ): Int {
            val current = listItemRepository.getBacklogItemsForContext(contextId)
            if (current.isEmpty()) return 0

            val sorted =
                when (resolveMode(rulesText, "backlog", default = "newest")) {
                    "oldest" -> current.sortedByDescending { it.order }
                    else -> current.sortedBy { it.order } // newest first for negative-timestamp order model
                }

            val reordered =
                sorted.mapIndexed { index, item ->
                    val newOrder = (index + 1).toLong()
                    if (item.order == newOrder) item else item.copy(order = newOrder)
                }
            listItemRepository.updateListItemsOrder(reordered)
            return reordered.size
        }

        private suspend fun sortInboxRecords(
            contextId: String,
            rulesText: String,
        ): Int {
            val current = inboxRepository.getInboxRecordsForContext(contextId)
            if (current.isEmpty()) return 0

            val sorted =
                when (resolveMode(rulesText, "inbox", default = "newest")) {
                    "oldest" -> current.sortedBy { it.createdAt }
                    "alpha" -> current.sortedBy { it.text.lowercase() }
                    else -> current.sortedByDescending { it.createdAt }
                }

            inboxRepository.updateInboxRecordsOrder(
                contextId = contextId,
                orders = sorted.mapIndexed { index, record -> record.id to (index + 1).toLong() }.toMap(),
            )
            return sorted.size
        }

        private suspend fun sortAttachments(
            contextId: String,
            rulesText: String,
        ): Int {
            val current = contextRepository.getAttachmentsForContextStream(contextId).first()
            if (current.isEmpty()) return 0

            val sorted =
                when (resolveMode(rulesText, "attachments", default = "newest")) {
                    "oldest" -> current.sortedBy { it.attachment.createdAt }
                    "type" -> current.sortedBy { it.attachment.attachmentType.lowercase() }
                    "alpha" -> current.sortedBy { attachmentSortLabel(it).lowercase() }
                    else -> current.sortedByDescending { it.attachment.createdAt }
                }

            contextRepository.updateAttachmentOrders(
                contextId = contextId,
                updates = sorted.mapIndexed { index, item -> item.attachment.id to (index + 1).toLong() },
            )
            return sorted.size
        }

        private fun attachmentSortLabel(item: AttachmentWithContext): String =
            item.attachment.roleCode?.takeIf { it.isNotBlank() }
                ?: item.attachment.attachmentType

        private fun resolveMode(
            rulesText: String,
            key: String,
            default: String,
        ): String {
            if (rulesText.isBlank()) return default
            val pattern = Regex("""(?im)^\s*${Regex.escape(key)}\s*:\s*([a-z_]+)\s*$""")
            return pattern.find(rulesText)?.groupValues?.getOrNull(1)?.trim()?.lowercase().orEmpty().ifBlank { default }
        }
    }

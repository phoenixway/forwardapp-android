package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentWithContext
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalInboxSortingRepository
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingMode
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingTarget
import com.romankozak.forwardappmobile.shared.core.domain.workspace.effectiveSortingMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxSortingService
    @Inject
    constructor(
        private val listItemRepository: ListItemRepository,
        private val backlogPlacementCommands: BacklogPlacementCommands,
        private val inboxRepository: InboxRepository,
        private val contextRepository: ContextRepository,
        private val canonicalConnectionsRepository: CanonicalConnectionsRepository,
        private val canonicalBacklogRepository: CanonicalBacklogRepository,
        private val canonicalInboxRepository: CanonicalInboxRepository,
        private val canonicalSortingRepository: CanonicalInboxSortingRepository,
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
        ): Int {
            canonicalSortingRepository.requireActive(contextId)
            val configuration = InboxSortingLegacyTextAdapter.decode(rulesText)
            return when (target) {
                SortTarget.BACKLOG -> {
                    canonicalBacklogRepository.requireActive(contextId)
                    sortBacklog(
                        contextId,
                        effectiveSortingMode(configuration, WorkspaceSortingTarget.BACKLOG),
                    )
                }
                SortTarget.INBOX_RECORDS -> {
                    canonicalInboxRepository.requireActive(contextId)
                    sortInboxRecords(
                        contextId,
                        effectiveSortingMode(configuration, WorkspaceSortingTarget.INBOX),
                    )
                }
                SortTarget.ATTACHMENTS -> {
                    canonicalConnectionsRepository.requireActive(contextId)
                    sortAttachments(
                        contextId,
                        effectiveSortingMode(configuration, WorkspaceSortingTarget.CONNECTIONS),
                    )
                }
            }
        }

        private suspend fun sortBacklog(
            contextId: String,
            mode: WorkspaceSortingMode,
        ): Int {
            val current = listItemRepository.getBacklogItemsForContext(contextId)
            if (current.isEmpty()) return 0

            val sorted =
                when (mode) {
                    WorkspaceSortingMode.OLDEST -> current.sortedByDescending { it.order }
                    else -> current.sortedBy { it.order } // newest first for negative-timestamp order model
                }

            backlogPlacementCommands.reorderContextBacked(sorted)
            return sorted.size
        }

        private suspend fun sortInboxRecords(
            contextId: String,
            mode: WorkspaceSortingMode,
        ): Int {
            val current = inboxRepository.getInboxRecordsForContext(contextId)
            if (current.isEmpty()) return 0

            val sorted =
                when (mode) {
                    WorkspaceSortingMode.OLDEST -> current.sortedBy { it.createdAt }
                    WorkspaceSortingMode.ALPHA -> current.sortedBy { it.text.lowercase() }
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
            mode: WorkspaceSortingMode,
        ): Int {
            val current = contextRepository.getAttachmentsForContextStream(contextId).first()
            if (current.isEmpty()) return 0

            val sorted =
                when (mode) {
                    WorkspaceSortingMode.OLDEST -> current.sortedBy { it.attachment.createdAt }
                    WorkspaceSortingMode.TYPE -> current.sortedBy { it.attachment.attachmentType.lowercase() }
                    WorkspaceSortingMode.ALPHA -> current.sortedBy { attachmentSortLabel(it).lowercase() }
                    else -> current.sortedByDescending { it.attachment.createdAt }
                }

            canonicalConnectionsRepository.reorder(
                workspaceId = contextId,
                orderedAttachmentIds = sorted.map { it.attachment.id },
            )
            return sorted.size
        }

        private fun attachmentSortLabel(item: AttachmentWithContext): String =
            item.attachment.roleCode?.takeIf { it.isNotBlank() }
                ?: item.attachment.attachmentType
    }

package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import javax.inject.Inject
import javax.inject.Singleton

data class BacklogPresentationMutationResult(
    val explicitPlacements: Int,
    val connections: Int,
    val ignored: Int,
)

/**
 * Compatibility lifecycle for ids displayed in the Context Backlog surface.
 *
 * A displayed id may identify a canonical BACKLOG placement, a CONNECTIONS
 * Attachment presentation, or a rebuildable projection. Removing a
 * presentation never deletes target content and undo restores it through the
 * same owning capability.
 */
@Singleton
class BacklogPresentationLifecycle
    @Inject
    constructor(
        private val canonicalBacklogRepository: CanonicalBacklogRepository,
        private val backlogPlacementCommands: BacklogPlacementCommands,
        private val attachmentRepository: AttachmentsRepository,
    ) {
        suspend fun remove(
            contextId: String,
            displayedIds: Collection<String>,
        ): BacklogPresentationMutationResult {
            val ids = displayedIds.map(String::trim).filter(String::isNotEmpty).distinct()
            if (ids.isEmpty()) return BacklogPresentationMutationResult(0, 0, 0)

            val canonical =
                canonicalBacklogRepository.getEntriesByIds(ids)
                    .filter { it.workspaceId == contextId }
            val canonicalIds = canonical.mapTo(hashSetOf()) { it.id }
            val liveCanonicalIds = canonical.filterNot { it.isDeleted }.map { it.id }
            if (liveCanonicalIds.isNotEmpty()) {
                backlogPlacementCommands.tombstoneContextBacked(liveCanonicalIds)
            }

            var connections = 0
            ids.filterNot { it in canonicalIds }.forEach { id ->
                val attachment = attachmentRepository.getAttachmentById(id) ?: return@forEach
                attachmentRepository.unlinkAttachmentFromContext(attachment.id, contextId)
                connections += 1
            }

            return BacklogPresentationMutationResult(
                explicitPlacements = liveCanonicalIds.size,
                connections = connections,
                ignored = ids.size - canonicalIds.size - connections,
            )
        }

        suspend fun restore(items: List<BacklogItem>): BacklogPresentationMutationResult {
            if (items.isEmpty()) return BacklogPresentationMutationResult(0, 0, 0)

            val distinctItems = items.distinctBy { it.id }
            val canonicalById =
                canonicalBacklogRepository.getEntriesByIds(distinctItems.map { it.id })
                    .associateBy { it.id }
            val explicitItems =
                distinctItems.filter { item ->
                    canonicalById[item.id]?.workspaceId == item.contextId
                }
            if (explicitItems.isNotEmpty()) {
                backlogPlacementCommands.restoreContextBacked(explicitItems)
            }

            var connections = 0
            distinctItems.filterNot { it.id in canonicalById }.forEach { item ->
                val attachment = attachmentRepository.getAttachmentById(item.id) ?: return@forEach
                attachmentRepository.linkAttachmentToContext(attachment.id, item.contextId)
                connections += 1
            }

            return BacklogPresentationMutationResult(
                explicitPlacements = explicitItems.size,
                connections = connections,
                ignored = distinctItems.size - explicitItems.size - connections,
            )
        }
    }

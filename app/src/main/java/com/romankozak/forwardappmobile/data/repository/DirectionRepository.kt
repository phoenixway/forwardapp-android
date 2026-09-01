package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionItem
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionItemKind
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalDirectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility boundary for the legacy Context/UI Direction API.
 *
 * Schema 156 persistence authority is canonical WorkspaceDirectionEntry +
 * DIRECTION Orientation state. DirectionItemEntity is only a compatibility DTO.
 */
@Singleton
class DirectionRepository
    @Inject
    constructor(
        private val canonicalDirectionRepository: CanonicalDirectionRepository,
    ) {
        fun getDirectionItemsForContext(contextId: String): Flow<List<DirectionItemEntity>> =
            canonicalDirectionRepository
                .observeItems(contextId)
                .map { items -> items.map { it.toCompatibilityEntity() } }

        suspend fun addDirectionItem(
            contextId: String,
            text: String,
            linkedContextId: String? = null,
        ) {
            val target = linkedContextId.normalizedId()
            if (target == null) {
                canonicalDirectionRepository.createSemanticDirection(
                    workspaceId = contextId,
                    title = text,
                )
            } else {
                canonicalDirectionRepository.createWorkspaceLink(
                    workspaceId = contextId,
                    targetWorkspaceId = target,
                    label = text,
                )
            }
        }

        suspend fun addDirectionItems(
            contextId: String,
            items: List<Pair<String, String?>>,
        ): Int {
            items.forEach { (text, linkedContextId) ->
                addDirectionItem(
                    contextId = contextId,
                    text = text,
                    linkedContextId = linkedContextId,
                )
            }
            return items.size
        }

        suspend fun updateDirectionItem(item: DirectionItemEntity) {
            val current =
                canonicalDirectionRepository
                    .getItemsByIds(listOf(item.id))
                    .singleOrNull()
                    ?: return

            val now = System.currentTimeMillis()
            val desiredText = item.text.trim()
            val desiredTarget = item.linkedContextId.normalizedId()

            when (current.kind) {
                CanonicalDirectionItemKind.SEMANTIC -> {
                    if (desiredTarget == null) {
                        if (desiredText != current.text) {
                            canonicalDirectionRepository.rename(item.id, desiredText, now)
                        }
                    } else {
                        canonicalDirectionRepository.convertSemanticToWorkspaceLink(
                            entryId = item.id,
                            targetWorkspaceId = desiredTarget,
                            now = now,
                        )
                        if (desiredText != current.text) {
                            canonicalDirectionRepository.rename(item.id, desiredText, now)
                        }
                    }
                }

                CanonicalDirectionItemKind.WORKSPACE_LINK -> {
                    if (desiredTarget == null) {
                        canonicalDirectionRepository.convertWorkspaceLinkToSemantic(
                            entryId = item.id,
                            now = now,
                        )
                        if (desiredText != current.text) {
                            canonicalDirectionRepository.rename(item.id, desiredText, now)
                        }
                    } else {
                        if (desiredTarget != current.targetWorkspaceId) {
                            canonicalDirectionRepository.retargetWorkspaceLink(
                                entryId = item.id,
                                targetWorkspaceId = desiredTarget,
                                now = now,
                            )
                        }
                        if (desiredText != current.text) {
                            canonicalDirectionRepository.rename(item.id, desiredText, now)
                        }
                    }
                }
            }
        }

        /**
         * Legacy callers use updateAll only for list reordering.
         */
        suspend fun updateAll(items: List<DirectionItemEntity>) {
            if (items.isEmpty()) return

            val workspaceIds = items.map { it.contextId }.distinct()
            require(workspaceIds.size == 1) {
                "Direction reorder must stay within one Workspace"
            }

            val orderedIds =
                items
                    .sortedWith(compareBy<DirectionItemEntity> { it.itemOrder }.thenBy { it.id })
                    .map { it.id }

            canonicalDirectionRepository.reorder(
                workspaceId = workspaceIds.single(),
                orderedEntryIds = orderedIds,
            )
        }

        suspend fun deleteDirectionItem(itemId: String) {
            val existing =
                canonicalDirectionRepository
                    .getItemsByIds(listOf(itemId))
                    .singleOrNull()
                    ?: return
            canonicalDirectionRepository.tombstone(existing.id)
        }

        suspend fun deleteDirectionItems(itemIds: List<String>): Int {
            if (itemIds.isEmpty()) return 0

            val existing =
                canonicalDirectionRepository.getItemsByIds(itemIds.distinct())
            if (existing.isEmpty()) return 0

            canonicalDirectionRepository.tombstoneMany(existing.map { it.id })
            return existing.size
        }

        suspend fun getDirectionItemsForContextSync(
            contextId: String,
        ): List<DirectionItemEntity> =
            canonicalDirectionRepository
                .getItems(contextId)
                .map { it.toCompatibilityEntity() }

        suspend fun getDirectionItemsByIds(
            itemIds: List<String>,
        ): List<DirectionItemEntity> {
            if (itemIds.isEmpty()) return emptyList()
            return canonicalDirectionRepository
                .getItemsByIds(itemIds)
                .map { it.toCompatibilityEntity() }
        }

        /**
         * Preserves the old Context auto-link rule: one child Workspace link at
         * the front of the parent's Direction list.
         */
        suspend fun addDirectionLinkedAtFront(
            parentContextId: String,
            childContextId: String,
            childContextName: String,
        ): Boolean {
            val existing = canonicalDirectionRepository.getItems(parentContextId)
            if (
                existing.any {
                    it.kind == CanonicalDirectionItemKind.WORKSPACE_LINK &&
                        it.targetWorkspaceId == childContextId
                }
            ) {
                return false
            }

            canonicalDirectionRepository.createWorkspaceLinkAtFront(
                workspaceId = parentContextId,
                targetWorkspaceId = childContextId,
                label = childContextName,
            )
            return true
        }

        suspend fun deleteWorkspaceLinksTargeting(
            contextIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int =
            canonicalDirectionRepository.tombstoneWorkspaceLinksTargeting(
                targetWorkspaceIds = contextIds,
                now = now,
            )

        suspend fun deleteDirectionsOwnedByWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int =
            canonicalDirectionRepository.tombstoneOwnedEntriesForWorkspaces(
                workspaceIds = workspaceIds,
                now = now,
            )
    }

private fun String?.normalizedId(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }

private fun CanonicalDirectionItem.toCompatibilityEntity(): DirectionItemEntity {
    require(order in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
        "Direction order $order does not fit legacy UI compatibility DTO"
    }

    return DirectionItemEntity(
        id = id,
        contextId = workspaceId,
        text = text,
        linkedContextId = targetWorkspaceId,
        itemOrder = order.toInt(),
        updatedAt = null,
        syncedAt = null,
        isDeleted = false,
        version = version,
    )
}

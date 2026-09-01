package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCanonicalTargetResolver
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.core.context.isDirectHierarchyChildContext
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Explicit BACKLOG placement mutation boundary.
 *
 * Schema 162 makes canonical workspace_backlog_entries authoritative for
 * Context-backed and canonical-only Workspaces. BacklogItem remains a
 * compatibility DTO only. There is deliberately no double-write path.
 */
@Singleton
class BacklogPlacementCommands
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val canonicalRepository: CanonicalBacklogRepository,
        private val canonicalTargetResolver: BacklogCanonicalTargetResolver,
    ) {
        suspend fun addToContextBacked(
            contextId: String,
            itemType: String,
            entityId: String,
        ): String =
            canonicalRepository.addEntryAtStart(
                workspaceId = contextId,
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
            )

        suspend fun addManyToContextBacked(
            contextId: String,
            entries: List<Pair<String, String>>,
        ): List<String> {
            if (entries.isEmpty()) return emptyList()
            return entries
                .asReversed()
                .map { (itemType, entityId) ->
                    canonicalRepository.addEntryAtStart(
                        workspaceId = contextId,
                        target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
                    )
                }
                .asReversed()
        }

        suspend fun addContextLinkToContextBacked(
            targetContextId: String,
            currentContextId: String,
        ): String? {
            if (
                contextDao.getContextById(targetContextId)?.let { target ->
                    isDirectHierarchyChildContext(currentContextId, target.parentId)
                } == true
            ) {
                return null
            }
            return canonicalRepository.addEntryAtStart(
                workspaceId = currentContextId,
                target =
                    canonicalTargetResolver.resolveLegacy(
                        itemType = "SUBLIST",
                        entityId = targetContextId,
                    ),
            )
        }

        suspend fun moveContextBacked(
            itemIds: List<String>,
            targetContextId: String,
        ) {
            canonicalRepository.moveEntries(itemIds, targetContextId)
        }

        suspend fun tombstoneContextBacked(itemIds: List<String>) {
            canonicalRepository.tombstoneMany(itemIds)
        }

        /**
         * Projection rows may be present in the Context screen list. They are
         * excluded and explicit placement order is re-densified before write.
         */
        suspend fun reorderContextBacked(items: List<BacklogItem>) {
            val explicit =
                items
                    .filter { it.associationOwnerContextId == null }
                    .mapIndexed { index, item ->
                        item.copy(order = index.toLong())
                    }
            canonicalRepository.reorder(
                workspaceId = explicit.firstOrNull()?.contextId ?: return,
                orderedEntryIds = explicit.map { it.id },
            )
        }

        suspend fun findFirstContextBackedWorkspaceId(
            itemType: String,
            entityId: String,
        ): String? =
            canonicalRepository.findFirstLiveWorkspaceId(
                canonicalTargetResolver.resolveLegacy(itemType, entityId),
            )

        suspend fun findLiveWorkspaceIds(
            itemType: String,
            entityId: String,
        ): List<String> =
            canonicalRepository
                .findLivePlacements(
                    canonicalTargetResolver.resolveLegacy(itemType, entityId),
                )
                .map { it.workspaceId }
                .distinct()
                .sorted()

        suspend fun restoreContextBacked(items: List<BacklogItem>) {
            val explicit =
                items.filter { item ->
                    item.associationOwnerContextId == null &&
                        item.contextId.isNotBlank()
                }
            if (explicit.isEmpty()) return

            explicit
                .groupBy { it.contextId }
                .forEach { (workspaceId, owned) ->
                    val restoredOrderById = linkedMapOf<String, Long>()

                    owned
                        .sortedWith(compareBy<BacklogItem> { it.order }.thenBy { it.id })
                        .forEach { item ->
                            val restoredId =
                                canonicalRepository.setPlacementVisible(
                                    workspaceId = workspaceId,
                                    target =
                                        canonicalTargetResolver.resolveLegacy(
                                            itemType = item.itemType,
                                            entityId = item.entityId,
                                        ),
                                    visible = true,
                                )
                            if (restoredId != null) {
                                restoredOrderById[restoredId] = item.order
                            }
                        }

                    if (restoredOrderById.isNotEmpty()) {
                        val current = canonicalRepository.getEntries(workspaceId)
                        val orderedIds =
                            current
                                .sortedWith(
                                    compareBy<com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity> {
                                        restoredOrderById[it.id] ?: it.entryOrder
                                    }.thenBy { it.id },
                                )
                                .map { it.id }

                        canonicalRepository.reorder(
                            workspaceId = workspaceId,
                            orderedEntryIds = orderedIds,
                        )
                    }
                }
        }

        suspend fun setContextBackedTargetVisible(
            contextId: String,
            itemType: String,
            entityId: String,
            visible: Boolean,
        ): String? =
            canonicalRepository.setPlacementVisible(
                workspaceId = contextId,
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
                visible = visible,
            )

        suspend fun hasContextBackedPlacementHistory(
            contextId: String,
            itemType: String,
            entityId: String,
        ): Boolean =
            canonicalRepository.findPlacement(
                workspaceId = contextId,
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
            ) != null

        suspend fun hasLiveContextBackedPlacement(
            contextId: String,
            itemType: String,
            entityId: String,
        ): Boolean =
            canonicalRepository.findPlacement(
                workspaceId = contextId,
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
            )?.isDeleted == false

        suspend fun setContextBackedPlacementVisible(
            contextId: String,
            itemType: String,
            entityId: String,
            visible: Boolean,
            now: Long = System.currentTimeMillis(),
        ): String? =
            canonicalRepository.setPlacementVisible(
                workspaceId = contextId,
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
                visible = visible,
                now = now,
            )

        suspend fun tombstoneContextBackedTarget(
            itemType: String,
            entityId: String,
            now: Long = System.currentTimeMillis(),
        ): Int =
            canonicalRepository.tombstoneEntriesTargeting(
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
                now = now,
            )

        suspend fun addLegacyTargetToCanonicalWorkspace(
            workspaceId: String,
            itemType: String,
            entityId: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            canonicalRepository.addEntry(
                workspaceId = workspaceId,
                target = canonicalTargetResolver.resolveLegacy(itemType, entityId),
                now = now,
            )

        suspend fun addCanonicalTarget(
            workspaceId: String,
            target: WorkspaceBacklogTargetRef,
            now: Long = System.currentTimeMillis(),
        ): String =
            canonicalRepository.addEntry(
                workspaceId = workspaceId,
                target = target,
                now = now,
            )

        suspend fun reorderCanonicalWorkspace(
            workspaceId: String,
            orderedEntryIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            canonicalRepository.reorder(workspaceId, orderedEntryIds, now)
        }

        suspend fun moveCanonical(
            entryIds: List<String>,
            targetWorkspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): Int =
            canonicalRepository.moveEntries(
                ids = entryIds,
                targetWorkspaceId = targetWorkspaceId,
                now = now,
            )

        suspend fun tombstoneCanonical(
            entryIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int = canonicalRepository.tombstoneMany(entryIds, now)
    }

package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceBacklogEntryDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfiguration
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogCapabilityConfigurationV2
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class BacklogCapabilityState(
    val lifecycleState: com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState,
    val isDeleted: Boolean,
    val configuration: BacklogCapabilityConfiguration,
)

/** Canonical BACKLOG lifecycle, typed configuration, and placement owner. */
@Singleton
class CanonicalBacklogRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val instanceStore: CanonicalCapabilityInstanceStore,
        private val entryDao: WorkspaceBacklogEntryDao,
        private val targetValidator: CanonicalBacklogTargetValidator,
    ) {
        suspend fun enable(workspaceId: String, now: Long = System.currentTimeMillis()): String =
            instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.disable(SPEC, workspaceId, now)

        suspend fun setEnabled(
            workspaceId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.setEnabled(SPEC, workspaceId, enabled, now)

        suspend fun establishDisabledIfMissing(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): Boolean = instanceStore.establishDisabledIfMissing(SPEC, workspaceId, now)

        suspend fun archive(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.restore(SPEC, workspaceId, now)

        /** Capability metadata deletion preserves placements and target content. */
        suspend fun deleteCapability(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.delete(SPEC, workspaceId, now)

        suspend fun requireActive(workspaceId: String) {
            instanceStore.requireActiveInstance(SPEC, workspaceId)
        }

        suspend fun hasEstablishedInstance(workspaceId: String): Boolean =
            instanceStore.hasEstablishedInstance(SPEC, workspaceId)

        suspend fun getState(workspaceId: String): BacklogCapabilityState? =
            instanceStore.findInstance(SPEC, workspaceId)?.toBacklogCapabilityState()

        internal fun getState(
            workspaceId: String,
            snapshot: CanonicalCapabilityReadSnapshot,
        ): BacklogCapabilityState? =
            instanceStore.findInstance(SPEC, workspaceId, snapshot)?.toBacklogCapabilityState()

        fun observeState(workspaceId: String): Flow<BacklogCapabilityState?> =
            instanceStore.observeInstance(SPEC, workspaceId).map { instance ->
                instance?.let { runCatching { it.toBacklogCapabilityState() }.getOrNull() }
            }

        fun observeEstablishedInstance(workspaceId: String): Flow<Boolean> =
            instanceStore.observeEstablishedInstance(SPEC, workspaceId)

        suspend fun updateConfiguration(
            workspaceId: String,
            configuration: BacklogCapabilityConfigurationV2,
            now: Long = System.currentTimeMillis(),
        ) {
            instanceStore.updateConfiguration(
                spec = SPEC,
                workspaceId = workspaceId,
                configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                configuration = BacklogCapabilityConfigurationCodec.encode(configuration),
                now = now,
            )
        }

        suspend fun migrateV1Configuration(
            workspaceId: String,
            configuration: BacklogCapabilityConfigurationV2,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            instanceStore.migrateConfigurationVersionPreservingLifecycle(
                spec = SPEC,
                workspaceId = workspaceId,
                fromVersion = 1,
                configurationVersion = BacklogCapabilityConfigurationCodec.CURRENT_VERSION,
                configuration = BacklogCapabilityConfigurationCodec.encode(configuration),
                now = now,
            )

        fun observeEntries(workspaceId: String): Flow<List<WorkspaceBacklogEntryEntity>> =
            entryDao.observeLive(workspaceId)

        suspend fun getEntries(workspaceId: String): List<WorkspaceBacklogEntryEntity> =
            entryDao.getLive(workspaceId)

        suspend fun getEntry(id: String): WorkspaceBacklogEntryEntity? = entryDao.getById(id)

        suspend fun getEntriesByIds(ids: Collection<String>): List<WorkspaceBacklogEntryEntity> =
            if (ids.isEmpty()) emptyList() else entryDao.getByIds(ids)

        suspend fun addEntry(
            workspaceId: String,
            target: WorkspaceBacklogTargetRef,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalizedTarget = target.copy(id = target.id.trim())
                targetValidator.requireLive(normalizedTarget)
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val existing =
                    entryDao.getLogicalPlacement(
                        capabilityInstanceId = capability.id,
                        targetKind = normalizedTarget.kind.name,
                        targetId = normalizedTarget.id,
                    )
                if (existing != null && !existing.isDeleted) return@withTransaction existing.id

                val next =
                    existing?.bump(now)?.copy(
                        entryOrder = nextOrder(workspaceId),
                        isDeleted = false,
                    ) ?: WorkspaceBacklogEntryEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        targetKind = normalizedTarget.kind.name,
                        targetId = normalizedTarget.id,
                        entryOrder = nextOrder(workspaceId),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                entryDao.upsert(listOf(next))
                next.id
            }

        /**
         * Context-backed Backlog historically prepends newly-created entries.
         *
         * We preserve that UX without rewriting every existing entry: canonical
         * order values need only be sortable, not permanently dense. Explicit
         * reorder and migration accounting still normalize them when required.
         */
        suspend fun addEntryAtStart(
            workspaceId: String,
            target: WorkspaceBacklogTargetRef,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalizedTarget = target.copy(id = target.id.trim())
                targetValidator.requireLive(normalizedTarget)
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val existing =
                    entryDao.getLogicalPlacement(
                        capabilityInstanceId = capability.id,
                        targetKind = normalizedTarget.kind.name,
                        targetId = normalizedTarget.id,
                    )
                if (existing != null && !existing.isDeleted) return@withTransaction existing.id

                val previousOrder =
                    (entryDao.getLive(workspaceId).minOfOrNull { it.entryOrder } ?: 0L) - 1L
                val changed =
                    existing?.bump(now)?.copy(
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        entryOrder = previousOrder,
                        isDeleted = false,
                    ) ?: WorkspaceBacklogEntryEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        targetKind = normalizedTarget.kind.name,
                        targetId = normalizedTarget.id,
                        entryOrder = previousOrder,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                entryDao.upsert(listOf(changed))
                changed.id
            }

        suspend fun findLivePlacements(
            target: WorkspaceBacklogTargetRef,
        ): List<WorkspaceBacklogEntryEntity> =
            entryDao.getLiveByTarget(
                targetKind = target.kind.name,
                targetId = target.id.trim(),
            )

        suspend fun findFirstLiveWorkspaceId(
            target: WorkspaceBacklogTargetRef,
        ): String? =
            findLivePlacements(target)
                .firstOrNull()
                ?.workspaceId

        suspend fun findPlacement(
            workspaceId: String,
            target: WorkspaceBacklogTargetRef,
        ): WorkspaceBacklogEntryEntity? {
            val capability = instanceStore.findInstance(SPEC, workspaceId) ?: return null
            return entryDao.getLogicalPlacement(
                capabilityInstanceId = capability.id,
                targetKind = target.kind.name,
                targetId = target.id.trim(),
            )
        }

        /**
         * Domain lifecycle cleanup is not an authoring operation.
         *
         * Target deletion must be able to tombstone placements even if the
         * owning BACKLOG capability is currently disabled.
         */
        suspend fun tombstoneEntriesTargeting(
            target: WorkspaceBacklogTargetRef,
            now: Long = System.currentTimeMillis(),
        ): Int =
            database.withTransaction {
                val entries =
                    entryDao.getLiveByTarget(
                        targetKind = target.kind.name,
                        targetId = target.id.trim(),
                    )
                if (entries.isEmpty()) return@withTransaction 0

                entryDao.upsert(
                    entries.map { entry ->
                        entry.bump(now).copy(isDeleted = true)
                    },
                )
                entries
                    .map { it.workspaceId }
                    .distinct()
                    .forEach { compactOrder(it, now) }
                entries.size
            }

        /**
         * Canonical post-cutover repair for live placements whose typed target
         * no longer exists, plus historical direct-child rows that merely
         * duplicated Workspace hierarchy.
         *
         * This deliberately reads only canonical placement and target state.
         * Retained list_items must never influence runtime cleanup after the
         * schema-162 authority switch.
         */
        suspend fun tombstoneDanglingAndStructuralEntries(
            now: Long = System.currentTimeMillis(),
        ): Int =
            database.withTransaction {
                val retired = entryDao.getLiveDanglingAndStructuralEntries()
                if (retired.isEmpty()) return@withTransaction 0

                entryDao.upsert(retired.map { it.bump(now).copy(isDeleted = true) })
                retired
                    .map { it.workspaceId }
                    .distinct()
                    .forEach { compactOrder(it, now) }
                retired.size
            }

        /**
         * Visibility policy for one logical placement.
         *
         * Hiding tombstones the existing placement. Showing restores its stable
         * placement id when possible, or creates it at the front when absent.
         */
        suspend fun setPlacementVisible(
            workspaceId: String,
            target: WorkspaceBacklogTargetRef,
            visible: Boolean,
            now: Long = System.currentTimeMillis(),
        ): String? =
            database.withTransaction {
                val normalizedTarget = target.copy(id = target.id.trim())
                if (visible) {
                    targetValidator.requireLive(normalizedTarget)
                }

                val capability =
                    if (visible) {
                        instanceStore.requireActiveInstance(SPEC, workspaceId)
                    } else {
                        instanceStore.findInstance(SPEC, workspaceId)
                            ?: return@withTransaction null
                    }

                val existing =
                    entryDao.getLogicalPlacement(
                        capabilityInstanceId = capability.id,
                        targetKind = normalizedTarget.kind.name,
                        targetId = normalizedTarget.id,
                    )

                if (!visible) {
                    if (existing == null || existing.isDeleted) return@withTransaction existing?.id
                    entryDao.upsert(
                        listOf(
                            existing.bump(now).copy(isDeleted = true),
                        ),
                    )
                    compactOrder(workspaceId, now)
                    return@withTransaction existing.id
                }

                if (existing != null && !existing.isDeleted) {
                    return@withTransaction existing.id
                }

                val previousOrder =
                    (entryDao.getLive(workspaceId).minOfOrNull { it.entryOrder } ?: 0L) - 1L
                val changed =
                    existing?.bump(now)?.copy(
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        entryOrder = previousOrder,
                        isDeleted = false,
                    ) ?: WorkspaceBacklogEntryEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        targetKind = normalizedTarget.kind.name,
                        targetId = normalizedTarget.id,
                        entryOrder = previousOrder,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                entryDao.upsert(listOf(changed))
                changed.id
            }

        suspend fun reorder(
            workspaceId: String,
            orderedEntryIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                instanceStore.requireActiveInstance(SPEC, workspaceId)
                require(orderedEntryIds.size == orderedEntryIds.distinct().size) {
                    "Backlog reorder contains duplicate ids"
                }
                val current = entryDao.getLive(workspaceId)
                require(orderedEntryIds.toSet() == current.map { it.id }.toSet()) {
                    "Backlog reorder must contain every live entry exactly once"
                }
                val byId = current.associateBy { it.id }
                val changes =
                    orderedEntryIds.mapIndexedNotNull { index, id ->
                        val entry = byId.getValue(id)
                        val order = index.toLong()
                        entry.takeIf { it.entryOrder != order }?.bump(now)?.copy(entryOrder = order)
                    }
                if (changes.isNotEmpty()) entryDao.upsert(changes)
            }
        }

        suspend fun tombstoneEntry(
            id: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val current = requireMutableEntry(id)
                instanceStore.requireActiveInstance(SPEC, current.workspaceId)
                entryDao.upsert(listOf(current.bump(now).copy(isDeleted = true)))
                compactOrder(current.workspaceId, now)
            }
        }

        suspend fun tombstoneMany(
            ids: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int {
            val requested = ids.map(String::trim).filter(String::isNotEmpty).distinct()
            if (requested.isEmpty()) return 0
            return database.withTransaction {
                val entries = entryDao.getByIds(requested).filterNot { it.isDeleted }
                val workspaces = entries.map { it.workspaceId }.distinct()
                workspaces.forEach { instanceStore.requireActiveInstance(SPEC, it) }
                if (entries.isNotEmpty()) {
                    entryDao.upsert(entries.map { it.bump(now).copy(isDeleted = true) })
                    workspaces.forEach { compactOrder(it, now) }
                }
                entries.size
            }
        }

        suspend fun moveEntries(
            ids: List<String>,
            targetWorkspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): Int {
            require(ids.size == ids.distinct().size) { "Backlog move contains duplicate ids" }
            if (ids.isEmpty()) return 0
            return database.withTransaction {
                val targetCapability = instanceStore.requireActiveInstance(SPEC, targetWorkspaceId)
                val entriesById = entryDao.getByIds(ids).associateBy { it.id }
                require(entriesById.keys == ids.toSet()) { "Backlog move contains unknown ids" }
                val entries = ids.map { requireMutableEntry(it, entriesById) }
                val sourceWorkspaces = entries.map { it.workspaceId }.distinct()
                sourceWorkspaces.forEach { instanceStore.requireActiveInstance(SPEC, it) }
                entries.forEach { entry ->
                    targetValidator.requireLive(entry.targetRef())
                }

                // Preserve the existing same-owner move semantics (append the
                // selected rows in request order, then compact) without
                // changing placement identity. Cross-owner moves use a new
                // destination placement identity instead.
                if (sourceWorkspaces.size == 1 && sourceWorkspaces.single() == targetWorkspaceId) {
                    var order = nextOrder(targetWorkspaceId)
                    entryDao.upsert(
                        entries.map { entry ->
                            entry.bump(now).copy(entryOrder = order++)
                        },
                    )
                    compactOrder(targetWorkspaceId, now)
                    return@withTransaction entries.size
                }

                val destinationByTarget = mutableMapOf<String, WorkspaceBacklogEntryEntity?>()
                entries.filter { it.workspaceId != targetWorkspaceId }.forEach { entry ->
                    val targetKey = "${entry.targetKind}\u0000${entry.targetId}"
                    require(!destinationByTarget.containsKey(targetKey)) {
                        "Target Backlog already contains ${entry.targetKind}:${entry.targetId}"
                    }
                    val collision =
                        entryDao.getLogicalPlacement(
                            targetCapability.id,
                            entry.targetKind,
                            entry.targetId,
                        )
                    require(collision == null || collision.isDeleted) {
                        "Target Backlog already contains ${entry.targetKind}:${entry.targetId}"
                    }
                    destinationByTarget[targetKey] = collision
                }

                // Tombstone each source placement first. The source ids never
                // change owner; only a destination placement can become live.
                entryDao.upsert(
                    entries.filter { it.workspaceId != targetWorkspaceId }
                        .map { it.bump(now).copy(isDeleted = true) },
                )
                sourceWorkspaces.filterNot { it == targetWorkspaceId }
                    .forEach { compactOrder(it, now) }

                var order = nextOrder(targetWorkspaceId)
                val destinationEntries = entries.map { entry ->
                    if (entry.workspaceId == targetWorkspaceId) {
                        entry.bump(now).copy(entryOrder = order++)
                    } else {
                        val collision = destinationByTarget["${entry.targetKind}\u0000${entry.targetId}"]
                        collision?.bump(now)?.copy(
                            entryOrder = order++,
                            isDeleted = false,
                        ) ?: WorkspaceBacklogEntryEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = targetWorkspaceId,
                            capabilityInstanceId = targetCapability.id,
                            targetKind = entry.targetKind,
                            targetId = entry.targetId,
                            entryOrder = order++,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        )
                    }
                }
                entryDao.upsert(destinationEntries)
                compactOrder(targetWorkspaceId, now)
                entries.size
            }
        }

        /** Owner deletion bypasses the active-capability authoring guard. */
        suspend fun tombstoneOwnedContentForWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int {
            val owners = workspaceIds.map(String::trim).filter(String::isNotEmpty).toSet()
            if (owners.isEmpty()) return 0
            return database.withTransaction {
                val entries = owners.flatMap { entryDao.getLive(it) }
                if (entries.isNotEmpty()) {
                    entryDao.upsert(entries.map { it.bump(now).copy(isDeleted = true) })
                }
                entries.size
            }
        }

        private suspend fun requireMutableEntry(id: String): WorkspaceBacklogEntryEntity {
            val entry = requireNotNull(entryDao.getById(id)) { "Backlog entry does not exist" }
            require(!entry.isDeleted) { "Backlog entry is deleted" }
            return entry
        }

        private fun requireMutableEntry(
            id: String,
            entriesById: Map<String, WorkspaceBacklogEntryEntity>,
        ): WorkspaceBacklogEntryEntity {
            val entry = requireNotNull(entriesById[id]) { "Backlog entry does not exist" }
            require(!entry.isDeleted) { "Backlog entry is deleted" }
            return entry
        }

        private suspend fun nextOrder(workspaceId: String): Long =
            (entryDao.getLive(workspaceId).maxOfOrNull { it.entryOrder } ?: -1L) + 1L

        private suspend fun compactOrder(workspaceId: String, now: Long) {
            val changes =
                entryDao.getLive(workspaceId).mapIndexedNotNull { index, entry ->
                    val order = index.toLong()
                    entry.takeIf { it.entryOrder != order }?.bump(now)?.copy(entryOrder = order)
                }
            if (changes.isNotEmpty()) entryDao.upsert(changes)
        }

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.BACKLOG,
                    configurationCodec = BacklogCapabilityConfigurationCodec,
                    workspaceAuthority =
                        CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }
    }

private fun WorkspaceBacklogEntryEntity.bump(now: Long) =
    copy(updatedAt = now, syncedAt = null, version = version + 1L)

private fun WorkspaceBacklogEntryEntity.targetRef() =
    WorkspaceBacklogTargetRef(
        kind = com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind.valueOf(targetKind),
        id = targetId,
    )

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
    .toBacklogCapabilityState() =
    BacklogCapabilityState(
        lifecycleState =
            com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState.valueOf(state),
        isDeleted = isDeleted,
        configuration = BacklogCapabilityConfigurationCodec.decode(configurationVersion, configuration),
    )

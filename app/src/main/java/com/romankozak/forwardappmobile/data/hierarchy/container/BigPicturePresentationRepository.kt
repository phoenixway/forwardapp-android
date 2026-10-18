package com.romankozak.forwardappmobile.data.hierarchy.container

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Presentation read/transport/ingress repository, NOT product structural authority.
 *
 * Internal historical structural mutators are retained for compatibility tests.
 * Production mixed mutations must use H1StructuralMutationCoordinator.
 */
class BigPicturePresentationRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.bigPicturePresentationDao()

    suspend fun snapshot() = db.withTransaction { BigPicturePresentationSnapshot(
        dao.containers().map { it.record }, dao.memberships().map { it.record },
    ) }

    /**
     * Whole-graph ingress validation. The structural caller must own the
     * enclosing Room write transaction so rejection rolls back every delta.
     *
     * This is not a production structural mutation coordinator.
     */
    suspend fun requireCurrentUnifiedStructuralGraphValid() {
        projectH1UnifiedStructuralGraph(
            db.hierarchyPlacementDao().getAll().map {
                it.toHierarchyPlacementStrict()
            },
            snapshot(),
        )
    }

    fun observe() = db.invalidationTracker.createFlow(
        "big_picture_containers", "big_picture_memberships", "hierarchy_placements",
    ).map { db.withTransaction { snapshot() } }.distinctUntilChanged().flowOn(Dispatchers.IO)

    internal suspend fun create(
        title: String, anchorPlacementId: String? = null, parentContainerId: String? = null,
        siblingOrder: Long = 0, now: Long, id: String = UUID.randomUUID().toString(),
    ): String = db.withTransaction {
        val before = snapshot()
        require(before.containers.none { it.id == id }) { "Container identity already exists" }
        require(anchorPlacementId == null || db.hierarchyPlacementDao().getById(anchorPlacementId)?.isDeleted == false) {
            "Container anchor unavailable"
        }
        val row = BigPictureContainerSnapshot(id, "GENERAL", title.trim(), anchorPlacementId, parentContainerId,
            siblingOrder, now, now, null, false, 1)
        validate(before.copy(containers = before.containers + row))
        dao.upsertContainers(listOf(BigPictureContainerEntity(row)))
        id
    }

    internal suspend fun update(
        id: String, title: String, parentContainerId: String?, siblingOrder: Long,
        expectedVersion: Long, now: Long,
    ) = db.withTransaction {
        val before = snapshot()
        val current = requireNotNull(before.containers.singleOrNull { it.id == id && !it.isDeleted })
        require(current.version == expectedVersion) { "Stale container version" }
        val row = current.copy(title = title.trim(), parentContainerId = parentContainerId,
            siblingOrder = siblingOrder, version = current.version + 1, updatedAt = now, syncedAt = null)
        validate(before.copy(containers = before.containers.map { if (it.id == id) row else it }))
        dao.upsertContainers(listOf(BigPictureContainerEntity(row)))
    }

    /** Policy B: historical memberships remain but become inert. Live nested sections reject deletion. */
    internal suspend fun tombstone(id: String, expectedVersion: Long, now: Long) = db.withTransaction {
        val before = snapshot()
        val current = requireNotNull(before.containers.singleOrNull { it.id == id })
        require(current.version == expectedVersion) { "Stale container version" }
        if (!current.isDeleted) {
            val row = current.copy(isDeleted = true, version = current.version + 1, updatedAt = now, syncedAt = null)
            validate(before.copy(containers = before.containers.map { if (it.id == id) row else it }))
            dao.upsertContainers(listOf(BigPictureContainerEntity(row)))
        }
    }

    internal suspend fun assign(placementId: String, containerId: String, now: Long, expectedVersion: Long? = null) = db.withTransaction {
        val before = snapshot()
        require(before.containers.any { it.id == containerId && !it.isDeleted }) { "Container unavailable" }
        val placement = db.hierarchyPlacementDao().getById(placementId)
        require(placement != null && !placement.isDeleted) { "Occurrence unavailable" }
        val current = before.memberships.singleOrNull { it.placementId == placementId }
        val unchanged = current != null && !current.isDeleted && current.containerId == containerId
        require(if (current == null) expectedVersion == null else
            current.version == expectedVersion || (unchanged && expectedVersion == null)) { "Stale membership version" }
        if (current == null || current.containerId != containerId || current.isDeleted) {
            val row = BigPictureMembershipSnapshot(placementId, "GENERAL", containerId,
                current?.createdAt ?: now, now, null, false, (current?.version ?: 0) + 1)
            validate(before.copy(memberships = before.memberships.filterNot { it.placementId == placementId } + row))
            dao.upsertMemberships(listOf(BigPictureMembershipEntity(row)))
        }
    }

    internal suspend fun unassign(placementId: String, expectedVersion: Long, now: Long) = db.withTransaction {
        val current = dao.memberships().singleOrNull { it.record.placementId == placementId }?.record
        require(current != null && current.version == expectedVersion) { "Stale membership version" }
        if (!current.isDeleted) dao.upsertMemberships(listOf(BigPictureMembershipEntity(
            current.copy(isDeleted = true, version = current.version + 1, updatedAt = now, syncedAt = null),
        )))
    }

    suspend fun restoreExact(incoming: BigPicturePresentationSnapshot) = db.withTransaction {
        validate(incoming)
        dao.clearMemberships()
        dao.clearContainers()
        dao.upsertContainers(incoming.containers.map(::BigPictureContainerEntity))
        dao.upsertMemberships(incoming.memberships.map(::BigPictureMembershipEntity))
    }

    suspend fun merge(incoming: BigPicturePresentationSnapshot) = db.withTransaction {
        require(incoming.formatVersion == 1) { "Unsupported Big Picture presentation format" }
        incoming.containers.forEach {
            require(it.id.isNotBlank() && it.title.isNotBlank() && it.hierarchyId == "GENERAL" && it.version >= 1)
        }
        incoming.memberships.forEach {
            require(it.placementId.isNotBlank() && it.containerId.isNotBlank() && it.hierarchyId == "GENERAL" && it.version >= 1)
        }
        val before = snapshot()
        require(incoming.containers.map { it.id }.distinct().size == incoming.containers.size)
        require(incoming.memberships.map { it.placementId }.distinct().size == incoming.memberships.size)
        val containers = before.containers.associateBy { it.id }.toMutableMap()
        val memberships = before.memberships.associateBy { it.placementId }.toMutableMap()
        incoming.containers.forEach { remote ->
            val local = containers[remote.id]
            require(local == null || (local.createdAt == remote.createdAt && local.hierarchyId == remote.hierarchyId))
            if (accept(
                    local?.version,
                    remote.version,
                    local?.copy(syncedAt = null) == remote.copy(syncedAt = null),
                )) {
                containers[remote.id] = remote.copy(syncedAt = null)
            }
        }
        incoming.memberships.forEach { remote ->
            val local = memberships[remote.placementId]
            require(local == null || (local.createdAt == remote.createdAt && local.hierarchyId == remote.hierarchyId))
            if (accept(
                    local?.version,
                    remote.version,
                    local?.copy(syncedAt = null) == remote.copy(syncedAt = null),
                )) {
                memberships[remote.placementId] = remote.copy(syncedAt = null)
            }
        }
        val result = BigPicturePresentationSnapshot(containers.values.toList(), memberships.values.toList())
        validate(result)
        val oldContainers = before.containers.associateBy { it.id }
        val oldMemberships = before.memberships.associateBy { it.placementId }
        val changedContainers = result.containers.filter { it != oldContainers[it.id] }
        val changedMemberships = result.memberships.filter { it != oldMemberships[it.placementId] }
        if (changedContainers.isNotEmpty()) dao.upsertContainers(changedContainers.map(::BigPictureContainerEntity))
        if (changedMemberships.isNotEmpty()) dao.upsertMemberships(changedMemberships.map(::BigPictureMembershipEntity))
    }

    suspend fun markSynced(ack: BigPicturePresentationAck, now: Long) = db.withTransaction {
        ack.containers.forEach { dao.acknowledgeContainer(it.id, it.version, now) }
        ack.memberships.forEach { dao.acknowledgeMembership(it.id, it.version, now) }
    }

    /** Dependency closure is intentionally a transport-adapter responsibility, not an ACK expansion. */
    suspend fun dirty() = snapshot().let { all ->
        all.copy(containers = all.containers.filter { it.syncedAt == null },
            memberships = all.memberships.filter { it.syncedAt == null })
    }

    suspend fun changedSince(timestamp: Long) = snapshot().let { all ->
        all.copy(containers = all.containers.filter { it.updatedAt > timestamp },
            memberships = all.memberships.filter { it.updatedAt > timestamp })
    }

    private suspend fun validate(snapshot: BigPicturePresentationSnapshot) = validateBigPicturePresentation(
        snapshot, db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() },
    )
}

/**
 * Peer admission only. The entire versioned payload, including title and
 * updatedAt, is immutable within one version; syncedAt is receiver-local.
 * Exact Restore deliberately uses a separate replacement contract.
 */
private fun accept(
    localVersion: Long?,
    incomingVersion: Long,
    identicalPayload: Boolean,
): Boolean =
    when {
        localVersion == null -> true
        incomingVersion > localVersion -> true
        incomingVersion < localVersion -> false
        identicalPayload -> false
        else -> error("Equal-version Big Picture presentation divergence")
    }

package com.romankozak.forwardappmobile.data.hierarchy.container

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureContainerSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureMembershipSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyChildPolicyRejectedException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementNotFoundException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementGroupScopeMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLinkedAppearanceMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementMove
import com.romankozak.forwardappmobile.data.hierarchy.HierarchySiblingSetMismatchException
import com.romankozak.forwardappmobile.data.hierarchy.InvalidHierarchyPositionException
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementPosition
import com.romankozak.forwardappmobile.data.hierarchy.requireValidProspectiveHierarchy
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementEntity
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import java.util.UUID
import javax.inject.Inject

/**
 * Typed destination for product-level H1 structural commands.
 *
 * Root, occurrence and generic container are the only structural destinations.
 * Semantic target relationships and historical GroupScope are not destinations.
 */
sealed interface H1StructuralParent {
    data object Root : H1StructuralParent

    data class Node(val ref: H1TypedNode) : H1StructuralParent
}

/**
 * Single product-level structural writer for the schema-199 GENERAL hierarchy.
 *
 * Commands plan the complete prospective typed graph, derive all physical
 * ancestry/membership/order changes, validate and persist them in one Room
 * transaction. Transport restore/merge/ACK and semantic writes are excluded.
 *
 * Legacy occurrence and presentation primitives remain compatibility-only
 * during the subsequent consumer cutover. They are not called from here.
 */
class H1StructuralMutationCoordinator @Inject constructor(
    private val db: AppDatabase,
) {
    /** Presentation title only; ancestry, membership and semantic identities are unchanged. */
    suspend fun renameContainer(id: String, title: String, now: Long = System.currentTimeMillis()) = mutate(now) {
        renameContainer(id, title)
    }

    suspend fun moveNode(
        node: H1TypedNode,
        destination: H1StructuralParent,
        position: Int? = null,
        now: Long = System.currentTimeMillis(),
    ) = mutate(now) { move(node, destination, position) }

    /**
     * Compatibility batch over one prospective typed graph.
     * Source occurrence identities are preserved.
     */
    suspend fun moveOccurrences(
        moves: List<HierarchyPlacementMove>,
        now: Long = System.currentTimeMillis(),
        historicalCompanion: suspend () -> Unit = {},
    ) {
        if (moves.isEmpty()) return

        require(moves.map { it.placementId }.distinct().size == moves.size) {
            "Duplicate occurrence in H1 batch move"
        }

        mutate(now, historicalCompanion) {
            moves.forEach { move ->
                moveOccurrence(
                    id = move.placementId.value,
                    parentId = move.newParentPlacementId?.value,
                    position = move.position,
                )
            }
        }
    }

    /**
     * Reorders the complete mixed sibling namespace, not only occurrences.
     * Rejects subsets, duplicates and foreign children.
     */
    suspend fun reorderChildren(
        parent: H1StructuralParent,
        orderedChildren: List<H1TypedNode>,
        now: Long = System.currentTimeMillis(),
    ) = mutate(now) { reorder(parent, orderedChildren) }

    /**
     * Legacy occurrence-only reorder is admitted only when
     * the complete immediate typed sibling set has no containers.
     */
    suspend fun reorderOccurrenceSiblings(
        parentPlacementId: String?,
        orderedPlacementIds: List<String>,
        now: Long = System.currentTimeMillis(),
    ) = mutate(now) {
        val destination = parentPlacementId?.let {
            H1StructuralParent.Node(H1TypedNode.Occurrence(it))
        } ?: H1StructuralParent.Root

        reorderOccurrenceOnly(destination, orderedPlacementIds)
    }

    /**
     * Creates an exact occurrence through the unified typed structural graph.
     * Destination may be root, occurrence, or generic container.
     */
    suspend fun createOccurrence(
        target: HierarchyTargetRef,
        placementKind: PlacementKind,
        destination: H1StructuralParent,
        position: Int? = null,
        now: Long = System.currentTimeMillis(),
        id: PlacementId = PlacementId(UUID.randomUUID().toString()),
    ): PlacementId = mutate(now) {
        createOccurrence(id, target, placementKind, destination, position, now)
        id
    }

    /** Identity-only Add: choose PRIMARY/LINK from the same locked graph as the write. */
    suspend fun addAppearance(
        target: HierarchyTargetRef,
        destination: H1StructuralParent,
        now: Long = System.currentTimeMillis(),
        id: PlacementId = PlacementId(UUID.randomUUID().toString()),
    ): PlacementId = mutate(now) {
        addAppearance(id, target, destination, now)
        id
    }

    /**
     * Legacy occurrence-only create admission.
     *
     * Position anchors are exact occurrence identities. Mixed sibling
     * namespaces must use the typed create API instead.
     */
    suspend fun createLegacyOccurrence(
        target: HierarchyTargetRef,
        placementKind: PlacementKind,
        parentPlacementId: PlacementId?,
        position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
        now: Long = System.currentTimeMillis(),
        id: PlacementId = PlacementId(UUID.randomUUID().toString()),
    ): PlacementId = mutate(now) {
        createLegacyOccurrence(
            id = id,
            target = target,
            placementKind = placementKind,
            parentPlacementId = parentPlacementId,
            position = position,
            now = now,
        )
        id
    }

    /**
     * Atomic legacy LINK/PRIMARY batch.
     * Every command shares one prospective typed graph and Room transaction.
     * A rejection at any index leaves no committed batch member.
     */
    suspend fun createLegacyOccurrences(
        creations: List<H1LegacyOccurrenceCreation>,
        now: Long = System.currentTimeMillis(),
    ): List<PlacementId> {
        if (creations.isEmpty()) return emptyList()

        require(creations.map { it.id }.distinct().size == creations.size) {
            "Duplicate occurrence identity in H1 creation batch"
        }

        return mutate(now) {
            creations.map { creation ->
                createLegacyOccurrence(
                    id = creation.id,
                    target = creation.target,
                    placementKind = creation.placementKind,
                    parentPlacementId = creation.parentPlacementId,
                    position = creation.position,
                    now = now,
                )
                creation.id
            }
        }
    }

    suspend fun createContainer(
        title: String,
        destination: H1StructuralParent,
        position: Int? = null,
        now: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString(),
    ): String = mutate(now) {
        createContainer(title, destination, position, id, now)
        id
    }

    /**
     * Retire exactly one live occurrence.
     * A live typed child, including a container, rejects the operation.
     * Semantic targets and historical identities are never deleted.
     */
    suspend fun removeOccurrence(
        placementId: PlacementId,
        now: Long = System.currentTimeMillis(),
    ) = mutate(now) {
        removeOccurrence(placementId)
    }

    /**
     * One transaction, one prospective graph, one tombstone event for all
     * occurrence identities. A late policy rejection rolls back the batch.
     * No semantic target or Beacon cross-reference is authored here.
     */
    suspend fun removeOccurrences(
        placementIds: List<PlacementId>,
        now: Long = System.currentTimeMillis(),
    ) {
        if (placementIds.isEmpty()) return

        require(placementIds.distinct().size == placementIds.size) {
            "Duplicate occurrence in H1 retirement batch"
        }

        mutate(now) {
            placementIds.forEach { placementId ->
                removeOccurrence(placementId)
            }
        }
    }

    /**
     * Retires the complete live mixed typed subtree rooted at one occurrence.
     * Containers and occurrence appearances become historical tombstones.
     * Semantic targets remain untouched.
     *
     * Returns retired occurrence identities in typed preorder.
     */
    suspend fun removeOccurrenceSubtree(
        placementId: PlacementId,
        now: Long = System.currentTimeMillis(),
    ): List<PlacementId> = mutate(now) {
        removeOccurrenceSubtree(placementId)
    }

    /**
     * Restore the SAME historical occurrence identity, never create a new one.
     *
     * Retained Policy B membership is interpreted by the typed projector.
     * Invalid parents, primary conflicts and incompatible active membership
     * fail closed before persistence. Exact linked-appearance provenance is
     * restored only when its tombstone matches this occurrence's retirement.
     */
    suspend fun restoreOccurrence(
        placementId: PlacementId,
        now: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        val coverageStore = com.romankozak.forwardappmobile.data.hierarchy.GroupScopeCoverageStore(db)
        coverageStore.validateCurrent()
        val beforePlacements = db.hierarchyPlacementDao().getAll()
            .map { it.toHierarchyPlacementStrict() }
        val current = beforePlacements.singleOrNull { it.id == placementId }
            ?: throw HierarchyPlacementNotFoundException(placementId)
        if (!current.isDeleted) return@withTransaction

        val presentationRepository = BigPicturePresentationRepository(db)
        val presentation = presentationRepository.snapshot()

        projectH1UnifiedStructuralGraph(beforePlacements, presentation)

        val restored = current.copy(
            isDeleted = false,
            version = nextH1StructuralVersion(current.version),
            updatedAt = nextH1StructuralTime(current.updatedAt, now),
            syncedAt = null,
        )
        val prospective = beforePlacements.map {
            if (it.id == placementId) restored else it
        }

        db.requireValidProspectiveHierarchy(prospective)
        projectH1UnifiedStructuralGraph(prospective, presentation)

        db.hierarchyPlacementDao().upsertAll(
            listOf(restored.toHierarchyPlacementEntity()),
        )

        HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
            .restoreIfRetiredWithPlacement(
                placementId = placementId,
                placementDeletedAt = current.updatedAt,
                now = restored.updatedAt,
            )

        presentationRepository.requireCurrentUnifiedStructuralGraphValid()
        coverageStore.validateCurrent()
    }

    /**
     * Tombstones only the specified generic container. All its direct live
     * typed children are promoted into the parent's sibling set in place.
     * Neither occurrences nor semantic targets are deleted.
     */
    suspend fun deleteContainer(
        id: String,
        now: Long = System.currentTimeMillis(),
    ) = mutate(now) { deleteContainer(id) }

    private suspend fun <T> mutate(
        now: Long,
        historicalCompanion: suspend () -> Unit = {},
        operation: H1StructuralPlan.() -> T,
    ): T = db.withTransaction {
        val beforePlacements = db.hierarchyPlacementDao().getAll()
            .map { it.toHierarchyPlacementStrict() }
        val beforePresentation = BigPicturePresentationRepository(db).snapshot()
        val coverageStore = com.romankozak.forwardappmobile.data.hierarchy.GroupScopeCoverageStore(db)
        coverageStore.ensureLegacy(beforePlacements)
        coverageStore.validateCurrent()

        val beforeGraph = projectH1UnifiedStructuralGraph(
            beforePlacements,
            beforePresentation,
        )
        val plan = H1StructuralPlan(
            beforePlacements,
            beforePresentation,
            beforeGraph,
        )

        val result = plan.operation()
        val retiredOccurrences = plan.retiredOccurrenceIds()

        // Retirement and provenance must share one monotonic event clock.
        // Restore correlates exact placement and linked-appearance tombstones
        // by updatedAt, so independently advancing their clocks is unsafe.
        var effectiveNow = now
        if (retiredOccurrences.isNotEmpty()) {
            val placementById = beforePlacements.associateBy { it.id }
            val linkedDao = db.hierarchyPlacementLinkedAppearanceDao()
            val scopeDao = db.hierarchyPlacementGroupScopeDao()

            retiredOccurrences.forEach { id ->
                val placement = requireNotNull(placementById[id]) {
                    "Retirement source missing: ${id.value}"
                }
                val linked = linkedDao.getByPlacementId(id.value)
                val scope = scopeDao.getByPlacementId(id.value)

                val newestPrevious = listOfNotNull(
                    placement.updatedAt,
                    linked?.updatedAt,
                    scope?.updatedAt,
                ).max()

                effectiveNow = maxOf(
                    effectiveNow,
                    nextH1StructuralTime(newestPrevious, now),
                )
            }
        }

        val prospective = plan.materialize(effectiveNow)

        db.requireValidProspectiveHierarchy(prospective.placements)
        projectH1UnifiedStructuralGraph(
            prospective.placements,
            prospective.presentation,
        )

        val beforePlacementsById = beforePlacements.associateBy { it.id }
        val changedPlacements = prospective.placements.filter {
            it != beforePlacementsById[it.id]
        }

        val beforeContainers = beforePresentation.containers.associateBy { it.id }
        val changedContainers = prospective.presentation.containers.filter {
            it != beforeContainers[it.id]
        }

        val beforeMemberships =
            beforePresentation.memberships.associateBy { it.placementId }
        val changedMemberships = prospective.presentation.memberships.filter {
            it != beforeMemberships[it.placementId]
        }

        if (changedPlacements.isNotEmpty()) {
            db.hierarchyPlacementDao().upsertAll(
                changedPlacements.map { it.toHierarchyPlacementEntity() },
            )
        }
        if (changedContainers.isNotEmpty()) {
            db.bigPicturePresentationDao().upsertContainers(
                changedContainers.map(::BigPictureContainerEntity),
            )
        }
        if (changedMemberships.isNotEmpty()) {
            db.bigPicturePresentationDao().upsertMemberships(
                changedMemberships.map(::BigPictureMembershipEntity),
            )
        }
        coverageStore.recordNative(prospective.placements.filter { it.id !in beforePlacementsById })

        // Historical lineage follows identity; generic structure never reactivates it.
        val finalGraph = projectH1UnifiedStructuralGraph(prospective.placements, prospective.presentation)
        val finalPlacementsById = prospective.placements.associateBy { it.id.value }
        db.hierarchyPlacementGroupScopeDao().getAll().filterNot { it.isDeleted }.forEach { scope ->
            val placement = finalPlacementsById[scope.placementId]
            if (placement != null && !placement.isDeleted &&
                finalGraph.parentOf(H1TypedNode.Occurrence(scope.placementId)) != null) {
                HierarchyPlacementGroupScopeMutationCoordinator(db).retireScope(placement.id, effectiveNow)
            }
        }

        // Provenance retirement participates in this same Room transaction.
        // Failure rolls back structural rows, memberships and sidecars together.
        if (retiredOccurrences.isNotEmpty()) {
            HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                .retireLinkedAppearancesForPlacements(
                    retiredOccurrences,
                    effectiveNow,
                )
            HierarchyPlacementGroupScopeMutationCoordinator(db)
                .retireScopesForPlacements(
                    retiredOccurrences,
                    effectiveNow,
                )
        }

        // Check actual persisted graph while the same transaction is active.
        // Any rejection rolls back every table, not only the failing write.
        BigPicturePresentationRepository(db)
            .requireCurrentUnifiedStructuralGraphValid()
        historicalCompanion()
        coverageStore.validateCurrent()

        result
    }
}

/**
 * Legacy occurrence-only batch input.
 * This is a compatibility request, not a second structural authority.
 */
data class H1LegacyOccurrenceCreation(
    val target: HierarchyTargetRef,
    val placementKind: PlacementKind,
    val parentPlacementId: PlacementId?,
    val position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
    val id: PlacementId = PlacementId(UUID.randomUUID().toString()),
)

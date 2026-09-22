package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.hierarchyPlacementSiblingComparator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CanonicalHierarchyPlacementRepository
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        private val dao: HierarchyPlacementDao
            get() = database.hierarchyPlacementDao()

        suspend fun getPlacement(id: PlacementId): HierarchyPlacement? =
            dao.getById(id.value)
                ?.toHierarchyPlacementStrict()
                ?.takeUnless { it.isDeleted }

        fun observeLiveHierarchy(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): Flow<List<HierarchyPlacement>> {
            requireSupportedHierarchy(hierarchyId)
            return dao.observeLiveHierarchy(hierarchyId.value)
                .map { rows ->
                    rows.map { it.toHierarchyPlacementStrict() }
                        .sortedWith(hierarchyPlacementSiblingComparator)
                }
        }

        suspend fun getLiveHierarchy(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): List<HierarchyPlacement> {
            requireSupportedHierarchy(hierarchyId)
            return dao.getLiveHierarchy(hierarchyId.value)
                .map { it.toHierarchyPlacementStrict() }
                .sortedWith(hierarchyPlacementSiblingComparator)
        }

        suspend fun getLiveChildren(
            parentPlacementId: PlacementId?,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): List<HierarchyPlacement> {
            requireSupportedHierarchy(hierarchyId)
            return dao.getLiveChildren(
                hierarchyId = hierarchyId.value,
                parentPlacementId = parentPlacementId?.value,
            ).map { it.toHierarchyPlacementStrict() }
        }

        suspend fun getLiveAppearances(
            target: HierarchyTargetRef,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): List<HierarchyPlacement> {
            requireSupportedHierarchy(hierarchyId)
            return dao.getLiveAppearances(
                hierarchyId = hierarchyId.value,
                targetType = target.type.name,
                targetId = target.id,
            ).map { it.toHierarchyPlacementStrict() }
        }

        suspend fun getPrimaryAppearance(
            target: HierarchyTargetRef,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): HierarchyPlacement? {
            val primary =
                getLiveAppearances(target, hierarchyId)
                    .filter { it.placementKind == PlacementKind.PRIMARY }
            if (primary.size > 1) {
                throw CorruptHierarchyPlacementException(
                    "Target ${target.type}:${target.id} has multiple live PRIMARY appearances in ${hierarchyId.value}",
                )
            }
            return primary.singleOrNull()
        }

        suspend fun createPrimaryAppearance(
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId? = null,
            position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long = System.currentTimeMillis(),
        ): PlacementId =
            createAppearance(
                id = PlacementId(UUID.randomUUID().toString()),
                hierarchyId = hierarchyId,
                target = target,
                parentPlacementId = parentPlacementId,
                placementKind = PlacementKind.PRIMARY,
                position = position,
                now = now,
            )

        /**
         * Caller owns the Room transaction.
         */
        internal suspend fun createPrimaryAppearanceInCurrentTransaction(
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId? = null,
            position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long,
        ): PlacementId =
            createAppearanceInCurrentTransaction(
                id = PlacementId(UUID.randomUUID().toString()),
                hierarchyId = hierarchyId,
                target = target,
                parentPlacementId = parentPlacementId,
                placementKind = PlacementKind.PRIMARY,
                position = position,
                now = now,
            )

        suspend fun createLinkAppearance(
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId? = null,
            position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long = System.currentTimeMillis(),
        ): PlacementId =
            createAppearance(
                id = PlacementId(UUID.randomUUID().toString()),
                hierarchyId = hierarchyId,
                target = target,
                parentPlacementId = parentPlacementId,
                placementKind = PlacementKind.LINK,
                position = position,
                now = now,
            )

        /**
         * H4/P2 readiness primitive. Generates the durable PlacementId while
         * participating in the caller's existing Room transaction.
         */
        internal suspend fun createLinkAppearanceInCurrentTransaction(
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId? = null,
            position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long,
        ): PlacementId =
            createAppearanceInCurrentTransaction(
                id = PlacementId(UUID.randomUUID().toString()),
                hierarchyId = hierarchyId,
                target = target,
                parentPlacementId = parentPlacementId,
                placementKind = PlacementKind.LINK,
                position = position,
                now = now,
            )

        /**
         * Deliberately internal supplied-id boundary for H2 materialization.
         * Normal runtime callers always receive a fresh PlacementId above.
         */
        internal suspend fun createAppearanceWithId(
            id: PlacementId,
            hierarchyId: HierarchyId,
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId?,
            placementKind: PlacementKind,
            position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
            now: Long,
        ): PlacementId =
            createAppearanceInCurrentTransaction(
                id = id,
                hierarchyId = hierarchyId,
                target = target,
                parentPlacementId = parentPlacementId,
                placementKind = placementKind,
                position = position,
                now = now,
            )

        suspend fun movePlacement(
            placementId: PlacementId,
            newParentPlacementId: PlacementId?,
            position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
            now: Long = System.currentTimeMillis(),
        ) {
            movePlacements(
                moves =
                    listOf(
                        HierarchyPlacementMove(
                            placementId = placementId,
                            newParentPlacementId = newParentPlacementId,
                            position = position,
                        ),
                    ),
                now = now,
            )
        }

        suspend fun movePlacements(
            moves: List<HierarchyPlacementMove>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                movePlacementsInCurrentTransaction(
                    moves = moves,
                    now = now,
                )
            }
        }

        /**
         * H4/P2 readiness primitive. Caller owns the Room transaction.
         */
        internal suspend fun movePlacementsInCurrentTransaction(
            moves: List<HierarchyPlacementMove>,
            now: Long,
        ) {
            if (moves.isEmpty()) return
            require(moves.map { it.placementId }.distinct().size == moves.size) {
                "Batch hierarchy move contains duplicate placement ids"
            }

            val before = loadCompleteState()
            val prospective = before.toMutableMap()

            val liveMoves =
                moves.map { move ->
                    move to requireLivePlacement(prospective, move.placementId)
                }

            // Parent changes are projected for the whole batch first. Position
            // references therefore see the final destination sibling set,
            // including other placements moved by the same command.
            liveMoves.forEach { (move, current) ->
                prospective[move.placementId] =
                    current.copy(parentPlacementId = move.newParentPlacementId)
            }

            liveMoves
                .map { (_, current) -> current.hierarchyId to current.parentPlacementId }
                .distinct()
                .forEach { (hierarchyId, parentPlacementId) ->
                    normalizeSiblingGroup(
                        state = prospective,
                        hierarchyId = hierarchyId,
                        parentPlacementId = parentPlacementId,
                    )
                }

            moves.forEach { move ->
                applyPosition(
                    state = prospective,
                    placementId = move.placementId,
                    newParentPlacementId = move.newParentPlacementId,
                    position = move.position,
                )
            }

            database.requireValidProspectiveHierarchy(prospective.values)
            persistChangedState(before, prospective, now)
        }

        suspend fun reorderPlacement(
            placementId: PlacementId,
            position: HierarchyPlacementPosition,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val before = loadCompleteState()
                val current = requireLivePlacement(before, placementId)
                val prospective = before.toMutableMap()

                applyPosition(
                    state = prospective,
                    placementId = placementId,
                    newParentPlacementId = current.parentPlacementId,
                    position = position,
                )

                database.requireValidProspectiveHierarchy(prospective.values)
                persistChangedState(before, prospective, now)
            }
        }

        /**
         * Atomically replaces the complete order of one concrete sibling
         * occurrence set.
         *
         * Parent and children are identified only by PlacementId. Repeated
         * targets therefore remain distinct and independently reorderable.
         */
        suspend fun reorderSiblings(
            parentPlacementId: PlacementId?,
            orderedPlacementIds: List<PlacementId>,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long = System.currentTimeMillis(),
        ) {
            requireSupportedHierarchy(hierarchyId)
            if (orderedPlacementIds.distinct().size != orderedPlacementIds.size) {
                throw HierarchySiblingSetMismatchException(
                    "Sibling reorder contains duplicate placement ids",
                )
            }

            database.withTransaction {
                val before = loadCompleteState()
                parentPlacementId?.let { requireLivePlacement(before, it) }

                val currentIds =
                    before.values
                        .filter {
                            !it.isDeleted &&
                                it.hierarchyId == hierarchyId &&
                                it.parentPlacementId == parentPlacementId
                        }
                        .sortedWith(hierarchyPlacementSiblingComparator)
                        .map { it.id }

                if (
                    orderedPlacementIds.size != currentIds.size ||
                    orderedPlacementIds.toSet() != currentIds.toSet()
                ) {
                    throw HierarchySiblingSetMismatchException(
                        "Sibling reorder must contain the complete live sibling occurrence set",
                    )
                }

                val prospective = before.toMutableMap()
                orderedPlacementIds.forEachIndexed { index, placementId ->
                    val current = requireLivePlacement(prospective, placementId)
                    prospective[placementId] =
                        current.copy(siblingOrder = index.toLong())
                }

                database.requireValidProspectiveHierarchy(prospective.values)
                persistChangedState(before, prospective, now)
            }
        }

        suspend fun removePlacement(
            placementId: PlacementId,
            childPolicy: HierarchyPlacementChildPolicy =
                HierarchyPlacementChildPolicy.REJECT_IF_HAS_LIVE_CHILDREN,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                removePlacementInCurrentTransaction(
                    placementId = placementId,
                    childPolicy = childPolicy,
                    now = now,
                )
            }
        }

        /**
         * H4/P2 readiness primitive. Caller owns the Room transaction.
         */
        internal suspend fun removePlacementInCurrentTransaction(
            placementId: PlacementId,
            childPolicy: HierarchyPlacementChildPolicy =
                HierarchyPlacementChildPolicy.REJECT_IF_HAS_LIVE_CHILDREN,
            now: Long,
        ) {
            val before = loadCompleteState()
            val current =
                before[placementId]
                    ?: throw HierarchyPlacementNotFoundException(placementId)
            if (current.isDeleted) return

            when (childPolicy) {
                HierarchyPlacementChildPolicy.REJECT_IF_HAS_LIVE_CHILDREN -> {
                    val hasLiveChildren =
                        before.values.any {
                            !it.isDeleted &&
                                it.hierarchyId == current.hierarchyId &&
                                it.parentPlacementId == placementId
                        }
                    if (hasLiveChildren) {
                        throw HierarchyChildPolicyRejectedException(placementId)
                    }
                }
            }

            val prospective =
                before.toMutableMap().apply {
                    this[placementId] = current.copy(isDeleted = true)
                }

            database.requireValidProspectiveHierarchy(prospective.values)
            HierarchyPlacementLinkedAppearanceMutationCoordinator(database)
                .retireLinkedAppearance(
                    placementId = placementId,
                    now = now,
                )
            persistChangedState(before, prospective, now)
        }

        suspend fun restorePlacement(
            placementId: PlacementId,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val before = loadCompleteState()
                val current =
                    before[placementId]
                        ?: throw HierarchyPlacementNotFoundException(placementId)
                if (!current.isDeleted) return@withTransaction

                val prospective =
                    before.toMutableMap().apply {
                        this[placementId] = current.copy(isDeleted = false)
                    }

                database.requireValidProspectiveHierarchy(prospective.values)
                val placementDeletedAt = current.updatedAt
                persistChangedState(before, prospective, now)
                HierarchyPlacementLinkedAppearanceMutationCoordinator(database)
                    .restoreIfRetiredWithPlacement(
                        placementId = placementId,
                        placementDeletedAt = placementDeletedAt,
                        now = now,
                    )
            }
        }

        private suspend fun createAppearance(
            id: PlacementId,
            hierarchyId: HierarchyId,
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId?,
            placementKind: PlacementKind,
            position: HierarchyPlacementPosition,
            now: Long,
        ): PlacementId =
            database.withTransaction {
                createAppearanceInCurrentTransaction(
                    id = id,
                    hierarchyId = hierarchyId,
                    target = target,
                    parentPlacementId = parentPlacementId,
                    placementKind = placementKind,
                    position = position,
                    now = now,
                )
            }

        private suspend fun createAppearanceInCurrentTransaction(
            id: PlacementId,
            hierarchyId: HierarchyId,
            target: HierarchyTargetRef,
            parentPlacementId: PlacementId?,
            placementKind: PlacementKind,
            position: HierarchyPlacementPosition,
            now: Long,
        ): PlacementId {
            requireSupportedHierarchy(hierarchyId)
            val before = loadCompleteState()
            if (id in before) throw HierarchyPlacementAlreadyExistsException(id)

            val prospective =
                before.toMutableMap().apply {
                    this[id] =
                        HierarchyPlacement(
                            id = id,
                            hierarchyId = hierarchyId,
                            target = target,
                            parentPlacementId = parentPlacementId,
                            placementKind = placementKind,
                            siblingOrder = 0L,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        )
                }

            applyPosition(
                state = prospective,
                placementId = id,
                newParentPlacementId = parentPlacementId,
                position = position,
            )

            database.requireValidProspectiveHierarchy(prospective.values)
            persistChangedState(before, prospective, now)
            return id
        }

        private suspend fun loadCompleteState(): Map<PlacementId, HierarchyPlacement> =
            dao.getAll()
                .map { it.toHierarchyPlacementStrict() }
                .associateBy { it.id }

        private fun requireLivePlacement(
            state: Map<PlacementId, HierarchyPlacement>,
            placementId: PlacementId,
        ): HierarchyPlacement =
            state[placementId]
                ?.takeUnless { it.isDeleted }
                ?: throw HierarchyPlacementNotFoundException(placementId)

        private fun applyPosition(
            state: MutableMap<PlacementId, HierarchyPlacement>,
            placementId: PlacementId,
            newParentPlacementId: PlacementId?,
            position: HierarchyPlacementPosition,
        ) {
            val current = requireNotNull(state[placementId])
            val oldParentPlacementId = current.parentPlacementId
            state[placementId] =
                current.copy(parentPlacementId = newParentPlacementId)

            if (oldParentPlacementId != newParentPlacementId) {
                normalizeSiblingGroup(
                    state = state,
                    hierarchyId = current.hierarchyId,
                    parentPlacementId = oldParentPlacementId,
                )
            }

            val siblings =
                state.values
                    .filter {
                        !it.isDeleted &&
                            it.id != placementId &&
                            it.hierarchyId == current.hierarchyId &&
                            it.parentPlacementId == newParentPlacementId
                    }
                    .sortedWith(hierarchyPlacementSiblingComparator)
                    .toMutableList()

            val insertionIndex =
                when (position) {
                    HierarchyPlacementPosition.FIRST -> 0
                    HierarchyPlacementPosition.LAST -> siblings.size

                    is HierarchyPlacementPosition.BEFORE -> {
                        val index =
                            siblings.indexOfFirst {
                                it.id == position.placementId
                            }
                        if (index < 0) {
                            throw InvalidHierarchyPositionException(
                                "BEFORE reference ${position.placementId.value} is not a live destination sibling",
                            )
                        }
                        index
                    }

                    is HierarchyPlacementPosition.AFTER -> {
                        val index =
                            siblings.indexOfFirst {
                                it.id == position.placementId
                            }
                        if (index < 0) {
                            throw InvalidHierarchyPositionException(
                                "AFTER reference ${position.placementId.value} is not a live destination sibling",
                            )
                        }
                        index + 1
                    }
                }

            siblings.add(insertionIndex, state.getValue(placementId))
            siblings.forEachIndexed { index, placement ->
                state[placement.id] =
                    placement.copy(siblingOrder = index.toLong())
            }
        }

        private fun normalizeSiblingGroup(
            state: MutableMap<PlacementId, HierarchyPlacement>,
            hierarchyId: HierarchyId,
            parentPlacementId: PlacementId?,
        ) {
            state.values
                .filter {
                    !it.isDeleted &&
                        it.hierarchyId == hierarchyId &&
                        it.parentPlacementId == parentPlacementId
                }
                .sortedWith(hierarchyPlacementSiblingComparator)
                .forEachIndexed { index, placement ->
                    state[placement.id] =
                        placement.copy(siblingOrder = index.toLong())
                }
        }

        private suspend fun persistChangedState(
            before: Map<PlacementId, HierarchyPlacement>,
            prospective: Map<PlacementId, HierarchyPlacement>,
            now: Long,
        ) {
            val changed =
                prospective.values
                    .sortedBy { it.id.value }
                    .mapNotNull { desired ->
                        val current = before[desired.id]
                        when {
                            current == null ->
                                desired.toHierarchyPlacementEntity()

                            current.sameMutableState(desired) ->
                                null

                            else ->
                                desired.copy(
                                    createdAt = current.createdAt,
                                    updatedAt = now,
                                    syncedAt = null,
                                    version = current.version + 1L,
                                ).toHierarchyPlacementEntity()
                        }
                    }

            if (changed.isNotEmpty()) {
                dao.upsertAll(changed)
            }
        }
    }

private fun requireSupportedHierarchy(hierarchyId: HierarchyId) {
    if (hierarchyId != HierarchyId.GENERAL) {
        throw UnsupportedHierarchyException(hierarchyId)
    }
}

package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.data.orientation.mainBeaconMembershipRelationId
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import javax.inject.Inject
import javax.inject.Singleton

data class CanonicalV2HierarchyOccurrenceWriteResult(
    val createdPlacementIds: List<PlacementId>,
)

/**
 * Dormant H4.0e fused writer for the Beacon occurrence-authority slice.
 *
 * No production caller exists. This does not activate P2 authority.
 */
@Singleton
class CanonicalV2HierarchyOccurrenceWriter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val repository: CanonicalHierarchyPlacementRepository,
        private val groupScopeCoordinator: HierarchyPlacementGroupScopeMutationCoordinator,
    ) {
        suspend fun executeBeaconPlan(
            plan: HierarchyActionPlan,
            now: Long = System.currentTimeMillis(),
        ): CanonicalV2HierarchyOccurrenceWriteResult {
            require(plan.operational.isEmpty()) {
                "V2 Beacon occurrence writer does not execute operational operands"
            }
            require(plan.target.isEmpty()) {
                "V2 Beacon occurrence writer does not execute target-domain operands"
            }

            val moves =
                plan.structural.mapNotNull { operand ->
                    when (operand) {
                        is HierarchyStructuralOperand.MoveOccurrence ->
                            HierarchyPlacementMove(
                                placementId = operand.placementId,
                                newParentPlacementId = operand.newParentPlacementId,
                            )

                        is HierarchyStructuralOperand.CreateAppearance -> null

                        is HierarchyStructuralOperand.RemoveOccurrence ->
                            error("V2 Beacon occurrence writer does not execute occurrence removal")

                        is HierarchyStructuralOperand.CreatePrimaryForClonedWorkspace ->
                            error("V2 Beacon occurrence writer does not execute Workspace cloning")
                    }
                }

            val nestedCreates =
                plan.structural.filterIsInstance<HierarchyStructuralOperand.CreateAppearance>()

            nestedCreates.forEach { operand ->
                require(operand.parentPlacementId != null) {
                    "Beacon appearance creation requires a concrete parent occurrence"
                }
                require(operand.placementKind == PlacementKind.LINK) {
                    "Beacon appearance creation must create LINK"
                }
                requireManagedSubjectTarget(operand.target)
            }

            val setScopes =
                plan.occurrenceScope
                    .filterIsInstance<HierarchyOccurrenceScopeOperand.SetRootGroupScope>()
            val rootCreates =
                plan.occurrenceScope
                    .filterIsInstance<HierarchyOccurrenceScopeOperand.CreateRootLinkAppearance>()
            val retireScopes =
                plan.occurrenceScope
                    .filterIsInstance<HierarchyOccurrenceScopeOperand.RetireRootGroupScope>()

            require(
                setScopes.size + rootCreates.size + retireScopes.size ==
                    plan.occurrenceScope.size,
            ) {
                "Unknown occurrence-scope operand"
            }

            val moveByPlacement = moves.associateBy { it.placementId }
            require(moveByPlacement.size == moves.size) {
                "Duplicate move placement ids"
            }
            require(setScopes.map { it.placementId }.distinct().size == setScopes.size) {
                "Duplicate SetRootGroupScope placement ids"
            }
            require(retireScopes.map { it.placementId }.distinct().size == retireScopes.size) {
                "Duplicate RetireRootGroupScope placement ids"
            }

            setScopes.forEach { operand ->
                val move =
                    requireNotNull(moveByPlacement[operand.placementId]) {
                        "SetRootGroupScope requires a matching exact occurrence move"
                    }
                require(move.newParentPlacementId == null) {
                    "SetRootGroupScope requires moving the occurrence to root"
                }
            }

            retireScopes.forEach { operand ->
                val move =
                    requireNotNull(moveByPlacement[operand.placementId]) {
                        "RetireRootGroupScope requires a matching exact occurrence move"
                    }
                require(move.newParentPlacementId != null) {
                    "RetireRootGroupScope requires a concrete destination parent"
                }
            }

            moves.forEach { move ->
                val hasSet = setScopes.any { it.placementId == move.placementId }
                val hasRetire = retireScopes.any { it.placementId == move.placementId }
                require(hasSet.xor(hasRetire)) {
                    "Every Beacon move requires exactly one fused Group-scope mutation"
                }
            }

            rootCreates.forEach { operand ->
                requireManagedSubjectTarget(operand.target)
                require(operand.groupSubjectId.isNotBlank()) {
                    "Root Group appearance requires canonical Group subject id"
                }
            }

            val semanticTargets =
                plan.semantic.map { operand ->
                    when (operand) {
                        is HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes ->
                            operand.target

                        is HierarchySemanticOperand.EnsureDirectionFrontLinkIfEnabled ->
                            error("V2 Beacon occurrence writer does not execute Direction semantics")
                    }
                }

            require(semanticTargets.distinct().size == semanticTargets.size) {
                "Duplicate PART_OF reconciliation targets"
            }
            semanticTargets.forEach(::requireManagedSubjectTarget)

            return database.withTransaction {
                groupScopeCoordinator.validateAuthoritativeState()

                val movedTargets =
                    moves.associate { move ->
                        val occurrence =
                            requireNotNull(repository.getPlacement(move.placementId)) {
                                "Move source ${move.placementId.value} does not exist"
                            }
                        requireManagedSubjectTarget(occurrence.target)
                        move.placementId to occurrence.target
                    }

                rootCreates.forEach { operand ->
                    val source =
                        requireNotNull(repository.getPlacement(operand.sourceOccurrenceId)) {
                            "Root LINK source ${operand.sourceOccurrenceId.value} does not exist"
                        }
                    require(source.target == operand.target) {
                        "Root LINK source target does not match planned target"
                    }
                }

                repository.movePlacementsInCurrentTransaction(
                    moves = moves,
                    now = now,
                )

                val created = mutableListOf<PlacementId>()

                nestedCreates.forEach { operand ->
                    created +=
                        repository.createLinkAppearanceInCurrentTransaction(
                            target = operand.target,
                            parentPlacementId = requireNotNull(operand.parentPlacementId),
                            now = now,
                        )
                }

                rootCreates.forEach { operand ->
                    val placementId =
                        repository.createLinkAppearanceInCurrentTransaction(
                            target = operand.target,
                            parentPlacementId = null,
                            now = now,
                        )
                    groupScopeCoordinator.setRootScope(
                        placementId = placementId,
                        groupSubjectId = operand.groupSubjectId,
                        now = now,
                    )
                    created += placementId
                }

                setScopes.forEach { operand ->
                    groupScopeCoordinator.setRootScope(
                        placementId = operand.placementId,
                        groupSubjectId = operand.groupSubjectId,
                        now = now,
                    )
                }

                retireScopes.forEach { operand ->
                    groupScopeCoordinator.retireScope(
                        placementId = operand.placementId,
                        now = now,
                    )
                }

                val expectedSemanticTargets =
                    buildSet {
                        setScopes.forEach {
                            add(movedTargets.getValue(it.placementId))
                        }
                        retireScopes.forEach {
                            add(movedTargets.getValue(it.placementId))
                        }
                        rootCreates.forEach {
                            add(it.target)
                        }
                    }

                require(semanticTargets.toSet() == expectedSemanticTargets) {
                    "Scope mutation and PART_OF reconciliation target sets diverge"
                }

                semanticTargets
                    .sortedWith(
                        compareBy<HierarchyTargetRef>({ it.type.name }, { it.id }),
                    )
                    .forEach { target ->
                        reconcilePartOfFromRootScopes(
                            target = target,
                            now = now,
                        )
                    }

                groupScopeCoordinator.validateAuthoritativeState()

                CanonicalV2HierarchyOccurrenceWriteResult(
                    createdPlacementIds = created.toList(),
                )
            }
        }

        private suspend fun reconcilePartOfFromRootScopes(
            target: HierarchyTargetRef,
            now: Long,
        ) {
            requireManagedSubjectTarget(target)

            val liveRoots =
                database.hierarchyPlacementDao()
                    .getAll()
                    .map { it.toHierarchyPlacementStrict() }
                    .filter {
                        !it.isDeleted &&
                            it.parentPlacementId == null &&
                            it.target == target
                    }

            val scopesByPlacement =
                database.hierarchyPlacementGroupScopeDao()
                    .getLiveForHierarchy("GENERAL")
                    .associateBy { it.placementId }

            val desiredOrderByGroup =
                liveRoots
                    .map { placement ->
                        val scope =
                            requireNotNull(scopesByPlacement[placement.id.value]) {
                                "Root ${placement.id.value} is missing Group-scope provenance"
                            }
                        placement to scope
                    }
                    .mapNotNull { (placement, scope) ->
                        scope.groupSubjectId?.let { it to placement.siblingOrder }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, orders) -> orders.minOrNull() ?: 0L }

            val orientationDao = database.orientationDao()
            val canonicalGroupSubjects =
                orientationDao.getAllLegacyMappings()
                    .asSequence()
                    .filter {
                        !it.isDeleted &&
                            it.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name &&
                            it.state == LegacySubjectMappingState.CUT_OVER.name
                    }
                    .mapTo(linkedSetOf()) { it.subjectId }

            require(desiredOrderByGroup.keys.all { it in canonicalGroupSubjects }) {
                "Root scope references a non-canonical Main Beacon Group"
            }

            val existing =
                orientationDao.getAllOrientationRelations()
                    .filter {
                        it.relationType == OrientationRelationType.PART_OF.name &&
                            it.fromOrientationId == target.id &&
                            it.toOrientationId in canonicalGroupSubjects
                    }

            val existingByGroup = existing.associateBy { it.toOrientationId }
            val changes = mutableListOf<OrientationRelationEntity>()

            desiredOrderByGroup
                .toSortedMap()
                .forEach { (groupSubjectId, relationOrder) ->
                    val current = existingByGroup[groupSubjectId]
                    when {
                        current == null ->
                            changes +=
                                OrientationRelationEntity(
                                    id =
                                        mainBeaconMembershipRelationId(
                                            target.id,
                                            groupSubjectId,
                                        ),
                                    fromOrientationId = target.id,
                                    toOrientationId = groupSubjectId,
                                    relationType = OrientationRelationType.PART_OF.name,
                                    relationOrder = relationOrder,
                                    createdAt = now,
                                    updatedAt = now,
                                    syncedAt = null,
                                    isDeleted = false,
                                    version = 1L,
                                )

                        current.isDeleted || current.relationOrder != relationOrder ->
                            changes +=
                                current.copy(
                                    relationOrder = relationOrder,
                                    updatedAt = now,
                                    syncedAt = null,
                                    isDeleted = false,
                                    version = current.version + 1L,
                                )
                    }
                }

            existing
                .filter {
                    !it.isDeleted &&
                        it.toOrientationId !in desiredOrderByGroup
                }
                .sortedBy { it.toOrientationId }
                .forEach { current ->
                    changes +=
                        current.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = current.version + 1L,
                        )
                }

            if (changes.isNotEmpty()) {
                orientationDao.upsertOrientationRelations(changes)
            }
        }

        private fun requireManagedSubjectTarget(target: HierarchyTargetRef) {
            require(target.type == HierarchyTargetType.MANAGED_SUBJECT) {
                "V2 Beacon occurrence writer requires MANAGED_SUBJECT targets"
            }
        }
    }

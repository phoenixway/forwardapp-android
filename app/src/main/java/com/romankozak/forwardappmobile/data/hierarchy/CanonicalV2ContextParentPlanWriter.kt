package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fused authority writer for Context compatibility CUT/LINK into a concrete
 * canonical parent occurrence.
 *
 * Structural V2 hierarchy mutations and the Direction companion semantic are
 * committed in one Room transaction.
 */
@Singleton
class CanonicalV2ContextParentPlanWriter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val coordinator: H1StructuralMutationCoordinator,
        private val contextRepository: ContextRepository,
    ) {
        suspend fun execute(
            plan: HierarchyActionPlan,
            now: Long = System.currentTimeMillis(),
        ) {
            require(plan.occurrenceScope.isEmpty()) {
                "Context parent paste must not contain occurrence-scope operands"
            }
            require(plan.operational.isEmpty()) {
                "Context parent paste must not contain operational operands"
            }
            require(plan.target.isEmpty()) {
                "Context CUT/LINK writer must not contain target-domain operands"
            }

            val removals =
                plan.structural.filterIsInstance<HierarchyStructuralOperand.RemoveOccurrence>()

            val moves =
                plan.structural
                    .filterIsInstance<HierarchyStructuralOperand.MoveOccurrence>()
                    .map { operand ->
                        HierarchyPlacementMove(
                            placementId = operand.placementId,
                            newParentPlacementId = operand.newParentPlacementId,
                        )
                    }

            val creates =
                plan.structural.filterIsInstance<HierarchyStructuralOperand.CreateAppearance>()

            require(
                removals.size + moves.size + creates.size == plan.structural.size,
            ) {
                "Context CUT/LINK writer received unsupported structural operand"
            }

            creates.forEach { operand ->
                require(operand.placementKind == PlacementKind.LINK) {
                    "Context LINK paste may create only LINK appearances"
                }
            }

            plan.semantic.forEach { operand ->
                require(operand is HierarchySemanticOperand.EnsureDirectionFrontLinkIfEnabled) {
                    "Context parent paste received unsupported semantic operand"
                }
            }

            database.withTransaction {
                // Structural authority is H1. All nested coordinator calls
                // participate in this outer semantic Room transaction.
                coordinator.removeOccurrences(
                    placementIds = removals.map { it.placementId },
                    now = now,
                )

                coordinator.moveOccurrences(
                    moves = moves,
                    now = now,
                )

                creates.forEach { operand ->
                    coordinator.createOccurrence(
                        target = operand.target,
                        placementKind = operand.placementKind,
                        destination = operand.parentPlacementId?.let {
                            H1StructuralParent.Node(
                                H1TypedNode.Occurrence(it.value),
                            )
                        } ?: H1StructuralParent.Root,
                        now = now,
                    )
                }

                plan.semantic.forEach { operand ->
                    operand as HierarchySemanticOperand.EnsureDirectionFrontLinkIfEnabled
                    contextRepository.ensureDirectionFrontLinkIfEnabled(
                        parentContextId = operand.parentWorkspaceTarget.id,
                        childContextId = operand.childWorkspaceTarget.id,
                    )
                }
            }
        }
    }

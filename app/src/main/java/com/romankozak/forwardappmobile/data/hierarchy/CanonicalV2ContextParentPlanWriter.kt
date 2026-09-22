package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.repository.ContextRepository
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
        private val repository: CanonicalHierarchyPlacementRepository,
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
                removals.forEach { operand ->
                    repository.removePlacementInCurrentTransaction(
                        placementId = operand.placementId,
                        now = now,
                    )
                }

                repository.movePlacementsInCurrentTransaction(
                    moves = moves,
                    now = now,
                )

                creates.forEach { operand ->
                    repository.createLinkAppearanceInCurrentTransaction(
                        target = operand.target,
                        parentPlacementId = operand.parentPlacementId,
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

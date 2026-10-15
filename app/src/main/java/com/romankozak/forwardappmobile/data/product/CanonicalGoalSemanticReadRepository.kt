package com.romankozak.forwardappmobile.data.product

import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class CanonicalGoalSemanticProjection(
    val goalId: String,
    val orientationId: String,
    val title: String,
    val description: String?,
    val lifecycle: OrientationLifecycle,
)

/**
 * Bulk, read-only bridge from a legacy Goal locator to canonical semantic state.
 *
 * Goal remains a specialized/command carrier. CUT_OVER common meaning is read
 * exclusively from the shared canonical product frame; missing or divergent
 * canonical state is an integrity failure rather than a Goal fallback.
 */
@Singleton
class CanonicalGoalSemanticReadRepository
    @Inject
    constructor(
        private val canonicalProductReadRepository: CanonicalProductReadRepository,
        private val orientationDao: OrientationDao,
    ) {
        fun observeAll(): Flow<List<CanonicalGoalSemanticProjection>> =
            combine(
                canonicalProductReadRepository.observe(),
                orientationDao.observeLegacyMappings(),
            ) { productRead, mappings -> projectCanonicalGoalSemantics(productRead, mappings) }

        suspend fun requireGoal(goalId: String): CanonicalGoalSemanticProjection =
            requireNotNull(observeAll().first().singleOrNull { it.goalId == goalId }) {
                "Goal $goalId has no live CUT_OVER canonical semantic projection"
            }

        fun projectGoals(legacyGoals: Flow<List<Goal>>): Flow<List<Goal>> =
            combine(legacyGoals, observeAll()) { goals, semantics ->
                val semanticsByGoalId = semantics.associateBy(CanonicalGoalSemanticProjection::goalId)
                goals.map { goal ->
                    val semantic =
                        requireNotNull(semanticsByGoalId[goal.id]) {
                            "Live Goal ${goal.id} has no canonical semantic projection"
                        }
                    goal.withCanonicalSemantic(semantic)
                }
            }

        suspend fun projectGoal(goal: Goal): Goal = goal.withCanonicalSemantic(requireGoal(goal.id))
    }

internal fun projectCanonicalGoalSemantics(
    productRead: CanonicalProductReadResult,
    mappings: List<LegacySubjectMappingEntity>,
): List<CanonicalGoalSemanticProjection> {
    val ready =
        productRead as? CanonicalProductReadResult.Ready
            ?: error("Canonical product frame is invalid; Goal semantic fallback is forbidden")
    val orientationsById = ready.orientations.associateBy { it.subject.id }
    val goalMappings =
        mappings.filter { mapping ->
            !mapping.isDeleted &&
                mapping.sourceType == LegacyOrientationSourceType.GOAL.name &&
                mapping.state == LegacySubjectMappingState.CUT_OVER.name
        }
    require(goalMappings.map { it.sourceId }.distinct().size == goalMappings.size) {
        "Live CUT_OVER Goal mappings must be unique by source id"
    }

    return goalMappings.map { mapping ->
        val projection =
            requireNotNull(orientationsById[mapping.subjectId]) {
                "CUT_OVER Goal ${mapping.sourceId} is missing canonical Orientation ${mapping.subjectId}"
            }
        require(projection.orientation.kind == OrientationKind.GOAL) {
            "CUT_OVER Goal ${mapping.sourceId} target is not GOAL"
        }
        CanonicalGoalSemanticProjection(
            goalId = mapping.sourceId,
            orientationId = mapping.subjectId,
            title = projection.subject.title,
            description = projection.subject.description,
            lifecycle =
                requireNotNull(projection.orientation.lifecycle) {
                    "CUT_OVER Goal ${mapping.sourceId} canonical lifecycle is missing"
                },
        )
    }
}

private fun Goal.withCanonicalSemantic(semantic: CanonicalGoalSemanticProjection): Goal =
    copy(
        text = semantic.title,
        description = semantic.description,
        goalStatus = semantic.lifecycle.toLegacyGoalStatus(),
        completed = semantic.lifecycle == OrientationLifecycle.REALIZED,
    )

private fun OrientationLifecycle.toLegacyGoalStatus(): String =
    when (this) {
        OrientationLifecycle.EXPLORING -> GoalStatusValues.UNSURE
        OrientationLifecycle.READY -> GoalStatusValues.ACTIVE
        OrientationLifecycle.ACTIVE -> GoalStatusValues.IN_WORK
        OrientationLifecycle.PAUSED -> GoalStatusValues.PAUSED
        OrientationLifecycle.REALIZED -> GoalStatusValues.DONE
        OrientationLifecycle.DROPPED -> GoalStatusValues.CANCELED
    }

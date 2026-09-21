package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility writer boundary for local Android Goal authoring after GOAL
 * identity cutover.
 *
 * Goal remains the temporary Android UI/compatibility command surface. Only the
 * semantic fields represented by Goal.toEffectiveOrientation are written
 * through to the already-existing canonical Orientation. Goal-only fields stay
 * in Goal storage and do not mutate canonical versions.
 *
 * Sync/merge ingestion deliberately bypasses this boundary; peer reconciliation
 * has its own ownership rules.
 */
@Singleton
class GoalOrientationBridge
    @Inject
    constructor(
        private val database: AppDatabase,
        private val goalDao: GoalDao,
        private val orientationDao: OrientationDao,
        private val hierarchyPlacementLifecycleCoordinator: HierarchyPlacementLifecycleCoordinator,
    ) {
        private val gson = Gson()

        suspend fun updateGoal(goal: Goal) {
            require(!goal.isDeleted) {
                "Deleted Goal must use the Goal deletion lifecycle"
            }
            database.withTransaction {
                val previous = goalDao.getGoalById(goal.id)
                goalDao.updateGoal(goal)
                writeCanonicalIfCutOver(previous, goal)
            }
        }

        suspend fun updateGoals(goals: List<Goal>) {
            if (goals.isEmpty()) return
            require(goals.none { it.isDeleted }) {
                "Deleted Goals must use the Goal deletion lifecycle"
            }
            database.withTransaction {
                val previousById =
                    goalDao.getGoalsByIdsSuspend(goals.map { it.id })
                        .associateBy { it.id }
                goalDao.updateGoals(goals)
                goals.forEach { goal ->
                    writeCanonicalIfCutOver(previousById[goal.id], goal)
                }
            }
        }

        /**
         * Tombstones canonical Goal identity after canonical BACKLOG placements
         * have already been tombstoned while the GOAL mapping is still live.
         *
         * Missing or not-yet-CUT_OVER mappings remain legacy authority and are
         * intentionally ignored.
         */
        suspend fun tombstoneCanonicalIfCutOver(
            goalId: String,
            now: Long,
        ) {
            val mapping =
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.GOAL.name,
                    goalId,
                ) ?: return
            if (mapping.state != LegacySubjectMappingState.CUT_OVER.name) return
            if (mapping.isDeleted) return

            val subject =
                requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)) {
                    "CUT_OVER Goal $goalId is missing canonical subject ${mapping.subjectId}"
                }
            require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                "CUT_OVER Goal $goalId points to a non-Orientation subject"
            }

            val orientations =
                orientationDao.getAllOrientations()
                    .filter { it.subjectId == mapping.subjectId }
            require(orientations.size == 1) {
                "CUT_OVER Goal $goalId must have exactly one canonical Orientation"
            }
            require(orientations.single().kind == OrientationKind.GOAL.name) {
                "CUT_OVER Goal $goalId canonical Orientation kind is not GOAL"
            }

            val currents =
                orientationDao.getAllAssessments()
                    .filter { it.orientationId == mapping.subjectId }
            require(currents.size == 1) {
                "CUT_OVER Goal $goalId must have exactly one current assessment"
            }
            val current = currents.single()

            if (!subject.isDeleted) {
                hierarchyPlacementLifecycleCoordinator.tombstoneManagedSubjectTarget(subject.id, now)
                orientationDao.upsertManagedSubjects(
                    listOf(
                        subject.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = subject.version + 1L,
                        ),
                    ),
                )
            }

            if (!current.isDeleted) {
                orientationDao.upsertAssessments(
                    listOf(
                        current.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = current.version + 1L,
                        ),
                    ),
                )
            }

            val relationChanges =
                orientationDao.getAllOrientationRelations()
                    .filter {
                        !it.isDeleted &&
                            (it.fromOrientationId == mapping.subjectId ||
                                it.toOrientationId == mapping.subjectId)
                    }.map {
                        it.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = it.version + 1L,
                        )
                    }
            if (relationChanges.isNotEmpty()) {
                orientationDao.upsertOrientationRelations(relationChanges)
            }

            if (!mapping.isDeleted) {
                orientationDao.upsertLegacyMappings(
                    listOf(
                        mapping.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = mapping.version + 1L,
                        ),
                    ),
                )
            }
        }

        private suspend fun writeCanonicalIfCutOver(
            previous: Goal?,
            goal: Goal,
        ) {
            val previousGoal =
                previous ?: run {
                    val mapping =
                        orientationDao.getLegacyMapping(
                            LegacyOrientationSourceType.GOAL.name,
                            goal.id,
                        ) ?: return

                    if (mapping.state != LegacySubjectMappingState.CUT_OVER.name) return
                    error("CUT_OVER Goal ${goal.id} has no previous compatibility row")
                }

            // The legacy Goal UI owns only these shared semantic slices while
            // it remains the compatibility command surface. A Goal-only edit
            // must not re-project unrelated canonical state.
            val subjectCommandChanged =
                previousGoal.text != goal.text ||
                    previousGoal.description != goal.description
            val lifecycleCommandChanged =
                previousGoal.goalStatus != goal.goalStatus
            val assessmentCommandChanged =
                previousGoal.valueImportance != goal.valueImportance ||
                    previousGoal.valueImpact != goal.valueImpact ||
                    previousGoal.scoringStatus != goal.scoringStatus

            if (
                !subjectCommandChanged &&
                !lifecycleCommandChanged &&
                !assessmentCommandChanged
            ) {
                return
            }

            val mapping =
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.GOAL.name,
                    goal.id,
                ) ?: return

            // A MATERIALIZED shadow has not crossed the write-authority boundary.
            if (mapping.state != LegacySubjectMappingState.CUT_OVER.name) return

            require(!mapping.isDeleted) {
                "CUT_OVER Goal ${goal.id} canonical mapping is deleted"
            }

            val projection =
                goal.toEffectiveOrientation(
                    LegacySubjectIdResolver { source ->
                        require(
                            source.sourceType == LegacyOrientationSourceType.GOAL &&
                                source.sourceId == goal.id,
                        ) {
                            "Goal projection requested an unexpected legacy source"
                        }
                        mapping.subjectId
                    },
                )

            require(!projection.subject.isDeleted) {
                "Live Goal update produced a deleted canonical projection"
            }

            val subject =
                requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)) {
                    "CUT_OVER Goal ${goal.id} is missing canonical subject ${mapping.subjectId}"
                }
            require(
                subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                    !subject.isDeleted,
            ) {
                "CUT_OVER Goal ${goal.id} canonical subject is not an active Orientation"
            }

            val orientations =
                orientationDao.getAllOrientations()
                    .filter { it.subjectId == mapping.subjectId }
            require(orientations.size == 1) {
                "CUT_OVER Goal ${goal.id} must have exactly one canonical Orientation"
            }
            val orientation = orientations.single()
            require(
                orientation.kind == OrientationKind.GOAL.name &&
                    projection.orientation.kind == OrientationKind.GOAL,
            ) {
                "CUT_OVER Goal ${goal.id} canonical Orientation kind is not GOAL"
            }

            val currents =
                orientationDao.getAllAssessments()
                    .filter { it.orientationId == mapping.subjectId }
            require(currents.size == 1) {
                "CUT_OVER Goal ${goal.id} must have exactly one current assessment"
            }
            val current = currents.single()
            require(!current.isDeleted) {
                "CUT_OVER Goal ${goal.id} current assessment is deleted"
            }

            val currentRevision =
                orientationDao.getAllAssessmentRevisions()
                    .filter { it.id == current.revisionId }
            require(currentRevision.size == 1) {
                "CUT_OVER Goal ${goal.id} current assessment revision is missing or duplicated"
            }
            val revision = currentRevision.single()
            require(
                !revision.isDeleted &&
                    revision.orientationId == mapping.subjectId,
            ) {
                "CUT_OVER Goal ${goal.id} current assessment references an invalid revision"
            }

            val revisionAssessment =
                requireNotNull(
                    runCatching {
                        gson.fromJson(
                            revision.assessmentJson,
                            OrientationAssessment::class.java,
                        )
                    }.getOrElse {
                        throw IllegalArgumentException(
                            "CUT_OVER Goal ${goal.id} current revision assessment is invalid",
                            it,
                        )
                    },
                )
            val currentAssessment = current.toModel()
            require(currentAssessment == revisionAssessment) {
                "CUT_OVER Goal ${goal.id} current assessment diverges from its immutable revision"
            }

            // Goal compatibility owns only legacy importance/impact projection.
            // Preserve canonical-only axes such as breadth, expected span,
            // target window, attention tier, commitment and confidence.
            val targetAssessment =
                if (assessmentCommandChanged) {
                    currentAssessment.copy(
                        importance = projection.orientation.assessment.importance,
                        impact = projection.orientation.assessment.impact,
                    )
                } else {
                    currentAssessment
                }

            require(
                validateOrientationAssessment(
                    OrientationKind.GOAL,
                    targetAssessment,
                ).isEmpty(),
            ) {
                "Goal canonical assessment violates DOMAIN-CONTRACT v1"
            }

            val subjectChanged =
                subjectCommandChanged &&
                    (
                        subject.title != projection.subject.title ||
                            subject.description != projection.subject.description
                    )
            val orientationChanged =
                lifecycleCommandChanged &&
                    (
                        orientation.lifecycle != projection.orientation.lifecycle?.name ||
                            orientation.lifecycleOrigin !=
                            projection.orientation.lifecycleOrigin.name
                    )
            val assessmentChanged =
                assessmentCommandChanged &&
                    currentAssessment != targetAssessment

            if (!subjectChanged && !orientationChanged && !assessmentChanged) return

            val now = goal.updatedAt ?: System.currentTimeMillis()

            // ManagedSubject is the aggregate freshness marker for shared
            // semantic Goal/Orientation mutations.
            orientationDao.upsertManagedSubjects(
                listOf(
                    subject.copy(
                        title =
                            if (subjectCommandChanged) {
                                projection.subject.title
                            } else {
                                subject.title
                            },
                        description =
                            if (subjectCommandChanged) {
                                projection.subject.description
                            } else {
                                subject.description
                            },
                        updatedAt = now,
                        syncedAt = null,
                        version = subject.version + 1L,
                    ),
                ),
            )

            if (orientationChanged) {
                orientationDao.upsertOrientations(
                    listOf(
                        orientation.copy(
                            lifecycle = projection.orientation.lifecycle?.name,
                            lifecycleOrigin =
                                projection.orientation.lifecycleOrigin.name,
                        ),
                    ),
                )
            }

            if (assessmentChanged) {
                val revisionId = UUID.randomUUID().toString()
                orientationDao.upsertAssessmentRevisions(
                    listOf(
                        OrientationAssessmentRevisionEntity(
                            id = revisionId,
                            orientationId = mapping.subjectId,
                            effectiveFrom = now,
                            recordedAt = now,
                            source = AssessmentRevisionSource.USER.name,
                            reason = GOAL_WRITE_THROUGH_REASON,
                            assessmentJson = gson.toJson(targetAssessment),
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )
                orientationDao.upsertAssessments(
                    listOf(
                        current.withAssessment(
                            assessment = targetAssessment,
                            revisionId = revisionId,
                            now = now,
                            gson = gson,
                        ),
                    ),
                )
            }
        }

    }

private fun OrientationAssessmentEntity.toModel() =
    OrientationAssessment(
        importance = AxisAssessment(importanceValue, ValueOrigin.valueOf(importanceOrigin)),
        impact = AxisAssessment(impactValue, ValueOrigin.valueOf(impactOrigin)),
        breadth = AxisAssessment(breadthValue, ValueOrigin.valueOf(breadthOrigin)),
        expectedSpan = AxisAssessment(expectedSpanValue, ValueOrigin.valueOf(expectedSpanOrigin)),
        targetWindow = AxisAssessment(targetWindowValue, ValueOrigin.valueOf(targetWindowOrigin)),
        attentionTier = AxisAssessment(attentionTierValue, ValueOrigin.valueOf(attentionTierOrigin)),
        commitment = AxisAssessment(commitmentValue, ValueOrigin.valueOf(commitmentOrigin)),
        confidence = AxisAssessment(confidenceValue, ValueOrigin.valueOf(confidenceOrigin)),
    )

private fun OrientationAssessmentEntity.withAssessment(
    assessment: OrientationAssessment,
    revisionId: String,
    now: Long,
    gson: Gson,
) = copy(
    revisionId = revisionId,
    importanceValue = assessment.importance.valueCode,
    importanceOrigin = assessment.importance.origin.name,
    impactValue = assessment.impact.valueCode,
    impactOrigin = assessment.impact.origin.name,
    breadthValue = assessment.breadth.valueCode,
    breadthOrigin = assessment.breadth.origin.name,
    expectedSpanValue = assessment.expectedSpan.valueCode,
    expectedSpanOrigin = assessment.expectedSpan.origin.name,
    targetWindowValue = assessment.targetWindow.valueCode,
    targetWindowOrigin = assessment.targetWindow.origin.name,
    attentionTierValue = assessment.attentionTier.valueCode,
    attentionTierOrigin = assessment.attentionTier.origin.name,
    commitmentValue = assessment.commitment.valueCode,
    commitmentOrigin = assessment.commitment.origin.name,
    confidenceValue = assessment.confidence.valueCode,
    confidenceOrigin = assessment.confidence.origin.name,
    provenanceJson =
        gson.toJson(
            listOf(
                assessment.importance,
                assessment.impact,
                assessment.breadth,
                assessment.expectedSpan,
                assessment.targetWindow,
                assessment.attentionTier,
                assessment.commitment,
                assessment.confidence,
            ).mapNotNull(AxisAssessment::provenance),
        ),
    updatedAt = now,
    syncedAt = null,
    isDeleted = false,
    version = version + 1L,
)

private const val GOAL_WRITE_THROUGH_REASON = "Goal compatibility write-through"

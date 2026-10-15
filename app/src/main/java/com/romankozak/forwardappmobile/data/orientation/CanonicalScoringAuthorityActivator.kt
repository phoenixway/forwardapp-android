package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationScoringOverride
import javax.inject.Inject
import javax.inject.Singleton

data class CanonicalScoringActivationReport(
    val performed: Boolean,
    val orientations: Int,
)

/** Finite, fail-closed cutover from Goal/Context scoring copies to canonical Orientation scoring. */
@Singleton
class CanonicalScoringAuthorityActivator
    @Inject
    constructor(
        private val database: AppDatabase,
        private val orientationDao: OrientationDao,
        private val goalDao: GoalDao,
        private val contextDao: ContextDao,
        private val markerDao: CanonicalScoringAuthorityStateDao,
    ) {
        private val gson = Gson()

        suspend fun ensureActivated(now: Long = System.currentTimeMillis()): CanonicalScoringActivationReport =
            database.withTransaction {
                markerDao.get()?.let { marker ->
                    require(marker.version == CURRENT_VERSION) {
                        "Unsupported canonical scoring authority marker version ${marker.version}"
                    }
                    // Marker-last activation already proved the complete set.
                    // Ordinary startup must not rescan every Orientation or
                    // re-project legacy Goal/Context scoring compatibility.
                    return@withTransaction CanonicalScoringActivationReport(false, 0)
                }

                // Current canonical Restore carries scoring policy records but
                // intentionally does not transport the local activation marker.
                // Adopt only a complete, internally valid canonical set; a
                // partial set is corruption and must never fall back to legacy.
                if (orientationDao.getAllScoringOverrides().isNotEmpty()) {
                    val restored = validateEstablishedState(projectCompatibility = true)
                    markerDao.upsert(
                        CanonicalScoringAuthorityStateEntity(
                            version = CURRENT_VERSION,
                            activatedAt = now,
                        ),
                    )
                    return@withTransaction CanonicalScoringActivationReport(true, restored)
                }

                val orientationsById = orientationDao.getAllOrientations().associateBy { it.subjectId }
                val subjectsById = orientationDao.getAllManagedSubjects().associateBy { it.id }
                val assessmentsById = orientationDao.getAllAssessments().associateBy { it.orientationId }
                val revisionsById = orientationDao.getAllAssessmentRevisions().associateBy { it.id }
                var migrated = 0

                orientationDao.getAllLegacyMappings()
                    .filter {
                        !it.isDeleted && it.state == LegacySubjectMappingState.CUT_OVER.name &&
                            (it.sourceType == LegacyOrientationSourceType.GOAL.name ||
                                it.sourceType == LegacyOrientationSourceType.CONTEXT.name)
                    }
                    .sortedWith(compareBy({ it.sourceType }, { it.sourceId }))
                    .forEach { mapping ->
                        val subject = requireNotNull(subjectsById[mapping.subjectId]) {
                            "CUT_OVER ${mapping.sourceType}:${mapping.sourceId} is missing subject ${mapping.subjectId}"
                        }
                        if (subject.subjectType == ManagedSubjectType.ASPECT.name) return@forEach
                        require(subject.subjectType == ManagedSubjectType.ORIENTATION.name && !subject.isDeleted) {
                            "CUT_OVER ${mapping.sourceType}:${mapping.sourceId} does not target a live Orientation"
                        }
                        val orientation = requireNotNull(orientationsById[mapping.subjectId]) {
                            "CUT_OVER ${mapping.sourceType}:${mapping.sourceId} is missing its Orientation"
                        }
                        val current = requireNotNull(assessmentsById[mapping.subjectId]) {
                            "CUT_OVER ${mapping.sourceType}:${mapping.sourceId} is missing its assessment"
                        }
                        require(!current.isDeleted) { "Live scoring target has a deleted assessment" }
                        val currentRevision = requireNotNull(revisionsById[current.revisionId]) {
                            "Current assessment ${current.orientationId} is missing revision ${current.revisionId}"
                        }
                        require(
                            !currentRevision.isDeleted &&
                                currentRevision.orientationId == current.orientationId,
                        ) { "Current assessment ${current.orientationId} diverges from its immutable revision" }

                        // Current columns contain value/origin, while the immutable
                        // revision also carries per-axis provenance. Verify every
                        // persisted assessment field without discarding provenance.
                        val revisionAssessment =
                            decodeOrientationAssessmentJson(gson, currentRevision.assessmentJson)
                        require(current.matchesAssessment(revisionAssessment)) {
                            "Current assessment ${current.orientationId} diverges from its immutable revision"
                        }

                        val input = legacyInput(mapping.sourceType, mapping.sourceId)
                        val projection =
                            if (input != null) {
                                projectLegacyScoringToCanonical(
                                    current = revisionAssessment,
                                    source = LegacySubjectRef(LegacyOrientationSourceType.valueOf(mapping.sourceType), mapping.sourceId),
                                    input = input,
                                    preserveCanonicalImportanceImpact = true,
                                )
                            } else {
                                CanonicalScoringProjection(
                                    assessment = revisionAssessment,
                                    scoringOverride = OrientationScoringOverride(),
                                )
                            }
                        val kind = OrientationKind.valueOf(orientation.kind)
                        require(validateOrientationAssessment(kind, projection.assessment).isEmpty()) {
                            "Canonical scoring migration violates assessment contract for ${mapping.subjectId}"
                        }

                        var storedAssessment = current
                        if (projection.assessment != revisionAssessment) {
                            val revisionId =
                                LegacySubjectUuid.uuidV5(
                                    REVISION_NAMESPACE,
                                    "SCORING:${mapping.subjectId}:$CURRENT_VERSION",
                                ).toString()
                            val existingRevision = revisionsById[revisionId]
                            if (existingRevision == null) {
                                orientationDao.upsertAssessmentRevisions(
                                    listOf(
                                        scoringAssessmentRevision(
                                            id = revisionId,
                                            orientationId = mapping.subjectId,
                                            assessment = projection.assessment,
                                            now = now,
                                            reason = MIGRATION_REASON,
                                            source = AssessmentRevisionSource.MIGRATION,
                                            gson = gson,
                                        ),
                                    ),
                                )
                            } else {
                                require(
                                    decodeOrientationAssessmentJson(gson, existingRevision.assessmentJson) ==
                                        projection.assessment,
                                ) { "Canonical scoring migration revision collision for ${mapping.subjectId}" }
                            }
                            storedAssessment = current.withCanonicalAssessment(projection.assessment, revisionId, now, gson)
                            orientationDao.upsertAssessments(listOf(storedAssessment))
                            orientationDao.upsertManagedSubjects(
                                listOf(subject.copy(updatedAt = now, syncedAt = null, version = subject.version + 1L)),
                            )
                        }

                        require(orientationDao.getScoringOverride(mapping.subjectId) == null) {
                            "Pre-activation scoring override already exists for ${mapping.subjectId}"
                        }
                        val override =
                            scoringOverrideEntity(
                                orientationId = mapping.subjectId,
                                override = projection.scoringOverride,
                                current = null,
                                now = now,
                            )
                        orientationDao.upsertScoringOverrides(listOf(override))
                        projectCompatibility(mapping.sourceType, mapping.sourceId, canonicalEvaluation(storedAssessment, override))
                        migrated += 1
                    }

                require(validateEstablishedState(projectCompatibility = false) == migrated)
                markerDao.upsert(CanonicalScoringAuthorityStateEntity(version = CURRENT_VERSION, activatedAt = now))
                CanonicalScoringActivationReport(true, migrated)
            }

        /**
         * Canonical Merge admission, never an ordinary startup operation.
         * Invoke inside the transaction that writes the incoming graph.
         * A durable marker proves the previous graph, not the merged graph.
         */
        suspend fun admitMergedState(
            now: Long = System.currentTimeMillis(),
        ): CanonicalScoringActivationReport =
            database.withTransaction {
                val marker = markerDao.get()
                if (marker == null) {
                    return@withTransaction ensureActivated(now)
                }
                require(marker.version == CURRENT_VERSION) {
                    "Unsupported canonical scoring authority marker version ${marker.version}"
                }
                val admitted = validateEstablishedState(projectCompatibility = true)
                CanonicalScoringActivationReport(
                    performed = false,
                    orientations = admitted,
                )
            }

        suspend fun requireActivated() {
            val marker = requireNotNull(markerDao.get()) { "Canonical scoring authority is not activated" }
            require(marker.version == CURRENT_VERSION)
        }

        /** Establishes scoring authority synchronously for a Goal created after global activation. */
        suspend fun establishForNewGoal(
            goal: Goal,
            now: Long = goal.updatedAt ?: goal.createdAt,
        ) {
            if (markerDao.get() == null) {
                ensureActivated(now)
                return
            }
            database.withTransaction {
                requireActivated()
                val mapping =
                    requireNotNull(
                        orientationDao.getLegacyMapping(LegacyOrientationSourceType.GOAL.name, goal.id),
                    ) { "New Goal ${goal.id} is missing its canonical mapping" }
                require(mapping.state == LegacySubjectMappingState.CUT_OVER.name && !mapping.isDeleted)
                val orientation =
                    requireNotNull(
                        orientationDao.getAllOrientations().singleOrNull { it.subjectId == mapping.subjectId },
                    )
                require(orientation.kind == OrientationKind.GOAL.name)
                val current =
                    requireNotNull(
                        orientationDao.getAllAssessments().singleOrNull { it.orientationId == mapping.subjectId },
                    )
                orientationDao.getScoringOverride(mapping.subjectId)?.let { existing ->
                    projectCompatibility(
                        LegacyOrientationSourceType.GOAL.name,
                        goal.id,
                        canonicalEvaluation(current, existing),
                    )
                    return@withTransaction
                }
                val projection =
                    projectLegacyScoringToCanonical(
                        current = current.toCanonicalAssessment(),
                        source = LegacySubjectRef(LegacyOrientationSourceType.GOAL, goal.id),
                        input = goal.toLegacyScoringInput(),
                        preserveCanonicalImportanceImpact = false,
                    )
                require(validateOrientationAssessment(OrientationKind.GOAL, projection.assessment).isEmpty())
                val storedAssessment =
                    if (projection.assessment != current.toCanonicalAssessment()) {
                        val revisionId =
                            LegacySubjectUuid.uuidV5(
                                REVISION_NAMESPACE,
                                "SCORING:${mapping.subjectId}:CREATE",
                            ).toString()
                        orientationDao.upsertAssessmentRevisions(
                            listOf(
                                scoringAssessmentRevision(
                                    id = revisionId,
                                    orientationId = mapping.subjectId,
                                    assessment = projection.assessment,
                                    now = now,
                                    reason = CREATION_REASON,
                                    source = AssessmentRevisionSource.USER,
                                    gson = gson,
                                ),
                            ),
                        )
                        current.withCanonicalAssessment(projection.assessment, revisionId, now, gson)
                            .also { orientationDao.upsertAssessments(listOf(it)) }
                    } else {
                        current
                    }
                val override =
                    scoringOverrideEntity(mapping.subjectId, projection.scoringOverride, null, now)
                orientationDao.upsertScoringOverrides(listOf(override))
                projectCompatibility(
                    LegacyOrientationSourceType.GOAL.name,
                    goal.id,
                    canonicalEvaluation(storedAssessment, override),
                )
            }
        }

        private suspend fun validateEstablishedState(projectCompatibility: Boolean): Int {
            val subjects = orientationDao.getAllManagedSubjects().associateBy { it.id }
            val orientations = orientationDao.getAllOrientations().associateBy { it.subjectId }
            val assessments = orientationDao.getAllAssessments().associateBy { it.orientationId }
            val revisions = orientationDao.getAllAssessmentRevisions().associateBy { it.id }
            var count = 0
            orientationDao.getAllLegacyMappings()
                .filter {
                    !it.isDeleted && it.state == LegacySubjectMappingState.CUT_OVER.name &&
                        (it.sourceType == LegacyOrientationSourceType.GOAL.name ||
                            it.sourceType == LegacyOrientationSourceType.CONTEXT.name)
                }
                .forEach { mapping ->
                    val subject = requireNotNull(subjects[mapping.subjectId])
                    if (subject.subjectType == ManagedSubjectType.ASPECT.name) return@forEach
                    require(subject.subjectType == ManagedSubjectType.ORIENTATION.name && !subject.isDeleted)
                    val orientation = requireNotNull(orientations[mapping.subjectId])
                    val assessment = requireNotNull(assessments[mapping.subjectId])
                    require(!assessment.isDeleted) {
                        "Activated scoring target ${mapping.subjectId} has a deleted assessment"
                    }
                    val revision = requireNotNull(revisions[assessment.revisionId]) {
                        "Activated scoring target ${mapping.subjectId} is missing current revision"
                    }
                    require(!revision.isDeleted && revision.orientationId == mapping.subjectId) {
                        "Activated scoring target ${mapping.subjectId} has an invalid current revision"
                    }
                    val revisionAssessment = decodeOrientationAssessmentJson(gson, revision.assessmentJson)
                    require(assessment.matchesAssessment(revisionAssessment)) {
                        "Activated scoring target ${mapping.subjectId} diverges from its immutable revision"
                    }
                    require(
                        validateOrientationAssessment(
                            OrientationKind.valueOf(orientation.kind),
                            revisionAssessment,
                        ).isEmpty(),
                    ) {
                        "Activated scoring target ${mapping.subjectId} violates assessment contract"
                    }
                    val override = requireNotNull(orientationDao.getScoringOverride(mapping.subjectId)) {
                        "Activated scoring target ${mapping.subjectId} is missing its policy/override record"
                    }
                    val evaluation = canonicalEvaluation(assessment, override)
                    if (projectCompatibility) projectCompatibility(mapping.sourceType, mapping.sourceId, evaluation)
                    count += 1
                }
            return count
        }

        private suspend fun legacyInput(sourceType: String, sourceId: String): LegacyScoringInput? =
            when (LegacyOrientationSourceType.valueOf(sourceType)) {
                LegacyOrientationSourceType.GOAL ->
                    requireNotNull(goalDao.getGoalById(sourceId)) {
                        "CUT_OVER Goal $sourceId is missing its compatibility row"
                    }.toLegacyScoringInput()
                LegacyOrientationSourceType.CONTEXT -> contextDao.getContextById(sourceId)?.toLegacyScoringInput()
                else -> error("Unsupported scoring source $sourceType")
            }

        private suspend fun projectCompatibility(
            sourceType: String,
            sourceId: String,
            evaluation: CanonicalOrientationEvaluation,
        ) {
            val value = evaluation.toLegacyProjection()
            when (LegacyOrientationSourceType.valueOf(sourceType)) {
                LegacyOrientationSourceType.GOAL -> goalDao.projectCanonicalScoring(sourceId, value.importance, value.impact, value.effort, value.cost, value.risk, value.effortWeight, value.costWeight, value.riskWeight, value.rawScore, value.displayScore, value.scoringStatus)
                LegacyOrientationSourceType.CONTEXT -> contextDao.projectCanonicalScoring(sourceId, value.importance, value.impact, value.effort, value.cost, value.risk, value.effortWeight, value.costWeight, value.riskWeight, value.rawScore, value.displayScore, value.scoringStatus)
                else -> error("Unsupported scoring source $sourceType")
            }
        }

        companion object {
            const val CURRENT_VERSION = 1
            private const val MIGRATION_REASON = "canonical-scoring-authority-cutover-v1"
            private const val CREATION_REASON = "canonical-scoring-authority-goal-create"
            private val REVISION_NAMESPACE = java.util.UUID.fromString("3f08d3db-3176-58e3-a2df-9dd7a05f5276")
        }
    }

private fun Goal.toLegacyScoringInput() =
    LegacyScoringInput(valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, scoringStatus)

private fun Context.toLegacyScoringInput() =
    LegacyScoringInput(valueImportance, valueImpact, effort, cost, risk, weightEffort, weightCost, weightRisk, scoringStatus)

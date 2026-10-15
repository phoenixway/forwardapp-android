package com.romankozak.forwardappmobile.data.orientation

import java.util.UUID
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private data class ContextOrientationAssessmentState(
    val subject: ManagedSubjectEntity,
    val orientation: OrientationEntity,
    val current: OrientationAssessmentEntity,
    val assessment: OrientationAssessment,
)

data class CanonicalOrientationSemantics(
    val subject: ManagedSubject,
    val kind: OrientationKind,
)

/** Canonical write boundary. Legacy repositories remain unchanged until a later cutover phase. */
@Singleton
class CanonicalOrientationRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: OrientationDao,
    ) {
        private val gson = Gson()

        fun observeSubjects(): Flow<List<ManagedSubjectEntity>> = dao.observeManagedSubjects()

        /**
         * Small canonical core read for identity editors and other single-entity
         * surfaces. Unlike the shared product graph, this cannot replay a frame
         * captured immediately before the entity's creation transaction.
         */
        suspend fun getOrientationSemantics(subjectId: String): CanonicalOrientationSemantics? =
            database.withTransaction {
                val subject = dao.getManagedSubject(subjectId) ?: return@withTransaction null
                if (subject.isDeleted) return@withTransaction null
                val orientations = dao.getAllOrientations().filter { it.subjectId == subjectId }
                requireCompleteActiveOrientationAggregate(
                    subjectId = subjectId,
                    subject = subject,
                    orientationNodes = orientations,
                    currents = dao.getAllAssessments().filter { it.orientationId == subjectId },
                    revisions = dao.getAllAssessmentRevisions(),
                )
                CanonicalOrientationSemantics(
                    subject =
                        ManagedSubject(
                            id = subject.id,
                            createdAt = subject.createdAt,
                            updatedAt = subject.updatedAt,
                            syncedAt = subject.syncedAt,
                            isDeleted = subject.isDeleted,
                            version = subject.version,
                            subjectType = ManagedSubjectType.ORIENTATION,
                            title = subject.title,
                            description = subject.description,
                        ),
                    kind = OrientationKind.valueOf(orientations.single().kind),
                )
            }

        suspend fun saveOrientation(
            subject: ManagedSubject,
            orientation: OrientationNode,
            revision: OrientationAssessmentRevision,
        ) {
            require(subject.subjectType == ManagedSubjectType.ORIENTATION)
            require(subject.id == orientation.subjectId)
            require(revision.orientationId == orientation.subjectId)
            require(revision.assessment == orientation.assessment)
            require(validateOrientationAssessment(orientation.kind, revision.assessment).isEmpty()) {
                "Orientation assessment violates DOMAIN-CONTRACT v1"
            }
            database.withTransaction {
                dao.upsertManagedSubjects(listOf(subject.toEntity()))
                dao.upsertOrientations(listOf(orientation.toEntity()))
                dao.upsertAssessmentRevisions(listOf(revision.toEntity(gson)))
                dao.upsertAssessments(listOf(revision.toCurrentEntity(gson)))
            }
        }

        /**
         * Fresh canonical Orientation authoring.
         *
         * Unlike ensureOrientationWithId(), this is not a migration/adoption
         * seam. It creates a new independently owned semantic Orientation with
         * a random identity and USER-authored initial assessment revision.
         */
        suspend fun createOrientation(
            kind: OrientationKind,
            title: String,
            description: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) {
                "Orientation title must not be blank"
            }
            val normalizedDescription =
                description?.trim()?.takeIf { it.isNotEmpty() }
            val id = UUID.randomUUID().toString()
            val assessment = initialOrientationAssessment(kind)
            val revision =
                OrientationAssessmentRevision(
                    id = UUID.randomUUID().toString(),
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    orientationId = id,
                    effectiveFrom = now,
                    recordedAt = now,
                    source = AssessmentRevisionSource.USER,
                    reason = "Canonical Orientation created",
                    assessment = assessment,
                )

            saveOrientation(
                subject =
                    ManagedSubject(
                        id = id,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        subjectType = ManagedSubjectType.ORIENTATION,
                        title = normalizedTitle,
                        description = normalizedDescription,
                    ),
                orientation =
                    OrientationNode(
                        subjectId = id,
                        kind = kind,
                        lifecycle = null,
                        lifecycleOrigin = ValueOrigin.UNSET,
                        assessment = assessment,
                    ),
                revision = revision,
            )
            return id
        }

        /**
         * Canonical semantic edit for an existing Orientation.
         *
         * Title/description belong to ManagedSubject. Editing them therefore
         * bumps the canonical subject version without manufacturing a new
         * assessment revision.
         */
        suspend fun updateOrientationSemantics(
            subjectId: String,
            title: String,
            description: String?,
            expectedVersion: Long? = null,
            now: Long = System.currentTimeMillis(),
        ) {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) {
                "Orientation title must not be blank"
            }
            val normalizedDescription =
                description?.trim()?.takeIf { it.isNotEmpty() }

            database.withTransaction {
                requireCompleteActiveOrientationAggregate(subjectId)

                val subject =
                    requireNotNull(dao.getManagedSubject(subjectId)) {
                        "Existing Orientation subject does not exist"
                    }
                require(expectedVersion == null || subject.version == expectedVersion) {
                    "Orientation changed after the editor loaded it"
                }

                if (
                    subject.title == normalizedTitle &&
                    subject.description == normalizedDescription
                ) {
                    return@withTransaction
                }

                dao.upsertManagedSubjects(
                    listOf(
                        subject.copy(
                            title = normalizedTitle,
                            description = normalizedDescription,
                            updatedAt = now,
                            syncedAt = null,
                            version = subject.version + 1L,
                        ),
                    ),
                )
            }
        }

        /**
         * Migration-only deterministic identity creation.
         *
         * A fresh id creates the complete canonical Orientation aggregate.
         * If anything already occupies the id, the pre-existing aggregate must
         * exactly match the initial migration-owned shape or creation fails
         * closed. This prevents blind upsert from adopting or rewriting an
         * unrelated canonical subject.
         */
        suspend fun ensureOrientationWithId(
            id: String,
            kind: OrientationKind,
            title: String,
            description: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val normalizedTitle = title.trim()
                require(normalizedTitle.isNotEmpty()) {
                    "Orientation title must not be blank"
                }
                val normalizedDescription =
                    description?.trim()?.takeIf { it.isNotEmpty() }
                val expectedAssessment = initialOrientationAssessment(kind)

                val existingSubject = dao.getManagedSubject(id)
                val existingOrientation =
                    dao.getAllOrientations().firstOrNull { it.subjectId == id }
                val existingAssessment =
                    dao.getAllAssessments().firstOrNull { it.orientationId == id }
                val existingRevisions =
                    dao.getAllAssessmentRevisions().filter {
                        it.orientationId == id
                    }

                val hasAnyExistingState =
                    existingSubject != null ||
                        existingOrientation != null ||
                        existingAssessment != null ||
                        existingRevisions.isNotEmpty()

                if (hasAnyExistingState) {
                    val subject =
                        requireNotNull(existingSubject) {
                            "Canonical Orientation identity is partially materialized"
                        }
                    val orientation =
                        requireNotNull(existingOrientation) {
                            "Canonical Orientation node is missing"
                        }
                    val current =
                        requireNotNull(existingAssessment) {
                            "Canonical Orientation current assessment is missing"
                        }

                    require(
                        subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                            !subject.isDeleted,
                    ) {
                        "Canonical Orientation id is occupied by a different subject"
                    }
                    require(
                        subject.title == normalizedTitle &&
                            subject.description == normalizedDescription,
                    ) {
                        "Canonical Orientation id already has different semantic content"
                    }
                    require(
                        orientation.kind == kind.name &&
                            orientation.lifecycle == null &&
                            orientation.lifecycleOrigin == ValueOrigin.UNSET.name,
                    ) {
                        "Canonical Orientation id already has a different Orientation shape"
                    }
                    require(
                        !current.isDeleted &&
                            current.matchesAssessment(expectedAssessment),
                    ) {
                        "Canonical Orientation id already has a different initial assessment"
                    }

                    val currentRevision =
                        existingRevisions.firstOrNull {
                            it.id == current.revisionId
                        }
                    requireNotNull(currentRevision) {
                        "Canonical Orientation current revision is missing"
                    }
                    require(
                        !currentRevision.isDeleted &&
                            currentRevision.source == AssessmentRevisionSource.MIGRATION.name &&
                            currentRevision.reason == CONTEXT_MIGRATION_INITIAL_REASON &&
                            decodeOrientationAssessmentJson(
                                gson,
                                currentRevision.assessmentJson,
                            ) == expectedAssessment,
                    ) {
                        "Canonical Orientation id is not the migration-owned initial aggregate"
                    }

                    return@withTransaction id
                }

                val revision =
                    OrientationAssessmentRevision(
                        id = UUID.randomUUID().toString(),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        orientationId = id,
                        effectiveFrom = now,
                        recordedAt = now,
                        source = AssessmentRevisionSource.MIGRATION,
                        reason = CONTEXT_MIGRATION_INITIAL_REASON,
                        assessment = expectedAssessment,
                    )

                dao.upsertManagedSubjects(
                    listOf(
                        ManagedSubject(
                            id = id,
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            subjectType = ManagedSubjectType.ORIENTATION,
                            title = normalizedTitle,
                            description = normalizedDescription,
                        ).toEntity(),
                    ),
                )
                dao.upsertOrientations(
                    listOf(
                        OrientationNode(
                            subjectId = id,
                            kind = kind,
                            lifecycle = null,
                            lifecycleOrigin = ValueOrigin.UNSET,
                            assessment = expectedAssessment,
                        ).toEntity(),
                    ),
                )
                dao.upsertAssessmentRevisions(
                    listOf(revision.toEntity(gson)),
                )
                dao.upsertAssessments(
                    listOf(revision.toCurrentEntity(gson)),
                )

                id
            }

        /**
         * Read-only adoption gate for an independently owned canonical
         * Orientation. It verifies the complete live aggregate without
         * imposing migration-specific revision provenance.
         */
        suspend fun requireCompleteActiveOrientationAggregate(subjectId: String) {
            requireCompleteActiveOrientationAggregate(
                subjectId = subjectId,
                subject = dao.getManagedSubject(subjectId),
                orientationNodes =
                    dao.getAllOrientations().filter { it.subjectId == subjectId },
                currents =
                    dao.getAllAssessments().filter { it.orientationId == subjectId },
                revisions = dao.getAllAssessmentRevisions(),
            )
        }

        /**
         * The same canonical aggregate gate over an already-loaded read snapshot.
         *
         * This exists so migration review can validate many adoption candidates
         * without re-reading whole Room tables for every candidate. It is still
         * the canonical validation owner and performs no writes.
         */
        internal fun requireCompleteActiveOrientationAggregate(
            subjectId: String,
            subject: ManagedSubjectEntity?,
            orientationNodes: List<OrientationEntity>,
            currents: List<OrientationAssessmentEntity>,
            revisions: List<OrientationAssessmentRevisionEntity>,
        ) {
            val requiredSubject =
                requireNotNull(subject) {
                    "Existing Orientation subject does not exist"
                }
            require(
                requiredSubject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                    !requiredSubject.isDeleted,
            ) {
                "Existing Orientation subject is not active"
            }

            require(orientationNodes.size == 1) {
                "Existing Orientation aggregate must contain exactly one Orientation node"
            }
            val kind =
                runCatching { OrientationKind.valueOf(orientationNodes.single().kind) }
                    .getOrElse {
                        throw IllegalArgumentException("Existing Orientation kind is invalid", it)
                    }

            require(currents.size == 1) {
                "Existing Orientation aggregate must contain exactly one current assessment"
            }
            val current = currents.single()
            require(!current.isDeleted) {
                "Existing Orientation current assessment is deleted"
            }

            val matchingRevisions =
                revisions.filter { it.id == current.revisionId }
            require(matchingRevisions.size == 1) {
                "Existing Orientation current assessment revision is missing or duplicated"
            }
            val revision = matchingRevisions.single()
            require(!revision.isDeleted && revision.orientationId == subjectId) {
                "Existing Orientation current assessment references an invalid revision"
            }
            val revisionAssessment =
                requireNotNull(
                    runCatching {
                        decodeOrientationAssessmentJson(gson, revision.assessmentJson)
                    }.getOrElse {
                        throw IllegalArgumentException(
                            "Existing Orientation revision assessment is invalid",
                            it,
                        )
                    },
                ) {
                    "Existing Orientation revision assessment is missing"
                }
            require(current.matchesAssessment(revisionAssessment)) {
                "Existing Orientation current assessment does not match its immutable revision"
            }
            // The field-for-field equality above means this single domain
            // validation proves both the immutable revision and current row.
            require(validateOrientationAssessment(kind, revisionAssessment).isEmpty()) {
                "Existing Orientation assessment violates DOMAIN-CONTRACT v1"
            }
        }

        suspend fun getContextAssessment(
            contextId: String,
        ): OrientationAssessment? =
            database.withTransaction {
                loadCutOverContextOrientationAssessment(contextId)?.assessment
            }

        suspend fun getContextEvaluation(
            contextId: String,
        ): CanonicalOrientationEvaluation? =
            database.withTransaction {
                val state = loadCutOverContextOrientationAssessment(contextId) ?: return@withTransaction null
                val override = requireNotNull(dao.getScoringOverride(state.subject.id)) {
                    "CUT_OVER Context $contextId is missing canonical scoring policy state"
                }
                canonicalEvaluation(state.current, override)
            }

        suspend fun updateContextEvaluation(
            contextId: String,
            importance: Float,
            impact: Float,
            effort: Float,
            cost: Float,
            risk: Float,
            effortWeight: Float,
            costWeight: Float,
            riskWeight: Float,
            scoringStatus: String,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            database.withTransaction {
                val state = loadCutOverContextOrientationAssessment(contextId) ?: return@withTransaction false
                val currentOverride = requireNotNull(dao.getScoringOverride(state.subject.id)) {
                    "CUT_OVER Context $contextId is missing canonical scoring policy state"
                }
                val projection =
                    projectLegacyScoringToCanonical(
                        current = state.assessment,
                        source =
                            com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef(
                                LegacyOrientationSourceType.CONTEXT,
                                contextId,
                            ),
                        input =
                            LegacyScoringInput(
                                importance,
                                impact,
                                effort,
                                cost,
                                risk,
                                effortWeight,
                                costWeight,
                                riskWeight,
                                scoringStatus,
                            ),
                        preserveCanonicalImportanceImpact = false,
                    )
                val kind = OrientationKind.valueOf(state.orientation.kind)
                require(validateOrientationAssessment(kind, projection.assessment).isEmpty()) {
                    "Context canonical assessment violates DOMAIN-CONTRACT v1"
                }
                val assessmentChanged = projection.assessment != state.assessment
                val overrideChanged = projection.scoringOverride != currentOverride.toModel()
                var storedAssessment = state.current
                if (assessmentChanged) {
                    val revisionId = UUID.randomUUID().toString()
                    dao.upsertAssessmentRevisions(
                        listOf(
                            scoringAssessmentRevision(
                                revisionId,
                                state.subject.id,
                                projection.assessment,
                                now,
                                CONTEXT_SCORING_EDIT_REASON,
                                AssessmentRevisionSource.USER,
                                gson,
                            ),
                        ),
                    )
                    storedAssessment =
                        state.current.withCanonicalAssessment(projection.assessment, revisionId, now, gson)
                    dao.upsertAssessments(listOf(storedAssessment))
                }
                val storedOverride =
                    if (overrideChanged) {
                        scoringOverrideEntity(
                            state.subject.id,
                            projection.scoringOverride,
                            currentOverride,
                            now,
                        ).also { dao.upsertScoringOverrides(listOf(it)) }
                    } else {
                        currentOverride
                    }
                if (assessmentChanged || overrideChanged) {
                    dao.upsertManagedSubjects(
                        listOf(
                            state.subject.copy(
                                updatedAt = now,
                                syncedAt = null,
                                version = state.subject.version + 1L,
                            ),
                        ),
                    )
                }
                val value = canonicalEvaluation(storedAssessment, storedOverride).toLegacyProjection()
                database.contextDao().projectCanonicalScoring(
                    contextId,
                    value.importance,
                    value.impact,
                    value.effort,
                    value.cost,
                    value.risk,
                    value.effortWeight,
                    value.costWeight,
                    value.riskWeight,
                    value.rawScore,
                    value.displayScore,
                    value.scoringStatus,
                )
                true
            }

        suspend fun updateContextAssessmentAxes(
            contextId: String,
            importance: AxisAssessment,
            impact: AxisAssessment,
            now: Long = System.currentTimeMillis(),
        ): Boolean =
            database.withTransaction {
                val state =
                    loadCutOverContextOrientationAssessment(contextId)
                        ?: return@withTransaction false

                val target =
                    state.assessment.copy(
                        importance = importance,
                        impact = impact,
                    )

                val kind =
                    runCatching {
                        OrientationKind.valueOf(state.orientation.kind)
                    }.getOrElse {
                        throw IllegalArgumentException(
                            "Context canonical Orientation kind is invalid",
                            it,
                        )
                    }

                require(validateOrientationAssessment(kind, target).isEmpty()) {
                    "Context canonical assessment violates DOMAIN-CONTRACT v1"
                }

                if (target == state.assessment) {
                    return@withTransaction true
                }

                val revisionId = UUID.randomUUID().toString()

                dao.upsertManagedSubjects(
                    listOf(
                        state.subject.copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = state.subject.version + 1L,
                        ),
                    ),
                )

                val revision =
                    OrientationAssessmentRevision(
                        id = revisionId,
                        orientationId = state.subject.id,
                        effectiveFrom = now,
                        recordedAt = now,
                        source = AssessmentRevisionSource.USER,
                        reason = CONTEXT_ASSESSMENT_EDIT_REASON,
                        assessment = target,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )

                dao.upsertAssessmentRevisions(
                    listOf(revision.toEntity(gson)),
                )
                dao.upsertAssessments(
                    listOf(
                        target.toCurrentEntity(
                            orientationId = state.subject.id,
                            revisionId = revisionId,
                            createdAt = state.current.createdAt,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = state.current.version + 1L,
                            gson = gson,
                        ),
                    ),
                )

                true
            }

        private suspend fun loadCutOverContextOrientationAssessment(
            contextId: String,
        ): ContextOrientationAssessmentState? {
            val mapping =
                dao.getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    contextId,
                ) ?: return null

            if (
                mapping.isDeleted ||
                mapping.state != LegacySubjectMappingState.CUT_OVER.name
            ) {
                return null
            }

            val subject =
                requireNotNull(dao.getManagedSubject(mapping.subjectId)) {
                    "CUT_OVER Context mapping points to a missing subject"
                }

            if (subject.subjectType == ManagedSubjectType.ASPECT.name) {
                return null
            }

            require(
                subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                    !subject.isDeleted,
            ) {
                "CUT_OVER Context assessment target is not an active Orientation"
            }

            val orientations =
                dao.getAllOrientations()
                    .filter { it.subjectId == mapping.subjectId }
            require(orientations.size == 1) {
                "CUT_OVER Context must have exactly one Orientation node"
            }
            val orientation = orientations.single()

            val currents =
                dao.getAllAssessments()
                    .filter { it.orientationId == mapping.subjectId }
            require(currents.size == 1) {
                "CUT_OVER Context must have exactly one current assessment"
            }
            val current = currents.single()
            require(!current.isDeleted) {
                "CUT_OVER Context current assessment is deleted"
            }

            val revisions =
                dao.getAllAssessmentRevisions()
                    .filter { it.id == current.revisionId }
            require(revisions.size == 1) {
                "CUT_OVER Context current revision is missing or duplicated"
            }
            val revision = revisions.single()
            require(
                !revision.isDeleted &&
                    revision.orientationId == mapping.subjectId,
            ) {
                "CUT_OVER Context current revision is invalid"
            }

            val assessment =
                requireNotNull(
                    runCatching {
                        decodeOrientationAssessmentJson(
                            gson,
                            revision.assessmentJson,
                        )
                    }.getOrElse {
                        throw IllegalArgumentException(
                            "CUT_OVER Context assessment JSON is invalid",
                            it,
                        )
                    },
                )

            require(current.matchesAssessment(assessment)) {
                "CUT_OVER Context current assessment diverges from revision"
            }

            val kind =
                runCatching {
                    OrientationKind.valueOf(orientation.kind)
                }.getOrElse {
                    throw IllegalArgumentException(
                        "CUT_OVER Context Orientation kind is invalid",
                        it,
                    )
                }

            require(validateOrientationAssessment(kind, assessment).isEmpty()) {
                "CUT_OVER Context assessment violates DOMAIN-CONTRACT v1"
            }

            return ContextOrientationAssessmentState(
                subject = subject,
                orientation = orientation,
                current = current,
                assessment = assessment,
            )
        }

        @Deprecated("Use CanonicalAspectRepository lifecycle commands")
        suspend fun saveAspect(subject: ManagedSubject, aspect: AspectNode) {
            require(subject.subjectType == ManagedSubjectType.ASPECT)
            require(subject.id == aspect.subjectId)
            database.withTransaction {
                val hierarchy =
                    dao.getAllAspects().associate { it.subjectId to it.parentAspectId }.toMutableMap()
                        .also { it[aspect.subjectId] = aspect.parentAspectId }
                require(validateSingleParentHierarchy(hierarchy).isEmpty()) {
                    "Aspect hierarchy violates DOMAIN-CONTRACT v1"
                }
                dao.upsertManagedSubjects(listOf(subject.toEntity()))
                dao.upsertAspects(listOf(aspect.toEntity()))
            }
        }
    }

private fun ManagedSubject.toEntity() =
    ManagedSubjectEntity(
        id = id,
        subjectType = subjectType.name,
        title = title,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun OrientationNode.toEntity() =
    OrientationEntity(
        subjectId = subjectId,
        kind = kind.name,
        lifecycle = lifecycle?.name,
        lifecycleOrigin = lifecycleOrigin.name,
    )

private fun AspectNode.toEntity() =
    AspectEntity(
        subjectId = subjectId,
        parentAspectId = parentAspectId,
        aspectOrder = order,
        archived = archived,
    )

private fun OrientationAssessmentRevision.toEntity(gson: Gson) =
    OrientationAssessmentRevisionEntity(
        id = id,
        orientationId = orientationId,
        effectiveFrom = effectiveFrom,
        recordedAt = recordedAt,
        source = source.name,
        reason = reason,
        assessmentJson = gson.toJson(assessment),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun OrientationAssessmentRevision.toCurrentEntity(gson: Gson) =
    assessment.toCurrentEntity(
        orientationId = orientationId,
        revisionId = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        gson = gson,
    )

private const val CONTEXT_MIGRATION_INITIAL_REASON =
    "Initial canonical Orientation created from Context migration"

private const val CONTEXT_ASSESSMENT_EDIT_REASON =
    "Canonical Context assessment edited from Context Settings"

private const val CONTEXT_SCORING_EDIT_REASON =
    "Canonical Context scoring bundle edited from Context Settings"

internal fun OrientationAssessmentEntity.matchesAssessment(
    expected: OrientationAssessment,
): Boolean =
    importanceValue == expected.importance.valueCode &&
        importanceOrigin == expected.importance.origin.name &&
        impactValue == expected.impact.valueCode &&
        impactOrigin == expected.impact.origin.name &&
        breadthValue == expected.breadth.valueCode &&
        breadthOrigin == expected.breadth.origin.name &&
        expectedSpanValue == expected.expectedSpan.valueCode &&
        expectedSpanOrigin == expected.expectedSpan.origin.name &&
        targetWindowValue == expected.targetWindow.valueCode &&
        targetWindowOrigin == expected.targetWindow.origin.name &&
        attentionTierValue == expected.attentionTier.valueCode &&
        attentionTierOrigin == expected.attentionTier.origin.name &&
        commitmentValue == expected.commitment.valueCode &&
        commitmentOrigin == expected.commitment.origin.name &&
        confidenceValue == expected.confidence.valueCode &&
        confidenceOrigin == expected.confidence.origin.name &&
        effortValue == expected.effort.valueCode &&
        effortOrigin == expected.effort.origin.name &&
        costValue == expected.cost.valueCode &&
        costOrigin == expected.cost.origin.name &&
        riskValue == expected.risk.valueCode &&
        riskOrigin == expected.risk.origin.name &&
        evaluationState == expected.evaluationState.name

private fun OrientationAssessment.toCurrentEntity(
    orientationId: String,
    revisionId: String,
    createdAt: Long,
    updatedAt: Long,
    syncedAt: Long?,
    isDeleted: Boolean,
    version: Long,
    gson: Gson,
) = OrientationAssessmentEntity(
    orientationId = orientationId,
    revisionId = revisionId,
    importanceValue = importance.valueCode,
    importanceOrigin = importance.origin.name,
    impactValue = impact.valueCode,
    impactOrigin = impact.origin.name,
    breadthValue = breadth.valueCode,
    breadthOrigin = breadth.origin.name,
    expectedSpanValue = expectedSpan.valueCode,
    expectedSpanOrigin = expectedSpan.origin.name,
    targetWindowValue = targetWindow.valueCode,
    targetWindowOrigin = targetWindow.origin.name,
    attentionTierValue = attentionTier.valueCode,
    attentionTierOrigin = attentionTier.origin.name,
    commitmentValue = commitment.valueCode,
    commitmentOrigin = commitment.origin.name,
    confidenceValue = confidence.valueCode,
    confidenceOrigin = confidence.origin.name,
    effortValue = effort.valueCode,
    effortOrigin = effort.origin.name,
    costValue = cost.valueCode,
    costOrigin = cost.origin.name,
    riskValue = risk.valueCode,
    riskOrigin = risk.origin.name,
    evaluationState = evaluationState.name,
    provenanceJson = gson.toJson(
        listOf(
            importance,
            impact,
            breadth,
            expectedSpan,
            targetWindow,
            attentionTier,
            commitment,
            confidence,
            effort,
            cost,
            risk,
        ).mapNotNull { it.provenance },
    ),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
    isDeleted = isDeleted,
    version = version,
)

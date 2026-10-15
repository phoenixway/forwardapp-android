package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Semantic authority boundary for MANUAL ArcQuest compatibility rows.
 *
 * Canonical QUEST owns semantic identity, title, description and liveness.
 * ArcQuest keeps Strategic placement/order/status and source-link metadata.
 * Source-backed ArcQuest rows continue to reference an existing source
 * Orientation and never manufacture a duplicate QUEST.
 */
@Singleton
class QuestOrientationBridge
    @Inject
    constructor(
        private val orientationDao: OrientationDao,
        private val bootstrapper: CanonicalOrientationBootstrapper,
    ) {
        private val gson = Gson()

        suspend fun ensureCutOver() {
            val report = bootstrapper.ensureBootstrapped()
            check(report.issues.isEmpty()) {
                "QUEST Orientation authority cutover is blocked by ${report.issues.size} bootstrap issue(s)"
            }
        }

        fun observeCanonicalArcQuests(
            quests: Flow<List<ArcQuestEntity>>,
        ): Flow<List<ArcQuestEntity>> =
            combine(
                quests,
                orientationDao.observeManagedSubjects(),
                orientationDao.observeLegacyMappings(),
                orientationDao.observeOrientations(),
            ) { rows, subjects, mappings, orientations ->
                projectCanonicalArcQuests(
                    quests = rows,
                    subjects = subjects,
                    mappings = mappings,
                    orientations = orientations,
                ).filterNot { it.isDeleted }
            }

        suspend fun projectCanonicalArcQuest(
            quest: ArcQuestEntity,
        ): ArcQuestEntity =
            projectCanonicalArcQuests(
                quests = listOf(quest),
                subjects = orientationDao.getAllManagedSubjects(),
                mappings = orientationDao.getAllLegacyMappings(),
                orientations = orientationDao.getAllOrientations(),
            ).single()

        /**
         * Must run inside the caller's Room transaction.
         *
         * status/order/arcKey remain Strategic specialized state and must not
         * mutate Orientation lifecycle or canonical assessment.
         */
        suspend fun writeCommon(
            previous: ArcQuestEntity?,
            desired: ArcQuestEntity,
            now: Long,
        ) {
            if (!desired.isManual()) return

            require(previous == null || previous.isManual()) {
                "ArcQuest ${desired.id} cannot change source-backed identity into MANUAL"
            }
            require(previous?.isDeleted != true || desired.isDeleted) {
                "Deleted manual ArcQuest ${desired.id} cannot be resurrected"
            }

            val expectedSubjectId =
                LegacySubjectUuid.resolve(
                    LegacySubjectRef(
                        LegacyOrientationSourceType.ARC_QUEST,
                        desired.id,
                    ),
                )

            val mapping =
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.ARC_QUEST.name,
                    desired.id,
                )

            if (mapping == null) {
                require(previous == null) {
                    "Existing manual ArcQuest ${desired.id} has no canonical QUEST mapping"
                }
                createCanonicalQuest(desired)
                return
            }

            require(mapping.subjectId == expectedSubjectId) {
                "Manual ArcQuest ${desired.id} mapping does not preserve deterministic identity"
            }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "Manual ArcQuest ${desired.id} mapping is not CUT_OVER"
            }
            require(!mapping.isDeleted || desired.isDeleted) {
                "Deleted QUEST mapping for ArcQuest ${desired.id} cannot be resurrected"
            }

            val subject =
                requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)) {
                    "CUT_OVER ArcQuest ${desired.id} is missing canonical subject ${mapping.subjectId}"
                }
            require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                "CUT_OVER ArcQuest ${desired.id} target is not an Orientation"
            }
            require(!subject.isDeleted || desired.isDeleted) {
                "Deleted canonical QUEST ${mapping.subjectId} cannot be resurrected from ArcQuest ${desired.id}"
            }

            val orientations =
                orientationDao.getAllOrientations()
                    .filter { it.subjectId == mapping.subjectId }
            require(orientations.size == 1) {
                "CUT_OVER ArcQuest ${desired.id} must have exactly one canonical Orientation"
            }
            require(orientations.single().kind == OrientationKind.QUEST.name) {
                "CUT_OVER ArcQuest ${desired.id} canonical kind is not QUEST"
            }

            val commonChanged =
                previous == null ||
                    previous.title != desired.title ||
                    previous.description != desired.description ||
                    previous.isDeleted != desired.isDeleted

            if (!commonChanged) return

            if (
                subject.title != desired.title ||
                subject.description != desired.description ||
                subject.isDeleted != desired.isDeleted
            ) {
                orientationDao.upsertManagedSubjects(
                    listOf(
                        subject.copy(
                            title = desired.title,
                            description = desired.description,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = desired.isDeleted,
                            version = subject.version + 1L,
                        ),
                    ),
                )
            }

            if (previous?.isDeleted != desired.isDeleted) {
                val assessments =
                    orientationDao.getAllAssessments()
                        .filter { it.orientationId == mapping.subjectId }
                require(assessments.size == 1) {
                    "CUT_OVER ArcQuest ${desired.id} must have exactly one current assessment"
                }
                val assessment = assessments.single()

                if (assessment.isDeleted != desired.isDeleted) {
                    orientationDao.upsertAssessments(
                        listOf(
                            assessment.copy(
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = desired.isDeleted,
                                version = assessment.version + 1L,
                            ),
                        ),
                    )
                }

                if (mapping.isDeleted != desired.isDeleted) {
                    orientationDao.upsertLegacyMappings(
                        listOf(
                            mapping.copy(
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = desired.isDeleted,
                                version = mapping.version + 1L,
                            ),
                        ),
                    )
                }
            }
        }

        private suspend fun createCanonicalQuest(
            quest: ArcQuestEntity,
        ) {
            val rows =
                quest.toCutOverCanonicalQuestRows(
                    gson = gson,
                    migrationVersion =
                        CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION,
                )

            require(orientationDao.getManagedSubject(rows.subject.id) == null) {
                "New ArcQuest ${quest.id} collides with canonical subject ${rows.subject.id}"
            }
            require(
                orientationDao.getAllLegacyMappings()
                    .none { it.subjectId == rows.subject.id },
            ) {
                "New ArcQuest ${quest.id} canonical subject already has a source mapping"
            }

            orientationDao.upsertManagedSubjects(listOf(rows.subject))
            orientationDao.upsertOrientations(listOf(rows.orientation))
            orientationDao.upsertAssessmentRevisions(listOf(rows.revision))
            orientationDao.upsertAssessments(listOf(rows.assessment))
            orientationDao.upsertLegacyMappings(listOf(rows.mapping))
        }
    }

internal fun ArcQuestEntity.toCutOverCanonicalQuestRows(
    gson: Gson,
    migrationVersion: Int,
): CanonicalOrientationRows {
    require(isManual()) {
        "Only MANUAL ArcQuest can create a standalone canonical QUEST"
    }

    val compatibility =
        (
            toCompatibilityProjection(LegacySubjectUuid)
                as ArcQuestCompatibilityProjection.ManualOrientation
        ).value
    val projection =
        compatibility.copy(
            orientation =
                compatibility.orientation.copy(
                    kind = OrientationKind.QUEST,
                    lifecycle = null,
                    lifecycleOrigin = ValueOrigin.UNSET,
                ),
        )

    val rows = projection.toCanonicalRows(gson, migrationVersion)
    return rows.copy(
        mapping =
            rows.mapping.copy(
                state = LegacySubjectMappingState.CUT_OVER.name,
            ),
    )
}

internal fun projectCanonicalArcQuests(
    quests: List<ArcQuestEntity>,
    subjects: List<ManagedSubjectEntity>,
    mappings: List<LegacySubjectMappingEntity>,
    orientations: List<OrientationEntity>,
): List<ArcQuestEntity> {
    val subjectsById = subjects.associateBy { it.id }
    val orientationsById = orientations.associateBy { it.subjectId }
    val mappingsBySource =
        mappings.groupBy { it.sourceType to it.sourceId }

    return quests.map { quest ->
        if (!quest.isManual()) return@map quest

        val sourceMappings =
            mappingsBySource[
                LegacyOrientationSourceType.ARC_QUEST.name to quest.id
            ].orEmpty()

        require(sourceMappings.size == 1) {
            "Manual ArcQuest ${quest.id} must have exactly one canonical source mapping"
        }

        val mapping = sourceMappings.single()
        require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
            "Manual ArcQuest ${quest.id} mapping is not CUT_OVER"
        }

        val expectedSubjectId =
            LegacySubjectUuid.resolve(
                LegacySubjectRef(
                    LegacyOrientationSourceType.ARC_QUEST,
                    quest.id,
                ),
            )
        require(mapping.subjectId == expectedSubjectId) {
            "Manual ArcQuest ${quest.id} mapping does not preserve deterministic identity"
        }

        val subject =
            requireNotNull(subjectsById[mapping.subjectId]) {
                "CUT_OVER ArcQuest ${quest.id} is missing canonical subject ${mapping.subjectId}"
            }
        require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
            "CUT_OVER ArcQuest ${quest.id} target is not an Orientation"
        }

        require(orientationsById[mapping.subjectId]?.kind == OrientationKind.QUEST.name) {
            "CUT_OVER ArcQuest ${quest.id} canonical kind is not QUEST"
        }

        quest.copy(
            title = subject.title,
            description = subject.description,
            isDeleted = subject.isDeleted,
        )
    }
}

private fun ArcQuestEntity.isManual(): Boolean =
    sourceType == ArcQuestSourceType.MANUAL.name

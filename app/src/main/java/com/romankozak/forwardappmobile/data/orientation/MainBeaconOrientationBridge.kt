package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class MainBeaconCommonProjection(
    val beaconsByLegacyId: Map<String, ManagedSubjectEntity>,
    val groupsByLegacyId: Map<String, ManagedSubjectEntity>,
)

/**
 * Phase-4 boundary between canonical common Orientation fields and specialized
 * legacy Beacon storage. Legacy title/description columns are write-through
 * compatibility projections, not a second authority.
 */
@Singleton
class MainBeaconOrientationBridge
    @Inject
    constructor(
        private val orientationDao: OrientationDao,
        private val mainBeaconDao: MainBeaconDao,
        private val bootstrapper: CanonicalOrientationBootstrapper,
    ) {
        private val gson = Gson()

        suspend fun ensureCutOver() {
            bootstrapper.ensureBootstrapped()
        }

        fun observeCommonProjection(): Flow<MainBeaconCommonProjection> =
            combine(
                orientationDao.observeManagedSubjects(),
                orientationDao.observeLegacyMappings(),
            ) { subjects, mappings ->
                buildProjection(subjects, mappings)
            }

        suspend fun project(beacon: MainBeacon): MainBeacon {
            val subject = canonicalSubject(LegacyOrientationSourceType.MAIN_BEACON, beacon.id) ?: return beacon
            return beacon.copy(title = subject.title, description = subject.description)
        }

        suspend fun project(group: MainBeaconGroup): MainBeaconGroup {
            val subject = canonicalSubject(LegacyOrientationSourceType.MAIN_BEACON_GROUP, group.id) ?: return group
            return group.copy(title = subject.title, description = subject.description)
        }

        suspend fun writeCommon(beacon: MainBeacon) {
            writeCommon(beacon.toEffectiveOrientation(LegacySubjectUuid))
        }

        suspend fun writeCommon(group: MainBeaconGroup) {
            writeCommon(group.toEffectiveOrientation(LegacySubjectUuid))
        }

        suspend fun tombstone(
            sourceType: LegacyOrientationSourceType,
            sourceId: String,
            now: Long,
        ) {
            val mapping = orientationDao.getLegacyMapping(sourceType.name, sourceId) ?: return
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "$sourceType:$sourceId is not CUT_OVER"
            }
            val subject = orientationDao.getManagedSubject(mapping.subjectId) ?: return
            if (!subject.isDeleted) {
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
            orientationDao.getAllAssessments()
                .firstOrNull { it.orientationId == mapping.subjectId && !it.isDeleted }
                ?.let { assessment ->
                    orientationDao.upsertAssessments(
                        listOf(
                            assessment.copy(
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = true,
                                version = assessment.version + 1L,
                            ),
                        ),
                    )
                }
            val relationChanges =
                orientationDao.getAllOrientationRelations()
                    .filter {
                        !it.isDeleted &&
                            (it.fromOrientationId == mapping.subjectId || it.toOrientationId == mapping.subjectId)
                    }.map {
                        it.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = it.version + 1L,
                        )
                    }
            if (relationChanges.isNotEmpty()) orientationDao.upsertOrientationRelations(relationChanges)
        }

        suspend fun syncMembershipProjection(now: Long = System.currentTimeMillis()) {
            val changes =
                planCanonicalMainBeaconMembershipChanges(
                    legacyMembers = mainBeaconDao.getAllGroupMembersSync(),
                    mappings = orientationDao.getAllLegacyMappings(),
                    existingRelations = orientationDao.getAllOrientationRelations(),
                    now = now,
                )
            if (changes.isNotEmpty()) orientationDao.upsertOrientationRelations(changes)
        }

        private suspend fun writeCommon(projection: EffectiveOrientation) {
            val mapping =
                orientationDao.getLegacyMapping(projection.source.sourceType.name, projection.source.sourceId)
            if (mapping == null) {
                val rows = projection.toCanonicalRows(gson, CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION)
                orientationDao.upsertManagedSubjects(listOf(rows.subject.copy(version = 1L)))
                orientationDao.upsertOrientations(listOf(rows.orientation))
                orientationDao.upsertAssessmentRevisions(listOf(rows.revision))
                orientationDao.upsertAssessments(listOf(rows.assessment))
                orientationDao.upsertLegacyMappings(
                    listOf(
                        rows.mapping.copy(
                            state = LegacySubjectMappingState.CUT_OVER.name,
                            version = 1L,
                        ),
                    ),
                )
                return
            }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "${projection.source.sourceType}:${projection.source.sourceId} is not CUT_OVER"
            }
            val orientation = orientationDao.getAllOrientations().firstOrNull { it.subjectId == mapping.subjectId }
            require(orientation?.kind == projection.orientation.kind.name) {
                "Canonical Orientation kind mismatch for ${projection.source.sourceType}:${projection.source.sourceId}"
            }
            val existing = requireNotNull(orientationDao.getManagedSubject(mapping.subjectId))
            val title = projection.subject.title
            val description = projection.subject.description
            if (existing.title != title || existing.description != description || existing.isDeleted) {
                orientationDao.upsertManagedSubjects(
                    listOf(
                        existing.copy(
                            title = title,
                            description = description,
                            updatedAt = projection.subject.updatedAt,
                            syncedAt = null,
                            isDeleted = false,
                            version = existing.version + 1L,
                        ),
                    ),
                )
                if (existing.isDeleted) {
                    orientationDao.getAllAssessments()
                        .firstOrNull { it.orientationId == mapping.subjectId && it.isDeleted }
                        ?.let { assessment ->
                            orientationDao.upsertAssessments(
                                listOf(
                                    assessment.copy(
                                        updatedAt = projection.subject.updatedAt,
                                        syncedAt = null,
                                        isDeleted = false,
                                        version = assessment.version + 1L,
                                    ),
                                ),
                            )
                        }
                }
            }
        }

        private suspend fun canonicalSubject(
            sourceType: LegacyOrientationSourceType,
            sourceId: String,
        ): ManagedSubjectEntity? {
            val mapping = orientationDao.getLegacyMapping(sourceType.name, sourceId) ?: return null
            if (mapping.state != LegacySubjectMappingState.CUT_OVER.name || mapping.isDeleted) return null
            return orientationDao.getManagedSubject(mapping.subjectId)?.takeUnless { it.isDeleted }
        }
    }

internal fun buildProjection(
    subjects: List<ManagedSubjectEntity>,
    mappings: List<LegacySubjectMappingEntity>,
): MainBeaconCommonProjection {
    val subjectsById = subjects.filterNot { it.isDeleted }.associateBy { it.id }
    fun projected(sourceType: LegacyOrientationSourceType): Map<String, ManagedSubjectEntity> =
        mappings.asSequence()
            .filter {
                !it.isDeleted &&
                    it.state == LegacySubjectMappingState.CUT_OVER.name &&
                    it.sourceType == sourceType.name
            }.mapNotNull { mapping -> subjectsById[mapping.subjectId]?.let { mapping.sourceId to it } }
            .toMap()
    return MainBeaconCommonProjection(
        beaconsByLegacyId = projected(LegacyOrientationSourceType.MAIN_BEACON),
        groupsByLegacyId = projected(LegacyOrientationSourceType.MAIN_BEACON_GROUP),
    )
}

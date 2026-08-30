package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapIssueEntity
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapStateEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestDao
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalOrientationBootstrapper
    @Inject
    constructor(
        private val database: AppDatabase,
        private val orientationDao: OrientationDao,
        private val goalDao: GoalDao,
        private val mainBeaconDao: MainBeaconDao,
        private val arcQuestDao: ArcQuestDao,
        private val canonicalDayThemeDao: CanonicalDayThemeDao,
        private val canonicalDayThemeBootstrapper: CanonicalDayThemeBootstrapper,
    ) {
        private val mutex = Mutex()
        private val gson = Gson()

        suspend fun ensureBootstrapped(): OrientationBootstrapReport =
            mutex.withLock {
                canonicalDayThemeBootstrapper.ensureBootstrapped()
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    val state = orientationDao.getBootstrapState()
                    val projections = loadLegacyProjections()
                    val existingMappings = orientationDao.getAllLegacyMappings()
                    val existingSubjects = orientationDao.getAllManagedSubjects().associateBy { it.id }
                    val plan = planBootstrap(projections, existingMappings, existingSubjects.keys, gson)

                    if (plan.rows.isNotEmpty()) {
                        orientationDao.upsertManagedSubjects(plan.rows.map { it.subject })
                        orientationDao.upsertOrientations(plan.rows.map { it.orientation })
                        orientationDao.upsertAssessmentRevisions(plan.rows.map { it.revision })
                        orientationDao.upsertAssessments(plan.rows.map { it.assessment })
                        orientationDao.upsertLegacyMappings(plan.rows.map { it.mapping })
                    }

                    val cutover =
                        planMainBeaconCutover(
                            projections = projections,
                            mappings = orientationDao.getAllLegacyMappings(),
                            subjects = orientationDao.getAllManagedSubjects(),
                            orientations = orientationDao.getAllOrientations(),
                            legacyMembers = mainBeaconDao.getAllGroupMembersSync(),
                            existingRelations = orientationDao.getAllOrientationRelations(),
                            now = now,
                            migrationVersion = CURRENT_BOOTSTRAP_VERSION,
                        )
                    if (cutover.mappings.isNotEmpty()) orientationDao.upsertLegacyMappings(cutover.mappings)
                    if (cutover.relationChanges.isNotEmpty()) {
                        orientationDao.upsertOrientationRelations(cutover.relationChanges)
                    }
                    ingestNewerMainBeaconCompatibilityWrites(orientationDao, mainBeaconDao)
                    repairMainBeaconCompatibilityProjections(orientationDao, mainBeaconDao)

                    val comparisonIssues = compareCanonicalRows(projections, orientationDao)
                    val issues = plan.issues + cutover.issues + comparisonIssues
                    orientationDao.resolveOpenBootstrapIssues(now)
                    if (issues.isNotEmpty()) orientationDao.upsertBootstrapIssues(issues)
                    orientationDao.upsertBootstrapState(
                        OrientationBootstrapStateEntity(
                            version = CURRENT_BOOTSTRAP_VERSION,
                            status = if (issues.isEmpty()) STATUS_COMPLETE else STATUS_BLOCKED,
                            completedAt = now.takeIf { issues.isEmpty() },
                            comparedAt = now,
                        ),
                    )
                    OrientationBootstrapReport(
                        performed =
                            plan.rows.isNotEmpty() ||
                                state == null ||
                                state.version != CURRENT_BOOTSTRAP_VERSION ||
                                state.status != STATUS_COMPLETE ||
                                issues.isNotEmpty(),
                        materialized = plan.rows.size,
                        compared = projections.size,
                        issues = issues,
                    )
                }
            }

        private suspend fun loadLegacyProjections(
        ): List<EffectiveOrientation> {
            val resolver = LegacySubjectUuid
            val manualArcQuests =
                arcQuestDao.getAllSync()
                    .filter(ArcQuestEntity::isManual)
                    .mapNotNull { quest ->
                        (quest.toCompatibilityProjection(resolver) as? ArcQuestCompatibilityProjection.ManualOrientation)?.value
                    }
            return buildList {
                addAll(mainBeaconDao.getAllBeaconsSync().map { it.toEffectiveOrientation(resolver) })
                addAll(mainBeaconDao.getAllGroupsSync().map { it.toEffectiveOrientation(resolver) })
                addAll(goalDao.getAllRaw().map { it.toEffectiveOrientation(resolver) })
                addAll(canonicalDayThemeDao.getAllThemeDefinitionsSync().map { it.toEffectiveOrientation(resolver) })
                addAll(manualArcQuests)
            }
        }

        companion object {
            const val CURRENT_BOOTSTRAP_VERSION: Int = 3
            const val STATUS_COMPLETE: String = "COMPLETE"
            const val STATUS_BLOCKED: String = "BLOCKED"
        }
    }

private fun ArcQuestEntity.isManual(): Boolean = sourceType == ArcQuestSourceType.MANUAL.name

internal data class OrientationBootstrapPlan(
    val rows: List<CanonicalOrientationRows>,
    val issues: List<OrientationBootstrapIssueEntity>,
)

internal fun planBootstrap(
    projections: List<EffectiveOrientation>,
    existingMappings: List<LegacySubjectMappingEntity>,
    existingSubjectIds: Set<String>,
    gson: Gson,
): OrientationBootstrapPlan {
    val mappingBySource = existingMappings.associateBy { it.sourceType to it.sourceId }
    val mappingBySubject = existingMappings.associateBy { it.subjectId }
    val rows = mutableListOf<CanonicalOrientationRows>()
    val issues = mutableListOf<OrientationBootstrapIssueEntity>()
    val plannedSubjects = mutableSetOf<String>()

    projections.forEach { projection ->
        val sourceKey = projection.source.sourceType.name to projection.source.sourceId
        val subjectId = projection.subject.id
        val sourceMapping = mappingBySource[sourceKey]
        val subjectMapping = mappingBySubject[subjectId]
        val collision =
            when {
                sourceMapping != null && sourceMapping.subjectId != subjectId -> "Source already maps to ${sourceMapping.subjectId}"
                subjectMapping != null &&
                    (subjectMapping.sourceType != sourceKey.first || subjectMapping.sourceId != sourceKey.second) ->
                    "Subject already maps from ${subjectMapping.sourceType}:${subjectMapping.sourceId}"
                subjectId in plannedSubjects -> "Duplicate deterministic subject ID in bootstrap input"
                else -> null
            }
        if (collision != null) {
            issues += projection.issue("IDENTITY_COLLISION", collision)
        } else if (sourceMapping == null && subjectId !in existingSubjectIds) {
            rows += projection.toCanonicalRows(gson, CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION)
            plannedSubjects += subjectId
        } else if (sourceMapping == null) {
            issues += projection.issue("UNMAPPED_CANONICAL_SUBJECT", "Canonical subject exists without durable source mapping")
        }
    }
    return OrientationBootstrapPlan(rows, issues)
}

private suspend fun compareCanonicalRows(
    projections: List<EffectiveOrientation>,
    dao: OrientationDao,
): List<OrientationBootstrapIssueEntity> {
    val subjects = dao.getAllManagedSubjects().associateBy { it.id }
    val orientations = dao.getAllOrientations().associateBy { it.subjectId }
    val assessments = dao.getAllAssessments().associateBy { it.orientationId }
    val mappings = dao.getAllLegacyMappings().associateBy { it.sourceType to it.sourceId }
    return projections.mapNotNull { expected ->
        val subject = subjects[expected.subject.id]
        val orientation = orientations[expected.subject.id]
        val assessment = assessments[expected.subject.id]
        val expectedAssessment =
            expected.toCanonicalRows(
                Gson(),
                CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION,
            ).assessment
        val mapping = mappings[expected.source.sourceType.name to expected.source.sourceId]
        val isCutOverMainBeacon =
            expected.source.sourceType in MAIN_BEACON_SOURCE_TYPES &&
                mapping?.state == LegacySubjectMappingState.CUT_OVER.name
        val identityMismatch =
            subject == null || orientation == null || assessment == null ||
                subject.subjectType != expected.subject.subjectType.name ||
                orientation.kind != expected.orientation.kind.name
        val shadowOnlyMismatch =
            if (isCutOverMainBeacon || subject == null || orientation == null || assessment == null) {
                false
            } else {
                subject.title != expected.subject.title ||
                    subject.description != expected.subject.description ||
                    subject.isDeleted != expected.subject.isDeleted ||
                    orientation.lifecycle != expected.orientation.lifecycle?.name ||
                    orientation.lifecycleOrigin != expected.orientation.lifecycleOrigin.name ||
                    subject.createdAt != expected.subject.createdAt ||
                    subject.updatedAt != expected.subject.updatedAt ||
                    !assessment.hasSameAxisValues(expectedAssessment)
            }
        val mismatch = identityMismatch || shadowOnlyMismatch
        expected.issue("SHADOW_MISMATCH", "Canonical row differs from the legacy projection").takeIf { mismatch }
    }
}

private suspend fun repairMainBeaconCompatibilityProjections(
    orientationDao: OrientationDao,
    mainBeaconDao: MainBeaconDao,
) {
    val mappings = orientationDao.getAllLegacyMappings()
    val subjects = orientationDao.getAllManagedSubjects().associateBy { it.id }
    mappings.filter {
        !it.isDeleted &&
            it.state == LegacySubjectMappingState.CUT_OVER.name
    }.forEach { mapping ->
        val subject = subjects[mapping.subjectId]?.takeUnless { it.isDeleted } ?: return@forEach
        when (mapping.sourceType) {
            LegacyOrientationSourceType.MAIN_BEACON.name ->
                mainBeaconDao.projectBeaconCommonFields(mapping.sourceId, subject.title, subject.description)
            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name ->
                mainBeaconDao.projectGroupCommonFields(mapping.sourceId, subject.title, subject.description)
        }
    }

    val canonicalMembers =
        projectCanonicalMainBeaconMemberships(
            mappings = mappings,
            relations = orientationDao.getAllOrientationRelations(),
        )
    val legacyMembers = mainBeaconDao.getAllGroupMembersSync()
    if (canonicalMembers != legacyMembers) {
        mainBeaconDao.deleteAllGroupMembers()
        if (canonicalMembers.isNotEmpty()) mainBeaconDao.insertGroupMembers(canonicalMembers)
    }
}

private suspend fun ingestNewerMainBeaconCompatibilityWrites(
    orientationDao: OrientationDao,
    mainBeaconDao: MainBeaconDao,
) {
    val mappings =
        orientationDao.getAllLegacyMappings().filter {
            !it.isDeleted &&
                it.state == LegacySubjectMappingState.CUT_OVER.name
        }
    val subjects = orientationDao.getAllManagedSubjects().associateBy { it.id }
    val beacons = mainBeaconDao.getAllBeaconsSync().associateBy { it.id }
    val groups = mainBeaconDao.getAllGroupsSync().associateBy { it.id }
    val changes =
        mappings.mapNotNull { mapping ->
            val subject = subjects[mapping.subjectId]?.takeUnless { it.isDeleted } ?: return@mapNotNull null
            val compatibilityValue =
                when (mapping.sourceType) {
                    LegacyOrientationSourceType.MAIN_BEACON.name ->
                        beacons[mapping.sourceId]?.let { Triple(it.title, it.description, it.updatedAt) }
                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name ->
                        groups[mapping.sourceId]?.let { Triple(it.title, it.description, it.updatedAt) }
                    else -> null
                } ?: return@mapNotNull null
            val (title, description, updatedAt) = compatibilityValue
            subject.takeIf {
                updatedAt > it.updatedAt && (title != it.title || description != it.description)
            }?.copy(
                title = title,
                description = description,
                updatedAt = updatedAt,
                syncedAt = null,
                version = subject.version + 1L,
            )
        }
    if (changes.isNotEmpty()) orientationDao.upsertManagedSubjects(changes)
}

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity.hasSameAxisValues(
    other: com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity,
): Boolean =
    importanceValue == other.importanceValue && importanceOrigin == other.importanceOrigin &&
        impactValue == other.impactValue && impactOrigin == other.impactOrigin &&
        breadthValue == other.breadthValue && breadthOrigin == other.breadthOrigin &&
        expectedSpanValue == other.expectedSpanValue && expectedSpanOrigin == other.expectedSpanOrigin &&
        targetWindowValue == other.targetWindowValue && targetWindowOrigin == other.targetWindowOrigin &&
        attentionTierValue == other.attentionTierValue && attentionTierOrigin == other.attentionTierOrigin &&
        commitmentValue == other.commitmentValue && commitmentOrigin == other.commitmentOrigin &&
        confidenceValue == other.confidenceValue && confidenceOrigin == other.confidenceOrigin

internal fun EffectiveOrientation.issue(code: String, detail: String): OrientationBootstrapIssueEntity {
    val stableName = "${source.sourceType.name}:${source.sourceId}:$code"
    return OrientationBootstrapIssueEntity(
        id =
            LegacySubjectUuid.uuidV5(
                UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
                "bootstrap-issue:$stableName",
            ).toString(),
        sourceType = source.sourceType.name,
        sourceId = source.sourceId,
        code = code,
        detail = detail,
        createdAt = System.currentTimeMillis(),
        resolvedAt = null,
    )
}

data class OrientationBootstrapReport(
    val performed: Boolean,
    val materialized: Int,
    val compared: Int,
    val issues: List<OrientationBootstrapIssueEntity>,
)

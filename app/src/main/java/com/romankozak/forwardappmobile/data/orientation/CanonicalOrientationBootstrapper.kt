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
import com.romankozak.forwardappmobile.features.contexts.data.dao.DirectionDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestDao
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
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
        private val directionDao: DirectionDao,
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

                    val now = System.currentTimeMillis()
                    val comparisonIssues = compareCanonicalRows(projections, orientationDao)
                    val issues = plan.issues + comparisonIssues
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

        private suspend fun loadLegacyProjections(): List<EffectiveOrientation> {
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
                addAll(directionDao.getAllRaw().map { it.toEffectiveOrientation(resolver) })
                addAll(canonicalDayThemeDao.getAllThemeDefinitionsSync().map { it.toEffectiveOrientation(resolver) })
                addAll(manualArcQuests)
            }
        }

        companion object {
            const val CURRENT_BOOTSTRAP_VERSION: Int = 1
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
    return projections.mapNotNull { expected ->
        val subject = subjects[expected.subject.id]
        val orientation = orientations[expected.subject.id]
        val assessment = assessments[expected.subject.id]
        val expectedAssessment =
            expected.toCanonicalRows(
                Gson(),
                CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION,
            ).assessment
        val mismatch =
            subject == null || orientation == null || assessment == null ||
                subject.subjectType != expected.subject.subjectType.name ||
                subject.title != expected.subject.title ||
                subject.description != expected.subject.description ||
                subject.createdAt != expected.subject.createdAt ||
                subject.updatedAt != expected.subject.updatedAt ||
                subject.isDeleted != expected.subject.isDeleted ||
                orientation.kind != expected.orientation.kind.name ||
                orientation.lifecycle != expected.orientation.lifecycle?.name ||
                orientation.lifecycleOrigin != expected.orientation.lifecycleOrigin.name ||
                !assessment.hasSameAxisValues(expectedAssessment)
        expected.issue("SHADOW_MISMATCH", "Canonical row differs from the legacy projection").takeIf { mismatch }
    }
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

private fun EffectiveOrientation.issue(code: String, detail: String): OrientationBootstrapIssueEntity {
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

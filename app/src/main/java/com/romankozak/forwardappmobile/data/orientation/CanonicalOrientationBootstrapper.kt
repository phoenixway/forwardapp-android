package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.google.gson.Gson
import com.romankozak.forwardappmobile.StartupTrace
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
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

        suspend fun ensureBootstrapped(
            ingestLegacyMainBeaconMemberships: Boolean = false,
        ): OrientationBootstrapReport =
            mutex.withLock {
                canonicalDayThemeBootstrapper.ensureBootstrapped()
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    val state = orientationDao.getBootstrapState()
                    val legacy =
                        StartupTrace.measure("Application.orientationBootstrap.loadLegacy") {
                            loadLegacyInput()
                        }
                    var canonical =
                        StartupTrace.measure("Application.orientationBootstrap.loadCanonical") {
                            loadCanonicalSnapshot()
                        }
                    val plan =
                        StartupTrace.measure("Application.orientationBootstrap.plan") {
                            planBootstrap(
                                legacy.projections,
                                canonical.mappings,
                                canonical.subjects.mapTo(hashSetOf()) { it.id },
                                gson,
                            )
                        }

                    if (plan.rows.isNotEmpty()) {
                        orientationDao.upsertManagedSubjects(plan.rows.map { it.subject })
                        orientationDao.upsertOrientations(plan.rows.map { it.orientation })
                        orientationDao.upsertAssessmentRevisions(plan.rows.map { it.revision })
                        orientationDao.upsertAssessments(plan.rows.map { it.assessment })
                        orientationDao.upsertLegacyMappings(plan.rows.map { it.mapping })
                        canonical =
                            canonical.copy(
                                mappings = mergeById(canonical.mappings, plan.rows.map { it.mapping }) { it.id },
                                subjects = mergeById(canonical.subjects, plan.rows.map { it.subject }) { it.id },
                                orientations =
                                    mergeById(canonical.orientations, plan.rows.map { it.orientation }) {
                                        it.subjectId
                                    },
                                assessments =
                                    mergeById(canonical.assessments, plan.rows.map { it.assessment }) {
                                        it.orientationId
                                    },
                            )
                    }

                    val cutover =
                        planMainBeaconCutover(
                            projections = legacy.projections,
                            mappings = canonical.mappings,
                            subjects = canonical.subjects,
                            orientations = canonical.orientations,
                            legacyMembers = legacy.groupMembers,
                            existingRelations = canonical.relations,
                            now = now,
                            migrationVersion = CURRENT_BOOTSTRAP_VERSION,
                            ingestLegacyMembershipsForExistingCutOver =
                                ingestLegacyMainBeaconMemberships,
                        )
                    if (cutover.mappings.isNotEmpty()) orientationDao.upsertLegacyMappings(cutover.mappings)
                    if (cutover.relationChanges.isNotEmpty()) {
                        orientationDao.upsertOrientationRelations(cutover.relationChanges)
                    }
                    canonical =
                        canonical.copy(
                            mappings = mergeById(canonical.mappings, cutover.mappings) { it.id },
                            relations = mergeById(canonical.relations, cutover.relationChanges) { it.id },
                        )

                    val compatibilitySubjectChanges =
                        planNewerMainBeaconCompatibilityWrites(
                            mappings = canonical.mappings,
                            subjects = canonical.subjects,
                            beacons = legacy.beacons,
                            groups = legacy.groups,
                        )
                    if (compatibilitySubjectChanges.isNotEmpty()) {
                        orientationDao.upsertManagedSubjects(compatibilitySubjectChanges)
                        canonical =
                            canonical.copy(
                                subjects = mergeById(canonical.subjects, compatibilitySubjectChanges) { it.id },
                            )
                    }
                    StartupTrace.measure("Application.orientationBootstrap.repairCompatibility") {
                        repairMainBeaconCompatibilityProjections(
                            orientationDao = orientationDao,
                            mainBeaconDao = mainBeaconDao,
                            mappings = canonical.mappings,
                            subjects = canonical.subjects,
                            relations = canonical.relations,
                            beacons = legacy.beacons,
                            groups = legacy.groups,
                            legacyMembers = legacy.groupMembers,
                        )
                    }

                    val comparisonIssues =
                        StartupTrace.measure("Application.orientationBootstrap.compare") {
                            compareCanonicalRows(
                                projections = legacy.projections,
                                subjects = canonical.subjects,
                                orientations = canonical.orientations,
                                assessments = canonical.assessments,
                                mappings = canonical.mappings,
                            )
                        }
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
                        compared = legacy.projections.size,
                        issues = issues,
                    )
                }
            }

        private suspend fun loadLegacyInput(): LegacyOrientationBootstrapInput {
            val resolver = LegacySubjectUuid
            val beacons =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.beacons") {
                    mainBeaconDao.getAllBeaconsSync()
                }
            val groups =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.groups") {
                    mainBeaconDao.getAllGroupsSync()
                }
            val groupMembers =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.groupMembers") {
                    mainBeaconDao.getAllGroupMembersSync()
                }
            val manualArcQuests =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.arcQuests") {
                    arcQuestDao.getAllSync()
                        .filter(ArcQuestEntity::isManual)
                        .mapNotNull { quest ->
                            (quest.toCompatibilityProjection(resolver) as? ArcQuestCompatibilityProjection.ManualOrientation)?.value
                        }
                }
            val goals =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.goals") {
                    goalDao.getOrientationBootstrapRows()
                }
            val dayThemes =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.dayThemes") {
                    canonicalDayThemeDao.getAllThemeDefinitionsSync()
                }
            val projections =
                StartupTrace.measure("Application.orientationBootstrap.loadLegacy.project") {
                    buildList {
                        addAll(beacons.map { it.toEffectiveOrientation(resolver) })
                        addAll(groups.map { it.toEffectiveOrientation(resolver) })
                        addAll(goals.map { it.toEffectiveOrientation(resolver) })
                        addAll(dayThemes.map { it.toEffectiveOrientation(resolver) })
                        addAll(manualArcQuests)
                    }
                }
            return LegacyOrientationBootstrapInput(
                beacons = beacons,
                groups = groups,
                groupMembers = groupMembers,
                projections = projections,
            )
        }
        private suspend fun loadCanonicalSnapshot() =
            CanonicalOrientationBootstrapSnapshot(
                mappings = orientationDao.getAllLegacyMappings(),
                subjects = orientationDao.getAllManagedSubjects(),
                orientations = orientationDao.getAllOrientations(),
                assessments = orientationDao.getAllAssessments(),
                relations = orientationDao.getAllOrientationRelations(),
            )

        companion object {
            const val CURRENT_BOOTSTRAP_VERSION: Int = 3
            const val STATUS_COMPLETE: String = "COMPLETE"
            const val STATUS_BLOCKED: String = "BLOCKED"
        }
    }

private data class LegacyOrientationBootstrapInput(
    val projections: List<EffectiveOrientation>,
    val beacons: List<MainBeacon>,
    val groups: List<MainBeaconGroup>,
    val groupMembers: List<MainBeaconGroupMember>,
)

private data class CanonicalOrientationBootstrapSnapshot(
    val mappings: List<LegacySubjectMappingEntity>,
    val subjects: List<ManagedSubjectEntity>,
    val orientations: List<OrientationEntity>,
    val assessments: List<OrientationAssessmentEntity>,
    val relations: List<OrientationRelationEntity>,
)

private fun <T, K> mergeById(
    existing: List<T>,
    changes: List<T>,
    key: (T) -> K,
): List<T> =
    if (changes.isEmpty()) {
        existing
    } else {
        (existing.associateBy(key) + changes.associateBy(key)).values.toList()
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
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    assessments: List<OrientationAssessmentEntity>,
    mappings: List<LegacySubjectMappingEntity>,
): List<OrientationBootstrapIssueEntity> {
    val subjectsById = subjects.associateBy { it.id }
    val orientationsById = orientations.associateBy { it.subjectId }
    val assessmentsById = assessments.associateBy { it.orientationId }
    val mappingsBySource = mappings.associateBy { it.sourceType to it.sourceId }
    return projections.mapNotNull { expected ->
        val subject = subjectsById[expected.subject.id]
        val orientation = orientationsById[expected.subject.id]
        val assessment = assessmentsById[expected.subject.id]
        val expectedAssessment = expected.orientation.assessment
        val mapping = mappingsBySource[expected.source.sourceType.name to expected.source.sourceId]
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
        if (mismatch) {
            expected.issue("SHADOW_MISMATCH", "Canonical row differs from the legacy projection")
        } else {
            null
        }
    }
}

private suspend fun repairMainBeaconCompatibilityProjections(
    orientationDao: OrientationDao,
    mainBeaconDao: MainBeaconDao,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    relations: List<OrientationRelationEntity>,
    beacons: List<MainBeacon>,
    groups: List<MainBeaconGroup>,
    legacyMembers: List<MainBeaconGroupMember>,
) {
    val subjectsById = subjects.associateBy { it.id }
    val beaconsById = beacons.associateBy { it.id }
    val groupsById = groups.associateBy { it.id }
    mappings.filter {
        !it.isDeleted &&
            it.state == LegacySubjectMappingState.CUT_OVER.name
    }.forEach { mapping ->
        val subject = subjectsById[mapping.subjectId]?.takeUnless { it.isDeleted } ?: return@forEach
        when (mapping.sourceType) {
            LegacyOrientationSourceType.MAIN_BEACON.name -> {
                val beacon = beaconsById[mapping.sourceId] ?: return@forEach
                if (beacon.title != subject.title || beacon.description != subject.description) {
                    mainBeaconDao.projectBeaconCommonFields(mapping.sourceId, subject.title, subject.description)
                }
            }
            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name -> {
                val group = groupsById[mapping.sourceId] ?: return@forEach
                if (group.title != subject.title || group.description != subject.description) {
                    mainBeaconDao.projectGroupCommonFields(mapping.sourceId, subject.title, subject.description)
                }
            }
        }
    }

    // Main Beacon group membership is a full-set compatibility projection.
    // An empty canonical relation set is authoritative only after every live
    // Main Beacon and Main Beacon Group has completed ownership cutover.
    // Otherwise MATERIALIZED / blocked rows still leave legacy membership
    // authoritative, and replacing it from the partial canonical projection
    // would destroy valid legacy relations.
    val activeMappingBySource =
        mappings
            .asSequence()
            .filterNot { it.isDeleted }
            .associateBy { it.sourceType to it.sourceId }
    val liveBeaconIds = beacons.mapTo(hashSetOf()) { it.id }
    val liveGroupIds = groups.mapTo(hashSetOf()) { it.id }
    val membershipCutoverComplete =
        liveBeaconIds.all { beaconId ->
            activeMappingBySource[
                LegacyOrientationSourceType.MAIN_BEACON.name to beaconId
            ]?.state == LegacySubjectMappingState.CUT_OVER.name
        } &&
            liveGroupIds.all { groupId ->
                activeMappingBySource[
                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name to groupId
                ]?.state == LegacySubjectMappingState.CUT_OVER.name
            }

    if (membershipCutoverComplete) {
        val canonicalMembers =
            projectCanonicalMainBeaconMemberships(
                mappings = mappings,
                relations = relations,
            )
        if (canonicalMembers != legacyMembers) {
            mainBeaconDao.deleteAllGroupMembers()
            if (canonicalMembers.isNotEmpty()) mainBeaconDao.insertGroupMembers(canonicalMembers)
        }
    }
}

private fun planNewerMainBeaconCompatibilityWrites(
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    beacons: List<MainBeacon>,
    groups: List<MainBeaconGroup>,
): List<ManagedSubjectEntity> {
    val activeMappings =
        mappings.filter {
            !it.isDeleted &&
                it.state == LegacySubjectMappingState.CUT_OVER.name
        }
    val subjectsById = subjects.associateBy { it.id }
    val beaconsById = beacons.associateBy { it.id }
    val groupsById = groups.associateBy { it.id }
    return activeMappings.mapNotNull { mapping ->
        val subject = subjectsById[mapping.subjectId]?.takeUnless { it.isDeleted } ?: return@mapNotNull null
        val compatibilityValue =
            when (mapping.sourceType) {
                LegacyOrientationSourceType.MAIN_BEACON.name ->
                    beaconsById[mapping.sourceId]?.let { Triple(it.title, it.description, it.updatedAt) }
                LegacyOrientationSourceType.MAIN_BEACON_GROUP.name ->
                    groupsById[mapping.sourceId]?.let { Triple(it.title, it.description, it.updatedAt) }
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
}

internal fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity.hasSameAxisValues(
    other: com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment,
): Boolean =
    importanceValue == other.importance.valueCode && importanceOrigin == other.importance.origin.name &&
        impactValue == other.impact.valueCode && impactOrigin == other.impact.origin.name &&
        breadthValue == other.breadth.valueCode && breadthOrigin == other.breadth.origin.name &&
        expectedSpanValue == other.expectedSpan.valueCode && expectedSpanOrigin == other.expectedSpan.origin.name &&
        targetWindowValue == other.targetWindow.valueCode && targetWindowOrigin == other.targetWindow.origin.name &&
        attentionTierValue == other.attentionTier.valueCode && attentionTierOrigin == other.attentionTier.origin.name &&
        commitmentValue == other.commitment.valueCode && commitmentOrigin == other.commitment.origin.name &&
        confidenceValue == other.confidence.valueCode && confidenceOrigin == other.confidence.origin.name

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

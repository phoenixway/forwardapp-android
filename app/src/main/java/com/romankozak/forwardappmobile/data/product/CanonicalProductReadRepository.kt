package com.romankozak.forwardappmobile.data.product

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.StartupTrace
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.decodeOrientationAssessmentJson
import com.romankozak.forwardappmobile.core.di.ApplicationScope
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.data.planning.day.toDomainStrict as toDayDomainStrict
import com.romankozak.forwardappmobile.data.planning.strategic.toDomainStrict as toStrategicDomainStrict
import com.romankozak.forwardappmobile.data.planning.tactical.toDomainStrict as toTacticalDomainStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadViolation
import com.romankozak.forwardappmobile.shared.core.domain.product.projectCanonicalProductRead
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelation
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.Workspace
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private data class OrientationCoreRows(
    val subjects: List<ManagedSubjectEntity>,
    val orientations: List<OrientationEntity>,
    val assessments: List<OrientationAssessmentEntity>,
    val revisions: List<OrientationAssessmentRevisionEntity>,
)

private data class OrientationEdgeRows(
    val aspects: List<AspectEntity>,
    val workspaceBindings: List<WorkspaceBindingEntity>,
    val relations: List<OrientationRelationEntity>,
    val aspectRefs: List<AspectOrientationRefEntity>,
)

private data class PlanningRows(
    val scopes: List<com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity>,
    val commitments: List<com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningCommitmentEntity>,
    val dayExtensions: List<com.romankozak.forwardappmobile.core.data.models.entities.planning.DayPlanningScopeExtensionEntity>,
    val tacticalExtensions: List<com.romankozak.forwardappmobile.core.data.models.entities.planning.TacticalPlanningScopeExtensionEntity>,
    val strategicExtensions: List<com.romankozak.forwardappmobile.core.data.models.entities.planning.StrategicPlanningScopeExtensionEntity>,
)

private data class CanonicalProductRows(
    val orientationCore: OrientationCoreRows,
    val orientationEdges: OrientationEdgeRows,
    val workspaces: List<WorkspaceEntity>,
    val placements: List<com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity>,
    val planning: PlanningRows,
    val legacyMappings: List<LegacySubjectMappingEntity>,
    val liveGoals: List<Goal>,
)

private val PRODUCT_READ_TABLES =
    arrayOf(
        "managed_subjects",
        "orientations",
        "orientation_assessments",
        "orientation_assessment_revisions",
        "legacy_subject_mappings",
        "goals",
        "aspects",
        "workspace_bindings",
        "orientation_relations",
        "aspect_orientation_refs",
        "workspaces",
        "hierarchy_placements",
        "planning_scopes",
        "planning_commitments",
        "day_planning_scope_extensions",
        "tactical_planning_scope_extensions",
        "strategic_planning_scope_extensions",
    )

internal data class CanonicalGoalReadFrame(
    val productRead: CanonicalProductReadResult,
    val legacyMappings: List<LegacySubjectMappingEntity>,
    val liveGoals: List<Goal>,
)

internal sealed interface CanonicalProductPersistenceFrame {
    data class Ready(
        val snapshot: CanonicalProductReadSnapshot,
        val legacyMappings: List<LegacySubjectMappingEntity> = emptyList(),
        val liveGoals: List<Goal> = emptyList(),
    ) : CanonicalProductPersistenceFrame

    data class IntegrityFailure(
        val violations: List<CanonicalProductReadViolation>,
    ) : CanonicalProductPersistenceFrame
}

/**
 * One Room-backed bulk source for the product-read graph.
 *
 * Every canonical collection is observed once. No per-Orientation persistence
 * query is permitted here; all joins are performed against one in-memory frame.
 */
@Singleton
internal class CanonicalProductReadRoomSource
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        private val gson = Gson()
        private val orientationDao = database.orientationDao()
        private val workspaceDao = database.workspaceDao()
        private val hierarchyDao = database.hierarchyPlacementDao()
        private val planningDao = database.planningDao()
        private val dayExtensionDao = database.dayPlanningScopeExtensionDao()
        private val tacticalExtensionDao = database.tacticalPlanningScopeExtensionDao()
        private val strategicExtensionDao = database.strategicPlanningScopeExtensionDao()

        fun observe(): Flow<CanonicalProductPersistenceFrame> =
            database.invalidationTracker
                .createFlow(*PRODUCT_READ_TABLES, emitInitialState = true)
                .map { StartupTrace.measure("ProductRead.loadRows") { loadRows() } }
                .map { rows ->
                    StartupTrace.measureSync("ProductRead.assembleFrame") {
                        assembleFrame(
                            orientationCore = rows.orientationCore,
                            orientationEdges = rows.orientationEdges,
                            workspaces = rows.workspaces,
                            placements = rows.placements,
                            planning = rows.planning,
                            legacyMappings = rows.legacyMappings,
                            liveGoals = rows.liveGoals,
                        )
                    }
                }.flowOn(Dispatchers.Default)

        private suspend fun loadRows(): CanonicalProductRows =
            database.withTransaction {
                CanonicalProductRows(
                    legacyMappings = orientationDao.getAllLegacyMappings(),
                    liveGoals = database.goalDao().getAllVisible(),
                    orientationCore =
                        OrientationCoreRows(
                            subjects = orientationDao.getAllManagedSubjects(),
                            orientations = orientationDao.getAllOrientations(),
                            assessments = orientationDao.getAllAssessments(),
                            revisions = orientationDao.getAllAssessmentRevisions(),
                        ),
                    orientationEdges =
                        OrientationEdgeRows(
                            aspects = orientationDao.getAllAspects(),
                            workspaceBindings = orientationDao.getAllWorkspaceBindings(),
                            relations = orientationDao.getAllOrientationRelations(),
                            aspectRefs = orientationDao.getAllAspectOrientationRefs(),
                        ),
                    workspaces = workspaceDao.getAll(),
                    placements = hierarchyDao.getLiveHierarchy(HierarchyId.GENERAL.value),
                    planning =
                        PlanningRows(
                            scopes = planningDao.getAllScopes(),
                            commitments = planningDao.getAllCommitments(),
                            dayExtensions = dayExtensionDao.getAll(),
                            tacticalExtensions = tacticalExtensionDao.getAll(),
                            strategicExtensions = strategicExtensionDao.getAll(),
                        ),
                )
            }

        private fun assembleFrame(
            orientationCore: OrientationCoreRows,
            orientationEdges: OrientationEdgeRows,
            workspaces: List<WorkspaceEntity>,
            placements: List<com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity>,
            planning: PlanningRows,
            legacyMappings: List<LegacySubjectMappingEntity>,
            liveGoals: List<Goal>,
        ): CanonicalProductPersistenceFrame {
            val violations = mutableListOf<CanonicalProductReadViolation>()

            val assessmentsByOrientation =
                orientationCore.assessments.associateBy { it.orientationId }
            val revisionsById =
                orientationCore.revisions.associateBy { it.id }

            if (assessmentsByOrientation.size != orientationCore.assessments.size) {
                violations +=
                    violation(
                        path = "orientationAssessments",
                        code = "DUPLICATE_CURRENT_ASSESSMENT",
                        message = "Current Orientation assessment rows must be unique by orientationId",
                    )
            }
            if (revisionsById.size != orientationCore.revisions.size) {
                violations +=
                    violation(
                        path = "orientationAssessmentRevisions",
                        code = "DUPLICATE_ASSESSMENT_REVISION",
                        message = "Orientation assessment revision ids must be unique",
                    )
            }

            val liveOrientations =
                selectProductLiveOrientationRows(
                    subjects = orientationCore.subjects,
                    orientations = orientationCore.orientations,
                    assessmentsByOrientation = assessmentsByOrientation,
                    violations = violations,
                )
            val orientationNodes =
                liveOrientations.mapNotNull { entity ->
                    entity.toDomainNodeOrNull(
                        current = assessmentsByOrientation[entity.subjectId],
                        revisionsById = revisionsById,
                        violations = violations,
                    )
                }

            val snapshot =
                runCatching {
                    CanonicalProductReadSnapshot(
                        subjects = orientationCore.subjects.map { it.toDomain() },
                        orientations = orientationNodes,
                        aspects = orientationEdges.aspects.map { it.toDomain() },
                        workspaces = workspaces.map { it.toDomain() },
                        workspaceBindings = orientationEdges.workspaceBindings.map { it.toDomain() },
                        orientationRelations = orientationEdges.relations.map { it.toDomain() },
                        aspectOrientationRefs = orientationEdges.aspectRefs.map { it.toDomain() },
                        hierarchyPlacements = placements.map { it.toHierarchyPlacementStrict() },
                        planningScopes = planning.scopes.map { it.toPlanningScopeStrict() },
                        planningCommitments =
                            planning.commitments.map {
                                it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                            },
                        dayExtensions = planning.dayExtensions.map { it.toDayDomainStrict() },
                        tacticalExtensions = planning.tacticalExtensions.map { it.toTacticalDomainStrict() },
                        strategicExtensions = planning.strategicExtensions.map { it.toStrategicDomainStrict() },
                    )
                }.getOrElse { failure ->
                    violations +=
                        violation(
                            path = "persistenceFrame",
                            code = "MALFORMED_CANONICAL_ROW",
                            message = failure.message ?: failure::class.java.simpleName,
                        )
                    null
                }

            if (violations.isNotEmpty() || snapshot == null) {
                return CanonicalProductPersistenceFrame.IntegrityFailure(violations)
            }
            return CanonicalProductPersistenceFrame.Ready(snapshot, legacyMappings, liveGoals)
        }

        private fun OrientationEntity.toDomainNodeOrNull(
            current: OrientationAssessmentEntity?,
            revisionsById: Map<String, OrientationAssessmentRevisionEntity>,
            violations: MutableList<CanonicalProductReadViolation>,
        ): OrientationNode? {
            val path = "orientations.$subjectId"
            if (current == null) {
                violations +=
                    violation(path, "MISSING_CURRENT_ASSESSMENT", "Orientation has no current assessment")
                return null
            }
            if (current.isDeleted) {
                violations +=
                    violation(path, "DELETED_CURRENT_ASSESSMENT", "Orientation current assessment is deleted")
                return null
            }

            val revision = revisionsById[current.revisionId]
            if (revision == null || revision.isDeleted || revision.orientationId != subjectId) {
                violations +=
                    violation(
                        path,
                        "INVALID_CURRENT_ASSESSMENT_REVISION",
                        "Orientation current assessment does not reference one live matching revision",
                    )
                return null
            }

            val assessment =
                runCatching {
                    decodeOrientationAssessmentJson(gson, revision.assessmentJson)
                }.getOrElse { failure ->
                    violations +=
                        violation(
                            path,
                            "INVALID_ASSESSMENT_JSON",
                            failure.message ?: "Orientation assessment JSON is invalid",
                        )
                    return null
                }

            if (!current.matchesAssessment(assessment)) {
                violations +=
                    violation(
                        path,
                        "CURRENT_ASSESSMENT_DIVERGED",
                        "Current assessment row diverges from immutable revision",
                    )
                return null
            }

            val kind =
                runCatching { OrientationKind.valueOf(kind) }.getOrElse {
                    violations += violation(path, "INVALID_ORIENTATION_KIND", "Unknown Orientation kind")
                    return null
                }
            val lifecycle =
                lifecycle?.let { raw ->
                    runCatching { OrientationLifecycle.valueOf(raw) }.getOrElse {
                        violations +=
                            violation(path, "INVALID_ORIENTATION_LIFECYCLE", "Unknown Orientation lifecycle")
                        return null
                    }
                }
            val origin =
                runCatching { ValueOrigin.valueOf(lifecycleOrigin) }.getOrElse {
                    violations +=
                        violation(path, "INVALID_LIFECYCLE_ORIGIN", "Unknown Orientation lifecycle origin")
                    return null
                }

            val assessmentViolations = validateOrientationAssessment(kind, assessment)
            if (assessmentViolations.isNotEmpty()) {
                violations +=
                    violation(
                        path,
                        "INVALID_ORIENTATION_ASSESSMENT",
                        assessmentViolations.joinToString(),
                    )
                return null
            }

            return OrientationNode(
                subjectId = subjectId,
                kind = kind,
                lifecycle = lifecycle,
                lifecycleOrigin = origin,
                assessment = assessment,
            )
        }
    }

/**
 * Tombstones are retained for sync and audit, but they do not participate
 * in the live product frame. Inconsistent live links remain fail-closed.
 */
internal fun selectProductLiveOrientationRows(
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    assessmentsByOrientation: Map<String, OrientationAssessmentEntity>,
    violations: MutableList<CanonicalProductReadViolation>,
): List<OrientationEntity> {
    val subjectsById = subjects.groupBy { it.id }

    return orientations.filter { orientation ->
        val matchingSubjects = subjectsById[orientation.subjectId].orEmpty()

        when {
            matchingSubjects.size != 1 -> {
                violations +=
                    violation(
                        path = "orientations.${orientation.subjectId}",
                        code = "ORIENTATION_SUBJECT_INVALID",
                        message = "Orientation must reference exactly one Managed Subject",
                    )
                false
            }

            matchingSubjects.single().isDeleted -> {
                val current = assessmentsByOrientation[orientation.subjectId]
                if (current != null && !current.isDeleted) {
                    violations +=
                        violation(
                            path = "orientations.${orientation.subjectId}",
                            code = "TOMBSTONED_ORIENTATION_LIVE_ASSESSMENT",
                            message = "Deleted Orientation subject still has a live assessment",
                        )
                }
                false
            }

            else -> true
        }
    }
}

/**
 * Android product-read seam. Database state is shared once at application
 * scope; current scope selection remains caller-owned application/domain state.
 */
@Singleton
class CanonicalProductReadRepository
    @Inject
    internal constructor(
        source: CanonicalProductReadRoomSource,
        @ApplicationScope applicationScope: CoroutineScope,
    ) {
        private val sharedFrames =
            source.observe().shareIn(
                scope = applicationScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                replay = 1,
            )

        /**
         * Canonical identities and legacy mappings share one Room snapshot.
         * Never combine separately observed mapping and identity streams.
         */
        internal fun observeWithLegacyMappings():
            Flow<Pair<CanonicalProductReadResult, List<LegacySubjectMappingEntity>>> =
            sharedFrames.map { frame ->
                when (frame) {
                    is CanonicalProductPersistenceFrame.Ready ->
                        projectCanonicalProductRead(
                            frame.snapshot.copy(currentScopeIds = emptySet()),
                        ) to frame.legacyMappings

                    is CanonicalProductPersistenceFrame.IntegrityFailure ->
                        CanonicalProductReadResult.IntegrityFailure(
                            frame.violations,
                        ) to emptyList()
                }
            }.distinctUntilChanged()
                .flowOn(Dispatchers.Default)

        /**
         * Live Goal locators, CUT_OVER mappings and canonical identities
         * originate from one Room read transaction and one emission.
         */
        internal fun observeGoalReadFrame(): Flow<CanonicalGoalReadFrame> =
            sharedFrames.map { frame ->
                when (frame) {
                    is CanonicalProductPersistenceFrame.Ready ->
                        CanonicalGoalReadFrame(
                            productRead =
                                projectCanonicalProductRead(
                                    frame.snapshot.copy(currentScopeIds = emptySet()),
                                ),
                            legacyMappings = frame.legacyMappings,
                            liveGoals = frame.liveGoals,
                        )

                    is CanonicalProductPersistenceFrame.IntegrityFailure ->
                        CanonicalGoalReadFrame(
                            productRead = CanonicalProductReadResult.IntegrityFailure(frame.violations),
                            legacyMappings = emptyList(),
                            liveGoals = emptyList(),
                        )
                }
            }.distinctUntilChanged()
                .flowOn(Dispatchers.Default)

        fun observe(
            currentScopeIds: Flow<Set<PlanningScopeId>> = flowOf(emptySet()),
        ): Flow<CanonicalProductReadResult> =
            combine(
                sharedFrames,
                currentScopeIds.distinctUntilChanged(),
            ) { frame, current ->
                when (frame) {
                    is CanonicalProductPersistenceFrame.Ready ->
                        StartupTrace.measureSync("ProductRead.project") {
                            projectCanonicalProductRead(
                                frame.snapshot.copy(currentScopeIds = current),
                            )
                        }

                    is CanonicalProductPersistenceFrame.IntegrityFailure ->
                        CanonicalProductReadResult.IntegrityFailure(frame.violations)
                }
            }.distinctUntilChanged()
                .flowOn(Dispatchers.Default)

        fun observeOrientation(
            orientationId: String,
            currentScopeIds: Flow<Set<PlanningScopeId>> = flowOf(emptySet()),
        ) =
            observe(currentScopeIds).map { result ->
                when (result) {
                    is CanonicalProductReadResult.Ready ->
                        CanonicalProductOrientationRead.Ready(
                            result.orientations.singleOrNull { it.orientation.subjectId == orientationId },
                        )

                    is CanonicalProductReadResult.IntegrityFailure ->
                        CanonicalProductOrientationRead.IntegrityFailure(result.violations)
                }
            }.distinctUntilChanged()

        fun observePlanningMemberships(
            orientationId: String,
            currentScopeIds: Flow<Set<PlanningScopeId>> = flowOf(emptySet()),
        ): Flow<CanonicalPlanningMembershipRead> =
            observeOrientation(
                orientationId = orientationId,
                currentScopeIds = currentScopeIds,
            ).map { read ->
                when (read) {
                    is CanonicalProductOrientationRead.Ready ->
                        CanonicalPlanningMembershipRead.Ready(
                            read.orientation
                                ?.planningMemberships
                                .orEmpty()
                                .map { membership ->
                                    PlanningMembershipIndicator(
                                        scopeId = membership.scope.id,
                                        scopeKind = membership.scope.kind,
                                        role = membership.commitment.role,
                                        status = membership.commitment.status,
                                        isCurrentScope = membership.isCurrentScope,
                                    )
                                },
                        )

                    is CanonicalProductOrientationRead.IntegrityFailure ->
                        CanonicalPlanningMembershipRead.IntegrityFailure(read.violations)
                }
            }.distinctUntilChanged()
    }

sealed interface CanonicalProductOrientationRead {
    data class Ready(
        val orientation:
            com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalOrientationProductProjection?,
    ) : CanonicalProductOrientationRead

    data class IntegrityFailure(
        val violations: List<CanonicalProductReadViolation>,
    ) : CanonicalProductOrientationRead
}

data class PlanningMembershipIndicator(
    val scopeId: PlanningScopeId,
    val scopeKind: PlanningScopeKind,
    val role: PlanningRoleCode,
    val status: PlanningCommitmentStatus,
    val isCurrentScope: Boolean,
)

sealed interface CanonicalPlanningMembershipRead {
    data class Ready(
        val memberships: List<PlanningMembershipIndicator>,
    ) : CanonicalPlanningMembershipRead

    data class IntegrityFailure(
        val violations: List<CanonicalProductReadViolation>,
    ) : CanonicalPlanningMembershipRead
}

private fun ManagedSubjectEntity.toDomain() =
    ManagedSubject(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        subjectType = ManagedSubjectType.valueOf(subjectType),
        title = title,
        description = description,
    )

private fun AspectEntity.toDomain() =
    AspectNode(
        subjectId = subjectId,
        parentAspectId = parentAspectId,
        order = aspectOrder,
        archived = archived,
    )

private fun WorkspaceEntity.toDomain() =
    Workspace(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        nameOverride = nameOverride,
        descriptionOverride = descriptionOverride,
        parentWorkspaceId = null,
        roleCode = roleCode,
        order = 0L,
        provenance = WorkspaceProvenance.valueOf(provenance),
        sourceContextId = sourceContextId,
    )

private fun WorkspaceBindingEntity.toDomain() =
    WorkspaceBinding(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        subjectId = subjectId,
        bindingType = WorkspaceBindingType.valueOf(bindingType),
        isPrimary = isPrimary,
        order = bindingOrder,
    )

private fun OrientationRelationEntity.toDomain() =
    OrientationRelation(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        fromOrientationId = fromOrientationId,
        toOrientationId = toOrientationId,
        relationType = OrientationRelationType.valueOf(relationType),
        order = relationOrder,
    )

private fun AspectOrientationRefEntity.toDomain() =
    AspectOrientationRef(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        aspectId = aspectId,
        orientationId = orientationId,
        relationType = AspectOrientationRelationType.valueOf(relationType),
        isPrimary = isPrimary,
        order = refOrder,
    )

private fun OrientationAssessmentEntity.matchesAssessment(
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

private fun violation(
    path: String,
    code: String,
    message: String,
) = CanonicalProductReadViolation(
    path = path,
    code = code,
    message = message,
)

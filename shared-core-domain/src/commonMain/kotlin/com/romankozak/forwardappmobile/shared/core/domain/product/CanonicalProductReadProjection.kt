package com.romankozak.forwardappmobile.shared.core.domain.product

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.validateProspectiveHierarchy
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateAspectOrientationRefs
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationRelations
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateWorkspaceBindings
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.planningCommitmentOrderComparator
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateDayPlanningScopeExtensionDependency
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateProspectivePlanningState
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtensionDependency
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateTacticalPlanningScopeExtensionDependency
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelation
import com.romankozak.forwardappmobile.shared.core.models.orientation.Workspace
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding

/** One immutable bulk frame used to derive product reads without persistence writes. */
data class CanonicalProductReadSnapshot(
    val subjects: List<ManagedSubject>,
    val orientations: List<OrientationNode>,
    val aspects: List<AspectNode> = emptyList(),
    val workspaces: List<Workspace> = emptyList(),
    val workspaceBindings: List<WorkspaceBinding> = emptyList(),
    val orientationRelations: List<OrientationRelation> = emptyList(),
    val aspectOrientationRefs: List<AspectOrientationRef> = emptyList(),
    val hierarchyPlacements: List<HierarchyPlacement> = emptyList(),
    val planningScopes: List<PlanningScope> = emptyList(),
    val planningCommitments: List<PlanningCommitment> = emptyList(),
    val dayExtensions: List<DayPlanningScopeExtension> = emptyList(),
    val tacticalExtensions: List<TacticalPlanningScopeExtension> = emptyList(),
    val strategicExtensions: List<StrategicPlanningScopeExtension> = emptyList(),
    /** Application/domain selection only; it is never persisted as a scope fact. */
    val currentScopeIds: Set<PlanningScopeId> = emptySet(),
)

sealed interface PlanningScopeExtensionProjection {
    data class Day(val value: DayPlanningScopeExtension) : PlanningScopeExtensionProjection

    data class Tactical(val value: TacticalPlanningScopeExtension) : PlanningScopeExtensionProjection

    data class Strategic(val value: StrategicPlanningScopeExtension) : PlanningScopeExtensionProjection
}

data class PlanningMembershipProjection(
    val scope: PlanningScope,
    val commitment: PlanningCommitment,
    val extension: PlanningScopeExtensionProjection,
    val isCurrentScope: Boolean,
)

data class WorkspaceMembershipProjection(
    val workspace: Workspace,
    val binding: WorkspaceBinding,
)

data class AspectMembershipProjection(
    val aspectSubject: ManagedSubject,
    val aspect: AspectNode,
    val reference: AspectOrientationRef,
)

enum class HierarchyAppearanceSource {
    ORIENTATION,
    BOUND_WORKSPACE,
}

data class HierarchyAppearanceProjection(
    val placement: HierarchyPlacement,
    val source: HierarchyAppearanceSource,
    val workspaceId: String? = null,
)

enum class OrientationRelationDirection {
    OUTGOING,
    INCOMING,
}

data class OrientationRelationshipProjection(
    val relation: OrientationRelation,
    val direction: OrientationRelationDirection,
    val counterpart: ManagedSubject,
)

/**
 * Product-facing semantic node. Memberships and appearances are edges; they
 * never manufacture a second Orientation identity.
 */
data class CanonicalOrientationProductProjection(
    val subject: ManagedSubject,
    val orientation: OrientationNode,
    val planningMemberships: List<PlanningMembershipProjection>,
    val workspaceMemberships: List<WorkspaceMembershipProjection>,
    val aspectMemberships: List<AspectMembershipProjection>,
    val hierarchyAppearances: List<HierarchyAppearanceProjection>,
    val relationships: List<OrientationRelationshipProjection>,
)

data class CanonicalProductReadViolation(
    val path: String,
    val code: String,
    val message: String,
)

sealed interface CanonicalProductReadResult {
    data class Ready(
        val orientations: List<CanonicalOrientationProductProjection>,
    ) : CanonicalProductReadResult

    data class IntegrityFailure(
        val violations: List<CanonicalProductReadViolation>,
    ) : CanonicalProductReadResult
}

/**
 * Pure, read-only canonical graph projection. All references are validated as
 * one frame so product callers cannot silently fall back to compatibility data.
 */
fun projectCanonicalProductRead(
    snapshot: CanonicalProductReadSnapshot,
    roleRegistry: PlanningRoleRegistry = PlanningRoleRegistry.INITIAL,
): CanonicalProductReadResult {
    val violations = validateSnapshot(snapshot, roleRegistry)
    if (violations.isNotEmpty()) {
        return CanonicalProductReadResult.IntegrityFailure(violations)
    }

    val liveSubjects = snapshot.subjects.filterNot { it.isDeleted }.associateBy { it.id }
    val liveOrientations = snapshot.orientations.associateBy { it.subjectId }
    val liveWorkspaces = snapshot.workspaces.filterNot { it.isDeleted }.associateBy { it.id }
    val liveAspects = snapshot.aspects.associateBy { it.subjectId }
    val liveScopes = snapshot.planningScopes.filterNot { it.isDeleted }.associateBy { it.id }
    val extensions = scopeExtensions(snapshot, liveScopes)
    val placements = snapshot.hierarchyPlacements.filterNot { it.isDeleted }
    val placementsByTarget = placements.groupBy { it.target }
    val bindingsBySubject =
        snapshot.workspaceBindings
            .filterNot { it.isDeleted }
            .sortedWith(compareBy<WorkspaceBinding> { it.order }.thenBy { it.id })
            .groupBy { it.subjectId }
    val commitmentsByOrientation =
        snapshot.planningCommitments
            .filterNot { it.isDeleted }
            .sortedWith(planningCommitmentOrderComparator)
            .groupBy { it.orientationId }
    val aspectRefsByOrientation =
        snapshot.aspectOrientationRefs
            .filterNot { it.isDeleted }
            .sortedWith(compareBy<AspectOrientationRef> { it.order }.thenBy { it.id })
            .groupBy { it.orientationId }
    val relationshipsByOrientation = relationshipsByOrientation(snapshot, liveSubjects)

    val projections =
        liveSubjects.values
            .filter { it.subjectType == ManagedSubjectType.ORIENTATION }
            .map { subject ->
                val orientation = liveOrientations.getValue(subject.id)
                val bindings = bindingsBySubject[subject.id].orEmpty()
                val workspaceMemberships = bindings.map { binding ->
                    WorkspaceMembershipProjection(liveWorkspaces.getValue(binding.workspaceId), binding)
                }
                val directAppearances =
                    placementsByTarget[HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, subject.id)]
                        .orEmpty()
                        .map { HierarchyAppearanceProjection(it, HierarchyAppearanceSource.ORIENTATION) }
                val workspaceAppearances = workspaceMemberships.flatMap { membership ->
                    placementsByTarget[
                        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, membership.workspace.id),
                    ].orEmpty().map { placement ->
                        HierarchyAppearanceProjection(
                            placement = placement,
                            source = HierarchyAppearanceSource.BOUND_WORKSPACE,
                            workspaceId = membership.workspace.id,
                        )
                    }
                }

                CanonicalOrientationProductProjection(
                    subject = subject,
                    orientation = orientation,
                    planningMemberships =
                        commitmentsByOrientation[subject.id]
                            .orEmpty()
                            .map { commitment ->
                                PlanningMembershipProjection(
                                    scope = liveScopes.getValue(commitment.scopeId),
                                    commitment = commitment,
                                    extension = extensions.getValue(commitment.scopeId),
                                    isCurrentScope = commitment.scopeId in snapshot.currentScopeIds,
                                )
                            },
                    workspaceMemberships = workspaceMemberships,
                    aspectMemberships =
                        aspectRefsByOrientation[subject.id]
                            .orEmpty()
                            .map { reference ->
                                AspectMembershipProjection(
                                    aspectSubject = liveSubjects.getValue(reference.aspectId),
                                    aspect = liveAspects.getValue(reference.aspectId),
                                    reference = reference,
                                )
                            },
                    hierarchyAppearances =
                        (directAppearances + workspaceAppearances)
                            .distinctBy { it.placement.id }
                            .sortedWith(compareBy({ it.placement.siblingOrder }, { it.placement.id.value })),
                    relationships = relationshipsByOrientation[subject.id].orEmpty(),
                )
            }.sortedBy { it.subject.id }

    return CanonicalProductReadResult.Ready(projections)
}

private fun validateSnapshot(
    snapshot: CanonicalProductReadSnapshot,
    roleRegistry: PlanningRoleRegistry,
): List<CanonicalProductReadViolation> {
    val violations = mutableListOf<CanonicalProductReadViolation>()
    duplicateIds("subjects", snapshot.subjects, ManagedSubject::id, violations)
    duplicateIds("orientations", snapshot.orientations, OrientationNode::subjectId, violations)
    duplicateIds("aspects", snapshot.aspects, AspectNode::subjectId, violations)
    duplicateIds("workspaces", snapshot.workspaces, Workspace::id, violations)
    duplicateIds("workspaceBindings", snapshot.workspaceBindings, WorkspaceBinding::id, violations)
    duplicateIds("orientationRelations", snapshot.orientationRelations, OrientationRelation::id, violations)
    duplicateIds("aspectOrientationRefs", snapshot.aspectOrientationRefs, AspectOrientationRef::id, violations)

    val subjects = snapshot.subjects.groupBy { it.id }
    val liveSubjects = snapshot.subjects.filterNot { it.isDeleted }.associateBy { it.id }
    val liveOrientationIds =
        liveSubjects.values.filter { it.subjectType == ManagedSubjectType.ORIENTATION }.mapTo(mutableSetOf()) { it.id }
    val liveAspectIds =
        liveSubjects.values.filter { it.subjectType == ManagedSubjectType.ASPECT }.mapTo(mutableSetOf()) { it.id }
    val liveWorkspaceIds = snapshot.workspaces.filterNot { it.isDeleted }.mapTo(mutableSetOf()) { it.id }
    val orientationNodeIds = snapshot.orientations.mapTo(hashSetOf()) { it.subjectId }
    val aspectNodeIds = snapshot.aspects.mapTo(hashSetOf()) { it.subjectId }

    snapshot.orientations.forEach { node ->
        if (subjects[node.subjectId]?.singleOrNull()?.subjectType != ManagedSubjectType.ORIENTATION) {
            violations += violation("orientations.${node.subjectId}", "ORIENTATION_SUBJECT_INVALID")
        }
    }
    liveOrientationIds.filter { it !in orientationNodeIds }.forEach { id ->
        violations += violation("orientations.$id", "MISSING_ORIENTATION_NODE")
    }
    snapshot.aspects.forEach { node ->
        if (subjects[node.subjectId]?.singleOrNull()?.subjectType != ManagedSubjectType.ASPECT) {
            violations += violation("aspects.${node.subjectId}", "ASPECT_SUBJECT_INVALID")
        }
    }
    liveAspectIds.filter { it !in aspectNodeIds }.forEach { id ->
        violations += violation("aspects.$id", "MISSING_ASPECT_NODE")
    }

    validateOrientationRelations(liveOrientationIds, snapshot.orientationRelations)
        .forEach { violations += CanonicalProductReadViolation(it.path, it.code, it.message) }
    validateAspectOrientationRefs(snapshot.aspectOrientationRefs)
        .forEach { violations += CanonicalProductReadViolation(it.path, it.code, it.message) }
    validateWorkspaceBindings(snapshot.workspaceBindings)
        .forEach { violations += CanonicalProductReadViolation(it.path, it.code, it.message) }

    snapshot.workspaceBindings.filterNot { it.isDeleted }.forEach { binding ->
        if (binding.workspaceId !in liveWorkspaceIds) {
            violations += violation("workspaceBindings.${binding.id}", "WORKSPACE_NOT_LIVE")
        }
        if (binding.subjectId !in liveSubjects) {
            violations += violation("workspaceBindings.${binding.id}", "SUBJECT_NOT_LIVE")
        }
    }
    snapshot.aspectOrientationRefs.filterNot { it.isDeleted }.forEach { reference ->
        if (reference.aspectId !in liveAspectIds || reference.orientationId !in liveOrientationIds) {
            violations += violation("aspectOrientationRefs.${reference.id}", "ENDPOINT_NOT_LIVE")
        }
    }

    validateProspectivePlanningState(
        scopes = snapshot.planningScopes,
        commitments = snapshot.planningCommitments,
        isOrientationLive = liveOrientationIds::contains,
        roleRegistry = roleRegistry,
    ).forEach { violations += CanonicalProductReadViolation(it.path, it.code, it.message) }

    val allScopes = snapshot.planningScopes.associateBy { it.id }
    val liveScopes = allScopes.filterValues { !it.isDeleted }
    validateExtensions(snapshot, allScopes, liveScopes, violations)
    snapshot.currentScopeIds.forEach { id ->
        if (id !in liveScopes) violations += violation("currentScopeIds.${id.value}", "SCOPE_NOT_LIVE")
    }

    validateProspectiveHierarchy(snapshot.hierarchyPlacements) { target ->
        when (target.type) {
            HierarchyTargetType.MANAGED_SUBJECT -> target.id in liveSubjects
            HierarchyTargetType.WORKSPACE -> target.id in liveWorkspaceIds
        }
    }.forEach { violations += CanonicalProductReadViolation(it.path, it.code, it.message) }

    return violations.distinctBy { it.path to it.code }
}

private fun validateExtensions(
    snapshot: CanonicalProductReadSnapshot,
    allScopes: Map<PlanningScopeId, PlanningScope>,
    liveScopes: Map<PlanningScopeId, PlanningScope>,
    violations: MutableList<CanonicalProductReadViolation>,
) {
    val allIds =
        snapshot.dayExtensions.map { it.scopeId } +
            snapshot.tacticalExtensions.map { it.scopeId } +
            snapshot.strategicExtensions.map { it.scopeId }
    val extensionCounts = allIds.groupingBy { it }.eachCount()
    val dayExtensionsByScope = snapshot.dayExtensions.groupBy { it.scopeId }
    val tacticalExtensionsByScope = snapshot.tacticalExtensions.groupBy { it.scopeId }
    val strategicExtensionsByScope = snapshot.strategicExtensions.groupBy { it.scopeId }

    extensionCounts.filterValues { it > 1 }.forEach { (id, _) ->
        violations += violation("scopeExtensions.${id.value}", "DUPLICATE_SCOPE_EXTENSION")
    }

    snapshot.dayExtensions.forEach { extension ->
        validateDayPlanningScopeExtensionDependency(extension, allScopes[extension.scopeId]).forEach {
            violations += CanonicalProductReadViolation(it.path, it.code, it.message)
        }
    }
    snapshot.tacticalExtensions.forEach { extension ->
        validateTacticalPlanningScopeExtensionDependency(extension, allScopes[extension.scopeId]).forEach {
            violations += CanonicalProductReadViolation(it.path, it.code, it.message)
        }
    }
    snapshot.strategicExtensions.forEach { extension ->
        validateStrategicPlanningScopeExtensionDependency(extension, allScopes[extension.scopeId]).forEach {
            violations += CanonicalProductReadViolation(it.path, it.code, it.message)
        }
    }

    liveScopes.values.forEach { scope ->
        val matching = extensionCounts[scope.id] ?: 0
        if (matching != 1) {
            violations += violation("scopeExtensions.${scope.id.value}", "MISSING_OR_AMBIGUOUS_SCOPE_EXTENSION")
        }
        val extensionDeleted =
            dayExtensionsByScope[scope.id]?.singleOrNull()?.isDeleted
                ?: tacticalExtensionsByScope[scope.id]?.singleOrNull()?.isDeleted
                ?: strategicExtensionsByScope[scope.id]?.singleOrNull()?.isDeleted
        if (extensionDeleted == true) {
            violations += violation("scopeExtensions.${scope.id.value}", "TOMBSTONED_SCOPE_EXTENSION")
        }
    }
}

private fun scopeExtensions(
    snapshot: CanonicalProductReadSnapshot,
    liveScopes: Map<PlanningScopeId, PlanningScope>,
): Map<PlanningScopeId, PlanningScopeExtensionProjection> =
    buildMap {
        snapshot.dayExtensions.filterNot { it.isDeleted }.forEach { put(it.scopeId, PlanningScopeExtensionProjection.Day(it)) }
        snapshot.tacticalExtensions.filterNot { it.isDeleted }.forEach {
            put(it.scopeId, PlanningScopeExtensionProjection.Tactical(it))
        }
        snapshot.strategicExtensions.filterNot { it.isDeleted }.forEach {
            put(it.scopeId, PlanningScopeExtensionProjection.Strategic(it))
        }
    }.filterKeys(liveScopes::containsKey)

private fun relationshipsByOrientation(
    snapshot: CanonicalProductReadSnapshot,
    liveSubjects: Map<String, ManagedSubject>,
): Map<String, List<OrientationRelationshipProjection>> {
    val indexed = mutableMapOf<String, MutableList<OrientationRelationshipProjection>>()
    snapshot.orientationRelations.filterNot { it.isDeleted }.forEach { relation ->
        indexed.getOrPut(relation.fromOrientationId, ::mutableListOf) +=
            OrientationRelationshipProjection(
                relation,
                OrientationRelationDirection.OUTGOING,
                liveSubjects.getValue(relation.toOrientationId),
            )
        indexed.getOrPut(relation.toOrientationId, ::mutableListOf) +=
            OrientationRelationshipProjection(
                relation,
                OrientationRelationDirection.INCOMING,
                liveSubjects.getValue(relation.fromOrientationId),
            )
    }
    val comparator =
        compareBy<OrientationRelationshipProjection>(
            { it.relation.order ?: Long.MAX_VALUE },
            { it.relation.id },
            { it.direction.name },
        )
    return indexed.mapValues { (_, values) -> values.sortedWith(comparator) }
}

private fun <T> duplicateIds(
    path: String,
    values: List<T>,
    id: (T) -> String,
    violations: MutableList<CanonicalProductReadViolation>,
) {
    values.groupingBy(id).eachCount().filterValues { it > 1 }.forEach { (duplicateId, _) ->
        violations += violation("$path.$duplicateId", "DUPLICATE_ID")
    }
}

private fun violation(path: String, code: String): CanonicalProductReadViolation =
    CanonicalProductReadViolation(path, code, "$path violates canonical product-read integrity: $code")

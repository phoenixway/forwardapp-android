package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningCommitmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.validatePlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.validatePlanningScope

internal class CorruptPlanningPersistenceException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal fun PlanningScopeEntity.toPlanningScopeStrict(): PlanningScope =
    try {
        PlanningScope(
            id = PlanningScopeId(id),
            kind = PlanningScopeKind.valueOf(kind),
            lifecycle = PlanningScopeLifecycle.valueOf(lifecycle),
            title = title,
            startsAt = startsAt,
            endsAt = endsAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also { scope ->
            require(validatePlanningScope(scope).isEmpty()) {
                "Persisted planning scope violates the V3 contract"
            }
        }
    } catch (failure: RuntimeException) {
        throw CorruptPlanningPersistenceException(
            message = "Malformed planning_scopes row id=$id",
            cause = failure,
        )
    }

internal fun PlanningScope.toPlanningScopeEntity(): PlanningScopeEntity =
    PlanningScopeEntity(
        id = id.value,
        kind = kind.name,
        lifecycle = lifecycle.name,
        title = title,
        startsAt = startsAt,
        endsAt = endsAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

internal fun PlanningCommitmentEntity.toPlanningCommitmentStrict(
    roleRegistry: PlanningRoleRegistry,
): PlanningCommitment =
    try {
        PlanningCommitment(
            id = PlanningCommitmentId(id),
            scopeId = PlanningScopeId(scopeId),
            orientationId = orientationId,
            role = PlanningRoleCode.fromStableCode(role),
            order = commitmentOrder,
            priorityLevel = priorityLevel?.let(PlanningPriorityLevel::valueOf),
            status = PlanningCommitmentStatus.valueOf(status),
            provenance =
                PlanningProvenance(
                    kind = PlanningProvenanceKind.valueOf(provenanceKind),
                    sourceType = provenanceSourceType,
                    sourceId = provenanceSourceId,
                ),
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also { commitment ->
            require(validatePlanningCommitment(commitment, roleRegistry).isEmpty()) {
                "Persisted planning commitment violates the V3 contract"
            }
        }
    } catch (failure: RuntimeException) {
        throw CorruptPlanningPersistenceException(
            message = "Malformed planning_commitments row id=$id",
            cause = failure,
        )
    }

internal fun PlanningCommitment.toPlanningCommitmentEntity(): PlanningCommitmentEntity =
    PlanningCommitmentEntity(
        id = id.value,
        scopeId = scopeId.value,
        orientationId = orientationId,
        role = role.value,
        commitmentOrder = order,
        priorityLevel = priorityLevel?.name,
        status = status.name,
        provenanceKind = provenance.kind.name,
        provenanceSourceType = provenance.sourceType,
        provenanceSourceId = provenance.sourceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

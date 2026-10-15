package com.romankozak.forwardappmobile.data.planning.strategic

import com.romankozak.forwardappmobile.core.data.models.entities.planning.StrategicPlanningScopeExtensionEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateStrategicPlanningScopeExtension

internal class CorruptStrategicPlanningScopeExtensionException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal fun StrategicPlanningScopeExtensionEntity.toDomainStrict(): StrategicPlanningScopeExtension =
    try {
        StrategicPlanningScopeExtension(
            scopeId = PlanningScopeId(scopeId),
            legacyArcKey = legacyArcKey,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also { extension ->
            require(validateStrategicPlanningScopeExtension(extension).isEmpty()) {
                "Persisted Strategic planning extension violates the V3 contract"
            }
        }
    } catch (failure: RuntimeException) {
        throw CorruptStrategicPlanningScopeExtensionException(
            message = "Malformed strategic_planning_scope_extensions row scopeId=$scopeId",
            cause = failure,
        )
    }

internal fun StrategicPlanningScopeExtension.toEntity(): StrategicPlanningScopeExtensionEntity =
    StrategicPlanningScopeExtensionEntity(
        scopeId = scopeId.value,
        legacyArcKey = legacyArcKey,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

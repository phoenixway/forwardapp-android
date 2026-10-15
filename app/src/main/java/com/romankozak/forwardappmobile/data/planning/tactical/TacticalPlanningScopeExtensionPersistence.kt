package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.planning.TacticalPlanningScopeExtensionEntity
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeType
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateTacticalPlanningScopeExtension

internal class CorruptTacticalPlanningScopeExtensionException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal fun TacticalPlanningScopeExtensionEntity.toDomainStrict(): TacticalPlanningScopeExtension =
    try {
        TacticalPlanningScopeExtension(
            scopeId = PlanningScopeId(scopeId),
            type = iterationType.toTacticalPlanningScopeTypeStable(),
            weekKey = weekKey,
            closedAt = closedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        ).also { extension ->
            require(validateTacticalPlanningScopeExtension(extension).isEmpty()) {
                "Persisted Tactical planning extension violates the V3 contract"
            }
        }
    } catch (failure: RuntimeException) {
        throw CorruptTacticalPlanningScopeExtensionException(
            message = "Malformed tactical_planning_scope_extensions row scopeId=$scopeId",
            cause = failure,
        )
    }

internal fun TacticalPlanningScopeExtension.toEntity(): TacticalPlanningScopeExtensionEntity =
    TacticalPlanningScopeExtensionEntity(
        scopeId = scopeId.value,
        iterationType = type.toStableCode(),
        weekKey = weekKey,
        closedAt = closedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

internal fun TacticalPlanningScopeType.toStableCode(): String =
    when (this) {
        TacticalPlanningScopeType.TIMEBOXED -> "TIMEBOXED"
        TacticalPlanningScopeType.OPEN_ENDED -> "OPEN_ENDED"
    }

internal fun String.toTacticalPlanningScopeTypeStable(): TacticalPlanningScopeType =
    when (this) {
        "TIMEBOXED" -> TacticalPlanningScopeType.TIMEBOXED
        "OPEN_ENDED" -> TacticalPlanningScopeType.OPEN_ENDED
        else -> throw IllegalArgumentException("Unknown Tactical planning scope type code: $this")
    }

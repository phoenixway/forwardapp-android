package com.romankozak.forwardappmobile.data.planning.tactical

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeType
import com.romankozak.forwardappmobile.shared.core.domain.planning.validatePlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.validateTacticalPlanningScopeExtensionDependency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V3.5g Tactical-cycle runtime authority adapter.
 *
 * Common planning facts come only from PlanningScope plus the typed Tactical
 * extension after marker admission. tactical_iterations is retained solely as
 * a one-way compatibility projection for older transport/runtime surfaces.
 */
@Singleton
class CanonicalTacticalPlanningAdapter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val authorityActivator: CanonicalTacticalPlanningAuthorityActivator,
    ) {
        fun observeIterations(): Flow<List<TacticalIteration>> =
            flow {
                authorityActivator.requireActivated()
                emitAll(
                    database.invalidationTracker.createFlow(
                        "tactical_planning_authority_state", "planning_scopes", "tactical_planning_scope_extensions",
                    ).map { database.withTransaction {
                        val marker = database.tacticalPlanningAuthorityStateDao().get()
                        val scopeEntities = database.planningDao().getAllScopes()
                        val extensionEntities = database.tacticalPlanningScopeExtensionDao().getAll()
                        requireNotNull(marker) {
                            "Canonical Tactical planning authority is not activated"
                        }
                        require(marker.version == CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION) {
                            "Unsupported Tactical planning authority marker version ${marker.version}"
                        }

                        val extensions =
                            extensionEntities
                                .map { it.toDomainStrict() }
                                .associateBy { it.scopeId.value }

                        scopeEntities
                            .map { it.toPlanningScopeStrict() }
                            .filter {
                                it.kind == PlanningScopeKind.TACTICAL_CYCLE &&
                                    !it.isDeleted
                            }
                            .map { scope ->
                                val extension =
                                    requireNotNull(extensions[scope.id.value]) {
                                        "Canonical Tactical scope ${scope.id.value} is missing its extension"
                                    }
                                scope.toTacticalIteration(extension)
                            }
                            .sortedWith(
                                compareByDescending<TacticalIteration> { it.startedAt }
                                    .thenByDescending { it.createdAt },
                            )
                    } }.distinctUntilChanged().flowOn(Dispatchers.Default),
                )
            }

        suspend fun getCurrentIteration(): TacticalIteration? {
            authorityActivator.requireActivated()
            val scopes =
                database.planningDao().getAllScopes()
                    .map { it.toPlanningScopeStrict() }
                    .filter {
                        it.kind == PlanningScopeKind.TACTICAL_CYCLE &&
                            !it.isDeleted &&
                            it.lifecycle in
                            setOf(
                                PlanningScopeLifecycle.DRAFT,
                                PlanningScopeLifecycle.ACTIVE,
                            )
                    }
                    .sortedWith(
                        compareByDescending<PlanningScope> { it.startsAt ?: Long.MIN_VALUE }
                            .thenByDescending { it.createdAt },
                    )

            return scopes.firstOrNull()?.let { project(it) }
        }

        suspend fun getById(iterationId: String): TacticalIteration? {
            authorityActivator.requireActivated()
            val scope =
                database.planningDao().getScope(iterationId)
                    ?.toPlanningScopeStrict()
                    ?: return null
            if (scope.kind != PlanningScopeKind.TACTICAL_CYCLE) return null
            return project(scope)
        }

        suspend fun saveIteration(iteration: TacticalIteration) {
            authorityActivator.requireActivated()
            val scope = iteration.toCanonicalTacticalPlanningScope()
            val extension = iteration.toCanonicalTacticalPlanningScopeExtension()

            database.withTransaction {
                val planningDao = database.planningDao()
                val extensionDao = database.tacticalPlanningScopeExtensionDao()

                val currentScope =
                    planningDao.getScope(scope.id.value)?.toPlanningScopeStrict()
                val currentExtension =
                    extensionDao.get(extension.scopeId.value)?.toDomainStrict()

                requireForwardScopeRevision(currentScope, scope)
                requireForwardExtensionRevision(currentExtension, extension)

                val scopeViolations = validatePlanningScope(scope)
                require(scopeViolations.isEmpty()) {
                    "Tactical PlanningScope violates V3 contract: $scopeViolations"
                }

                val extensionViolations =
                    validateTacticalPlanningScopeExtensionDependency(extension, scope)
                require(extensionViolations.isEmpty()) {
                    "Tactical planning extension violates V3 contract: $extensionViolations"
                }

                planningDao.upsertScope(scope.toPlanningScopeEntity())
                extensionDao.upsert(extension.toEntity())

                // Compatibility projection only. Runtime reads never consume
                // common planning facts from this row after admission.
                database.tacticalIterationDao().insert(iteration)
            }
        }

        private suspend fun project(scope: PlanningScope): TacticalIteration {
            val extension =
                requireNotNull(
                    database.tacticalPlanningScopeExtensionDao()
                        .get(scope.id.value)
                        ?.toDomainStrict(),
                ) {
                    "Canonical Tactical scope ${scope.id.value} is missing its extension"
                }
            return scope.toTacticalIteration(extension)
        }
    }

private fun requireForwardScopeRevision(
    current: PlanningScope?,
    incoming: PlanningScope,
) {
    if (current == null) return
    if (current == incoming) return

    require(current.kind == PlanningScopeKind.TACTICAL_CYCLE) {
        "Planning scope kind is immutable"
    }
    require(incoming.createdAt == current.createdAt) {
        "Tactical scope createdAt is immutable"
    }
    require(incoming.version > current.version) {
        "Tactical scope update must advance version"
    }
}

private fun requireForwardExtensionRevision(
    current: TacticalPlanningScopeExtension?,
    incoming: TacticalPlanningScopeExtension,
) {
    if (current == null) return
    if (current == incoming) return

    require(incoming.createdAt == current.createdAt) {
        "Tactical extension createdAt is immutable"
    }
    require(incoming.version > current.version) {
        "Tactical extension update must advance version"
    }
}

internal fun PlanningScope.toTacticalIteration(
    extension: TacticalPlanningScopeExtension,
): TacticalIteration {
    require(kind == PlanningScopeKind.TACTICAL_CYCLE) {
        "Planning scope ${id.value} is not a Tactical cycle"
    }
    val violations =
        validateTacticalPlanningScopeExtensionDependency(extension, this)
    require(violations.isEmpty()) {
        "Canonical Tactical scope ${id.value} violates extension dependency: $violations"
    }

    val tacticalTitle =
        requireNotNull(title) {
            "Canonical Tactical scope ${id.value} has no title"
        }
    val startedAt =
        requireNotNull(startsAt) {
            "Canonical Tactical scope ${id.value} has no start timestamp"
        }

    return TacticalIteration(
        id = id.value,
        title = tacticalTitle,
        startedAt = startedAt,
        plannedEndAt = endsAt,
        closedAt = extension.closedAt,
        status =
            when (lifecycle) {
                PlanningScopeLifecycle.DRAFT -> TacticalIterationStatus.DRAFT
                PlanningScopeLifecycle.ACTIVE -> TacticalIterationStatus.ACTIVE
                PlanningScopeLifecycle.CLOSED -> TacticalIterationStatus.CLOSED
                PlanningScopeLifecycle.ARCHIVED -> TacticalIterationStatus.ARCHIVED
            },
        type =
            when (extension.type) {
                TacticalPlanningScopeType.TIMEBOXED -> TacticalIterationType.TIMEBOXED
                TacticalPlanningScopeType.OPEN_ENDED -> TacticalIterationType.OPEN_ENDED
            },
        weekKey = extension.weekKey,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )
}

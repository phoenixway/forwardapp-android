package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.orientation.ThemeOrientationBridge
import com.romankozak.forwardappmobile.data.planning.PlanningDao
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import javax.inject.Inject
import javax.inject.Singleton

data class DayPlanningActivationReport(
    val performed: Boolean,
    val scopes: Int,
    val commitments: Int,
)

/** One-shot, fail-closed activation of canonical Day planning authority. */
@Singleton
class CanonicalDayPlanningAuthorityActivator
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dayPlanDao: DayPlanDao,
        private val dayThemeDao: CanonicalDayThemeDao,
        private val orientationDao: OrientationDao,
        private val planningDao: PlanningDao,
        private val extensionDao: DayPlanningScopeExtensionDao,
        private val markerDao: DayPlanningAuthorityStateDao,
        private val themeOrientationBridge: ThemeOrientationBridge,
    ) {
        suspend fun ensureActivated(now: Long = System.currentTimeMillis()): DayPlanningActivationReport {
            themeOrientationBridge.ensureCutOver()
            return database.withTransaction {
                val marker = markerDao.get()
                if (marker != null) {
                    require(marker.version == CURRENT_VERSION) {
                        "Unsupported Day planning authority marker version ${marker.version}"
                    }
                    validateEstablishedState()
                    return@withTransaction currentReport(performed = false)
                }

                val plan = buildPlan()
                requirePristineOrSemanticallyExact(plan)
                val missingScopes =
                    plan.scopes.mapNotNull { scope ->
                        scope.takeIf { planningDao.getScope(scope.id.value) == null }
                            ?.toPlanningScopeEntity()
                    }
                val missingExtensions =
                    plan.extensions.mapNotNull { extension ->
                        extension.takeIf { extensionDao.get(extension.scopeId.value) == null }
                            ?.toEntity()
                    }
                val missingCommitments =
                    plan.commitments.mapNotNull { commitment ->
                        commitment.takeIf { planningDao.getCommitment(commitment.id.value) == null }
                            ?.toPlanningCommitmentEntity()
                    }
                planningDao.upsertScopes(missingScopes)
                extensionDao.upsertAll(missingExtensions)
                planningDao.upsertCommitments(missingCommitments)
                requireSemanticParity(plan)
                markerDao.upsert(
                    DayPlanningAuthorityStateEntity(
                        version = CURRENT_VERSION,
                        activatedAt = now,
                    ),
                )
                DayPlanningActivationReport(true, plan.scopes.size, plan.commitments.size)
            }
        }

        suspend fun requireActivated() {
            val marker = requireNotNull(markerDao.get()) {
                "Canonical Day planning authority is not activated"
            }
            require(marker.version == CURRENT_VERSION) {
                "Unsupported Day planning authority marker version ${marker.version}"
            }
        }

        /**
         * Explicit compatibility ingress only. Converts the accepted legacy merge winner once;
         * ordinary startup and runtime reads never invoke this path.
         */
        suspend fun ingestLegacyCompatibilityProjection(now: Long = System.currentTimeMillis()) {
            themeOrientationBridge.ensureCutOver()
            database.withTransaction {
                requireNotNull(markerDao.get()) {
                    "Legacy Day planning ingress requires activated canonical authority"
                }
                val desired = buildPlan()
                desired.scopes.forEach { incoming ->
                    val current = planningDao.getScope(incoming.id.value)?.toPlanningScopeStrict()
                    val stored =
                        when {
                            current == null -> incoming
                            current.sameDayPlanningFacts(incoming) -> current
                            else ->
                                current.copy(
                                    lifecycle = incoming.lifecycle,
                                    title = incoming.title,
                                    startsAt = incoming.startsAt,
                                    endsAt = incoming.endsAt,
                                    updatedAt = now,
                                    syncedAt = null,
                                    version = current.version + 1L,
                                    isDeleted = incoming.isDeleted,
                                )
                        }
                    planningDao.upsertScope(stored.toPlanningScopeEntity())
                }
                desired.extensions.forEach { incoming ->
                    val current = extensionDao.get(incoming.scopeId.value)?.toDomainStrict()
                    val stored =
                        when {
                            current == null -> incoming
                            current.sameDayPlanningFacts(incoming) -> current
                            else ->
                                current.copy(
                                    calendarDayStartMillis = incoming.calendarDayStartMillis,
                                    outcome = incoming.outcome,
                                    plannedBudgetMinutes = incoming.plannedBudgetMinutes,
                                    updatedAt = now,
                                    syncedAt = null,
                                    version = current.version + 1L,
                                    isDeleted = incoming.isDeleted,
                                )
                        }
                    extensionDao.upsert(stored.toEntity())
                }
                desired.commitments.forEach { incoming ->
                    val current =
                        planningDao.getCommitment(incoming.id.value)
                            ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                    val stored =
                        when {
                            current == null -> incoming
                            current.sameDayPlanningFacts(incoming) -> current
                            else -> {
                                require(current.orientationId == incoming.orientationId) {
                                    "Legacy DAY.THEME ingress cannot change canonical target"
                                }
                                current.copy(
                                    order = incoming.order,
                                    status = incoming.status,
                                    updatedAt = now,
                                    syncedAt = null,
                                    version = current.version + 1L,
                                    isDeleted = incoming.isDeleted,
                                )
                            }
                        }
                    planningDao.upsertCommitment(stored.toPlanningCommitmentEntity())
                }
                requireSemanticParity(desired)
            }
        }

        /** Refreshes physical compatibility columns from canonical owners without creating sync authority. */
        suspend fun projectCompatibilityRows() {
            requireActivated()
            database.withTransaction {
                val scopes = planningDao.getAllScopes().associateBy { it.id }
                val extensions = extensionDao.getAll().associateBy { it.scopeId }
                dayPlanDao.getAllPlansSync().forEach { legacy ->
                    val scope =
                        requireNotNull(scopes[legacy.id]) { "Canonical Day ${legacy.id} is missing its scope" }
                            .toPlanningScopeStrict()
                    val extension =
                        requireNotNull(extensions[legacy.id]) { "Canonical Day ${legacy.id} is missing its extension" }
                            .toDomainStrict()
                    dayPlanDao.projectCanonicalPlanningFields(
                        planId = legacy.id,
                        date = extension.calendarDayStartMillis,
                        name = scope.title,
                        status = legacyStatus(scope.lifecycle, extension.outcome),
                        predictedDurationMinutes = extension.plannedBudgetMinutes,
                    )
                }
                val commitments =
                    planningDao.getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .filter { it.role == com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode.DAY_THEME }
                        .associateBy { it.provenance.sourceId }
                dayThemeDao.getAllDayThemesSync().filterNot { it.isDeleted }.forEach { legacy ->
                    val commitment = requireNotNull(commitments[legacy.id]) {
                        "DayTheme ${legacy.id} is missing its canonical commitment"
                    }
                    dayThemeDao.projectPlanningFields(
                        id = legacy.id,
                        order = commitment.order,
                        isActive = commitment.status ==
                            com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus.ACTIVE,
                    )
                }
            }
        }

        private suspend fun buildPlan(): CanonicalDayPlanningCutoverPlan =
            planCanonicalDayPlanningCutover(
                dayPlans = dayPlanDao.getAllPlansSync(),
                dayThemes = dayThemeDao.getAllDayThemesSync(),
                mappings = orientationDao.getAllLegacyMappings(),
                subjects = orientationDao.getAllManagedSubjects(),
                orientations = orientationDao.getAllOrientations(),
            )

        private suspend fun requirePristineOrSemanticallyExact(plan: CanonicalDayPlanningCutoverPlan) {
            plan.scopes.forEach { expected ->
                planningDao.getScope(expected.id.value)?.toPlanningScopeStrict()?.let { actual ->
                    require(actual.sameDayPlanningFacts(expected)) {
                        "Existing canonical DAY scope ${expected.id.value} diverges from migration input"
                    }
                }
            }
            plan.extensions.forEach { expected ->
                extensionDao.get(expected.scopeId.value)?.toDomainStrict()?.let { actual ->
                    require(actual.sameDayPlanningFacts(expected)) {
                        "Existing Day extension ${expected.scopeId.value} diverges from migration input"
                    }
                }
            }
            plan.commitments.forEach { expected ->
                planningDao.getCommitment(expected.id.value)
                    ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                    ?.let { actual ->
                        require(actual.sameDayPlanningFacts(expected)) {
                            "Existing DAY.THEME commitment ${expected.id.value} diverges from migration input"
                        }
                    }
            }
        }

        private suspend fun requireSemanticParity(plan: CanonicalDayPlanningCutoverPlan) {
            plan.scopes.forEach { expected ->
                require(
                    planningDao.getScope(expected.id.value)?.toPlanningScopeStrict()
                        ?.sameDayPlanningFacts(expected) == true,
                )
            }
            plan.extensions.forEach { expected ->
                require(
                    extensionDao.get(expected.scopeId.value)?.toDomainStrict()
                        ?.sameDayPlanningFacts(expected) == true,
                )
            }
            plan.commitments.forEach { expected ->
                require(
                    planningDao.getCommitment(expected.id.value)
                        ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                        ?.sameDayPlanningFacts(expected) == true,
                )
            }
        }

        private suspend fun validateEstablishedState() {
            dayPlanDao.getAllPlansSync().forEach { legacy ->
                val scope =
                    requireNotNull(planningDao.getScope(legacy.id)?.toPlanningScopeStrict()) {
                        "Activated Day ${legacy.id} is missing its canonical scope"
                    }
                require(scope.kind.name == "DAY") {
                    "Activated Day ${legacy.id} canonical scope is not DAY"
                }
                requireNotNull(extensionDao.get(legacy.id)?.toDomainStrict()) {
                    "Activated Day ${legacy.id} is missing its canonical extension"
                }
            }
            dayThemeDao.getAllDayThemesSync().filterNot { it.isDeleted }.forEach { legacy ->
                requireNotNull(
                    planningDao.getCommitment(canonicalDayThemeCommitmentId(legacy.id).value),
                ) { "Activated DayTheme ${legacy.id} is missing its canonical commitment" }
            }
        }

        private suspend fun currentReport(performed: Boolean): DayPlanningActivationReport {
            val scopes = dayPlanDao.getAllPlansSync().size
            val commitments = dayThemeDao.getAllDayThemesSync().size
            return DayPlanningActivationReport(performed, scopes, commitments)
        }

        companion object {
            const val CURRENT_VERSION = 1
        }
    }

private fun com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope.sameDayPlanningFacts(
    other: com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope,
): Boolean =
    id == other.id && kind == other.kind && lifecycle == other.lifecycle && title == other.title &&
        startsAt == other.startsAt && endsAt == other.endsAt && isDeleted == other.isDeleted

private fun com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension.sameDayPlanningFacts(
    other: com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension,
): Boolean =
    scopeId == other.scopeId && calendarDayStartMillis == other.calendarDayStartMillis &&
        outcome == other.outcome && plannedBudgetMinutes == other.plannedBudgetMinutes &&
        isDeleted == other.isDeleted

private fun com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment.sameDayPlanningFacts(
    other: com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment,
): Boolean =
    id == other.id && scopeId == other.scopeId && orientationId == other.orientationId &&
        role == other.role && order == other.order && priorityLevel == other.priorityLevel &&
        status == other.status && isDeleted == other.isDeleted

private fun legacyStatus(
    lifecycle: com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle,
    outcome: com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome?,
): com.romankozak.forwardappmobile.core.data.models.entities.DayStatus =
    when (lifecycle) {
        com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle.DRAFT ->
            com.romankozak.forwardappmobile.core.data.models.entities.DayStatus.PLANNED
        com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle.ACTIVE ->
            com.romankozak.forwardappmobile.core.data.models.entities.DayStatus.IN_PROGRESS
        com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle.CLOSED ->
            when (outcome) {
                com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome.COMPLETED ->
                    com.romankozak.forwardappmobile.core.data.models.entities.DayStatus.COMPLETED
                com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome.MISSED ->
                    com.romankozak.forwardappmobile.core.data.models.entities.DayStatus.MISSED
                null -> error("Closed DAY scope requires outcome")
            }
        com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle.ARCHIVED ->
            com.romankozak.forwardappmobile.core.data.models.entities.DayStatus.ARCHIVED
    }

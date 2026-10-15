package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.data.orientation.ThemeOrientationBridge
import com.romankozak.forwardappmobile.data.planning.PlanningDao
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CanonicalDayThemePlanningAdapter
    @Inject
    constructor(
        private val planningDao: PlanningDao,
        private val themeBridge: ThemeOrientationBridge,
        private val authorityActivator: CanonicalDayPlanningAuthorityActivator,
    ) {
        fun observeDayThemeCommitments(dayPlanId: String): Flow<List<PlanningCommitment>> =
            planningDao.observeLiveCommitmentsForScope(dayPlanId).map { rows ->
                rows.map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                    .filter { it.role == PlanningRoleCode.DAY_THEME }
            }

        suspend fun getDayThemeCommitments(dayPlanId: String): List<PlanningCommitment> =
            planningDao.getLiveCommitmentsForRole(dayPlanId, PlanningRoleCode.DAY_THEME.value)
                .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }

        suspend fun applyProjectionChanges(changes: List<DayThemeSnapshot>) {
            authorityActivator.requireActivated()
            changes.forEach { source ->
                val id = canonicalDayThemeCommitmentId(source.id)
                val existing =
                    planningDao.getCommitment(id.value)
                        ?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                if (source.isDeleted) {
                    if (existing != null && !existing.isDeleted) {
                        planningDao.upsertCommitment(
                            existing.copy(
                                status = PlanningCommitmentStatus.DROPPED,
                                updatedAt = source.updatedAt,
                                syncedAt = null,
                                version = existing.version + 1L,
                                isDeleted = true,
                            ).toPlanningCommitmentEntity(),
                        )
                    }
                    return@forEach
                }

                val orientationId = themeBridge.requireLiveThemeSubjectId(source.themeId)
                val desiredStatus =
                    if (source.isActive) PlanningCommitmentStatus.ACTIVE else PlanningCommitmentStatus.PAUSED
                if (existing == null) {
                    planningDao.upsertCommitment(
                        PlanningCommitment(
                            id = id,
                            scopeId = PlanningScopeId(source.dayPlanId),
                            orientationId = orientationId,
                            role = PlanningRoleCode.DAY_THEME,
                            order = source.order,
                            priorityLevel = null,
                            status = desiredStatus,
                            provenance =
                                PlanningProvenance(
                                    PlanningProvenanceKind.MANUAL,
                                    sourceType = "DAY_THEME",
                                    sourceId = source.id,
                                ),
                            createdAt = source.createdAt,
                            updatedAt = source.updatedAt,
                            syncedAt = null,
                            version = 1L,
                            isDeleted = false,
                        ).toPlanningCommitmentEntity(),
                    )
                } else if (
                    existing.isDeleted ||
                    existing.orientationId != orientationId ||
                    existing.order != source.order ||
                    existing.status != desiredStatus
                ) {
                    require(existing.orientationId == orientationId) {
                        "DAY.THEME commitment ${existing.id.value} cannot change semantic target"
                    }
                    planningDao.upsertCommitment(
                        existing.copy(
                            order = source.order,
                            status = desiredStatus,
                            updatedAt = source.updatedAt,
                            syncedAt = null,
                            version = existing.version + 1L,
                            isDeleted = false,
                        ).toPlanningCommitmentEntity(),
                    )
                }
            }
        }

        fun applyCanonicalProjection(
            rows: List<DayThemeSnapshot>,
            commitments: List<PlanningCommitment>,
        ): List<DayThemeSnapshot> {
            val bySourceId =
                commitments.associateBy { commitment ->
                    require(commitment.provenance.sourceType == "DAY_THEME") {
                        "DAY.THEME commitment ${commitment.id.value} has invalid provenance"
                    }
                    requireNotNull(commitment.provenance.sourceId)
                }
            return rows.map { row ->
                if (row.isDeleted) return@map row
                val commitment = requireNotNull(bySourceId[row.id]) {
                    "DayTheme ${row.id} is missing canonical DAY.THEME commitment"
                }
                row.copy(
                    order = commitment.order,
                    isActive = commitment.status == PlanningCommitmentStatus.ACTIVE,
                )
            }
        }
    }

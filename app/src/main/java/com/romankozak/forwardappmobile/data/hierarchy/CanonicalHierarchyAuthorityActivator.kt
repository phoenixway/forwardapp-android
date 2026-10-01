package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.database.HierarchyAuthorityActivationStateEntity
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOriginEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import javax.inject.Inject
import javax.inject.Singleton

data class CanonicalHierarchyAuthorityActivationReport(
    val performed: Boolean,
    val materialization: CanonicalV1HierarchyMaterializationReport?,
)

/**
 * Finite pre-P2 establishment boundary for GENERAL hierarchy authority.
 *
 * This is not a runtime V1 -> V2 reconciler and does not switch the production
 * authority mode. On the first successful invocation it selects explicit
 * origin-authorized establishment evidence, requires exact/pristine deterministic
 * H2 materialization, establishes all
 * canonical occurrence provenance, and writes the durable activation marker in
 * the same Room transaction.
 *
 * Once the marker exists, later invocations return before reading any
 * establishment source. Therefore post-cutover legacy drift can never
 * rematerialize or repair H1.
 */
@Singleton
class CanonicalHierarchyAuthorityActivator
    @Inject
    constructor(
        private val database: AppDatabase,
        private val freshSource: CanonicalFreshHierarchyEstablishmentSource,
        private val snapshotBuilder: CanonicalV1HierarchySnapshotBuilder,
        private val materializer: CanonicalV1HierarchyMaterializer,
    ) {
        suspend fun ensureEstablished(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long = System.currentTimeMillis(),
        ): CanonicalHierarchyAuthorityActivationReport {
            require(hierarchyId == HierarchyId.GENERAL) {
                "P2 authority activation currently supports only GENERAL"
            }

            return database.withTransaction {
                val markerDao = database.hierarchyAuthorityActivationStateDao()
                val existing = markerDao.get(hierarchyId.value)
                if (existing != null) {
                    require(existing.version in MIN_SUPPORTED_ACTIVATION_VERSION..CURRENT_ACTIVATION_VERSION) {
                        "Unsupported hierarchy authority activation marker version " +
                            "${existing.version} for ${hierarchyId.value}"
                    }
                    if (existing.version < CURRENT_ACTIVATION_VERSION) {
                        // Any supported older marker already proves H1 authority.
                        // Schema 180 has no physical legacy topology left, so
                        // convergence is marker-only.
                        markerDao.upsert(
                            existing.copy(version = CURRENT_ACTIVATION_VERSION),
                        )
                    }
                    database.hierarchyEstablishmentOriginDao().upsert(
                        HierarchyEstablishmentOriginEntity(
                            hierarchyId = hierarchyId.value,
                            origin = HierarchyEstablishmentOrigin.ESTABLISHED.name,
                        ),
                    )
                    return@withTransaction CanonicalHierarchyAuthorityActivationReport(
                        performed = false,
                        materialization = null,
                    )
                }

                val origin = requireEstablishmentOrigin(hierarchyId)
                val input =
                    when (origin) {
                        HierarchyEstablishmentOrigin.FRESH_NATIVE ->
                            freshSource.readInCurrentTransaction()

                        HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE ->
                            error(
                                "Schema 180 cannot establish hierarchy from retired legacy storage",
                            )

                        HierarchyEstablishmentOrigin.ESTABLISHED ->
                            error(
                                "Hierarchy ${hierarchyId.value} is classified ESTABLISHED " +
                                    "without an activation marker",
                            )
                    }

                val snapshot =
                    snapshotBuilder.build(
                        input = input,
                        hierarchyId = hierarchyId,
                    )
                val report =
                    materializer.materializeInCurrentTransaction(
                        snapshot = snapshot,
                        now = now,
                    )

                // Schema 180 stores GENERAL topology only in canonical H1.
                // No physical legacy hierarchy source remains to neutralize.

                markerDao.upsert(
                    HierarchyAuthorityActivationStateEntity(
                        hierarchyId = hierarchyId.value,
                        version = CURRENT_ACTIVATION_VERSION,
                        activatedAt = now,
                    ),
                )
                database.hierarchyEstablishmentOriginDao().upsert(
                    HierarchyEstablishmentOriginEntity(
                        hierarchyId = hierarchyId.value,
                        origin = HierarchyEstablishmentOrigin.ESTABLISHED.name,
                    ),
                )

                CanonicalHierarchyAuthorityActivationReport(
                    performed = true,
                    materialization = report,
                )
            }
        }

        private suspend fun requireEstablishmentOrigin(
            hierarchyId: HierarchyId,
        ): HierarchyEstablishmentOrigin {
            val stored =
                requireNotNull(
                    database.hierarchyEstablishmentOriginDao().get(hierarchyId.value),
                ) {
                    "Missing durable hierarchy establishment origin for ${hierarchyId.value}"
                }
            return runCatching { HierarchyEstablishmentOrigin.valueOf(stored.origin) }
                .getOrElse {
                    error(
                        "Unsupported hierarchy establishment origin ${stored.origin} " +
                            "for ${hierarchyId.value}",
                    )
                }
        }

        companion object {
            private const val MIN_SUPPORTED_ACTIVATION_VERSION: Int = 1
            /**
             * v1: H1 authority established.
             * v2: embedded Workspace topology neutralized.
             * v3: Workspace topology plus MainBeacon embedded/additional-parent
             * topology neutralized; Beacon flat-list order is preserved.
             */
            const val CURRENT_ACTIVATION_VERSION: Int = 3
        }
    }

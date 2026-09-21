package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.database.HierarchyAuthorityActivationStateEntity
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
 * authority mode. On the first successful invocation it captures CURRENT V1,
 * requires an exact/pristine deterministic H2 materialization, establishes all
 * canonical occurrence provenance, and writes the durable activation marker in
 * the same Room transaction.
 *
 * Once the marker exists, later invocations return before reading CURRENT V1.
 * Therefore post-cutover legacy drift can never rematerialize or repair H1.
 */
@Singleton
class CanonicalHierarchyAuthorityActivator
    @Inject
    constructor(
        private val database: AppDatabase,
        private val snapshotReader: CanonicalV1HierarchySnapshotReader,
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
                    require(existing.version == CURRENT_ACTIVATION_VERSION) {
                        "Unsupported hierarchy authority activation marker version " +
                            "${existing.version} for ${hierarchyId.value}"
                    }
                    return@withTransaction CanonicalHierarchyAuthorityActivationReport(
                        performed = false,
                        materialization = null,
                    )
                }

                val snapshot =
                    snapshotReader.captureInCurrentTransaction(hierarchyId)
                val report =
                    materializer.materializeInCurrentTransaction(
                        snapshot = snapshot,
                        now = now,
                    )

                markerDao.upsert(
                    HierarchyAuthorityActivationStateEntity(
                        hierarchyId = hierarchyId.value,
                        version = CURRENT_ACTIVATION_VERSION,
                        activatedAt = now,
                    ),
                )

                CanonicalHierarchyAuthorityActivationReport(
                    performed = true,
                    materialization = report,
                )
            }
        }

        companion object {
            const val CURRENT_ACTIVATION_VERSION: Int = 1
        }
    }

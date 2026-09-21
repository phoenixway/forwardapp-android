package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot H2 migration boundary.
 *
 * V1 snapshot capture and V2 placement materialization happen under one Room
 * transaction, so the occurrence graph cannot be assembled from mixed V1
 * database moments. This is deliberately not wired as an ongoing dual-write.
 */
@Singleton
class CanonicalV1HierarchyMigration
    @Inject
    constructor(
        private val database: AppDatabase,
        private val snapshotReader: CanonicalV1HierarchySnapshotReader,
        private val materializer: CanonicalV1HierarchyMaterializer,
    ) {
        suspend fun materializeFromCurrentV1(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
            now: Long = System.currentTimeMillis(),
        ): CanonicalV1HierarchyMaterializationReport =
            database.withTransaction {
                val snapshot =
                    snapshotReader.captureInCurrentTransaction(hierarchyId)
                materializer.materializeInCurrentTransaction(snapshot, now)
            }
    }

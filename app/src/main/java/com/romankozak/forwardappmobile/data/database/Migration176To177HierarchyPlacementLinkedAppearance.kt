package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds exact occurrence-scoped Workspace linked-presentation provenance.
 *
 * The migration deliberately creates an empty side-stream. It never infers
 * presentation provenance from PlacementKind, H1 topology, legacy parent/order
 * fields, or target identity. The finite H2 cutover materializer may populate
 * deterministic rows from preserved V1 source-authority evidence.
 */
val MIGRATION_176_177 =
    object : Migration(176, 177) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hierarchy_placement_linked_appearances` (
                    `placementId` TEXT NOT NULL,
                    `hierarchyId` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`placementId`),
                    FOREIGN KEY(`placementId`, `hierarchyId`)
                        REFERENCES `hierarchy_placements`(`id`, `hierarchyId`)
                        ON UPDATE NO ACTION
                        ON DELETE NO ACTION
                        DEFERRABLE INITIALLY DEFERRED
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placement_linked_appearances_placementId_hierarchyId` " +
                    "ON `hierarchy_placement_linked_appearances` (`placementId`, `hierarchyId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placement_linked_appearances_hierarchyId_isDeleted_placementId` " +
                    "ON `hierarchy_placement_linked_appearances` " +
                    "(`hierarchyId`, `isDeleted`, `placementId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placement_linked_appearances_updatedAt` " +
                    "ON `hierarchy_placement_linked_appearances` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placement_linked_appearances_syncedAt` " +
                    "ON `hierarchy_placement_linked_appearances` (`syncedAt`)",
            )
        }
    }

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the Canonical V2 hierarchy persistence foundation only.
 *
 * No Canonical V1 hierarchy data is materialized here. Workspace parent/order,
 * ContextParentLink, and MainBeacon hierarchy state remain untouched.
 */
val MIGRATION_174_175 =
    object : Migration(174, 175) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hierarchy_placements` (
                    `id` TEXT NOT NULL,
                    `hierarchyId` TEXT NOT NULL,
                    `targetType` TEXT NOT NULL,
                    `targetId` TEXT NOT NULL,
                    `parentPlacementId` TEXT,
                    `placementKind` TEXT NOT NULL,
                    `siblingOrder` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`parentPlacementId`, `hierarchyId`)
                        REFERENCES `hierarchy_placements`(`id`, `hierarchyId`)
                        ON UPDATE NO ACTION
                        ON DELETE NO ACTION
                        DEFERRABLE INITIALLY DEFERRED
                )
                """.trimIndent(),
            )

            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_hierarchy_placements_id_hierarchyId` " +
                    "ON `hierarchy_placements` (`id`, `hierarchyId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placements_parentPlacementId_hierarchyId_isDeleted_siblingOrder_id` " +
                    "ON `hierarchy_placements` (`parentPlacementId`, `hierarchyId`, `isDeleted`, `siblingOrder`, `id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placements_hierarchyId_isDeleted_parentPlacementId_siblingOrder_id` " +
                    "ON `hierarchy_placements` (`hierarchyId`, `isDeleted`, `parentPlacementId`, `siblingOrder`, `id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placements_hierarchyId_targetType_targetId_isDeleted` " +
                    "ON `hierarchy_placements` (`hierarchyId`, `targetType`, `targetId`, `isDeleted`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hierarchy_placements_updatedAt` " +
                    "ON `hierarchy_placements` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hierarchy_placements_syncedAt` " +
                    "ON `hierarchy_placements` (`syncedAt`)",
            )
        }
    }

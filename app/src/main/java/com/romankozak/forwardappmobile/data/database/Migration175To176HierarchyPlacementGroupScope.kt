package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds canonical occurrence-scoped Beacon Group presentation provenance.
 *
 * The migration deliberately creates an empty table. It does not infer Group
 * scope from V1 hierarchy, PART_OF, PlacementId shape, list order, or any other
 * compatibility source.
 */
val MIGRATION_175_176 =
    object : Migration(175, 176) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hierarchy_placement_group_scopes` (
                    `placementId` TEXT NOT NULL,
                    `hierarchyId` TEXT NOT NULL,
                    `groupSubjectId` TEXT,
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
                    "`index_hierarchy_placement_group_scopes_placementId_hierarchyId` " +
                    "ON `hierarchy_placement_group_scopes` (`placementId`, `hierarchyId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_hierarchy_placement_group_scopes_hierarchyId_isDeleted_groupSubjectId_placementId` " +
                    "ON `hierarchy_placement_group_scopes` " +
                    "(`hierarchyId`, `isDeleted`, `groupSubjectId`, `placementId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hierarchy_placement_group_scopes_updatedAt` " +
                    "ON `hierarchy_placement_group_scopes` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_hierarchy_placement_group_scopes_syncedAt` " +
                    "ON `hierarchy_placement_group_scopes` (`syncedAt`)",
            )
        }
    }

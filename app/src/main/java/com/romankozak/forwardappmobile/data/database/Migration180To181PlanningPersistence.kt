package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds dormant Canonical V3 planning persistence without migrating domain data or authority. */
val MIGRATION_180_181 =
    object : Migration(180, 181) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `planning_scopes` (
                    `id` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `lifecycle` TEXT NOT NULL,
                    `title` TEXT,
                    `startsAt` INTEGER,
                    `endsAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_planning_scopes_kind_isDeleted_lifecycle`
                ON `planning_scopes` (`kind`, `isDeleted`, `lifecycle`)
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planning_scopes_updatedAt` " +
                    "ON `planning_scopes` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planning_scopes_syncedAt` " +
                    "ON `planning_scopes` (`syncedAt`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `planning_commitments` (
                    `id` TEXT NOT NULL,
                    `scopeId` TEXT NOT NULL,
                    `orientationId` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `commitmentOrder` INTEGER NOT NULL,
                    `priorityLevel` TEXT,
                    `status` TEXT NOT NULL,
                    `provenanceKind` TEXT NOT NULL,
                    `provenanceSourceType` TEXT,
                    `provenanceSourceId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`scopeId`) REFERENCES `planning_scopes`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION,
                    FOREIGN KEY(`orientationId`) REFERENCES `orientations`(`subjectId`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_planning_commitments_scopeId_role_isDeleted_commitmentOrder_id`
                ON `planning_commitments`
                (`scopeId`, `role`, `isDeleted`, `commitmentOrder`, `id`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_planning_commitments_scopeId_orientationId_role_isDeleted`
                ON `planning_commitments`
                (`scopeId`, `orientationId`, `role`, `isDeleted`)
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planning_commitments_orientationId` " +
                    "ON `planning_commitments` (`orientationId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planning_commitments_updatedAt` " +
                    "ON `planning_commitments` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_planning_commitments_syncedAt` " +
                    "ON `planning_commitments` (`syncedAt`)",
            )
        }
    }

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_186_187 =
    object : Migration(186, 187) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `orientation_scoring_overrides` (
                    `orientationId` TEXT NOT NULL,
                    `policyId` TEXT NOT NULL,
                    `policyVersion` INTEGER NOT NULL,
                    `formulaVersion` INTEGER NOT NULL,
                    `importanceCoordinate` REAL,
                    `impactCoordinate` REAL,
                    `effortWeight` REAL,
                    `costWeight` REAL,
                    `riskWeight` REAL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isDeleted` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    PRIMARY KEY(`orientationId`),
                    FOREIGN KEY(`orientationId`) REFERENCES `orientations`(`subjectId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_orientation_scoring_overrides_policyId` " +
                    "ON `orientation_scoring_overrides` (`policyId`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canonical_scoring_authority_state` (
                    `id` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `activatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

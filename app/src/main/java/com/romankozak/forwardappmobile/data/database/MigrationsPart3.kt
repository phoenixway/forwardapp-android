package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds canonical Day Theme persistence without touching legacy day_theme_documents.
 * Legacy JSON backfill is intentionally performed later in Kotlin where it can use
 * the tested canonical migration mapper instead of reimplementing JSON migration in SQL.
 */
val MIGRATION_146_147 =
    object : Migration(146, 147) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `theme_definitions` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `colorArgb` INTEGER NOT NULL,
                    `iconKey` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `carryForward` INTEGER NOT NULL,
                    `archived` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_themes` (
                    `id` TEXT NOT NULL,
                    `themeId` TEXT NOT NULL,
                    `dayPlanId` TEXT NOT NULL,
                    `budgetPercent` INTEGER NOT NULL,
                    `order` INTEGER NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_themes_themeId` ON `day_themes` (`themeId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_themes_dayPlanId` ON `day_themes` (`dayPlanId`)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_day_themes_dayPlanId_themeId` ON `day_themes` (`dayPlanId`, `themeId`)",
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_theme_assignment_documents` (
                    `dayPlanId` TEXT NOT NULL,
                    `assignmentsJson` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`dayPlanId`)
                )
                """.trimIndent(),
            )
        }
    }

/** Adds a local versioned marker for transactional legacy -> canonical Day Theme bootstrap. */
val MIGRATION_147_148 =
    object : Migration(147, 148) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_theme_canonical_bootstrap_state` (
                    `id` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `completedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds dormant typed Day scope-extension storage; it deliberately materializes no Day data. */
val MIGRATION_181_182 =
    object : Migration(181, 182) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_planning_scope_extensions` (
                    `scopeId` TEXT NOT NULL,
                    `calendarDayStartMillis` INTEGER NOT NULL,
                    `outcome` TEXT,
                    `plannedBudgetMinutes` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER,
                    `syncedAt` INTEGER,
                    `version` INTEGER NOT NULL,
                    `isDeleted` INTEGER NOT NULL,
                    PRIMARY KEY(`scopeId`),
                    FOREIGN KEY(`scopeId`) REFERENCES `planning_scopes`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_day_planning_scope_extensions_calendarDayStartMillis_isDeleted`
                ON `day_planning_scope_extensions` (`calendarDayStartMillis`, `isDeleted`)
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_day_planning_scope_extensions_updatedAt` " +
                    "ON `day_planning_scope_extensions` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_day_planning_scope_extensions_syncedAt` " +
                    "ON `day_planning_scope_extensions` (`syncedAt`)",
            )
        }
    }

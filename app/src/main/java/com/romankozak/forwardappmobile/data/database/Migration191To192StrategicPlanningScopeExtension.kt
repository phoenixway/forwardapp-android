package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds dormant typed Strategic scope-extension storage; it materializes no Strategic data. */
val MIGRATION_191_192 =
    object : Migration(191, 192) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `strategic_planning_scope_extensions` (
                    `scopeId` TEXT NOT NULL,
                    `legacyArcKey` TEXT NOT NULL,
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
                "CREATE INDEX IF NOT EXISTS `index_strategic_planning_scope_extensions_updatedAt` " +
                    "ON `strategic_planning_scope_extensions` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_strategic_planning_scope_extensions_syncedAt` " +
                    "ON `strategic_planning_scope_extensions` (`syncedAt`)",
            )
        }
    }

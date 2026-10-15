package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds dormant typed Tactical scope-extension storage; it materializes no Tactical data. */
val MIGRATION_189_190 =
    object : Migration(189, 190) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tactical_planning_scope_extensions` (
                    `scopeId` TEXT NOT NULL,
                    `iterationType` TEXT NOT NULL,
                    `weekKey` TEXT,
                    `closedAt` INTEGER,
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
                "CREATE INDEX IF NOT EXISTS `index_tactical_planning_scope_extensions_updatedAt` " +
                    "ON `tactical_planning_scope_extensions` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tactical_planning_scope_extensions_syncedAt` " +
                    "ON `tactical_planning_scope_extensions` (`syncedAt`)",
            )
        }
    }

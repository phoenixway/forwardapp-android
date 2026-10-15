package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds only the local marker; deterministic Tactical materialization remains an explicit transaction. */
val MIGRATION_190_191 =
    object : Migration(190, 191) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tactical_planning_authority_state` (
                    `id` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `activatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds only the local marker; Strategic materialization remains an explicit transaction. */
val MIGRATION_192_193 =
    object : Migration(192, 193) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `strategic_planning_authority_state` (
                    `id` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `activatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds only the local cutover marker. Materialization requires canonical Theme resolution at startup. */
val MIGRATION_183_184 =
    object : Migration(183, 184) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `day_planning_authority_state` (
                    `id` INTEGER NOT NULL,
                    `version` INTEGER NOT NULL,
                    `activatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

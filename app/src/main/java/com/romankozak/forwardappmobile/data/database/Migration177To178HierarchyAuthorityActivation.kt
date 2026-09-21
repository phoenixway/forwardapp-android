package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds only the durable local P2 authority-activation marker.
 *
 * The migration does not materialize H1, reconstruct provenance, infer linked
 * presentation state, or activate V2 authority. The marker starts absent and is
 * written only by the explicit one-time activation transaction.
 */
val MIGRATION_177_178 =
    object : Migration(177, 178) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `hierarchy_authority_activation_state` (
                    `hierarchyId` TEXT NOT NULL,
                    `version` INTEGER NOT NULL,
                    `activatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`hierarchyId`)
                )
                """.trimIndent(),
            )
        }
    }

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Retires the normalized Context tag compatibility index.
 *
 * Canonical tag authority lives in workspace_tag_refs. Historical Context.tags
 * remains restore/compatibility ingress and is intentionally not rebuilt here.
 */
val MIGRATION_187_188 =
    object : Migration(187, 188) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `context_tag_refs`")
        }
    }

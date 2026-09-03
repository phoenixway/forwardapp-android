package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema-only bridge release.
 *
 * Artifact and Journal are intentionally removed by MIGRATION_164_165.
 * No content preservation or compatibility materialization is performed.
 */
val MIGRATION_163_164 =
    object : Migration(163, 164) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

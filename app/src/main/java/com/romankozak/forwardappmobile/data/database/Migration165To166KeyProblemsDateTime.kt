package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Restores the generic KEY_PROBLEMS date/time datum from the historical
 * issue tracker. No deadline, reminder, or scheduling semantics are inferred.
 *
 * Revised 156 -> 157 chains already materialize this column, so the bridge is
 * intentionally idempotent.
 */
val MIGRATION_165_166 =
    object : Migration(165, 166) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!hasWorkspaceProblemDateTimeColumn(db)) {
                db.execSQL("ALTER TABLE workspace_problems ADD COLUMN dateTime INTEGER")
            }
        }
    }

private fun hasWorkspaceProblemDateTimeColumn(db: SupportSQLiteDatabase): Boolean =
    db.query("PRAGMA table_info(workspace_problems)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == "dateTime") return@use true
        }
        false
    }

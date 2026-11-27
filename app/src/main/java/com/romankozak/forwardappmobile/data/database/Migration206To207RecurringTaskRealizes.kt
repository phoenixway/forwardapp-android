package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_206_207 = object : Migration(206, 207) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE day_tasks ADD COLUMN realizesOverride TEXT")
        db.execSQL("ALTER TABLE day_planning_scope_extensions ADD COLUMN localDate TEXT")
        db.execSQL("ALTER TABLE day_planning_scope_extensions ADD COLUMN timeZoneId TEXT")
    }
}

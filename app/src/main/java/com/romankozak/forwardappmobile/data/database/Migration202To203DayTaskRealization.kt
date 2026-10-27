package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_202_203 = object : Migration(202, 203) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE day_tasks ADD COLUMN realizedDayFocusItemIds TEXT NOT NULL DEFAULT '[]'")
    }
}

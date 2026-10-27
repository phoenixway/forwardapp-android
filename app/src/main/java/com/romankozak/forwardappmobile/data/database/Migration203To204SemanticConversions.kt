package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Empty history only: existing semantic identities are never inferred or converted. */
val MIGRATION_203_204 = object : Migration(203, 204) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS managed_subject_conversions (
            id TEXT NOT NULL PRIMARY KEY, subjectId TEXT NOT NULL, formatVersion INTEGER NOT NULL,
            fromType TEXT NOT NULL, toType TEXT NOT NULL, targetOrientationKind TEXT NOT NULL,
            fromSubjectVersion INTEGER NOT NULL, toSubjectVersion INTEGER NOT NULL,
            previousAspectParentId TEXT, previousAspectOrder INTEGER NOT NULL, previousAspectArchived INTEGER NOT NULL,
            createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, version INTEGER NOT NULL, syncedAt INTEGER)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_managed_subject_conversions_subjectId ON managed_subject_conversions(subjectId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_managed_subject_conversions_subjectId_toSubjectVersion ON managed_subject_conversions(subjectId, toSubjectVersion)")
    }
}

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserve immutable v1 Aspect facts; never infer a historical kind change. */
val MIGRATION_204_205 = object : Migration(204, 205) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE managed_subject_conversions ADD COLUMN fromOrientationKind TEXT")
        db.execSQL("ALTER TABLE managed_subject_conversions ADD COLUMN previousAssessmentRevisionIdsJson TEXT")
    }
}

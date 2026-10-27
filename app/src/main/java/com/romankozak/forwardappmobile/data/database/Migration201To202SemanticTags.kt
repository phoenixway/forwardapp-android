package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_201_202 = object : Migration(201, 202) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS managed_subject_tag_refs (
            id TEXT NOT NULL PRIMARY KEY, subjectType TEXT NOT NULL, subjectId TEXT NOT NULL,
            normalizedTag TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
            syncedAt INTEGER, isDeleted INTEGER NOT NULL, version INTEGER NOT NULL,
            FOREIGN KEY(subjectId) REFERENCES managed_subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        db.execSQL("CREATE UNIQUE INDEX index_managed_subject_tag_refs_subjectType_subjectId_normalizedTag ON managed_subject_tag_refs(subjectType,subjectId,normalizedTag)")
        listOf("subjectId", "normalizedTag", "updatedAt", "syncedAt").forEach {
            db.execSQL("CREATE INDEX index_managed_subject_tag_refs_$it ON managed_subject_tag_refs($it)")
        }
    }
}

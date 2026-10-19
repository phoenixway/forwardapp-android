package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Legacy Context links remain explicit compatibility data; absence is not mapping evidence. */
val MIGRATION_200_201 = object : Migration(200, 201) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS planning_references (
            id TEXT NOT NULL PRIMARY KEY, ownerKind TEXT NOT NULL, ownerId TEXT NOT NULL,
            targetKind TEXT NOT NULL, targetId TEXT NOT NULL, role TEXT NOT NULL,
            createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER,
            version INTEGER NOT NULL, isDeleted INTEGER NOT NULL
        )""".trimIndent())
        db.execSQL("CREATE UNIQUE INDEX index_planning_references_ownerKind_ownerId_targetKind_targetId_role ON planning_references(ownerKind,ownerId,targetKind,targetId,role)")
        db.execSQL("CREATE INDEX index_planning_references_targetKind_targetId ON planning_references(targetKind,targetId)")
        db.execSQL("CREATE INDEX index_planning_references_updatedAt ON planning_references(updatedAt)")
        db.execSQL("CREATE INDEX index_planning_references_syncedAt ON planning_references(syncedAt)")
    }
}

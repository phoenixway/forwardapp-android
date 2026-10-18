package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive presentation-only authority. No historical hierarchy or semantic row is rewritten. */
val MIGRATION_198_199 = object : Migration(198, 199) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS big_picture_containers (
                id TEXT NOT NULL PRIMARY KEY, hierarchyId TEXT NOT NULL, title TEXT NOT NULL,
                anchorPlacementId TEXT, parentContainerId TEXT, siblingOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER,
                isDeleted INTEGER NOT NULL, version INTEGER NOT NULL,
                FOREIGN KEY(parentContainerId, hierarchyId) REFERENCES big_picture_containers(id, hierarchyId)
                    ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED,
                FOREIGN KEY(anchorPlacementId, hierarchyId) REFERENCES hierarchy_placements(id, hierarchyId)
                    ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS big_picture_memberships (
                placementId TEXT NOT NULL PRIMARY KEY, hierarchyId TEXT NOT NULL, containerId TEXT NOT NULL,
                createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER,
                isDeleted INTEGER NOT NULL, version INTEGER NOT NULL,
                FOREIGN KEY(containerId, hierarchyId) REFERENCES big_picture_containers(id, hierarchyId)
                    ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED,
                FOREIGN KEY(placementId, hierarchyId) REFERENCES hierarchy_placements(id, hierarchyId)
                    ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED
            )
        """.trimIndent())
        index(db, "big_picture_containers", listOf("id", "hierarchyId"), unique = true)
        index(db, "big_picture_containers", listOf("parentContainerId", "hierarchyId"))
        index(db, "big_picture_containers", listOf("anchorPlacementId", "hierarchyId"))
        index(db, "big_picture_memberships", listOf("placementId", "hierarchyId"))
        index(db, "big_picture_memberships", listOf("containerId", "hierarchyId"))
        listOf("big_picture_containers", "big_picture_memberships").forEach { table ->
            index(db, table, listOf("updatedAt"))
            index(db, table, listOf("syncedAt"))
        }
    }
    private fun index(db: SupportSQLiteDatabase, table: String, columns: List<String>, unique: Boolean = false) {
        db.execSQL("CREATE ${if (unique) "UNIQUE " else ""}INDEX IF NOT EXISTS index_${table}_${columns.joinToString("_")} ON $table (${columns.joinToString()})")
    }
}

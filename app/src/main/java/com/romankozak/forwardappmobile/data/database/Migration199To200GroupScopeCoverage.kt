package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserve every historical sidecar, never guess Orientation lineage from kind or position. */
val MIGRATION_199_200 = object : Migration(199, 200) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS hierarchy_group_scope_applicability (
            placementId TEXT NOT NULL PRIMARY KEY, hierarchyId TEXT NOT NULL, mode TEXT NOT NULL,
            createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, syncedAt INTEGER, version INTEGER NOT NULL,
            FOREIGN KEY(placementId,hierarchyId) REFERENCES hierarchy_placements(id,hierarchyId)
            ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED
        )""".trimIndent())
        listOf("placementId,hierarchyId", "updatedAt", "syncedAt").forEach { columns ->
            db.execSQL("CREATE INDEX IF NOT EXISTS index_hierarchy_group_scope_applicability_${columns.replace(',', '_')} ON hierarchy_group_scope_applicability($columns)")
        }
        db.execSQL("""INSERT INTO hierarchy_group_scope_applicability
            SELECT p.id, p.hierarchyId,
              CASE WHEN EXISTS(SELECT 1 FROM hierarchy_placement_group_scopes s WHERE s.placementId=p.id)
                   THEN 'HISTORICAL'
                   WHEN EXISTS(SELECT 1 FROM managed_subjects m JOIN aspects a ON a.subjectId=m.id
                               WHERE m.id=p.targetId AND m.subjectType='ASPECT')
                        AND NOT EXISTS(SELECT 1 FROM legacy_subject_mappings l WHERE l.subjectId=p.targetId
                                       AND l.sourceType IN ('MAIN_BEACON','MAIN_BEACON_GROUP'))
                   THEN 'NATIVE' ELSE 'LEGACY_UNKNOWN' END,
              p.createdAt, p.updatedAt, NULL, 1
            FROM hierarchy_placements p WHERE p.hierarchyId='GENERAL' AND p.targetType='MANAGED_SUBJECT'
        """.trimIndent())
    }
}

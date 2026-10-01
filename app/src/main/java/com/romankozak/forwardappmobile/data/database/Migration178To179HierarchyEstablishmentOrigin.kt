package com.romankozak.forwardappmobile.data.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds local lineage metadata only; hierarchy materialization remains a runtime transaction. */
val MIGRATION_178_179 =
    object : Migration(178, 179) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_HIERARCHY_ESTABLISHMENT_ORIGIN_TABLE)
            db.execSQL(
                """
                INSERT INTO hierarchy_establishment_origin(hierarchyId, origin)
                VALUES(
                    'GENERAL',
                    CASE
                        WHEN EXISTS(
                            SELECT 1
                            FROM hierarchy_authority_activation_state
                            WHERE hierarchyId = 'GENERAL'
                        ) THEN '${HierarchyEstablishmentOrigin.ESTABLISHED.name}'
                        ELSE '${HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE.name}'
                    END
                )
                """.trimIndent(),
            )
        }
    }

/** Direct current-schema creation does not execute [MIGRATION_178_179]. */
val HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK =
    object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO hierarchy_establishment_origin(hierarchyId, origin)
                VALUES('GENERAL', '${HierarchyEstablishmentOrigin.FRESH_NATIVE.name}')
                """.trimIndent(),
            )
        }
    }

private const val CREATE_HIERARCHY_ESTABLISHMENT_ORIGIN_TABLE =
    """
    CREATE TABLE IF NOT EXISTS `hierarchy_establishment_origin` (
        `hierarchyId` TEXT NOT NULL,
        `origin` TEXT NOT NULL,
        PRIMARY KEY(`hierarchyId`)
    )
    """

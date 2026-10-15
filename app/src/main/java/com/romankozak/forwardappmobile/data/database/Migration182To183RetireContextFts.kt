package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Retires the unused Context FTS4 external-content projection.
 *
 * Context persistence and all remaining Context-family tables stay intact.
 * Room generated four content-sync triggers for contexts_fts; they must be
 * removed before dropping the virtual table so subsequent Context writes do
 * not target a retired FTS table.
 */
val MIGRATION_182_183 =
    object : Migration(182, 183) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "DROP TRIGGER IF EXISTS `room_fts_content_sync_contexts_fts_BEFORE_UPDATE`",
            )
            db.execSQL(
                "DROP TRIGGER IF EXISTS `room_fts_content_sync_contexts_fts_BEFORE_DELETE`",
            )
            db.execSQL(
                "DROP TRIGGER IF EXISTS `room_fts_content_sync_contexts_fts_AFTER_UPDATE`",
            )
            db.execSQL(
                "DROP TRIGGER IF EXISTS `room_fts_content_sync_contexts_fts_AFTER_INSERT`",
            )
            db.execSQL("DROP TABLE IF EXISTS `contexts_fts`")
        }
    }

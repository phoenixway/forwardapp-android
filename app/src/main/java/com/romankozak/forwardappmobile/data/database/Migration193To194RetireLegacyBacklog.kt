package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Physically retires the local Context-era BACKLOG compatibility stores.
 *
 * Canonical Workspace BACKLOG authority has lived in workspace_backlog_entries
 * since schema 162. Historical backup payloads remain readable and are
 * translated directly into canonical entries before local Restore writes.
 */
val MIGRATION_193_194 =
    object : Migration(193, 194) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE `backlog_orders`")
            db.execSQL("DROP TABLE `list_items`")
        }
    }

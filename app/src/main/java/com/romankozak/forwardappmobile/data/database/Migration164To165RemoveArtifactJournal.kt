package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hard removal of the retired Artifact and Journal concepts.
 *
 * Intentional data loss:
 * - all legacy ContextArtifact rows;
 * - all Stage-A materialized RETIRED_ARTIFACT_* documents/attachments;
 * - all JOURNAL_DOCUMENT attachments and their NoteDocument payloads;
 * - all system_journal_log_* documents;
 * - canonical BACKLOG placements targeting JOURNAL_DOCUMENT;
 * - retired artifact/journal capability instances when represented in
 *   workspace capability state.
 *
 * NOTE_DOCUMENT remains the only ordinary text-document type.
 */
val MIGRATION_164_165 =
    object : Migration(164, 165) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TEMP TABLE IF NOT EXISTS removed_artifact_journal_documents(
                    id TEXT PRIMARY KEY
                )
                """.trimIndent(),
            )

            // Capture document ids before deleting attachment rows.
            db.execSQL(
                """
                INSERT OR IGNORE INTO removed_artifact_journal_documents(id)
                SELECT entity_id
                FROM attachments
                WHERE attachment_type = 'JOURNAL_DOCUMENT'
                   OR id LIKE 'RETIRED_ARTIFACT_ATTACHMENT:%'
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO removed_artifact_journal_documents(id)
                SELECT id
                FROM note_documents
                WHERE id LIKE 'system_journal_log_%'
                   OR id LIKE 'RETIRED_ARTIFACT_DOCUMENT:%'
                """.trimIndent(),
            )

            // Placement first because workspace_connections has an attachment FK.
            db.execSQL(
                """
                DELETE FROM workspace_connections
                WHERE attachmentId IN (
                    SELECT id
                    FROM attachments
                    WHERE attachment_type = 'JOURNAL_DOCUMENT'
                       OR id LIKE 'RETIRED_ARTIFACT_ATTACHMENT:%'
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                DELETE FROM attachments
                WHERE attachment_type = 'JOURNAL_DOCUMENT'
                   OR id LIKE 'RETIRED_ARTIFACT_ATTACHMENT:%'
                   OR entity_id IN (
                       SELECT id FROM removed_artifact_journal_documents
                   )
                """.trimIndent(),
            )

            // Canonical BACKLOG placement of removed journal documents.
            if (tableExists(db, "workspace_backlog_entries")) {
                val targetKindColumn =
                    when {
                        columnExists(db, "workspace_backlog_entries", "targetKind") -> "targetKind"
                        columnExists(db, "workspace_backlog_entries", "target_kind") -> "target_kind"
                        else -> null
                    }
                if (targetKindColumn != null) {
                    db.execSQL(
                        "DELETE FROM workspace_backlog_entries WHERE $targetKindColumn = 'JOURNAL_DOCUMENT'",
                    )
                }
            }

            // Remove actual document payloads last.
            db.execSQL(
                """
                DELETE FROM note_documents
                WHERE id IN (
                    SELECT id FROM removed_artifact_journal_documents
                )
                """.trimIndent(),
            )

            // Legacy Artifact storage is no longer supported.
            if (tableExists(db, "context_artifacts")) {
                db.execSQL("DROP TABLE context_artifacts")
            }

            db.execSQL("""CREATE TABLE IF NOT EXISTS `structure_presets_without_artifact` (`id` TEXT NOT NULL, `code` TEXT NOT NULL, `label` TEXT NOT NULL, `description` TEXT, `enable_inbox` INTEGER, `enable_log` INTEGER, `enable_advanced` INTEGER, `enable_dashboard` INTEGER, `enable_backlog` INTEGER, `enable_attachments` INTEGER, `enable_auto_link_subprojects` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
            db.execSQL("""
                INSERT INTO `structure_presets_without_artifact` (`id`, `code`, `label`, `description`, `enable_inbox`, `enable_log`, `enable_advanced`, `enable_dashboard`, `enable_backlog`, `enable_attachments`, `enable_auto_link_subprojects`, `createdAt`, `updatedAt`, `version`, `isDeleted`)
                SELECT `id`, `code`, `label`, `description`, `enable_inbox`, `enable_log`, `enable_advanced`, `enable_dashboard`, `enable_backlog`, `enable_attachments`, `enable_auto_link_subprojects`, `createdAt`, `updatedAt`, `version`, `isDeleted`
                FROM `structure_presets`
            """.trimIndent())
            db.execSQL("DROP TABLE `structure_presets`")
            db.execSQL("ALTER TABLE `structure_presets_without_artifact` RENAME TO `structure_presets`")
            db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_structure_presets_code` ON `structure_presets` (`code`)""")

            db.execSQL("""CREATE TABLE IF NOT EXISTS `context_structures_without_artifact` (`id` TEXT NOT NULL, `contextId` TEXT NOT NULL, `base_preset_code` TEXT, `experimental_capability_ids` TEXT NOT NULL, `apply_mode` TEXT NOT NULL, `enable_inbox` INTEGER, `enable_log` INTEGER, `enable_advanced` INTEGER, `enable_dashboard` INTEGER, `enable_backlog` INTEGER, `enable_attachments` INTEGER, `enable_auto_link_subprojects` INTEGER, `remove_inbox_entry_after_tag_autocopy` INTEGER, `remove_backlog_entry_after_tag_autocopy` INTEGER, `updatedAt` INTEGER NOT NULL, `version` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
            db.execSQL("""
                INSERT INTO `context_structures_without_artifact` (`id`, `contextId`, `base_preset_code`, `experimental_capability_ids`, `apply_mode`, `enable_inbox`, `enable_log`, `enable_advanced`, `enable_dashboard`, `enable_backlog`, `enable_attachments`, `enable_auto_link_subprojects`, `remove_inbox_entry_after_tag_autocopy`, `remove_backlog_entry_after_tag_autocopy`, `updatedAt`, `version`, `isDeleted`)
                SELECT `id`, `contextId`, `base_preset_code`, `experimental_capability_ids`, `apply_mode`, `enable_inbox`, `enable_log`, `enable_advanced`, `enable_dashboard`, `enable_backlog`, `enable_attachments`, `enable_auto_link_subprojects`, `remove_inbox_entry_after_tag_autocopy`, `remove_backlog_entry_after_tag_autocopy`, `updatedAt`, `version`, `isDeleted`
                FROM `context_structures`
            """.trimIndent())
            db.execSQL("DROP TABLE `context_structures`")
            db.execSQL("ALTER TABLE `context_structures_without_artifact` RENAME TO `context_structures`")
            db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_context_structures_contextId` ON `context_structures` (`contextId`)""")

            // Remove persisted retired capability instances where the schema
            // exposes a recognizable capability id/type column.
            if (tableExists(db, "workspace_capability_instances")) {
                listOf(
                    "capabilityId",
                    "capability_id",
                    "capabilityType",
                    "capability_type",
                    "type",
                ).firstOrNull { columnExists(db, "workspace_capability_instances", it) }
                    ?.let { column ->
                        db.execSQL(
                            """
                            DELETE FROM workspace_capability_instances
                            WHERE lower($column) IN ('artifact', 'journal', 'journal_log')
                            """.trimIndent(),
                        )
                    }
            }

            db.execSQL("DROP TABLE removed_artifact_journal_documents")
        }
    }

private fun tableExists(
    db: SupportSQLiteDatabase,
    table: String,
): Boolean =
    db.query(
        "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
        arrayOf(table),
    ).use { it.moveToFirst() }

private fun columnExists(
    db: SupportSQLiteDatabase,
    table: String,
    column: String,
): Boolean =
    db.query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0 && cursor.getString(nameIndex) == column) {
                return@use true
            }
        }
        false
    }

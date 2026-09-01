package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Frozen schema-157 -> 158 INBOX hard cutover. */
val MIGRATION_157_158 =
    object : Migration(157, 158) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCanonicalInboxTable(db)
            check(scalarLong158(db, "SELECT COUNT(*) FROM workspace_inbox_records") == 0L) {
                "INBOX cutover blocked: canonical table already contains data"
            }

            val sources = loadInboxSources(db)
            val diagnostics = mutableListOf<String>()
            sources.filter { !it.isDeleted && it.hideInOwnerInbox }.forEach {
                diagnostics += "LEGACY_HIDE_FLAG_REQUIRES_REVIEW: ${it.id}"
            }
            sources.filter { it.version < 0L }.forEach {
                diagnostics += "INVALID_VERSION: ${it.id}"
            }

            typeExistingInboxCapabilities(db)

            val ownerPlans =
                sources.map { it.contextId }.distinct().associateWith { contextId ->
                    resolveOwner(db, contextId, diagnostics)
                }
            ownerPlans.values.filterNotNull().forEach { owner ->
                ensureTypedInboxCapability(db, owner, diagnostics)
            }

            check(diagnostics.isEmpty()) {
                "INBOX cutover blocked:\n${diagnostics.distinct().joinToString("\n")}"
            }

            val canonical =
                sources.groupBy { it.contextId }.values.flatMap { owned ->
                    owned.sortedWith(
                        compareByDescending<LegacyInbox158> { it.order }
                            .thenByDescending { it.createdAt }
                            .thenBy { it.id },
                    ).mapIndexed { index, source ->
                        val owner = requireNotNull(ownerPlans[source.contextId])
                        CanonicalInbox158(
                            source = source,
                            workspaceId = owner.workspaceId,
                            capabilityInstanceId = owner.capabilityId,
                            order = index.toLong(),
                        )
                    }
                }

            canonical.forEach { insertCanonicalInbox(db, it) }
            check(
                scalarLong158(db, "SELECT COUNT(*) FROM workspace_inbox_records") ==
                    sources.size.toLong(),
            ) {
                "INBOX cutover blocked: source accounting mismatch"
            }

            replaceAssociationCacheForeignKey(db)
            db.execSQL("DROP TABLE inbox_records")
        }
    }

private fun createCanonicalInboxTable(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS workspace_inbox_records (
            id TEXT NOT NULL,
            workspaceId TEXT NOT NULL,
            capabilityInstanceId TEXT NOT NULL,
            text TEXT NOT NULL,
            recordOrder INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncedAt INTEGER,
            isDeleted INTEGER NOT NULL,
            version INTEGER NOT NULL,
            PRIMARY KEY(id)
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX index_workspace_inbox_records_workspaceId ON workspace_inbox_records(workspaceId)")
    db.execSQL("CREATE INDEX index_workspace_inbox_records_capabilityInstanceId ON workspace_inbox_records(capabilityInstanceId)")
    db.execSQL("CREATE INDEX index_workspace_inbox_records_updatedAt ON workspace_inbox_records(updatedAt)")
    db.execSQL("CREATE INDEX index_workspace_inbox_records_isDeleted ON workspace_inbox_records(isDeleted)")
    db.execSQL(
        "CREATE INDEX index_workspace_inbox_records_capabilityInstanceId_recordOrder " +
            "ON workspace_inbox_records(capabilityInstanceId, recordOrder)",
    )
}

private fun loadInboxSources(db: SupportSQLiteDatabase): List<LegacyInbox158> =
    db.query(
        """
        SELECT id, contextId, text, createdAt, item_order, updatedAt,
               synced_at, is_deleted, hide_in_owner_inbox, version
        FROM inbox_records
        """.trimIndent(),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    LegacyInbox158(
                        id = cursor.getString(0),
                        contextId = cursor.getString(1),
                        text = cursor.getString(2),
                        createdAt = cursor.getLong(3),
                        order = cursor.getLong(4),
                        updatedAt = if (cursor.isNull(5)) cursor.getLong(3) else cursor.getLong(5),
                        syncedAt = if (cursor.isNull(6)) null else cursor.getLong(6),
                        isDeleted = cursor.getInt(7) != 0,
                        hideInOwnerInbox = cursor.getInt(8) != 0,
                        version = cursor.getLong(9),
                    ),
                )
            }
        }
    }

private fun resolveOwner(
    db: SupportSQLiteDatabase,
    contextId: String,
    diagnostics: MutableList<String>,
): InboxOwner158? {
    val workspaces =
        db.query(
            """
            SELECT id FROM workspaces
            WHERE sourceContextId = ? AND provenance = 'CONTEXT_BACKED'
            """.trimIndent(),
            arrayOf(contextId),
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    if (workspaces.size != 1) {
        diagnostics += "UNRESOLVED_OWNER_WORKSPACE: Context $contextId resolved ${workspaces.size} Workspaces"
        return null
    }

    val workspaceId = workspaces.single()
    val capabilities =
        db.query(
            """
            SELECT id FROM workspace_capability_instances
            WHERE workspaceId = ? AND capabilityType = 'INBOX' AND instanceKey = 'default'
            """.trimIndent(),
            arrayOf(workspaceId),
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    if (capabilities.size > 1) {
        diagnostics += "MULTIPLE_CAPABILITY_INSTANCES: Workspace $workspaceId has ${capabilities.size} INBOX anchors"
        return null
    }
    return InboxOwner158(
        contextId = contextId,
        workspaceId = workspaceId,
        capabilityId = capabilities.singleOrNull() ?: stableInboxCapabilityId(workspaceId),
        capabilityExists = capabilities.isNotEmpty(),
    )
}

private fun ensureTypedInboxCapability(
    db: SupportSQLiteDatabase,
    owner: InboxOwner158,
    diagnostics: MutableList<String>,
) {
    val hideWhenAssociated =
        db.query(
            """
            SELECT remove_inbox_entry_after_tag_autocopy
            FROM context_structures
            WHERE contextId = ? AND isDeleted = 0
            LIMIT 1
            """.trimIndent(),
            arrayOf(owner.contextId),
        ).use { cursor -> cursor.moveToFirst() && !cursor.isNull(0) && cursor.getInt(0) != 0 }
    val configuration =
        if (hideWhenAssociated) {
            "{\"ownerVisibility\":\"HIDE_WHEN_ASSOCIATED\"}"
        } else {
            "{\"ownerVisibility\":\"KEEP_VISIBLE\"}"
        }
    val now = System.currentTimeMillis()

    if (owner.capabilityExists) return

    val workspaceExists =
        scalarLong158(db, "SELECT COUNT(*) FROM workspaces WHERE id = '${sqlLiteral158(owner.workspaceId)}'") == 1L
    if (!workspaceExists) {
        diagnostics += "UNRESOLVED_OWNER_WORKSPACE: ${owner.workspaceId}"
        return
    }
    val order =
        scalarLong158(
            db,
            "SELECT COALESCE(MAX(capabilityOrder), -1) + 1 FROM workspace_capability_instances " +
                "WHERE workspaceId = '${sqlLiteral158(owner.workspaceId)}'",
        )
    db.execSQL(
        """
        INSERT INTO workspace_capability_instances (
            id, workspaceId, capabilityType, instanceKey, capabilityOrder, state,
            configurationVersion, configuration, createdAt, updatedAt,
            syncedAt, isDeleted, version
        ) VALUES (?, ?, 'INBOX', 'default', ?, 'ACTIVE', 1, ?, ?, ?, NULL, 0, 1)
        """.trimIndent(),
        arrayOf<Any?>(owner.capabilityId, owner.workspaceId, order, configuration, now, now),
    )
}

private fun typeExistingInboxCapabilities(db: SupportSQLiteDatabase) {
    val rows =
        db.query(
            """
            SELECT capability.id, workspaces.sourceContextId
            FROM workspace_capability_instances capability
            JOIN workspaces ON workspaces.id = capability.workspaceId
            WHERE capability.capabilityType = 'INBOX' AND capability.instanceKey = 'default'
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to if (cursor.isNull(1)) null else cursor.getString(1))
                }
            }
        }
    val now = System.currentTimeMillis()
    rows.forEach { (capabilityId, contextId) ->
        val hideWhenAssociated =
            contextId != null &&
                db.query(
                    """
                    SELECT remove_inbox_entry_after_tag_autocopy
                    FROM context_structures
                    WHERE contextId = ? AND isDeleted = 0
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(contextId),
                ).use { cursor -> cursor.moveToFirst() && !cursor.isNull(0) && cursor.getInt(0) != 0 }
        val configuration =
            if (hideWhenAssociated) {
                "{\"ownerVisibility\":\"HIDE_WHEN_ASSOCIATED\"}"
            } else {
                "{\"ownerVisibility\":\"KEEP_VISIBLE\"}"
            }
        db.execSQL(
            """
            UPDATE workspace_capability_instances
            SET configurationVersion = 1, configuration = ?, updatedAt = ?,
                syncedAt = NULL, version = version + 1
            WHERE id = ?
            """.trimIndent(),
            arrayOf(configuration, now, capabilityId),
        )
    }
}

private fun insertCanonicalInbox(db: SupportSQLiteDatabase, record: CanonicalInbox158) {
    val source = record.source
    db.execSQL(
        """
        INSERT INTO workspace_inbox_records (
            id, workspaceId, capabilityInstanceId, text, recordOrder,
            createdAt, updatedAt, syncedAt, isDeleted, version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            source.id,
            record.workspaceId,
            record.capabilityInstanceId,
            source.text,
            record.order,
            source.createdAt,
            source.updatedAt,
            source.syncedAt,
            if (source.isDeleted) 1 else 0,
            source.version,
        ),
    )
}

private fun replaceAssociationCacheForeignKey(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE inbox_record_links RENAME TO inbox_record_links_legacy_158")
    db.execSQL(
        """
        CREATE TABLE inbox_record_links (
            record_id TEXT NOT NULL,
            context_id TEXT NOT NULL,
            owner_context_id TEXT NOT NULL,
            association_tag TEXT,
            linked_at INTEGER NOT NULL,
            PRIMARY KEY(record_id, context_id),
            FOREIGN KEY(record_id) REFERENCES workspace_inbox_records(id) ON DELETE CASCADE,
            FOREIGN KEY(context_id) REFERENCES contexts(id) ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL(
        """
        INSERT INTO inbox_record_links
        SELECT links.record_id, links.context_id, links.owner_context_id,
               links.association_tag, links.linked_at
        FROM inbox_record_links_legacy_158 links
        JOIN workspace_inbox_records records ON records.id = links.record_id
        JOIN contexts contexts ON contexts.id = links.context_id
        """.trimIndent(),
    )
    db.execSQL("DROP TABLE inbox_record_links_legacy_158")
    db.execSQL("CREATE INDEX index_inbox_record_links_context_id ON inbox_record_links(context_id)")
    db.execSQL("CREATE INDEX index_inbox_record_links_record_id ON inbox_record_links(record_id)")
    db.execSQL(
        "CREATE INDEX index_inbox_record_links_owner_context_id_record_id " +
            "ON inbox_record_links(owner_context_id, record_id)",
    )
}

private fun stableInboxCapabilityId(workspaceId: String): String =
    UUID.nameUUIDFromBytes("WORKSPACE:CAPABILITY:$workspaceId:INBOX:default".toByteArray(StandardCharsets.UTF_8)).toString()

private fun scalarLong158(db: SupportSQLiteDatabase, sql: String): Long =
    db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

private fun sqlLiteral158(value: String): String = value.replace("'", "''")

private data class LegacyInbox158(
    val id: String,
    val contextId: String,
    val text: String,
    val createdAt: Long,
    val order: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val hideInOwnerInbox: Boolean,
    val version: Long,
)

private data class InboxOwner158(
    val contextId: String,
    val workspaceId: String,
    val capabilityId: String,
    val capabilityExists: Boolean,
)

private data class CanonicalInbox158(
    val source: LegacyInbox158,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val order: Long,
)

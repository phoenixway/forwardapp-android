package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Frozen schema-158 -> 159 CONNECTIONS hard cutover. */
val MIGRATION_158_159 =
    object : Migration(158, 159) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createWorkspaceConnectionsTable(db)
            check(scalarLong159(db, "SELECT COUNT(*) FROM workspace_connections") == 0L) {
                "CONNECTIONS cutover blocked: canonical table already contains data"
            }

            val diagnostics = mutableListOf<String>()
            validateLegacyConnections(db, diagnostics)
            ensureTypedConnectionsCapabilities(db)
            validateResolvedOwners(db, diagnostics)
            validateResolvedCapabilities(db, diagnostics)
            validateResolvedAttachments(db, diagnostics)
            check(diagnostics.isEmpty()) {
                "CONNECTIONS cutover blocked:\n${diagnostics.distinct().joinToString("\n")}"
            }

            insertCanonicalConnections(db)
            check(
                scalarLong159(db, "SELECT COUNT(*) FROM workspace_connections") ==
                    scalarLong159(db, "SELECT COUNT(*) FROM context_attachment_cross_ref"),
            ) {
                "CONNECTIONS cutover blocked: source accounting mismatch"
            }
            db.execSQL("DROP TABLE context_attachment_cross_ref")
        }
    }

private fun createWorkspaceConnectionsTable(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS workspace_connections (
            id TEXT NOT NULL,
            workspaceId TEXT NOT NULL,
            capabilityInstanceId TEXT NOT NULL,
            attachmentId TEXT NOT NULL,
            connectionOrder INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL,
            syncedAt INTEGER,
            isDeleted INTEGER NOT NULL,
            version INTEGER NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(workspaceId) REFERENCES workspaces(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(capabilityInstanceId) REFERENCES workspace_capability_instances(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(attachmentId) REFERENCES attachments(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    db.execSQL("CREATE INDEX index_workspace_connections_workspaceId ON workspace_connections(workspaceId)")
    db.execSQL(
        "CREATE INDEX index_workspace_connections_capabilityInstanceId ON " +
            "workspace_connections(capabilityInstanceId)",
    )
    db.execSQL("CREATE INDEX index_workspace_connections_attachmentId ON workspace_connections(attachmentId)")
    db.execSQL("CREATE INDEX index_workspace_connections_updatedAt ON workspace_connections(updatedAt)")
    db.execSQL("CREATE INDEX index_workspace_connections_isDeleted ON workspace_connections(isDeleted)")
    db.execSQL(
        "CREATE INDEX index_workspace_connections_capabilityInstanceId_connectionOrder ON " +
            "workspace_connections(capabilityInstanceId, connectionOrder)",
    )
    db.execSQL(
        "CREATE UNIQUE INDEX index_workspace_connections_capabilityInstanceId_attachmentId ON " +
            "workspace_connections(capabilityInstanceId, attachmentId)",
    )
}

private fun validateLegacyConnections(
    db: SupportSQLiteDatabase,
    diagnostics: MutableList<String>,
) {
    db.query(
        """
        SELECT context_id, attachment_id
        FROM context_attachment_cross_ref
        WHERE context_id = '' OR attachment_id = ''
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            diagnostics += "BLANK_SOURCE_ID: ${cursor.getString(0)} / ${cursor.getString(1)}"
        }
    }
    db.query(
        """
        SELECT context_id, attachment_id, version
        FROM context_attachment_cross_ref
        WHERE version < 0
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            diagnostics += "INVALID_VERSION: ${cursor.getString(0)} / ${cursor.getString(1)}"
        }
    }
}

private fun ensureTypedConnectionsCapabilities(db: SupportSQLiteDatabase) {
    val owners =
        db.query(
            """
            SELECT DISTINCT w.id, w.createdAt
            FROM context_attachment_cross_ref ref
            JOIN workspaces w
              ON w.sourceContextId = ref.context_id
             AND w.provenance = 'CONTEXT_BACKED'
             AND w.isDeleted = 0
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getLong(1))
            }
        }

    owners.forEach { (workspaceId, createdAt) ->
        val existing =
            db.query(
                """
                SELECT id FROM workspace_capability_instances
                WHERE workspaceId = ?
                  AND capabilityType = 'CONNECTIONS'
                  AND instanceKey = 'default'
                """.trimIndent(),
                arrayOf(workspaceId),
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

        if (existing == null) {
            val id = stableConnections159("CAPABILITY:$workspaceId:CONNECTIONS:default")
            val order =
                scalarLong159(
                    db,
                    "SELECT COALESCE(MAX(capabilityOrder), -1) + 1 FROM workspace_capability_instances " +
                        "WHERE workspaceId = '${sqlLiteral159(workspaceId)}'",
                )
            db.execSQL(
                """
                INSERT INTO workspace_capability_instances (
                    id, workspaceId, capabilityType, instanceKey, capabilityOrder,
                    state, configurationVersion, configuration, createdAt, updatedAt,
                    syncedAt, isDeleted, version
                ) VALUES (
                    '${sqlLiteral159(id)}', '${sqlLiteral159(workspaceId)}', 'CONNECTIONS',
                    'default', $order, 'ACTIVE', 1, '{}', $createdAt, $createdAt,
                    NULL, 0, 1
                )
                """.trimIndent(),
            )
        } else {
            db.execSQL(
                """
                UPDATE workspace_capability_instances
                SET configurationVersion = 1,
                    configuration = '{}',
                    state = 'ACTIVE',
                    isDeleted = 0,
                    syncedAt = NULL,
                    version = version + 1
                WHERE id = '${sqlLiteral159(existing)}'
                """.trimIndent(),
            )
        }
    }
}

private fun validateResolvedOwners(
    db: SupportSQLiteDatabase,
    diagnostics: MutableList<String>,
) {
    db.query(
        """
        SELECT ref.context_id, COUNT(DISTINCT w.id)
        FROM context_attachment_cross_ref ref
        LEFT JOIN workspaces w
          ON w.sourceContextId = ref.context_id
         AND w.provenance = 'CONTEXT_BACKED'
         AND w.isDeleted = 0
        GROUP BY ref.context_id
        HAVING COUNT(DISTINCT w.id) != 1
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            diagnostics += "UNRESOLVED_OWNER_WORKSPACE: ${cursor.getString(0)} resolved ${cursor.getLong(1)} Workspaces"
        }
    }
}

private fun validateResolvedCapabilities(
    db: SupportSQLiteDatabase,
    diagnostics: MutableList<String>,
) {
    db.query(
        """
        SELECT ref.context_id, COUNT(DISTINCT cap.id)
        FROM context_attachment_cross_ref ref
        JOIN workspaces w
          ON w.sourceContextId = ref.context_id
         AND w.provenance = 'CONTEXT_BACKED'
         AND w.isDeleted = 0
        LEFT JOIN workspace_capability_instances cap
          ON cap.workspaceId = w.id
         AND cap.capabilityType = 'CONNECTIONS'
         AND cap.instanceKey = 'default'
         AND cap.isDeleted = 0
        GROUP BY ref.context_id
        HAVING COUNT(DISTINCT cap.id) != 1
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            diagnostics += "UNRESOLVED_CAPABILITY_INSTANCE: ${cursor.getString(0)} resolved ${cursor.getLong(1)} instances"
        }
    }
}

private fun validateResolvedAttachments(
    db: SupportSQLiteDatabase,
    diagnostics: MutableList<String>,
) {
    db.query(
        """
        SELECT ref.context_id, ref.attachment_id
        FROM context_attachment_cross_ref ref
        LEFT JOIN attachments a ON a.id = ref.attachment_id
        WHERE a.id IS NULL
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            diagnostics += "UNRESOLVED_ATTACHMENT: ${cursor.getString(0)} / ${cursor.getString(1)}"
        }
    }
    db.query(
        """
        SELECT ref.context_id, ref.attachment_id
        FROM context_attachment_cross_ref ref
        JOIN attachments a ON a.id = ref.attachment_id
        WHERE ref.isDeleted = 0 AND a.isDeleted = 1
        """.trimIndent(),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            diagnostics += "LIVE_PLACEMENT_TARGETS_DELETED_ATTACHMENT: ${cursor.getString(0)} / ${cursor.getString(1)}"
        }
    }
}

private fun insertCanonicalConnections(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT INTO workspace_connections (
            id, workspaceId, capabilityInstanceId, attachmentId, connectionOrder,
            createdAt, updatedAt, syncedAt, isDeleted, version
        )
        SELECT
            'WORKSPACE_CONNECTION:' || length(cap.id) || ':' || cap.id || ':' ||
                length(ranked.attachment_id) || ':' || ranked.attachment_id,
            ranked.workspaceId,
            ranked.capabilityInstanceId,
            ranked.attachment_id,
            ranked.canonicalOrder,
            0,
            COALESCE(ranked.updatedAt, 0),
            NULL,
            ranked.isDeleted,
            ranked.version
        FROM (
            SELECT ref.*,
                   w.id AS workspaceId,
                   cap.id AS capabilityInstanceId,
                   ROW_NUMBER() OVER (
                       PARTITION BY cap.id
                       ORDER BY
                           CASE WHEN ref.isDeleted = 0 THEN 0 ELSE 1 END,
                           ref.attachment_order ASC,
                           a.createdAt DESC,
                           ref.attachment_id ASC
                   ) - 1 AS canonicalOrder
            FROM context_attachment_cross_ref ref
            JOIN workspaces w
              ON w.sourceContextId = ref.context_id
             AND w.provenance = 'CONTEXT_BACKED'
             AND w.isDeleted = 0
            JOIN workspace_capability_instances cap
              ON cap.workspaceId = w.id
             AND cap.capabilityType = 'CONNECTIONS'
             AND cap.instanceKey = 'default'
             AND cap.isDeleted = 0
            JOIN attachments a ON a.id = ref.attachment_id
        ) ranked
        JOIN workspace_capability_instances cap ON cap.id = ranked.capabilityInstanceId
        """.trimIndent(),
    )
}

private fun scalarLong159(db: SupportSQLiteDatabase, sql: String): Long =
    db.query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected scalar result for $sql" }
        cursor.getLong(0)
    }

private fun stableConnections159(name: String): String =
    UUID.nameUUIDFromBytes(
        ("ForwardApp Workspace Connections v1:$name").toByteArray(StandardCharsets.UTF_8),
    ).toString()

private fun sqlLiteral159(value: String): String = value.replace("'", "''")

package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Frozen schema-158 -> 159 CONNECTIONS hard cutover. */
val MIGRATION_158_159 =
    object : Migration(158, 159) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            createWorkspaceConnectionsTable(db)
            check(scalarLong159(db, "SELECT COUNT(*) FROM workspace_connections") == 0L) {
                "CONNECTIONS cutover blocked: canonical table already contains data"
            }

            val diagnostics = mutableListOf<String>()
            validateLegacyConnections(db, diagnostics)
            ensureConnectionsOwnerWorkspaces(db, diagnostics)
            ensureTypedConnectionsCapabilities(db, now)
            validateResolvedOwners(db, diagnostics)
            validateResolvedCapabilities(db, diagnostics)
            validateResolvedAttachments(db, diagnostics)
            check(diagnostics.isEmpty()) {
                "CONNECTIONS cutover blocked:\n${diagnostics.distinct().joinToString("\n")}"
            }

            insertCanonicalConnections(db, now)
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

private data class ConnectionsContext159(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
    val order: Long,
    val roleCode: String?,
)

private fun ensureConnectionsOwnerWorkspaces(
    db: SupportSQLiteDatabase,
    diagnostics: MutableList<String>,
) {
    val contexts = loadConnectionsContexts159(db)
    val cycleContextIds = cycleMembers159(contexts.mapValues { it.value.parentId })

    val ownerContextIds =
        db.query(
            """
            SELECT DISTINCT context_id
            FROM context_attachment_cross_ref
            ORDER BY context_id
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    ownerContextIds.forEach { contextId ->
        val context = contexts[contextId]
        if (context == null) {
            diagnostics += "UNRESOLVED_OWNER_CONTEXT: Context $contextId does not exist"
            return@forEach
        }

        ensureConnectionsContextWorkspace159(
            db = db,
            context = context,
            contexts = contexts,
            cycleContextIds = cycleContextIds,
            diagnostics = diagnostics,
        )
    }
}

private fun ensureConnectionsContextWorkspace159(
    db: SupportSQLiteDatabase,
    context: ConnectionsContext159,
    contexts: Map<String, ConnectionsContext159>,
    cycleContextIds: Set<String>,
    diagnostics: MutableList<String>,
) {
    val resolvedWorkspaceIds =
        db.query(
            """
            SELECT id
            FROM workspaces
            WHERE sourceContextId = ?
              AND provenance = 'CONTEXT_BACKED'
            ORDER BY id
            """.trimIndent(),
            arrayOf<Any?>(context.id),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    // Existing canonical ownership is authoritative at schema 158. Do not
    // re-project an already materialized Workspace from legacy Context data.
    if (resolvedWorkspaceIds.isNotEmpty()) return

    val parentContextId =
        context.parentId
            ?.takeIf { it in contexts }
            ?.takeUnless { context.id in cycleContextIds }

    if (parentContextId != null) {
        ensureConnectionsContextWorkspace159(
            db = db,
            context = requireNotNull(contexts[parentContextId]),
            contexts = contexts,
            cycleContextIds = cycleContextIds,
            diagnostics = diagnostics,
        )

        val parentExists =
            scalarLong159(
                db,
                "SELECT COUNT(*) FROM workspaces " +
                    "WHERE id = '${sqlLiteral159(parentContextId)}' " +
                    "AND provenance = 'CONTEXT_BACKED' " +
                    "AND sourceContextId = '${sqlLiteral159(parentContextId)}'",
            ) == 1L

        if (!parentExists) {
            diagnostics +=
                "UNRESOLVED_PARENT_WORKSPACE: Context ${context.id} parent $parentContextId " +
                    "has no canonical CONTEXT_BACKED Workspace"
            return
        }
    }

    val collision =
        db.query(
            """
            SELECT provenance, sourceContextId
            FROM workspaces
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf<Any?>(context.id),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                val provenance = cursor.getString(0)
                val sourceContextId = if (cursor.isNull(1)) null else cursor.getString(1)
                provenance to sourceContextId
            }
        }

    if (collision != null) {
        diagnostics +=
            "OWNER_WORKSPACE_ID_COLLISION: Context ${context.id} collides with " +
                "${collision.first} Workspace sourceContextId=${collision.second}"
        return
    }

    db.execSQL(
        """
        INSERT INTO workspaces (
            id,
            nameOverride,
            descriptionOverride,
            parentWorkspaceId,
            roleCode,
            workspaceOrder,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version,
            provenance,
            sourceContextId
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, 'CONTEXT_BACKED', ?)
        """.trimIndent(),
        arrayOf<Any?>(
            context.id,
            context.name,
            context.description,
            parentContextId,
            context.roleCode,
            context.order,
            context.createdAt,
            context.updatedAt ?: context.createdAt,
            if (context.isDeleted) 1 else 0,
            context.version.coerceAtLeast(1L),
            context.id,
        ),
    )
}

private fun loadConnectionsContexts159(
    db: SupportSQLiteDatabase,
): Map<String, ConnectionsContext159> =
    db.query(
        """
        SELECT
            id,
            name,
            description,
            parentId,
            createdAt,
            updatedAt,
            is_deleted,
            version,
            goal_order,
            role_code
        FROM contexts
        """.trimIndent(),
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                val row =
                    ConnectionsContext159(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        description = if (cursor.isNull(2)) null else cursor.getString(2),
                        parentId = if (cursor.isNull(3)) null else cursor.getString(3),
                        createdAt = cursor.getLong(4),
                        updatedAt = if (cursor.isNull(5)) null else cursor.getLong(5),
                        isDeleted = cursor.getInt(6) != 0,
                        version = cursor.getLong(7),
                        order = cursor.getLong(8),
                        roleCode = if (cursor.isNull(9)) null else cursor.getString(9),
                    )
                put(row.id, row)
            }
        }
    }

private fun cycleMembers159(parentById: Map<String, String?>): Set<String> {
    val result = mutableSetOf<String>()

    parentById.keys.forEach { start ->
        val path = mutableListOf<String>()
        val indexById = mutableMapOf<String, Int>()
        var current: String? = start

        while (current != null && current in parentById && current !in result) {
            val repeatedAt = indexById[current]
            if (repeatedAt != null) {
                result += path.drop(repeatedAt)
                break
            }
            indexById[current] = path.size
            path += current
            current = parentById[current]
        }
    }

    return result
}

private fun ensureTypedConnectionsCapabilities(
    db: SupportSQLiteDatabase,
    now: Long,
) {
    val owners =
        db.query(
            """
            SELECT DISTINCT w.id, w.createdAt, w.isDeleted
            FROM context_attachment_cross_ref ref
            JOIN workspaces w
              ON w.sourceContextId = ref.context_id
             AND w.provenance = 'CONTEXT_BACKED'
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Triple(
                            cursor.getString(0),
                            cursor.getLong(1),
                            cursor.getInt(2) != 0,
                        ),
                    )
                }
            }
        }

    owners.forEach { (workspaceId, createdAt, ownerDeleted) ->
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
            val canonicalUpdatedAt = if (ownerDeleted) now else createdAt
            val canonicalDeleted = if (ownerDeleted) 1 else 0
            db.execSQL(
                """
                INSERT INTO workspace_capability_instances (
                    id, workspaceId, capabilityType, instanceKey, capabilityOrder,
                    state, configurationVersion, configuration, createdAt, updatedAt,
                    syncedAt, isDeleted, version
                ) VALUES (
                    '${sqlLiteral159(id)}', '${sqlLiteral159(workspaceId)}', 'CONNECTIONS',
                    'default', $order, 'ACTIVE', 1, '{}', $createdAt, $canonicalUpdatedAt,
                    NULL, $canonicalDeleted, 1
                )
                """.trimIndent(),
            )
        } else if (ownerDeleted) {
            // Capability deletion preserves lifecycle state. A historical
            // placement under an already-deleted owner therefore needs a
            // tombstoned capability anchor, not capability resurrection.
            db.execSQL(
                """
                UPDATE workspace_capability_instances
                SET configurationVersion = 1,
                    configuration = '{}',
                    updatedAt = $now,
                    syncedAt = NULL,
                    isDeleted = 1,
                    version = version + 1
                WHERE id = '${sqlLiteral159(existing)}'
                  AND (
                      isDeleted = 0 OR
                      configurationVersion != 1 OR
                      configuration != '{}'
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
        LEFT JOIN workspace_capability_instances cap
          ON cap.workspaceId = w.id
         AND cap.capabilityType = 'CONNECTIONS'
         AND cap.instanceKey = 'default'
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
}

private fun insertCanonicalConnections(
    db: SupportSQLiteDatabase,
    now: Long,
) {
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
            CASE
                WHEN ranked.isDeleted = 0 AND
                     (ranked.attachmentIsDeleted = 1 OR ranked.workspaceIsDeleted = 1)
                    THEN $now
                ELSE COALESCE(ranked.updatedAt, 0)
            END,
            NULL,
            CASE
                WHEN ranked.isDeleted != 0 OR
                     ranked.attachmentIsDeleted != 0 OR
                     ranked.workspaceIsDeleted != 0
                    THEN 1
                ELSE 0
            END,
            CASE
                WHEN ranked.isDeleted = 0 AND
                     (ranked.attachmentIsDeleted = 1 OR ranked.workspaceIsDeleted = 1)
                    THEN ranked.version + 1
                ELSE ranked.version
            END
        FROM (
            SELECT ref.*,
                   w.id AS workspaceId,
                   cap.id AS capabilityInstanceId,
                   ROW_NUMBER() OVER (
                       PARTITION BY cap.id
                       ORDER BY
                           CASE
                               WHEN ref.isDeleted = 0 AND
                                    a.isDeleted = 0 AND
                                    w.isDeleted = 0
                                   THEN 0
                               ELSE 1
                           END,
                           ref.attachment_order ASC,
                           a.createdAt DESC,
                           ref.attachment_id ASC
                   ) - 1 AS canonicalOrder,
                   a.isDeleted AS attachmentIsDeleted,
                   w.isDeleted AS workspaceIsDeleted
            FROM context_attachment_cross_ref ref
            JOIN workspaces w
              ON w.sourceContextId = ref.context_id
             AND w.provenance = 'CONTEXT_BACKED'
            JOIN workspace_capability_instances cap
              ON cap.workspaceId = w.id
             AND cap.capabilityType = 'CONNECTIONS'
             AND cap.instanceKey = 'default'
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

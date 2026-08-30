package com.romankozak.forwardappmobile.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Android DIRECTION authority cutover.
 *
 * Schema 155 still owns DIRECTION through direction_items. The successful
 * 155 -> 156 transaction explicitly accounts for every legacy row in canonical
 * WorkspaceDirectionEntry state before direction_items is removed.
 *
 * Unlinked legacy rows are semantic DIRECTION Orientations. Linked rows remain
 * Workspace navigation entries and never infer semantic Orientation intent.
 *
 * This migration is intentionally fail-closed. A failure rolls back the whole
 * Room migration transaction, leaving the schema-155 database authoritative.
 */
val MIGRATION_155_156 =
    object : Migration(155, 156) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val contexts = loadDirectionCutoverContexts(db)
            val cycleContextIds = cycleMembers(contexts.mapValues { it.value.parentId })
            val legacyRows = loadLegacyDirectionRows(db)

            // Schema 156 makes canonical DIRECTION runtime authority immediately.
            // Provision every live legacy Context before direction_items disappears,
            // including Contexts that never had a Direction row. Otherwise their
            // first post-cutover Direction write could lack its default capability.
            contexts.values
                .asSequence()
                .filterNot { it.isDeleted }
                .sortedBy { it.id }
                .forEach { context ->
                    ensureContextBackedWorkspace(
                        db = db,
                        context = context,
                        contexts = contexts,
                        cycleContextIds = cycleContextIds,
                        now = now,
                    )
                    ensureDirectionCapability(
                        db = db,
                        context = context,
                        now = now,
                    )
                }

            legacyRows.forEach { row ->
                val owner =
                    requireNotNull(contexts[row.contextId]) {
                        "DIRECTION cutover blocked for ${row.id}: missing owner Context ${row.contextId}"
                    }

                if (!row.isDeleted) {
                    check(!owner.isDeleted) {
                        "DIRECTION cutover blocked for ${row.id}: live row belongs to deleted Context ${owner.id}"
                    }
                }

                ensureContextBackedWorkspace(
                    db = db,
                    context = owner,
                    contexts = contexts,
                    cycleContextIds = cycleContextIds,
                    now = now,
                )

                val capabilityId =
                    ensureDirectionCapability(
                        db = db,
                        context = owner,
                        now = now,
                    )

                val linkedContextId = row.linkedContextId?.trim()?.takeIf { it.isNotEmpty() }

                val orientationId =
                    if (linkedContextId == null) {
                        materializeSemanticDirection(
                            db = db,
                            row = row,
                        )
                    } else {
                        quarantineLinkedSemanticShadow(
                            db = db,
                            row = row,
                            now = now,
                        )

                        val target =
                            requireNotNull(contexts[linkedContextId]) {
                                "DIRECTION cutover blocked for ${row.id}: missing target Context $linkedContextId"
                            }

                        if (!row.isDeleted) {
                            check(!target.isDeleted) {
                                "DIRECTION cutover blocked for ${row.id}: live row targets deleted Context $linkedContextId"
                            }
                        }

                        ensureContextBackedWorkspace(
                            db = db,
                            context = target,
                            contexts = contexts,
                            cycleContextIds = cycleContextIds,
                            now = now,
                        )
                        null
                    }

                materializeDirectionEntry(
                    db = db,
                    row = row,
                    capabilityId = capabilityId,
                    orientationId = orientationId,
                    targetWorkspaceId = linkedContextId,
                )
            }

            verifyNoOrphanLegacyDirectionEntries(
                db = db,
                sourceIds = legacyRows.mapTo(hashSetOf()) { it.id },
            )

            legacyRows.forEach { row ->
                verifyLegacyDirectionAccounted(
                    db = db,
                    row = row,
                )
            }

            check(
                countRows(db, "direction_items") ==
                    countLegacyDirectionEntriesForSourceIds(
                        db = db,
                        sourceIds = legacyRows.map { it.id },
                    ),
            ) {
                "DIRECTION cutover blocked: legacy row/accounting count mismatch"
            }

            db.execSQL("DROP TABLE `direction_items`")
        }
    }

private data class DirectionCutoverContext(
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

private data class Direction155Row(
    val id: String,
    val contextId: String,
    val text: String,
    val linkedContextId: String?,
    val itemOrder: Int,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

private data class ExistingWorkspaceProjection(
    val id: String,
    val nameOverride: String?,
    val descriptionOverride: String?,
    val parentWorkspaceId: String?,
    val roleCode: String?,
    val workspaceOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val version: Long,
    val provenance: String,
    val sourceContextId: String?,
)

private data class ExistingCapabilityProjection(
    val id: String,
    val workspaceId: String,
    val capabilityType: String,
    val instanceKey: String,
    val capabilityOrder: Long,
    val state: String,
    val configurationVersion: Int,
    val configuration: String,
    val createdAt: Long,
    val isDeleted: Boolean,
    val version: Long,
)

private fun loadDirectionCutoverContexts(
    db: SupportSQLiteDatabase,
): Map<String, DirectionCutoverContext> =
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
                    DirectionCutoverContext(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        description = cursor.stringOrNull(2),
                        parentId = cursor.stringOrNull(3),
                        createdAt = cursor.getLong(4),
                        updatedAt = cursor.longOrNull(5),
                        isDeleted = cursor.getInt(6) != 0,
                        version = cursor.getLong(7),
                        order = cursor.getLong(8),
                        roleCode = cursor.stringOrNull(9),
                    )
                put(row.id, row)
            }
        }
    }

private fun loadLegacyDirectionRows(
    db: SupportSQLiteDatabase,
): List<Direction155Row> =
    db.query(
        """
        SELECT
            id,
            contextId,
            text,
            linked_context_id,
            itemOrder,
            updatedAt,
            synced_at,
            is_deleted,
            version
        FROM direction_items
        ORDER BY contextId, itemOrder, id
        """.trimIndent(),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Direction155Row(
                        id = cursor.getString(0),
                        contextId = cursor.getString(1),
                        text = cursor.getString(2),
                        linkedContextId = cursor.stringOrNull(3),
                        itemOrder = cursor.getInt(4),
                        updatedAt = cursor.longOrNull(5),
                        syncedAt = cursor.longOrNull(6),
                        isDeleted = cursor.getInt(7) != 0,
                        version = cursor.getLong(8),
                    ),
                )
            }
        }
    }

private fun ensureContextBackedWorkspace(
    db: SupportSQLiteDatabase,
    context: DirectionCutoverContext,
    contexts: Map<String, DirectionCutoverContext>,
    cycleContextIds: Set<String>,
    now: Long,
) {
    val parentWorkspaceId =
        context.parentId
            ?.takeIf { parentId -> parentId in contexts }
            ?.takeUnless { context.id in cycleContextIds }

    if (parentWorkspaceId != null) {
        val parent = requireNotNull(contexts[parentWorkspaceId])
        ensureContextBackedWorkspace(
            db = db,
            context = parent,
            contexts = contexts,
            cycleContextIds = cycleContextIds,
            now = now,
        )
    }

    val existing = loadWorkspace(db, context.id)
    val desiredUpdatedAt = context.updatedAt ?: context.createdAt
    val desiredVersion = context.version.coerceAtLeast(1L)

    if (existing == null) {
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
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                context.id,
                context.name,
                context.description,
                parentWorkspaceId,
                context.roleCode,
                context.order,
                context.createdAt,
                desiredUpdatedAt,
                boolInt(context.isDeleted),
                desiredVersion,
                CONTEXT_BACKED_PROVENANCE,
                context.id,
            ),
        )
        return
    }

    check(existing.provenance == CONTEXT_BACKED_PROVENANCE) {
        "DIRECTION cutover blocked: Context ${context.id} collides with ${existing.provenance} Workspace"
    }
    check(existing.sourceContextId == context.id) {
        "DIRECTION cutover blocked: Workspace ${context.id} has sourceContextId=${existing.sourceContextId}"
    }

    val sameProjection =
        existing.nameOverride == context.name &&
            existing.descriptionOverride == context.description &&
            existing.parentWorkspaceId == parentWorkspaceId &&
            existing.roleCode == context.roleCode &&
            existing.workspaceOrder == context.order &&
            existing.isDeleted == context.isDeleted

    if (sameProjection) return

    db.execSQL(
        """
        UPDATE workspaces
        SET
            nameOverride = ?,
            descriptionOverride = ?,
            parentWorkspaceId = ?,
            roleCode = ?,
            workspaceOrder = ?,
            updatedAt = ?,
            syncedAt = NULL,
            isDeleted = ?,
            version = ?,
            provenance = ?,
            sourceContextId = ?
        WHERE id = ?
        """.trimIndent(),
        arrayOf<Any?>(
            context.name,
            context.description,
            parentWorkspaceId,
            context.roleCode,
            context.order,
            now,
            boolInt(context.isDeleted),
            existing.version + 1L,
            CONTEXT_BACKED_PROVENANCE,
            context.id,
            context.id,
        ),
    )
}

private fun loadWorkspace(
    db: SupportSQLiteDatabase,
    workspaceId: String,
): ExistingWorkspaceProjection? =
    db.query(
        """
        SELECT
            id,
            nameOverride,
            descriptionOverride,
            parentWorkspaceId,
            roleCode,
            workspaceOrder,
            createdAt,
            updatedAt,
            isDeleted,
            version,
            provenance,
            sourceContextId
        FROM workspaces
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(workspaceId),
    ).use { cursor ->
        if (!cursor.moveToFirst()) {
            null
        } else {
            ExistingWorkspaceProjection(
                id = cursor.getString(0),
                nameOverride = cursor.stringOrNull(1),
                descriptionOverride = cursor.stringOrNull(2),
                parentWorkspaceId = cursor.stringOrNull(3),
                roleCode = cursor.stringOrNull(4),
                workspaceOrder = cursor.getLong(5),
                createdAt = cursor.getLong(6),
                updatedAt = cursor.getLong(7),
                isDeleted = cursor.getInt(8) != 0,
                version = cursor.getLong(9),
                provenance = cursor.getString(10),
                sourceContextId = cursor.stringOrNull(11),
            )
        }
    }

private fun ensureDirectionCapability(
    db: SupportSQLiteDatabase,
    context: DirectionCutoverContext,
    now: Long,
): String {
    val autoLinkChildWorkspaces = loadAutoLinkSubprojects(db, context.id) ?: true
    val configuration =
        "{\"autoLinkChildWorkspaces\":$autoLinkChildWorkspaces}"

    val existing = loadDefaultDirectionCapability(db, context.id)

    if (existing != null) {
        val sameProjection =
            existing.workspaceId == context.id &&
                existing.capabilityType == DIRECTION_CAPABILITY_TYPE &&
                existing.instanceKey == DEFAULT_CAPABILITY_INSTANCE_KEY &&
                existing.capabilityOrder == DIRECTION_CAPABILITY_ORDER &&
                existing.state == DIRECTION_CAPABILITY_STATE_ACTIVE &&
                existing.configurationVersion == DIRECTION_CAPABILITY_CONFIGURATION_VERSION &&
                existing.configuration == configuration &&
                !existing.isDeleted

        if (!sameProjection) {
            db.execSQL(
                """
                UPDATE workspace_capability_instances
                SET
                    workspaceId = ?,
                    capabilityType = ?,
                    instanceKey = ?,
                    capabilityOrder = ?,
                    state = ?,
                    configurationVersion = ?,
                    configuration = ?,
                    updatedAt = ?,
                    syncedAt = NULL,
                    isDeleted = 0,
                    version = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf<Any?>(
                    context.id,
                    DIRECTION_CAPABILITY_TYPE,
                    DEFAULT_CAPABILITY_INSTANCE_KEY,
                    DIRECTION_CAPABILITY_ORDER,
                    DIRECTION_CAPABILITY_STATE_ACTIVE,
                    DIRECTION_CAPABILITY_CONFIGURATION_VERSION,
                    configuration,
                    now,
                    existing.version + 1L,
                    existing.id,
                ),
            )
        }
        return existing.id
    }

    val id = stableWorkspaceId("CAPABILITY:${context.id}:${DIRECTION_CAPABILITY_TYPE}:$DEFAULT_CAPABILITY_INSTANCE_KEY")

    db.query(
        """
        SELECT workspaceId, capabilityType, instanceKey
        FROM workspace_capability_instances
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(id),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            "DIRECTION cutover blocked: deterministic capability id $id is already owned by another capability"
        }
    }

    db.execSQL(
        """
        INSERT INTO workspace_capability_instances (
            id,
            workspaceId,
            capabilityType,
            instanceKey,
            capabilityOrder,
            state,
            configurationVersion,
            configuration,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, 1)
        """.trimIndent(),
        arrayOf<Any?>(
            id,
            context.id,
            DIRECTION_CAPABILITY_TYPE,
            DEFAULT_CAPABILITY_INSTANCE_KEY,
            DIRECTION_CAPABILITY_ORDER,
            DIRECTION_CAPABILITY_STATE_ACTIVE,
            DIRECTION_CAPABILITY_CONFIGURATION_VERSION,
            configuration,
            context.createdAt,
            now,
        ),
    )

    return id
}

private fun loadDefaultDirectionCapability(
    db: SupportSQLiteDatabase,
    workspaceId: String,
): ExistingCapabilityProjection? =
    db.query(
        """
        SELECT
            id,
            workspaceId,
            capabilityType,
            instanceKey,
            capabilityOrder,
            state,
            configurationVersion,
            configuration,
            createdAt,
            isDeleted,
            version
        FROM workspace_capability_instances
        WHERE workspaceId = ?
          AND capabilityType = ?
          AND instanceKey = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(
            workspaceId,
            DIRECTION_CAPABILITY_TYPE,
            DEFAULT_CAPABILITY_INSTANCE_KEY,
        ),
    ).use { cursor ->
        if (!cursor.moveToFirst()) {
            null
        } else {
            ExistingCapabilityProjection(
                id = cursor.getString(0),
                workspaceId = cursor.getString(1),
                capabilityType = cursor.getString(2),
                instanceKey = cursor.getString(3),
                capabilityOrder = cursor.getLong(4),
                state = cursor.getString(5),
                configurationVersion = cursor.getInt(6),
                configuration = cursor.getString(7),
                createdAt = cursor.getLong(8),
                isDeleted = cursor.getInt(9) != 0,
                version = cursor.getLong(10),
            )
        }
    }

private fun loadAutoLinkSubprojects(
    db: SupportSQLiteDatabase,
    contextId: String,
): Boolean? =
    db.query(
        """
        SELECT enable_auto_link_subprojects
        FROM context_structures
        WHERE contextId = ?
          AND isDeleted = 0
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(contextId),
    ).use { cursor ->
        if (!cursor.moveToFirst() || cursor.isNull(0)) {
            null
        } else {
            cursor.getInt(0) != 0
        }
    }

private fun materializeSemanticDirection(
    db: SupportSQLiteDatabase,
    row: Direction155Row,
): String {
    val subjectId = stableLegacyDirectionSubjectId(row.id)
    val revisionId = stableLegacyDirectionRevisionId(row.id)
    val timestamp = row.updatedAt ?: 0L

    validateSemanticDirectionIdentity(
        db = db,
        row = row,
        subjectId = subjectId,
        revisionId = revisionId,
    )

    db.execSQL(
        """
        INSERT OR IGNORE INTO managed_subjects (
            id,
            subjectType,
            title,
            description,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        ) VALUES (?, ?, ?, NULL, ?, ?, NULL, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            subjectId,
            ORIENTATION_SUBJECT_TYPE,
            row.text,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
        ),
    )

    db.execSQL(
        """
        UPDATE managed_subjects
        SET
            subjectType = ?,
            title = ?,
            description = NULL,
            createdAt = ?,
            updatedAt = ?,
            syncedAt = NULL,
            isDeleted = ?,
            version = ?
        WHERE id = ?
        """.trimIndent(),
        arrayOf<Any?>(
            ORIENTATION_SUBJECT_TYPE,
            row.text,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
            subjectId,
        ),
    )

    db.execSQL(
        """
        INSERT OR IGNORE INTO orientations (
            subjectId,
            kind,
            lifecycle,
            lifecycleOrigin
        ) VALUES (?, ?, NULL, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            subjectId,
            DIRECTION_ORIENTATION_KIND,
            VALUE_ORIGIN_UNSET,
        ),
    )

    db.execSQL(
        """
        UPDATE orientations
        SET
            kind = ?,
            lifecycle = NULL,
            lifecycleOrigin = ?
        WHERE subjectId = ?
        """.trimIndent(),
        arrayOf<Any?>(
            DIRECTION_ORIENTATION_KIND,
            VALUE_ORIGIN_UNSET,
            subjectId,
        ),
    )

    db.execSQL(
        """
        INSERT OR IGNORE INTO orientation_assessment_revisions (
            id,
            orientationId,
            effectiveFrom,
            recordedAt,
            source,
            reason,
            assessmentJson,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            revisionId,
            subjectId,
            timestamp,
            timestamp,
            ASSESSMENT_REVISION_SOURCE_MIGRATION,
            DIRECTION_ASSESSMENT_REVISION_REASON,
            EMPTY_DIRECTION_ASSESSMENT_JSON,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
        ),
    )

    db.execSQL(
        """
        UPDATE orientation_assessment_revisions
        SET
            orientationId = ?,
            effectiveFrom = ?,
            recordedAt = ?,
            source = ?,
            reason = ?,
            assessmentJson = ?,
            createdAt = ?,
            updatedAt = ?,
            syncedAt = NULL,
            isDeleted = ?,
            version = ?
        WHERE id = ?
        """.trimIndent(),
        arrayOf<Any?>(
            subjectId,
            timestamp,
            timestamp,
            ASSESSMENT_REVISION_SOURCE_MIGRATION,
            DIRECTION_ASSESSMENT_REVISION_REASON,
            EMPTY_DIRECTION_ASSESSMENT_JSON,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
            revisionId,
        ),
    )

    db.execSQL(
        """
        INSERT OR REPLACE INTO orientation_assessments (
            orientationId,
            revisionId,
            importanceValue,
            importanceOrigin,
            impactValue,
            impactOrigin,
            breadthValue,
            breadthOrigin,
            expectedSpanValue,
            expectedSpanOrigin,
            targetWindowValue,
            targetWindowOrigin,
            attentionTierValue,
            attentionTierOrigin,
            commitmentValue,
            commitmentOrigin,
            confidenceValue,
            confidenceOrigin,
            provenanceJson,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        ) VALUES (
            ?, ?,
            NULL, ?,
            NULL, ?,
            NULL, ?,
            NULL, ?,
            NULL, ?,
            NULL, ?,
            NULL, ?,
            NULL, ?,
            ?,
            ?, ?,
            NULL,
            ?, ?
        )
        """.trimIndent(),
        arrayOf<Any?>(
            subjectId,
            revisionId,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            VALUE_ORIGIN_UNSET,
            EMPTY_ASSESSMENT_PROVENANCE_JSON,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
        ),
    )

    db.execSQL(
        """
        INSERT OR REPLACE INTO legacy_subject_mappings (
            id,
            sourceType,
            sourceId,
            subjectId,
            migrationVersion,
            state,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            subjectId,
            DIRECTION_LEGACY_SOURCE_TYPE,
            row.id,
            subjectId,
            DIRECTION_CUTOVER_MIGRATION_VERSION,
            DIRECTION_MAPPING_STATE_CUT_OVER,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
        ),
    )

    return subjectId
}

private fun validateSemanticDirectionIdentity(
    db: SupportSQLiteDatabase,
    row: Direction155Row,
    subjectId: String,
    revisionId: String,
) {
    db.query(
        """
        SELECT id, subjectId, state
        FROM legacy_subject_mappings
        WHERE sourceType = 'DIRECTION'
          AND sourceId = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(row.id),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            check(cursor.getString(1) == subjectId) {
                "DIRECTION cutover blocked for ${row.id}: mapping points to ${cursor.getString(1)}"
            }
        }
    }

    db.query(
        """
        SELECT sourceType, sourceId, subjectId
        FROM legacy_subject_mappings
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(subjectId),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            check(
                cursor.getString(0) == "DIRECTION" &&
                    cursor.getString(1) == row.id &&
                    cursor.getString(2) == subjectId,
            ) {
                "DIRECTION cutover blocked for ${row.id}: deterministic mapping id collision"
            }
        }
    }

    db.query(
        """
        SELECT subjectType
        FROM managed_subjects
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(subjectId),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            check(cursor.getString(0) == ORIENTATION_SUBJECT_TYPE) {
                "DIRECTION cutover blocked for ${row.id}: subject id collision"
            }
        }
    }

    db.query(
        """
        SELECT kind
        FROM orientations
        WHERE subjectId = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(subjectId),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            check(cursor.getString(0) == DIRECTION_ORIENTATION_KIND) {
                "DIRECTION cutover blocked for ${row.id}: Orientation kind collision"
            }
        }
    }

    db.query(
        """
        SELECT orientationId
        FROM orientation_assessment_revisions
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(revisionId),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            check(cursor.getString(0) == subjectId) {
                "DIRECTION cutover blocked for ${row.id}: assessment revision id collision"
            }
        }
    }
}


/**
 * Removes any pre-cutover semantic shadow for a legacy row that is now known
 * to be Workspace navigation.
 *
 * The deterministic DIRECTION identity is used only to identify an old shadow.
 * No semantic Orientation is created for a linked row. Historical mapping is
 * retained as QUARANTINED provenance at migration version 4.
 */
private fun quarantineLinkedSemanticShadow(
    db: SupportSQLiteDatabase,
    row: Direction155Row,
    now: Long,
) {
    val expectedSubjectId = stableLegacyDirectionSubjectId(row.id)

    var mappingCount = 0
    var mappingId: String? = null
    var mappedSubjectId: String? = null
    var mappingState: String? = null
    var mappingVersion = 0
    var mappingDeleted = false
    var mappingRowVersion = 0L

    db.query(
        """
        SELECT id, subjectId, state, migrationVersion, isDeleted, version
        FROM legacy_subject_mappings
        WHERE sourceType = 'DIRECTION'
          AND sourceId = ?
        """.trimIndent(),
        arrayOf<Any?>(row.id),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            mappingCount += 1
            check(mappingCount == 1) {
                "DIRECTION cutover blocked for ${row.id}: duplicate legacy DIRECTION mappings"
            }
            mappingId = cursor.getString(0)
            mappedSubjectId = cursor.getString(1)
            mappingState = cursor.getString(2)
            mappingVersion = cursor.getInt(3)
            mappingDeleted = cursor.getInt(4) != 0
            mappingRowVersion = cursor.getLong(5)
        }
    }

    if (mappingCount == 0) {
        db.query(
            """
            SELECT sourceType, sourceId, subjectId
            FROM legacy_subject_mappings
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf<Any?>(expectedSubjectId),
        ).use { cursor ->
            check(!cursor.moveToFirst()) {
                "DIRECTION cutover blocked for ${row.id}: deterministic mapping id is owned by another source"
            }
        }

        db.query(
            """
            SELECT id
            FROM managed_subjects
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf<Any?>(expectedSubjectId),
        ).use { cursor ->
            check(!cursor.moveToFirst()) {
                "DIRECTION cutover blocked for ${row.id}: deterministic semantic subject exists without source mapping"
            }
        }
        return
    }

    check(mappingId == expectedSubjectId) {
        "DIRECTION cutover blocked for ${row.id}: unexpected legacy mapping id $mappingId"
    }
    check(mappedSubjectId == expectedSubjectId) {
        "DIRECTION cutover blocked for ${row.id}: linked-row mapping points to $mappedSubjectId"
    }
    check(mappingState != DIRECTION_MAPPING_STATE_CUT_OVER) {
        "DIRECTION cutover blocked for ${row.id}: linked row already owns a CUT_OVER semantic mapping"
    }

    var subjectDeleted = false
    var subjectVersion = 0L
    db.query(
        """
        SELECT subjectType, isDeleted, version
        FROM managed_subjects
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(expectedSubjectId),
    ).use { cursor ->
        check(cursor.moveToFirst()) {
            "DIRECTION cutover blocked for ${row.id}: mapped semantic shadow subject is missing"
        }
        check(cursor.getString(0) == ORIENTATION_SUBJECT_TYPE) {
            "DIRECTION cutover blocked for ${row.id}: mapped shadow subject is not ORIENTATION"
        }
        subjectDeleted = cursor.getInt(1) != 0
        subjectVersion = cursor.getLong(2)
    }

    db.query(
        """
        SELECT kind
        FROM orientations
        WHERE subjectId = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(expectedSubjectId),
    ).use { cursor ->
        check(cursor.moveToFirst()) {
            "DIRECTION cutover blocked for ${row.id}: mapped shadow Orientation is missing"
        }
        check(cursor.getString(0) == DIRECTION_ORIENTATION_KIND) {
            "DIRECTION cutover blocked for ${row.id}: mapped shadow is not DIRECTION kind"
        }
    }

    db.query(
        """
        SELECT id
        FROM workspace_direction_entries
        WHERE orientationId = ?
          AND id != ?
          AND isDeleted = 0
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(expectedSubjectId, row.id),
    ).use { cursor ->
        check(!cursor.moveToFirst()) {
            "DIRECTION cutover blocked for ${row.id}: semantic shadow is referenced by another live DIRECTION entry"
        }
    }

    if (!subjectDeleted) {
        db.execSQL(
            """
            UPDATE managed_subjects
            SET
                updatedAt = ?,
                syncedAt = NULL,
                isDeleted = 1,
                version = ?
            WHERE id = ?
            """.trimIndent(),
            arrayOf<Any?>(now, subjectVersion + 1L, expectedSubjectId),
        )
    }

    val expectedMappingDeleted = row.isDeleted
    val mappingNeedsUpdate =
        mappingVersion != DIRECTION_CUTOVER_MIGRATION_VERSION ||
            mappingState != DIRECTION_MAPPING_STATE_QUARANTINED ||
            mappingDeleted != expectedMappingDeleted

    if (mappingNeedsUpdate) {
        db.execSQL(
            """
            UPDATE legacy_subject_mappings
            SET
                migrationVersion = ?,
                state = ?,
                updatedAt = ?,
                syncedAt = NULL,
                isDeleted = ?,
                version = ?
            WHERE id = ?
            """.trimIndent(),
            arrayOf<Any?>(
                DIRECTION_CUTOVER_MIGRATION_VERSION,
                DIRECTION_MAPPING_STATE_QUARANTINED,
                now,
                boolInt(expectedMappingDeleted),
                mappingRowVersion + 1L,
                expectedSubjectId,
            ),
        )
    }
}

private fun materializeDirectionEntry(
    db: SupportSQLiteDatabase,
    row: Direction155Row,
    capabilityId: String,
    orientationId: String?,
    targetWorkspaceId: String?,
) {
    check((orientationId == null) xor (targetWorkspaceId == null)) {
        "DIRECTION cutover blocked for ${row.id}: canonical entry must have exactly one target"
    }

    db.query(
        """
        SELECT provenance
        FROM workspace_direction_entries
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(row.id),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            check(cursor.getString(0) == LEGACY_DIRECTION_ENTRY_PROVENANCE) {
                "DIRECTION cutover blocked for ${row.id}: canonical-owned entry id collision"
            }
        }
    }

    val timestamp = row.updatedAt ?: 0L
    val labelOverride = if (targetWorkspaceId == null) null else row.text

    db.execSQL(
        """
        INSERT OR REPLACE INTO workspace_direction_entries (
            id,
            workspaceId,
            capabilityInstanceId,
            orientationId,
            targetWorkspaceId,
            labelOverride,
            entryOrder,
            provenance,
            createdAt,
            updatedAt,
            syncedAt,
            isDeleted,
            version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            row.id,
            row.contextId,
            capabilityId,
            orientationId,
            targetWorkspaceId,
            labelOverride,
            row.itemOrder.toLong(),
            LEGACY_DIRECTION_ENTRY_PROVENANCE,
            timestamp,
            timestamp,
            boolInt(row.isDeleted),
            row.version,
        ),
    )
}

private fun verifyNoOrphanLegacyDirectionEntries(
    db: SupportSQLiteDatabase,
    sourceIds: Set<String>,
) {
    db.query(
        """
        SELECT id
        FROM workspace_direction_entries
        WHERE provenance = ?
        """.trimIndent(),
        arrayOf<Any?>(LEGACY_DIRECTION_ENTRY_PROVENANCE),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            check(id in sourceIds) {
                "DIRECTION cutover blocked: orphan LEGACY_DIRECTION_ITEM entry $id has no direction_items source"
            }
        }
    }
}

private fun verifyLegacyDirectionAccounted(
    db: SupportSQLiteDatabase,
    row: Direction155Row,
) {
    val expectedTargetWorkspaceId = row.linkedContextId?.trim()?.takeIf { it.isNotEmpty() }

    db.query(
        """
        SELECT
            workspaceId,
            orientationId,
            targetWorkspaceId,
            labelOverride,
            entryOrder,
            provenance,
            syncedAt,
            isDeleted,
            version
        FROM workspace_direction_entries
        WHERE id = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(row.id),
    ).use { cursor ->
        check(cursor.moveToFirst()) {
            "DIRECTION cutover blocked for ${row.id}: canonical entry missing"
        }

        val orientationId = cursor.stringOrNull(1)
        val targetWorkspaceId = cursor.stringOrNull(2)

        check(cursor.getString(0) == row.contextId) {
            "DIRECTION cutover blocked for ${row.id}: owner mismatch"
        }
        check(cursor.getLong(4) == row.itemOrder.toLong()) {
            "DIRECTION cutover blocked for ${row.id}: order mismatch"
        }
        check(cursor.getString(5) == LEGACY_DIRECTION_ENTRY_PROVENANCE) {
            "DIRECTION cutover blocked for ${row.id}: provenance mismatch"
        }
        check(cursor.isNull(6)) {
            "DIRECTION cutover blocked for ${row.id}: canonical entry must be dirty after cutover"
        }
        check((cursor.getInt(7) != 0) == row.isDeleted) {
            "DIRECTION cutover blocked for ${row.id}: tombstone mismatch"
        }
        check(cursor.getLong(8) == row.version) {
            "DIRECTION cutover blocked for ${row.id}: version mismatch"
        }

        if (expectedTargetWorkspaceId == null) {
            check(orientationId != null && targetWorkspaceId == null) {
                "DIRECTION cutover blocked for ${row.id}: semantic target shape mismatch"
            }
            check(cursor.isNull(3)) {
                "DIRECTION cutover blocked for ${row.id}: semantic entry unexpectedly owns labelOverride"
            }
            verifySemanticDirectionCutOver(
                db = db,
                rowId = row.id,
                orientationId = orientationId,
            )
        } else {
            check(orientationId == null && targetWorkspaceId == expectedTargetWorkspaceId) {
                "DIRECTION cutover blocked for ${row.id}: Workspace target shape mismatch"
            }
            check(cursor.stringOrNull(3) == row.text) {
                "DIRECTION cutover blocked for ${row.id}: Workspace link label mismatch"
            }
            verifyLinkedDirectionSemanticShadowQuarantined(
                db = db,
                row = row,
            )
        }
    }
}


private fun verifyLinkedDirectionSemanticShadowQuarantined(
    db: SupportSQLiteDatabase,
    row: Direction155Row,
) {
    val expectedSubjectId = stableLegacyDirectionSubjectId(row.id)
    var mappingCount = 0

    db.query(
        """
        SELECT
            m.subjectId,
            m.migrationVersion,
            m.state,
            m.isDeleted,
            s.subjectType,
            s.isDeleted,
            o.kind
        FROM legacy_subject_mappings m
        LEFT JOIN managed_subjects s ON s.id = m.subjectId
        LEFT JOIN orientations o ON o.subjectId = m.subjectId
        WHERE m.sourceType = 'DIRECTION'
          AND m.sourceId = ?
        """.trimIndent(),
        arrayOf<Any?>(row.id),
    ).use { cursor ->
        while (cursor.moveToNext()) {
            mappingCount += 1
            check(mappingCount == 1) {
                "DIRECTION cutover blocked for ${row.id}: duplicate linked-row mappings after cleanup"
            }
            check(cursor.getString(0) == expectedSubjectId) {
                "DIRECTION cutover blocked for ${row.id}: linked-row mapping identity mismatch after cleanup"
            }
            check(cursor.getInt(1) == DIRECTION_CUTOVER_MIGRATION_VERSION) {
                "DIRECTION cutover blocked for ${row.id}: linked-row mapping version mismatch"
            }
            check(cursor.getString(2) == DIRECTION_MAPPING_STATE_QUARANTINED) {
                "DIRECTION cutover blocked for ${row.id}: linked-row semantic mapping is not QUARANTINED"
            }
            check((cursor.getInt(3) != 0) == row.isDeleted) {
                "DIRECTION cutover blocked for ${row.id}: linked-row mapping tombstone mismatch"
            }
            check(!cursor.isNull(4) && cursor.getString(4) == ORIENTATION_SUBJECT_TYPE) {
                "DIRECTION cutover blocked for ${row.id}: quarantined subject is not ORIENTATION"
            }
            check(!cursor.isNull(5) && cursor.getInt(5) != 0) {
                "DIRECTION cutover blocked for ${row.id}: linked-row semantic subject remains live"
            }
            check(!cursor.isNull(6) && cursor.getString(6) == DIRECTION_ORIENTATION_KIND) {
                "DIRECTION cutover blocked for ${row.id}: quarantined Orientation kind mismatch"
            }
        }
    }

    if (mappingCount == 0) {
        db.query(
            """
            SELECT id
            FROM managed_subjects
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf<Any?>(expectedSubjectId),
        ).use { cursor ->
            check(!cursor.moveToFirst()) {
                "DIRECTION cutover blocked for ${row.id}: unmapped semantic shadow survived linked-row cleanup"
            }
        }
    }
}

private fun verifySemanticDirectionCutOver(
    db: SupportSQLiteDatabase,
    rowId: String,
    orientationId: String,
) {
    db.query(
        """
        SELECT
            m.subjectId,
            m.migrationVersion,
            m.state,
            s.subjectType,
            o.kind
        FROM legacy_subject_mappings m
        JOIN managed_subjects s ON s.id = m.subjectId
        JOIN orientations o ON o.subjectId = m.subjectId
        WHERE m.sourceType = 'DIRECTION'
          AND m.sourceId = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf<Any?>(rowId),
    ).use { cursor ->
        check(cursor.moveToFirst()) {
            "DIRECTION cutover blocked for $rowId: semantic mapping missing"
        }
        check(cursor.getString(0) == orientationId) {
            "DIRECTION cutover blocked for $rowId: semantic mapping identity mismatch"
        }
        check(cursor.getInt(1) == DIRECTION_CUTOVER_MIGRATION_VERSION) {
            "DIRECTION cutover blocked for $rowId: mapping version mismatch"
        }
        check(cursor.getString(2) == DIRECTION_MAPPING_STATE_CUT_OVER) {
            "DIRECTION cutover blocked for $rowId: mapping is not CUT_OVER"
        }
        check(cursor.getString(3) == ORIENTATION_SUBJECT_TYPE) {
            "DIRECTION cutover blocked for $rowId: subject is not ORIENTATION"
        }
        check(cursor.getString(4) == DIRECTION_ORIENTATION_KIND) {
            "DIRECTION cutover blocked for $rowId: Orientation kind mismatch"
        }
    }
}

private fun countRows(
    db: SupportSQLiteDatabase,
    table: String,
): Int =
    db.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }

private fun countLegacyDirectionEntriesForSourceIds(
    db: SupportSQLiteDatabase,
    sourceIds: List<String>,
): Int {
    if (sourceIds.isEmpty()) return 0

    val placeholders = sourceIds.joinToString(",") { "?" }
    return db.query(
        """
        SELECT COUNT(*)
        FROM workspace_direction_entries
        WHERE provenance = ?
          AND id IN ($placeholders)
        """.trimIndent(),
        arrayOf<Any?>(LEGACY_DIRECTION_ENTRY_PROVENANCE, *sourceIds.toTypedArray()),
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }
}


private fun stableLegacyDirectionSubjectId(sourceId: String): String =
    frozenDirectionUuidV5("DIRECTION:$sourceId")

private fun stableLegacyDirectionRevisionId(sourceId: String): String =
    frozenDirectionUuidV5(
        "DIRECTION:$sourceId:assessment:$DIRECTION_CUTOVER_MIGRATION_VERSION",
    )

private fun stableWorkspaceId(name: String): String =
    frozenDirectionUuidV5("WORKSPACE:$name")

private fun frozenDirectionUuidV5(name: String): String {
    val namespace = UUID.fromString(DIRECTION_LEGACY_NAMESPACE_UUID)
    val namespaceBytes =
        ByteBuffer.allocate(16)
            .putLong(namespace.mostSignificantBits)
            .putLong(namespace.leastSignificantBits)
            .array()

    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(namespaceBytes)
    val hash = digest.digest(name.toByteArray(StandardCharsets.UTF_8))
    hash[6] = ((hash[6].toInt() and 0x0f) or 0x50).toByte()
    hash[8] = ((hash[8].toInt() and 0x3f) or 0x80).toByte()

    val bytes = ByteBuffer.wrap(hash)
    return UUID(bytes.long, bytes.long).toString()
}

private fun cycleMembers(parentById: Map<String, String?>): Set<String> {
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

private fun android.database.Cursor.stringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.longOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun boolInt(value: Boolean): Int = if (value) 1 else 0

private const val DIRECTION_CUTOVER_MIGRATION_VERSION = 4

// Frozen schema-155 -> 156 contract. Do not replace these with runtime enums,
// codecs, adapters, or ordinals: historical Room migrations must remain stable.
private const val DIRECTION_LEGACY_NAMESPACE_UUID = "1ae36c1a-cb9d-5e7c-8b3a-3bca70de4830"
private const val DIRECTION_LEGACY_SOURCE_TYPE = "DIRECTION"
private const val ORIENTATION_SUBJECT_TYPE = "ORIENTATION"
private const val DIRECTION_ORIENTATION_KIND = "DIRECTION"
private const val VALUE_ORIGIN_UNSET = "UNSET"
private const val ASSESSMENT_REVISION_SOURCE_MIGRATION = "MIGRATION"
private const val DIRECTION_MAPPING_STATE_CUT_OVER = "CUT_OVER"
private const val DIRECTION_MAPPING_STATE_QUARANTINED = "QUARANTINED"

private const val DIRECTION_CAPABILITY_TYPE = "DIRECTION"
private const val DIRECTION_CAPABILITY_ORDER = 4L
private const val DIRECTION_CAPABILITY_STATE_ACTIVE = "ACTIVE"
private const val DIRECTION_CAPABILITY_CONFIGURATION_VERSION = 1
private const val DEFAULT_CAPABILITY_INSTANCE_KEY = "default"

private const val CONTEXT_BACKED_PROVENANCE = "CONTEXT_BACKED"
private const val LEGACY_DIRECTION_ENTRY_PROVENANCE = "LEGACY_DIRECTION_ITEM"

private const val DIRECTION_ASSESSMENT_REVISION_REASON = "Legacy shadow bootstrap v4"
private const val EMPTY_ASSESSMENT_PROVENANCE_JSON = "[]"
private const val EMPTY_DIRECTION_ASSESSMENT_JSON =
    """{"importance":{"origin":"UNSET"},"impact":{"origin":"UNSET"},"breadth":{"origin":"UNSET"},"expectedSpan":{"origin":"UNSET"},"targetWindow":{"origin":"UNSET"},"attentionTier":{"origin":"UNSET"},"commitment":{"origin":"UNSET"},"confidence":{"origin":"UNSET"}}"""

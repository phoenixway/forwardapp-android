package com.romankozak.forwardappmobile.data.hierarchy

import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.validateProspectiveHierarchy
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

internal enum class CanonicalHierarchyMigrationCheckpoint {
    AFTER_PLACEMENTS,
    AFTER_NEUTRALIZATION,
}

internal data class CanonicalHierarchyMigrationEstablishmentReport(
    val performed: Boolean,
    val occurrenceCount: Int,
)

/**
 * Production migration-time hierarchy establishment kernel.
 *
 * This component is intentionally dormant until a future Room migration registers it.
 * It owns raw SQL mechanics only. All occurrence semantics flow
 * through [CanonicalV1HierarchySnapshotBuilder].
 */
internal class CanonicalHierarchyMigrationEstablisher(
    private val prerequisiteAdapter: CanonicalHierarchyMigrationPrerequisiteAdapter =
        CanonicalHierarchyMigrationPrerequisiteAdapter(),
    private val evidenceReader: CanonicalHierarchyMigrationEvidenceReader =
        CanonicalHierarchyMigrationEvidenceReader(),
    private val builder: CanonicalV1HierarchySnapshotBuilder =
        CanonicalV1HierarchySnapshotBuilder(),
) {
    fun establish(
        db: SupportSQLiteDatabase,
        now: Long,
        onCheckpoint: (CanonicalHierarchyMigrationCheckpoint) -> Unit = {},
    ): CanonicalHierarchyMigrationEstablishmentReport {
        val marker = readMarker(db)
        if (marker != null) {
            require(marker in 1..CURRENT_MARKER_VERSION) {
                "Unsupported hierarchy marker version $marker"
            }
            if (marker < CURRENT_MARKER_VERSION) {
                neutralize(db)
                onCheckpoint(CanonicalHierarchyMigrationCheckpoint.AFTER_NEUTRALIZATION)
                writeMarker(db, now)
            }
            writeOrigin(db, HierarchyEstablishmentOrigin.ESTABLISHED)
            return CanonicalHierarchyMigrationEstablishmentReport(
                performed = false,
                occurrenceCount = 0,
            )
        }

        val origin = readRequiredOrigin(db)
        val input =
            when (origin) {
                HierarchyEstablishmentOrigin.FRESH_NATIVE -> {
                    prerequisiteAdapter.converge(db, origin, now)
                    readFreshInput(db)
                }

                HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE -> {
                    prerequisiteAdapter.converge(db, origin, now)
                    evidenceReader.read(db)
                }

                HierarchyEstablishmentOrigin.ESTABLISHED ->
                    error("Hierarchy is ESTABLISHED without an activation marker")
            }

        val snapshot = builder.build(input, HierarchyId.GENERAL)
        CanonicalHierarchyMigrationPersistenceAdapter().persist(
            db = db,
            snapshot = snapshot,
            now = now,
        )
        onCheckpoint(CanonicalHierarchyMigrationCheckpoint.AFTER_PLACEMENTS)

        neutralize(db)
        onCheckpoint(CanonicalHierarchyMigrationCheckpoint.AFTER_NEUTRALIZATION)
        writeOrigin(db, HierarchyEstablishmentOrigin.ESTABLISHED)
        writeMarker(db, now)
        return CanonicalHierarchyMigrationEstablishmentReport(
            performed = true,
            occurrenceCount = snapshot.occurrences.size,
        )
    }

    private fun readFreshInput(db: SupportSQLiteDatabase): CanonicalHierarchyEstablishmentInput {
        require(count(db, "contexts") == 0L) { "Fresh migration found Context state" }
        require(count(db, "main_beacons") == 0L) { "Fresh migration found MainBeacon state" }
        require(count(db, "main_beacon_groups") == 0L) { "Fresh migration found Group state" }
        require(count(db, "main_beacon_group_members") == 0L) {
            "Fresh migration found Group membership state"
        }
        require(count(db, "hierarchy_placements") == 0L) { "Fresh migration found H1 state" }

        val expected = SystemOperationalDefinitions.all
        val rows =
            db.query(
                """
                SELECT id, nameOverride, provenance, sourceContextId, isDeleted,
                       parentWorkspaceId, workspaceOrder
                FROM workspaces ORDER BY id
                """.trimIndent(),
            ).use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(
                            cursor.getString(0),
                            FreshWorkspaceRow(
                                name = cursor.stringOrNull(1),
                                provenance = cursor.getString(2),
                                sourceContextId = cursor.stringOrNull(3),
                                isDeleted = cursor.getInt(4) != 0,
                                parentId = cursor.stringOrNull(5),
                                order = cursor.getLong(6),
                            ),
                        )
                    }
                }
            }
        require(rows.keys == expected.mapTo(linkedSetOf()) { it.id }) {
            "Fresh migration Workspace universe differs from System definitions"
        }

        return CanonicalHierarchyEstablishmentInput(
            workspaces =
                expected.mapIndexed { ordinal, definition ->
                    val row = rows.getValue(definition.id)
                    require(
                        !row.isDeleted &&
                            row.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                            row.sourceContextId == null &&
                            row.parentId == null &&
                            row.order == 0L &&
                            row.name == definition.defaultName,
                    ) {
                        "Malformed fresh System Workspace ${definition.id}"
                    }
                    CanonicalHierarchyEstablishmentWorkspaceInput(
                        id = definition.id,
                        name = requireNotNull(row.name),
                        canonicalParentId = definition.defaultParentId,
                        order = 0L,
                        sourceOrdinal = ordinal,
                    )
                },
            beacons = emptyList(),
            groups = emptyList(),
            additionalWorkspaceRoutes = emptyList(),
            additionalBeaconRoutes = emptyList(),
        )
    }

    private fun readMarker(db: SupportSQLiteDatabase): Int? =
        db.query(
            "SELECT version FROM hierarchy_authority_activation_state WHERE hierarchyId = ?",
            arrayOf<Any?>(HierarchyId.GENERAL.value),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else null }

    private fun readRequiredOrigin(db: SupportSQLiteDatabase): HierarchyEstablishmentOrigin {
        val raw =
            db.query(
                "SELECT origin FROM hierarchy_establishment_origin WHERE hierarchyId = ?",
                arrayOf<Any?>(HierarchyId.GENERAL.value),
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        requireNotNull(raw) { "Missing durable hierarchy establishment origin" }
        return runCatching { HierarchyEstablishmentOrigin.valueOf(raw) }
            .getOrElse { error("Unsupported hierarchy establishment origin $raw") }
    }

    private fun neutralize(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE workspaces SET parentWorkspaceId = NULL, workspaceOrder = 0
            WHERE parentWorkspaceId IS NOT NULL OR workspaceOrder != 0
            """.trimIndent(),
        )
        db.execSQL(
            "UPDATE main_beacons SET parent_beacon_id = NULL WHERE parent_beacon_id IS NOT NULL",
        )
        db.execSQL("DELETE FROM main_beacon_parent_links")
    }

    private fun writeOrigin(
        db: SupportSQLiteDatabase,
        origin: HierarchyEstablishmentOrigin,
    ) {
        db.execSQL(
            """
            INSERT OR REPLACE INTO hierarchy_establishment_origin(hierarchyId, origin)
            VALUES(?, ?)
            """.trimIndent(),
            arrayOf<Any?>(HierarchyId.GENERAL.value, origin.name),
        )
    }

    private fun writeMarker(
        db: SupportSQLiteDatabase,
        now: Long,
    ) {
        db.execSQL(
            """
            INSERT OR REPLACE INTO hierarchy_authority_activation_state(
                hierarchyId, version, activatedAt
            ) VALUES(?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(HierarchyId.GENERAL.value, CURRENT_MARKER_VERSION, now),
        )
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Long =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val CURRENT_MARKER_VERSION = 3
    }
}

/** Raw-SQL storage adapter using the same deterministic snapshot projections as runtime. */
internal class CanonicalHierarchyMigrationPersistenceAdapter {
    fun persist(
        db: SupportSQLiteDatabase,
        snapshot: CanonicalV1HierarchySnapshot,
        now: Long,
    ) {
        validateCanonicalV1HierarchySnapshotShape(snapshot)
        val desired = snapshot.toDeterministicHierarchyPlacements(now)
        val groupScopes = snapshot.toDeterministicHierarchyPlacementGroupScopes(now)
        val linkedAppearances = snapshot.toDeterministicHierarchyPlacementLinkedAppearances(now)
        validateTargetsAndHierarchy(db, desired)
        validateGroupScopes(db, groupScopes)

        val existing = readPlacements(db)
        when {
            existing.isEmpty() -> desired.forEach { insertPlacement(db, it) }
            exactCanonicalV1HierarchyStructuralMatch(existing, desired) -> Unit
            else -> throw CanonicalV1HierarchyMaterializationConflictException(
                "Migration establishment found structurally divergent H1",
            )
        }

        val existingScopes = readGroupScopes(db)
        when {
            existingScopes.isEmpty() -> groupScopes.forEach { insertGroupScope(db, it) }
            exactDeterministicHierarchyGroupScopeMatch(existingScopes, groupScopes) -> Unit
            else -> throw CanonicalV1HierarchyMaterializationConflictException(
                buildGroupScopeConflictMessage(existingScopes, groupScopes),
            )
        }

        val existingLinks = readLinkedAppearances(db)
        when {
            existingLinks.isEmpty() -> linkedAppearances.forEach { insertLinkedAppearance(db, it) }
            exactDeterministicHierarchyLinkedAppearanceMatch(existingLinks, linkedAppearances) -> Unit
            else -> throw CanonicalV1HierarchyMaterializationConflictException(
                buildLinkedAppearanceConflictMessage(existingLinks, linkedAppearances),
            )
        }
    }

    /**
     * Hard pre-retirement gate for schema 180.
     *
     * Physical legacy hierarchy storage may be destroyed only after durable
     * authority state and surviving canonical H1 independently validate.
     */
    fun validateCurrentState(db: SupportSQLiteDatabase) {
        val marker =
            db.query(
                "SELECT version FROM hierarchy_authority_activation_state WHERE hierarchyId = ?",
                arrayOf<Any?>(HierarchyId.GENERAL.value),
            ).use { cursor ->
                require(cursor.moveToFirst()) {
                    "Physical hierarchy retirement requires an activation marker"
                }
                cursor.getInt(0)
            }
        require(marker == 3) {
            "Physical hierarchy retirement requires activation marker v3, found v$marker"
        }

        val origin =
            db.query(
                "SELECT origin FROM hierarchy_establishment_origin WHERE hierarchyId = ?",
                arrayOf<Any?>(HierarchyId.GENERAL.value),
            ).use { cursor ->
                require(cursor.moveToFirst()) {
                    "Physical hierarchy retirement requires durable establishment origin"
                }
                cursor.getString(0)
            }
        require(origin == HierarchyEstablishmentOrigin.ESTABLISHED.name) {
            "Physical hierarchy retirement requires ESTABLISHED origin, found $origin"
        }

        val placements = readPlacements(db)
        // An empty canonical hierarchy is valid when there are no live
        // hierarchy targets. Validate shape/targets without inventing a
        // non-empty requirement.
        validateTargetsAndHierarchy(db, placements)

        val placementById = placements.associateBy { it.id.value }

        val scopes = readGroupScopes(db)
        validateGroupScopes(db, scopes)
        scopes.forEach { scope ->
            val placement =
                requireNotNull(placementById[scope.placementId]) {
                    "Hierarchy GroupScope references missing placement ${scope.placementId}"
                }
            require(scope.hierarchyId == placement.hierarchyId.value) {
                "Hierarchy GroupScope hierarchy disagrees with placement ${scope.placementId}"
            }
        }

        readLinkedAppearances(db).forEach { link ->
            val placement =
                requireNotNull(placementById[link.placementId]) {
                    "Linked-appearance provenance references missing placement ${link.placementId}"
                }
            require(link.hierarchyId == placement.hierarchyId.value) {
                "Linked-appearance provenance hierarchy disagrees with placement ${link.placementId}"
            }
            require(placement.placementKind == PlacementKind.LINK) {
                "Linked-appearance provenance references non-LINK placement ${link.placementId}"
            }
        }
    }

    private fun validateTargetsAndHierarchy(
        db: SupportSQLiteDatabase,
        placements: List<HierarchyPlacement>,
    ) {
        val liveTargets = placements.filterNot { it.isDeleted }.map { it.target }.distinct()
        val live =
            liveTargets.associateWith { target ->
                val (table, idColumn, deletedColumn) =
                    when (target.type) {
                        HierarchyTargetType.WORKSPACE -> Triple("workspaces", "id", "isDeleted")
                        HierarchyTargetType.MANAGED_SUBJECT ->
                            Triple("managed_subjects", "id", "isDeleted")
                    }
                db.query(
                    "SELECT $deletedColumn FROM $table WHERE $idColumn = ? LIMIT 1",
                    arrayOf<Any?>(target.id),
                ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 0 }
            }
        require(live.values.all { it }) { "Migration H1 references missing/deleted target" }
        val violations = validateProspectiveHierarchy(placements) { target -> live[target] == true }
        require(violations.isEmpty()) {
            "Migration H1 validation failed: ${violations.first().code}"
        }
    }

    private fun validateGroupScopes(
        db: SupportSQLiteDatabase,
        scopes: List<HierarchyPlacementGroupScopeEntity>,
    ) {
        scopes.mapNotNull { it.groupSubjectId }.toSet().forEach { subjectId ->
            val valid =
                db.query(
                    """
                    SELECT 1
                    FROM managed_subjects s
                    JOIN orientations o ON o.subjectId = s.id
                    JOIN legacy_subject_mappings m ON m.subjectId = s.id
                    WHERE s.id = ? AND s.isDeleted = 0
                      AND s.subjectType = 'ORIENTATION'
                      AND o.kind = 'MAIN_BEACON_GROUP'
                      AND m.sourceType = 'MAIN_BEACON_GROUP'
                      AND m.state = 'CUT_OVER' AND m.isDeleted = 0
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf<Any?>(subjectId),
                ).use { it.moveToFirst() }
            require(valid) { "Invalid migration GroupScope subject $subjectId" }
        }
    }

    private fun insertPlacement(
        db: SupportSQLiteDatabase,
        placement: HierarchyPlacement,
    ) {
        db.execSQL(
            """
            INSERT INTO hierarchy_placements(
                id, hierarchyId, targetType, targetId, parentPlacementId,
                placementKind, siblingOrder, createdAt, updatedAt, syncedAt,
                isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                placement.id.value, placement.hierarchyId.value,
                placement.target.type.name, placement.target.id,
                placement.parentPlacementId?.value, placement.placementKind.name,
                placement.siblingOrder, placement.createdAt, placement.updatedAt,
                placement.syncedAt, if (placement.isDeleted) 1 else 0, placement.version,
            ),
        )
    }

    private fun insertGroupScope(
        db: SupportSQLiteDatabase,
        row: HierarchyPlacementGroupScopeEntity,
    ) {
        db.execSQL(
            """
            INSERT INTO hierarchy_placement_group_scopes(
                placementId, hierarchyId, groupSubjectId, createdAt, updatedAt,
                syncedAt, isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                row.placementId, row.hierarchyId, row.groupSubjectId,
                row.createdAt, row.updatedAt, row.syncedAt,
                if (row.isDeleted) 1 else 0, row.version,
            ),
        )
    }

    private fun insertLinkedAppearance(
        db: SupportSQLiteDatabase,
        row: HierarchyPlacementLinkedAppearanceEntity,
    ) {
        db.execSQL(
            """
            INSERT INTO hierarchy_placement_linked_appearances(
                placementId, hierarchyId, createdAt, updatedAt, syncedAt,
                isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                row.placementId, row.hierarchyId, row.createdAt, row.updatedAt,
                row.syncedAt, if (row.isDeleted) 1 else 0, row.version,
            ),
        )
    }

    internal fun readPlacements(db: SupportSQLiteDatabase): List<HierarchyPlacement> =
        db.query(
            """
            SELECT id, hierarchyId, targetType, targetId, parentPlacementId,
                   placementKind, siblingOrder, createdAt, updatedAt, syncedAt,
                   isDeleted, version
            FROM hierarchy_placements ORDER BY id
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        HierarchyPlacement(
                            id = PlacementId(cursor.getString(0)),
                            hierarchyId = HierarchyId(cursor.getString(1)),
                            target =
                                HierarchyTargetRef(
                                    HierarchyTargetType.valueOf(cursor.getString(2)),
                                    cursor.getString(3),
                                ),
                            parentPlacementId = cursor.stringOrNull(4)?.let(::PlacementId),
                            placementKind = PlacementKind.valueOf(cursor.getString(5)),
                            siblingOrder = cursor.getLong(6),
                            createdAt = cursor.getLong(7),
                            updatedAt = cursor.getLong(8),
                            syncedAt = cursor.longOrNull(9),
                            isDeleted = cursor.getInt(10) != 0,
                            version = cursor.getLong(11),
                        ),
                    )
                }
            }
        }

    internal fun readGroupScopes(
        db: SupportSQLiteDatabase,
    ): List<HierarchyPlacementGroupScopeEntity> =
        db.query(
            """
            SELECT placementId, hierarchyId, groupSubjectId, createdAt, updatedAt,
                   syncedAt, isDeleted, version
            FROM hierarchy_placement_group_scopes ORDER BY placementId
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        HierarchyPlacementGroupScopeEntity(
                            placementId = cursor.getString(0), hierarchyId = cursor.getString(1),
                            groupSubjectId = cursor.stringOrNull(2), createdAt = cursor.getLong(3),
                            updatedAt = cursor.getLong(4), syncedAt = cursor.longOrNull(5),
                            isDeleted = cursor.getInt(6) != 0, version = cursor.getLong(7),
                        ),
                    )
                }
            }
        }

    internal fun readLinkedAppearances(
        db: SupportSQLiteDatabase,
    ): List<HierarchyPlacementLinkedAppearanceEntity> =
        db.query(
            """
            SELECT placementId, hierarchyId, createdAt, updatedAt, syncedAt,
                   isDeleted, version
            FROM hierarchy_placement_linked_appearances ORDER BY placementId
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        HierarchyPlacementLinkedAppearanceEntity(
                            placementId = cursor.getString(0), hierarchyId = cursor.getString(1),
                            createdAt = cursor.getLong(2), updatedAt = cursor.getLong(3),
                            syncedAt = cursor.longOrNull(4), isDeleted = cursor.getInt(5) != 0,
                            version = cursor.getLong(6),
                        ),
                    )
                }
            }
        }
}

private data class FreshWorkspaceRow(
    val name: String?,
    val provenance: String,
    val sourceContextId: String?,
    val isDeleted: Boolean,
    val parentId: String?,
    val order: Long,
)

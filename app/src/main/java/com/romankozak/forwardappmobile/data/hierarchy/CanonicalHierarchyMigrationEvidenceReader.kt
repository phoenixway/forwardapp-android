package com.romankozak.forwardappmobile.data.hierarchy

import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.orientation.buildProjection
import com.romankozak.forwardappmobile.data.workspace.WorkspacePresentationState
import com.romankozak.forwardappmobile.data.workspace.projectPresentationUniverseFromState
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType

/** Reads the converged schema-179 legacy evidence boundary for a future migration. */
internal class CanonicalHierarchyMigrationEvidenceReader {
    fun read(db: SupportSQLiteDatabase): CanonicalHierarchyEstablishmentInput {
        val contexts = readContexts(db)
        val activeContexts = contexts.filterNot { it.isDeleted }.sortedBy { it.order }
        val workspaces = readWorkspaces(db)
        val retiredOrdinaryIds =
            contexts.asSequence()
                .filter { it.isDeleted && !SystemContexts.isSystem(ContextId(it.id)) }
                .mapTo(hashSetOf()) { it.id }
        val canonicalTags =
            workspaces.asSequence()
                .map { it.presentationState }
                .filter { SystemContexts.isSystem(ContextId(it.id)) }
                .associate { it.id to emptyList<String>() }
        val presentations =
            projectPresentationUniverseFromState(
                contexts = activeContexts,
                workspaces = workspaces.map { it.presentationState },
                canonicalTagsById = canonicalTags,
                retiredOrdinaryContextIds = retiredOrdinaryIds,
            )
        requireDistinct("migration hierarchy presentation", presentations.map { it.id })

        val workspacesById =
            workspaces
                .filterNot { it.presentationState.isDeleted }
                .associateBy { it.presentationState.id }
        val workspaceInputs =
            presentations.mapIndexed { ordinal, presentation ->
                val workspace =
                    requireNotNull(workspacesById[presentation.id]) {
                        "Visible migration presentation ${presentation.id} has no live Workspace"
                    }
                CanonicalHierarchyEstablishmentWorkspaceInput(
                    id = workspace.presentationState.id,
                    name = presentation.name,
                    canonicalParentId = workspace.canonicalParentId,
                    order = workspace.order,
                    sourceOrdinal = ordinal,
                )
            }

        val projection = buildProjection(readManagedSubjects(db), readLegacyMappings(db))
        val ownerRows = readOwnerRows(db).groupBy { it.beaconId }
        ownerRows.forEach { (beaconId, rows) ->
            requireDistinct("Beacon $beaconId operational owner", rows.map { it.workspaceId })
        }
        val membersByBeacon = readGroupMembers(db).groupBy { it.beaconId }

        val beacons =
            db.query(
                """
                SELECT id, title, parent_beacon_id, beacon_order
                FROM main_beacons
                ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC, id ASC
                """.trimIndent(),
            ).use { cursor ->
                buildList {
                    var ordinal = 0
                    while (cursor.moveToNext()) {
                        val sourceId = cursor.getString(0)
                        val target =
                            requireNotNull(projection.beaconsByLegacyId[sourceId]) {
                                "Visible Main Beacon $sourceId has no live CUT_OVER target"
                            }
                        val members = membersByBeacon[sourceId].orEmpty()
                        add(
                            CanonicalHierarchyEstablishmentBeaconInput(
                                sourceId = sourceId,
                                target =
                                    HierarchyTargetRef(
                                        HierarchyTargetType.MANAGED_SUBJECT,
                                        target.id,
                                    ),
                                title = target.title,
                                order = cursor.getLong(3),
                                canonicalParentSourceId = cursor.stringOrNull(2),
                                operationalOwnerWorkspaceIds =
                                    ownerRows[sourceId].orEmpty().map { it.workspaceId },
                                groupIds = members.map { it.groupId },
                                groupOrders = members.associate { it.groupId to it.order },
                                sourceOrdinal = ordinal++,
                            ),
                        )
                    }
                }
            }

        val groups =
            db.query(
                """
                SELECT id, title, group_order
                FROM main_beacon_groups
                ORDER BY group_order ASC, title COLLATE NOCASE ASC, id ASC
                """.trimIndent(),
            ).use { cursor ->
                buildList {
                    var ordinal = 0
                    while (cursor.moveToNext()) {
                        val sourceId = cursor.getString(0)
                        val target =
                            requireNotNull(projection.groupsByLegacyId[sourceId]) {
                                "Visible Main Beacon Group $sourceId has no live CUT_OVER target"
                            }
                        add(
                            CanonicalHierarchyEstablishmentGroupInput(
                                sourceId = sourceId,
                                title = target.title,
                                order = cursor.getLong(2),
                                sourceOrdinal = ordinal++,
                                canonicalSubjectId = target.id,
                            ),
                        )
                    }
                }
            }

        return CanonicalHierarchyEstablishmentInput(
            workspaces = workspaceInputs,
            beacons = beacons,
            groups = groups,
            additionalWorkspaceRoutes = readContextParentLinks(db),
            additionalBeaconRoutes = readBeaconParentLinks(db),
        )
    }

    private fun readContexts(db: SupportSQLiteDatabase): List<Context> =
        db.query(
            """
            SELECT id, name, description, parentId, createdAt, updatedAt,
                   is_deleted, version, goal_order, role_code
            FROM contexts
            ORDER BY goal_order ASC, id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Context(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            description = cursor.stringOrNull(2),
                            parentId = cursor.stringOrNull(3).normalizedParent(),
                            createdAt = cursor.getLong(4),
                            updatedAt = cursor.longOrNull(5),
                            isDeleted = cursor.getInt(6) != 0,
                            version = cursor.getLong(7),
                            order = cursor.getLong(8),
                            roleCode = cursor.stringOrNull(9),
                        ),
                    )
                }
            }
        }

    private fun readWorkspaces(db: SupportSQLiteDatabase): List<WorkspaceMigrationEvidenceRow> =
        db.query(
            """
            SELECT id, nameOverride, descriptionOverride, parentWorkspaceId,
                   roleCode, workspaceOrder, isDeleted, provenance, sourceContextId
            FROM workspaces
            ORDER BY id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        WorkspaceMigrationEvidenceRow(
                            presentationState =
                                WorkspacePresentationState(
                                    id = cursor.getString(0),
                                    nameOverride = cursor.stringOrNull(1),
                                    descriptionOverride = cursor.stringOrNull(2),
                                    roleCode = cursor.stringOrNull(4),
                                    isDeleted = cursor.getInt(6) != 0,
                                    provenance = cursor.getString(7),
                                    sourceContextId = cursor.stringOrNull(8),
                                ),
                            canonicalParentId = cursor.stringOrNull(3).normalizedParent(),
                            order = cursor.getLong(5),
                        ),
                    )
                }
            }
        }

    private fun readManagedSubjects(db: SupportSQLiteDatabase): List<ManagedSubjectEntity> =
        db.query(
            """
            SELECT id, subjectType, title, description, createdAt, updatedAt,
                   syncedAt, isDeleted, version
            FROM managed_subjects
            ORDER BY id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ManagedSubjectEntity(
                            id = cursor.getString(0),
                            subjectType = cursor.getString(1),
                            title = cursor.getString(2),
                            description = cursor.stringOrNull(3),
                            createdAt = cursor.getLong(4),
                            updatedAt = cursor.getLong(5),
                            syncedAt = cursor.longOrNull(6),
                            isDeleted = cursor.getInt(7) != 0,
                            version = cursor.getLong(8),
                        ),
                    )
                }
            }
        }

    internal fun readLegacyMappings(db: SupportSQLiteDatabase): List<LegacySubjectMappingEntity> =
        db.query(
            """
            SELECT id, sourceType, sourceId, subjectId, migrationVersion, state,
                   createdAt, updatedAt, syncedAt, isDeleted, version
            FROM legacy_subject_mappings
            ORDER BY sourceType ASC, sourceId ASC, id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        LegacySubjectMappingEntity(
                            id = cursor.getString(0),
                            sourceType = cursor.getString(1),
                            sourceId = cursor.getString(2),
                            subjectId = cursor.getString(3),
                            migrationVersion = cursor.getInt(4),
                            state = cursor.getString(5),
                            createdAt = cursor.getLong(6),
                            updatedAt = cursor.getLong(7),
                            syncedAt = cursor.longOrNull(8),
                            isDeleted = cursor.getInt(9) != 0,
                            version = cursor.getLong(10),
                        ),
                    )
                }
            }
        }

    private fun readGroupMembers(db: SupportSQLiteDatabase): List<GroupMemberRow> =
        db.query(
            """
            SELECT group_id, beacon_id, member_order
            FROM main_beacon_group_members
            ORDER BY group_id ASC, member_order ASC, beacon_id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(GroupMemberRow(cursor.getString(0), cursor.getString(1), cursor.getLong(2)))
                }
            }
        }

    private fun readOwnerRows(db: SupportSQLiteDatabase): List<OwnerRow> =
        db.query(
            """
            SELECT beacon_id, context_id, ref_order FROM main_beacon_context_cross_ref
            UNION ALL
            SELECT beacon_id, workspace_id, ref_order FROM main_beacon_workspace_cross_ref
            ORDER BY beacon_id ASC, ref_order ASC, context_id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(OwnerRow(cursor.getString(0), cursor.getString(1)))
                }
            }
        }

    private fun readContextParentLinks(
        db: SupportSQLiteDatabase,
    ): List<CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute> =
        db.query(
            """
            SELECT parent_context_id, child_context_id, link_order
            FROM context_parent_links
            WHERE is_deleted = 0
            ORDER BY parent_context_id ASC, link_order ASC, child_context_id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                var ordinal = 0
                while (cursor.moveToNext()) {
                    add(
                        CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute(
                            parentWorkspaceId = cursor.getString(0),
                            childWorkspaceId = cursor.getString(1),
                            order = cursor.getLong(2),
                            sourceOrdinal = ordinal++,
                        ),
                    )
                }
            }
        }

    private fun readBeaconParentLinks(
        db: SupportSQLiteDatabase,
    ): List<CanonicalHierarchyEstablishmentAdditionalBeaconRoute> =
        db.query(
            """
            SELECT parent_beacon_id, child_beacon_id, link_order
            FROM main_beacon_parent_links
            ORDER BY parent_beacon_id ASC, link_order ASC, child_beacon_id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                var ordinal = 0
                while (cursor.moveToNext()) {
                    add(
                        CanonicalHierarchyEstablishmentAdditionalBeaconRoute(
                            parentSourceId = cursor.getString(0),
                            childSourceId = cursor.getString(1),
                            order = cursor.getLong(2),
                            sourceOrdinal = ordinal++,
                        ),
                    )
                }
            }
        }

    private fun requireDistinct(label: String, ids: List<String>) {
        val duplicate = ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }
        require(duplicate == null) {
            "$label id ${duplicate?.key} appears ${duplicate?.value} times"
        }
    }
}

private data class WorkspaceMigrationEvidenceRow(
    val presentationState: WorkspacePresentationState,
    val canonicalParentId: String?,
    val order: Long,
)

private data class GroupMemberRow(
    val groupId: String,
    val beaconId: String,
    val order: Long,
)

private data class OwnerRow(
    val beaconId: String,
    val workspaceId: String,
)

internal fun android.database.Cursor.stringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

internal fun android.database.Cursor.longOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun String?.normalizedParent(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

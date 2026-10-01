package com.romankozak.forwardappmobile.data.hierarchy

import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.data.database.HierarchyEstablishmentOrigin
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.mainBeaconEffectiveOrientation
import com.romankozak.forwardappmobile.data.orientation.planBootstrap
import com.romankozak.forwardappmobile.data.orientation.planMainBeaconCutover
import com.romankozak.forwardappmobile.data.orientation.toEffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

/** Minimal schema-179 prerequisites needed before raw migration H2 capture. */
internal class CanonicalHierarchyMigrationPrerequisiteAdapter {
    fun converge(
        db: SupportSQLiteDatabase,
        origin: HierarchyEstablishmentOrigin,
        now: Long,
    ) {
        convergeSystemWorkspaces(db, origin, now)
        if (origin == HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE) {
            convergeMainBeaconTargets(db, now)
        }
    }

    private fun convergeSystemWorkspaces(
        db: SupportSQLiteDatabase,
        origin: HierarchyEstablishmentOrigin,
        now: Long,
    ) {
        require(origin != HierarchyEstablishmentOrigin.ESTABLISHED)
        SystemOperationalDefinitions.all.forEach { definition ->
            val existing =
                db.query(
                    """
                    SELECT provenance, sourceContextId, isDeleted
                    FROM workspaces WHERE id = ? LIMIT 1
                    """.trimIndent(),
                    arrayOf<Any?>(definition.id),
                ).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        null
                    } else {
                        ExistingWorkspace(
                            provenance = cursor.getString(0),
                            sourceContextId = cursor.stringOrNull(1),
                            isDeleted = cursor.getInt(2) != 0,
                        )
                    }
                }

            if (existing != null) {
                require(!existing.isDeleted) { "Deleted System Workspace ${definition.id}" }
                when (existing.provenance) {
                    WorkspaceProvenance.CANONICAL_ONLY.name ->
                        require(existing.sourceContextId == null) {
                            "Canonical System Workspace ${definition.id} retains sourceContextId"
                        }

                    WorkspaceProvenance.CONTEXT_BACKED.name -> {
                        require(origin == HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE) {
                            "Fresh migration cannot promote Context-backed System Workspace ${definition.id}"
                        }
                        require(existing.sourceContextId == definition.id)
                        db.execSQL(
                            """
                            UPDATE workspaces
                            SET provenance = ?, sourceContextId = NULL,
                                updatedAt = ?, syncedAt = NULL, version = version + 1
                            WHERE id = ?
                            """.trimIndent(),
                            arrayOf<Any?>(WorkspaceProvenance.CANONICAL_ONLY.name, now, definition.id),
                        )
                    }

                    else -> error("Unsupported System Workspace provenance ${existing.provenance}")
                }
                return@forEach
            }

            val legacy = readContextEvidence(db, definition.id)
            require(
                origin == HierarchyEstablishmentOrigin.LEGACY_UPGRADE_REQUIRES_CAPTURE || legacy == null,
            ) {
                "Fresh migration cannot consume System Context ${definition.id}"
            }
            require(legacy?.isDeleted != true) { "Deleted System Context ${definition.id}" }

            val parentId =
                if (origin == HierarchyEstablishmentOrigin.FRESH_NATIVE) {
                    null
                } else {
                    legacy?.parentId ?: definition.defaultParentId
                }
            val workspaceOrder =
                if (origin == HierarchyEstablishmentOrigin.FRESH_NATIVE) 0L else legacy?.order ?: 0L
            val createdAt = legacy?.createdAt ?: now
            val updatedAt = legacy?.updatedAt ?: createdAt
            db.execSQL(
                """
                INSERT INTO workspaces(
                    id, nameOverride, descriptionOverride, parentWorkspaceId,
                    roleCode, workspaceOrder, createdAt, updatedAt, syncedAt,
                    isDeleted, version, provenance, sourceContextId
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, ?, ?, NULL)
                """.trimIndent(),
                arrayOf<Any?>(
                    definition.id,
                    legacy?.name ?: definition.defaultName,
                    legacy?.description,
                    parentId,
                    legacy?.roleCode,
                    workspaceOrder,
                    createdAt,
                    updatedAt,
                    (legacy?.version ?: 1L).coerceAtLeast(1L),
                    WorkspaceProvenance.CANONICAL_ONLY.name,
                ),
            )
        }
    }

    private fun convergeMainBeaconTargets(
        db: SupportSQLiteDatabase,
        now: Long,
    ) {
        val beacons = readBeacons(db)
        val groups = readGroups(db)
        if (beacons.isEmpty() && groups.isEmpty()) return

        val projections =
            buildList {
                addAll(
                    beacons.map { beacon ->
                        mainBeaconEffectiveOrientation(
                            resolver = LegacySubjectUuid,
                            id = beacon.id,
                            title = beacon.title,
                            description = beacon.description,
                            createdAt = beacon.createdAt,
                            updatedAt = beacon.updatedAt,
                        )
                    },
                )
                addAll(groups.map { it.toEffectiveOrientation(LegacySubjectUuid) })
            }
        var subjects = readSubjects(db)
        var mappings = readMappings(db)
        val bootstrap =
            planBootstrap(
                projections = projections,
                existingMappings = mappings,
                existingSubjectIds = subjects.mapTo(hashSetOf()) { it.id },
                gson = Gson(),
            )
        require(bootstrap.issues.isEmpty()) {
            "Migration prerequisite bootstrap issues: ${bootstrap.issues.map { it.code }}"
        }
        bootstrap.rows.forEach { rows -> persistCanonicalRows(db, rows) }

        subjects = readSubjects(db)
        mappings = readMappings(db)
        val orientations = readOrientations(db)
        val relations = readRelations(db)
        val cutover =
            planMainBeaconCutover(
                projections = projections,
                mappings = mappings,
                subjects = subjects,
                orientations = orientations,
                legacyMembers = readGroupMembers(db),
                existingRelations = relations,
                now = now,
                migrationVersion = CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION,
            )
        require(cutover.issues.isEmpty()) {
            "Migration prerequisite cutover issues: ${cutover.issues.map { it.code }}"
        }
        cutover.mappings.forEach { persistMapping(db, it) }
        cutover.relationChanges.forEach { persistRelation(db, it) }
    }

    private fun persistCanonicalRows(
        db: SupportSQLiteDatabase,
        rows: com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRows,
    ) {
        val subject = rows.subject
        db.execSQL(
            """
            INSERT OR REPLACE INTO managed_subjects(
                id, subjectType, title, description, createdAt, updatedAt,
                syncedAt, isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                subject.id, subject.subjectType, subject.title, subject.description,
                subject.createdAt, subject.updatedAt, subject.syncedAt,
                subject.isDeleted.asInt(), subject.version,
            ),
        )
        val orientation = rows.orientation
        db.execSQL(
            """
            INSERT OR REPLACE INTO orientations(subjectId, kind, lifecycle, lifecycleOrigin)
            VALUES(?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                orientation.subjectId, orientation.kind,
                orientation.lifecycle, orientation.lifecycleOrigin,
            ),
        )
        val revision = rows.revision
        db.execSQL(
            """
            INSERT OR REPLACE INTO orientation_assessment_revisions(
                id, orientationId, effectiveFrom, recordedAt, source, reason,
                assessmentJson, createdAt, updatedAt, syncedAt, isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                revision.id, revision.orientationId, revision.effectiveFrom,
                revision.recordedAt, revision.source, revision.reason,
                revision.assessmentJson, revision.createdAt, revision.updatedAt,
                revision.syncedAt, revision.isDeleted.asInt(), revision.version,
            ),
        )
        val assessment = rows.assessment
        db.execSQL(
            """
            INSERT OR REPLACE INTO orientation_assessments(
                orientationId, revisionId,
                importanceValue, importanceOrigin, impactValue, impactOrigin,
                breadthValue, breadthOrigin, expectedSpanValue, expectedSpanOrigin,
                targetWindowValue, targetWindowOrigin, attentionTierValue, attentionTierOrigin,
                commitmentValue, commitmentOrigin, confidenceValue, confidenceOrigin,
                provenanceJson, createdAt, updatedAt, syncedAt, isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                assessment.orientationId, assessment.revisionId,
                assessment.importanceValue, assessment.importanceOrigin,
                assessment.impactValue, assessment.impactOrigin,
                assessment.breadthValue, assessment.breadthOrigin,
                assessment.expectedSpanValue, assessment.expectedSpanOrigin,
                assessment.targetWindowValue, assessment.targetWindowOrigin,
                assessment.attentionTierValue, assessment.attentionTierOrigin,
                assessment.commitmentValue, assessment.commitmentOrigin,
                assessment.confidenceValue, assessment.confidenceOrigin,
                assessment.provenanceJson, assessment.createdAt, assessment.updatedAt,
                assessment.syncedAt, assessment.isDeleted.asInt(), assessment.version,
            ),
        )
        persistMapping(db, rows.mapping)
    }

    private fun persistMapping(
        db: SupportSQLiteDatabase,
        mapping: LegacySubjectMappingEntity,
    ) {
        db.execSQL(
            """
            INSERT OR REPLACE INTO legacy_subject_mappings(
                id, sourceType, sourceId, subjectId, migrationVersion, state,
                createdAt, updatedAt, syncedAt, isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                mapping.id, mapping.sourceType, mapping.sourceId, mapping.subjectId,
                mapping.migrationVersion, mapping.state, mapping.createdAt,
                mapping.updatedAt, mapping.syncedAt, mapping.isDeleted.asInt(), mapping.version,
            ),
        )
    }

    private fun persistRelation(
        db: SupportSQLiteDatabase,
        relation: OrientationRelationEntity,
    ) {
        db.execSQL(
            """
            INSERT OR REPLACE INTO orientation_relations(
                id, fromOrientationId, toOrientationId, relationType, relationOrder,
                createdAt, updatedAt, syncedAt, isDeleted, version
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                relation.id, relation.fromOrientationId, relation.toOrientationId,
                relation.relationType, relation.relationOrder, relation.createdAt,
                relation.updatedAt, relation.syncedAt, relation.isDeleted.asInt(), relation.version,
            ),
        )
    }

    private fun readContextEvidence(
        db: SupportSQLiteDatabase,
        id: String,
    ): LegacyContextEvidence? =
        db.query(
            """
            SELECT name, description, parentId, role_code, goal_order,
                   createdAt, updatedAt, is_deleted, version
            FROM contexts WHERE id = ? LIMIT 1
            """.trimIndent(),
            arrayOf<Any?>(id),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            LegacyContextEvidence(
                name = cursor.getString(0),
                description = cursor.stringOrNull(1),
                parentId = cursor.stringOrNull(2),
                roleCode = cursor.stringOrNull(3),
                order = cursor.getLong(4),
                createdAt = cursor.getLong(5),
                updatedAt = cursor.longOrNull(6),
                isDeleted = cursor.getInt(7) != 0,
                version = cursor.getLong(8),
            )
        }

    private fun readBeacons(db: SupportSQLiteDatabase): List<MainBeaconMigrationEvidenceRow> =
        db.query(
            """
            SELECT id, title, description, readiness_status, updatedAt, createdAt
            FROM main_beacons
            ORDER BY beacon_order ASC, updatedAt DESC, createdAt DESC, id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    // Preserve the old fail-closed behavior for malformed persisted readiness values.
                    MainBeaconReadinessStatus.valueOf(cursor.getString(3))
                    add(
                        MainBeaconMigrationEvidenceRow(
                            id = cursor.getString(0),
                            title = cursor.getString(1),
                            description = cursor.stringOrNull(2),
                            updatedAt = cursor.getLong(4),
                            createdAt = cursor.getLong(5),
                        ),
                    )
                }
            }
        }

    private fun readGroups(db: SupportSQLiteDatabase): List<MainBeaconGroup> =
        db.query(
            """
            SELECT id, title, description, group_order, updatedAt, createdAt
            FROM main_beacon_groups
            ORDER BY group_order ASC, title COLLATE NOCASE ASC, id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MainBeaconGroup(
                            id = cursor.getString(0),
                            title = cursor.getString(1),
                            description = cursor.stringOrNull(2),
                            order = cursor.getLong(3),
                            updatedAt = cursor.getLong(4),
                            createdAt = cursor.getLong(5),
                        ),
                    )
                }
            }
        }

    private fun readGroupMembers(db: SupportSQLiteDatabase): List<MainBeaconGroupMember> =
        db.query(
            """
            SELECT group_id, beacon_id, member_order
            FROM main_beacon_group_members
            ORDER BY group_id ASC, member_order ASC, beacon_id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MainBeaconGroupMember(
                            groupId = cursor.getString(0),
                            beaconId = cursor.getString(1),
                            order = cursor.getLong(2),
                        ),
                    )
                }
            }
        }

    private fun readSubjects(db: SupportSQLiteDatabase): List<ManagedSubjectEntity> =
        db.query(
            """
            SELECT id, subjectType, title, description, createdAt, updatedAt,
                   syncedAt, isDeleted, version FROM managed_subjects
            ORDER BY id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ManagedSubjectEntity(
                            id = cursor.getString(0), subjectType = cursor.getString(1),
                            title = cursor.getString(2), description = cursor.stringOrNull(3),
                            createdAt = cursor.getLong(4), updatedAt = cursor.getLong(5),
                            syncedAt = cursor.longOrNull(6), isDeleted = cursor.getInt(7) != 0,
                            version = cursor.getLong(8),
                        ),
                    )
                }
            }
        }

    private fun readMappings(db: SupportSQLiteDatabase): List<LegacySubjectMappingEntity> =
        CanonicalHierarchyMigrationEvidenceReader().readLegacyMappings(db)

    private fun readOrientations(db: SupportSQLiteDatabase): List<OrientationEntity> =
        db.query(
            "SELECT subjectId, kind, lifecycle, lifecycleOrigin " +
                "FROM orientations ORDER BY subjectId ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        OrientationEntity(
                            subjectId = cursor.getString(0), kind = cursor.getString(1),
                            lifecycle = cursor.stringOrNull(2), lifecycleOrigin = cursor.getString(3),
                        ),
                    )
                }
            }
        }

    private fun readRelations(db: SupportSQLiteDatabase): List<OrientationRelationEntity> =
        db.query(
            """
            SELECT id, fromOrientationId, toOrientationId, relationType, relationOrder,
                   createdAt, updatedAt, syncedAt, isDeleted, version
            FROM orientation_relations
            ORDER BY relationType ASC, fromOrientationId ASC, relationOrder ASC,
                     toOrientationId ASC, id ASC
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        OrientationRelationEntity(
                            id = cursor.getString(0), fromOrientationId = cursor.getString(1),
                            toOrientationId = cursor.getString(2), relationType = cursor.getString(3),
                            relationOrder = cursor.longOrNull(4), createdAt = cursor.getLong(5),
                            updatedAt = cursor.getLong(6), syncedAt = cursor.longOrNull(7),
                            isDeleted = cursor.getInt(8) != 0, version = cursor.getLong(9),
                        ),
                    )
                }
            }
        }
}

private data class MainBeaconMigrationEvidenceRow(
    val id: String,
    val title: String,
    val description: String?,
    val updatedAt: Long,
    val createdAt: Long,
)

private data class ExistingWorkspace(
    val provenance: String,
    val sourceContextId: String?,
    val isDeleted: Boolean,
)

private data class LegacyContextEvidence(
    val name: String,
    val description: String?,
    val parentId: String?,
    val roleCode: String?,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

private fun Boolean.asInt(): Int = if (this) 1 else 0

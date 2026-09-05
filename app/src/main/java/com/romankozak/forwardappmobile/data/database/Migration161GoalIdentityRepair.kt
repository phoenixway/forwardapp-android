package com.romankozak.forwardappmobile.data.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.orientation.toCanonicalRows
import com.romankozak.forwardappmobile.data.orientation.toEffectiveOrientation

/**
 * Repairs only canonical Goal identities that are required by the frozen
 * schema-161 BACKLOG evidence.
 *
 * Historical Orientation bootstrap materialized deterministic Goal shadows.
 * Some later legacy Goal write paths could create a Goal/ListItem pair after
 * that bootstrap without creating the canonical graph. Schema 162 cannot own
 * a GOAL placement without a durable Orientation identity, so the cutover
 * reconstructs the missing bootstrap projection from the legacy Goal itself
 * and then promotes the mapping to CUT_OVER.
 *
 * Deleted Goal lifecycle is preserved. A deleted Goal therefore receives or
 * retains a deleted canonical graph/mapping; only the durable identity state
 * transitions to CUT_OVER.
 */
internal fun repairRequiredGoalIdentities161(
    db: SupportSQLiteDatabase,
    now: Long,
    requiredGoalIds: Set<String>,
    diagnostics: MutableList<String>,
) {
    if (requiredGoalIds.isEmpty()) return

    val mappingsBySource = mutableMapOf<String, GoalMapping161>()
    val mappingsBySubject = mutableMapOf<String, GoalMapping161>()

    db.query(
        """
        SELECT id, sourceId, subjectId
        FROM legacy_subject_mappings
        WHERE sourceType = 'GOAL'
        """.trimIndent(),
    ).use { cursor ->
        val idColumn = cursor.column161("id")
        val sourceColumn = cursor.column161("sourceId")
        val subjectColumn = cursor.column161("subjectId")

        while (cursor.moveToNext()) {
            val mapping =
                GoalMapping161(
                    id = cursor.getString(idColumn),
                    sourceId = cursor.getString(sourceColumn),
                    subjectId = cursor.getString(subjectColumn),
                )
            mappingsBySource[mapping.sourceId] = mapping
            mappingsBySubject[mapping.subjectId] = mapping
        }
    }

    val existingSubjectIds = mutableSetOf<String>()
    db.query("SELECT id FROM managed_subjects").use { cursor ->
        val idColumn = cursor.column161("id")
        while (cursor.moveToNext()) {
            existingSubjectIds += cursor.getString(idColumn)
        }
    }

    val physicalGoalIds = mutableSetOf<String>()
    val gson = Gson()

    db.query(
        """
        SELECT
            id,
            text,
            description,
            completed,
            goal_status,
            createdAt,
            updatedAt,
            synced_at,
            is_deleted,
            version,
            valueImportance,
            valueImpact,
            scoring_status
        FROM goals
        """.trimIndent(),
    ).use { cursor ->
        val idColumn = cursor.column161("id")
        val textColumn = cursor.column161("text")
        val descriptionColumn = cursor.column161("description")
        val completedColumn = cursor.column161("completed")
        val goalStatusColumn = cursor.column161("goal_status")
        val createdAtColumn = cursor.column161("createdAt")
        val updatedAtColumn = cursor.column161("updatedAt")
        val syncedAtColumn = cursor.column161("synced_at")
        val deletedColumn = cursor.column161("is_deleted")
        val versionColumn = cursor.column161("version")
        val importanceColumn = cursor.column161("valueImportance")
        val impactColumn = cursor.column161("valueImpact")
        val scoringStatusColumn = cursor.column161("scoring_status")

        while (cursor.moveToNext()) {
            val goalId = cursor.getString(idColumn)
            if (goalId !in requiredGoalIds) continue

            physicalGoalIds += goalId
            if (goalId in mappingsBySource) continue

            val goal =
                Goal(
                    id = goalId,
                    text = cursor.getString(textColumn),
                    description = cursor.stringOrNull161(descriptionColumn),
                    completed = cursor.getInt(completedColumn) != 0,
                    goalStatus = cursor.getString(goalStatusColumn),
                    createdAt = cursor.getLong(createdAtColumn),
                    updatedAt = cursor.longOrNull161(updatedAtColumn),
                    syncedAt = cursor.longOrNull161(syncedAtColumn),
                    isDeleted = cursor.getInt(deletedColumn) != 0,
                    version = cursor.getLong(versionColumn),
                    valueImportance = cursor.getFloat(importanceColumn),
                    valueImpact = cursor.getFloat(impactColumn),
                    scoringStatus = cursor.getString(scoringStatusColumn),
                )

            val projection = goal.toEffectiveOrientation(LegacySubjectUuid)
            val expectedSubjectId = projection.subject.id

            if (expectedSubjectId in existingSubjectIds) {
                diagnostics +=
                    "GOAL_UNMAPPED_CANONICAL_SUBJECT: Goal $goalId expects existing subject " +
                        "$expectedSubjectId without its durable GOAL mapping"
                continue
            }

            val foreignMapping = mappingsBySubject[expectedSubjectId]
            if (foreignMapping != null) {
                diagnostics +=
                    "GOAL_SUBJECT_MAPPING_COLLISION: Goal $goalId expects subject " +
                        "$expectedSubjectId already mapped from Goal ${foreignMapping.sourceId}"
                continue
            }

            val rows =
                projection.toCanonicalRows(
                    gson = gson,
                    migrationVersion = GOAL_IDENTITY_CUTOVER_VERSION_161,
                )

            require(rows.subject.id == expectedSubjectId)
            require(rows.mapping.id == expectedSubjectId)
            require(rows.mapping.subjectId == expectedSubjectId)
            require(rows.mapping.sourceId == goalId)

            db.execSQL(
                """
                INSERT INTO managed_subjects (
                    id, subjectType, title, description,
                    createdAt, updatedAt, syncedAt, isDeleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    rows.subject.id,
                    rows.subject.subjectType,
                    rows.subject.title,
                    rows.subject.description,
                    rows.subject.createdAt,
                    rows.subject.updatedAt,
                    rows.subject.syncedAt,
                    if (rows.subject.isDeleted) 1 else 0,
                    rows.subject.version,
                ),
            )

            db.execSQL(
                """
                INSERT INTO orientations (
                    subjectId, kind, lifecycle, lifecycleOrigin
                ) VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    rows.orientation.subjectId,
                    rows.orientation.kind,
                    rows.orientation.lifecycle,
                    rows.orientation.lifecycleOrigin,
                ),
            )

            db.execSQL(
                """
                INSERT INTO orientation_assessment_revisions (
                    id, orientationId, effectiveFrom, recordedAt,
                    source, reason, assessmentJson,
                    createdAt, updatedAt, syncedAt, isDeleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    rows.revision.id,
                    rows.revision.orientationId,
                    rows.revision.effectiveFrom,
                    rows.revision.recordedAt,
                    rows.revision.source,
                    rows.revision.reason,
                    rows.revision.assessmentJson,
                    rows.revision.createdAt,
                    rows.revision.updatedAt,
                    rows.revision.syncedAt,
                    if (rows.revision.isDeleted) 1 else 0,
                    rows.revision.version,
                ),
            )

            db.execSQL(
                """
                INSERT INTO orientation_assessments (
                    orientationId, revisionId,
                    importanceValue, importanceOrigin,
                    impactValue, impactOrigin,
                    breadthValue, breadthOrigin,
                    expectedSpanValue, expectedSpanOrigin,
                    targetWindowValue, targetWindowOrigin,
                    attentionTierValue, attentionTierOrigin,
                    commitmentValue, commitmentOrigin,
                    confidenceValue, confidenceOrigin,
                    provenanceJson,
                    createdAt, updatedAt, syncedAt, isDeleted, version
                ) VALUES (
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?,
                    ?, ?, ?, ?, ?
                )
                """.trimIndent(),
                arrayOf<Any?>(
                    rows.assessment.orientationId,
                    rows.assessment.revisionId,
                    rows.assessment.importanceValue,
                    rows.assessment.importanceOrigin,
                    rows.assessment.impactValue,
                    rows.assessment.impactOrigin,
                    rows.assessment.breadthValue,
                    rows.assessment.breadthOrigin,
                    rows.assessment.expectedSpanValue,
                    rows.assessment.expectedSpanOrigin,
                    rows.assessment.targetWindowValue,
                    rows.assessment.targetWindowOrigin,
                    rows.assessment.attentionTierValue,
                    rows.assessment.attentionTierOrigin,
                    rows.assessment.commitmentValue,
                    rows.assessment.commitmentOrigin,
                    rows.assessment.confidenceValue,
                    rows.assessment.confidenceOrigin,
                    rows.assessment.provenanceJson,
                    rows.assessment.createdAt,
                    rows.assessment.updatedAt,
                    rows.assessment.syncedAt,
                    if (rows.assessment.isDeleted) 1 else 0,
                    rows.assessment.version,
                ),
            )

            db.execSQL(
                """
                INSERT INTO legacy_subject_mappings (
                    id, sourceType, sourceId, subjectId,
                    migrationVersion, state,
                    createdAt, updatedAt, syncedAt, isDeleted, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    rows.mapping.id,
                    rows.mapping.sourceType,
                    rows.mapping.sourceId,
                    rows.mapping.subjectId,
                    rows.mapping.migrationVersion,
                    rows.mapping.state,
                    rows.mapping.createdAt,
                    rows.mapping.updatedAt,
                    rows.mapping.syncedAt,
                    if (rows.mapping.isDeleted) 1 else 0,
                    rows.mapping.version,
                ),
            )

            val materialized =
                GoalMapping161(
                    id = rows.mapping.id,
                    sourceId = rows.mapping.sourceId,
                    subjectId = rows.mapping.subjectId,
                )
            mappingsBySource[goalId] = materialized
            mappingsBySubject[expectedSubjectId] = materialized
            existingSubjectIds += expectedSubjectId
        }
    }

    requiredGoalIds
        .asSequence()
        .filter { it !in physicalGoalIds && it !in mappingsBySource }
        .sorted()
        .forEach { goalId ->
            diagnostics +=
                "GOAL_SOURCE_MISSING: BACKLOG requires Goal $goalId, " +
                    "but no legacy Goal or durable canonical mapping exists"
        }

    val diagnosticsBeforePromotion = diagnostics.size
    val promotions = mutableListOf<GoalPromotion161>()

    db.query(
        """
        SELECT
            m.id AS mapping_id,
            m.sourceId AS source_id,
            m.subjectId AS subject_id,
            m.version AS mapping_version,
            m.updatedAt AS mapping_updated_at,
            m.isDeleted AS mapping_deleted,
            g.is_deleted AS goal_deleted,
            s.subjectType AS subject_type,
            s.isDeleted AS subject_deleted,
            o.kind AS orientation_kind
        FROM legacy_subject_mappings m
        JOIN goals g
          ON g.id = m.sourceId
        LEFT JOIN managed_subjects s
          ON s.id = m.subjectId
        LEFT JOIN orientations o
          ON o.subjectId = m.subjectId
        WHERE m.sourceType = 'GOAL'
          AND m.state = 'MATERIALIZED'
        """.trimIndent(),
    ).use { cursor ->
        val mappingId = cursor.column161("mapping_id")
        val sourceId = cursor.column161("source_id")
        val subjectId = cursor.column161("subject_id")
        val mappingVersion = cursor.column161("mapping_version")
        val mappingUpdatedAt = cursor.column161("mapping_updated_at")
        val mappingDeleted = cursor.column161("mapping_deleted")
        val goalDeleted = cursor.column161("goal_deleted")
        val subjectType = cursor.column161("subject_type")
        val subjectDeleted = cursor.column161("subject_deleted")
        val orientationKind = cursor.column161("orientation_kind")

        while (cursor.moveToNext()) {
            val source = cursor.getString(sourceId)
            if (source !in requiredGoalIds) continue

            val mapping = cursor.getString(mappingId)
            val subject = cursor.getString(subjectId)
            val expectedSubject =
                goalSubjectId161(source)

            val goalDeletedValue = cursor.getInt(goalDeleted) != 0
            val mappingDeletedValue = cursor.getInt(mappingDeleted) != 0
            val subjectTypeValue = cursor.stringOrNull161(subjectType)
            val subjectDeletedValue =
                if (cursor.isNull(subjectDeleted)) null else cursor.getInt(subjectDeleted) != 0
            val orientationKindValue = cursor.stringOrNull161(orientationKind)

            when {
                subject != expectedSubject ->
                    diagnostics +=
                        "GOAL_SUBJECT_IDENTITY_MISMATCH: Goal $source maps to " +
                            "$subject instead of $expectedSubject"

                mapping != subject ->
                    diagnostics +=
                        "GOAL_MAPPING_IDENTITY_MISMATCH: Goal mapping $mapping " +
                            "points to subject $subject"

                mappingDeletedValue != goalDeletedValue ->
                    diagnostics +=
                        "GOAL_MAPPING_LIFECYCLE_MISMATCH: Goal $source deleted=$goalDeletedValue " +
                            "but mapping $mapping deleted=$mappingDeletedValue"

                subjectTypeValue != ORIENTATION_SUBJECT_TYPE_161 ->
                    diagnostics +=
                        "GOAL_SUBJECT_INVALID: Goal mapping $mapping does not point to " +
                            "an ORIENTATION subject"

                subjectDeletedValue != goalDeletedValue ->
                    diagnostics +=
                        "GOAL_SUBJECT_LIFECYCLE_MISMATCH: Goal $source deleted=$goalDeletedValue " +
                            "but canonical subject deleted=$subjectDeletedValue"

                orientationKindValue != GOAL_ORIENTATION_KIND_161 ->
                    diagnostics +=
                        "GOAL_ORIENTATION_INVALID: Goal mapping $mapping does not point " +
                            "to GOAL Orientation"

                else ->
                    promotions +=
                        GoalPromotion161(
                            mappingId = mapping,
                            version = cursor.getLong(mappingVersion),
                            updatedAt = cursor.getLong(mappingUpdatedAt),
                        )
            }
        }
    }

    if (diagnostics.size != diagnosticsBeforePromotion) return

    promotions.forEach { promotion ->
        val nextVersion =
            if (promotion.version == Long.MAX_VALUE) Long.MAX_VALUE else promotion.version + 1L
        val nextUpdatedAt =
            if (promotion.updatedAt == Long.MAX_VALUE) {
                Long.MAX_VALUE
            } else {
                maxOf(now, promotion.updatedAt + 1L)
            }

        db.execSQL(
            """
            UPDATE legacy_subject_mappings
            SET
                migrationVersion = ?,
                state = 'CUT_OVER',
                updatedAt = ?,
                syncedAt = NULL,
                version = ?
            WHERE id = ?
              AND sourceType = 'GOAL'
              AND state = 'MATERIALIZED'
            """.trimIndent(),
            arrayOf<Any?>(
                GOAL_IDENTITY_CUTOVER_VERSION_161,
                nextUpdatedAt,
                nextVersion,
                promotion.mappingId,
            ),
        )
    }
}

private fun goalSubjectId161(goalId: String): String =
    Goal(
        id = goalId,
        text = "_",
        completed = false,
        createdAt = 0L,
        updatedAt = 0L,
    ).toEffectiveOrientation(LegacySubjectUuid).subject.id

private fun Cursor.column161(name: String): Int {
    val index = getColumnIndex(name)
    check(index >= 0) { "Required schema-161 column is missing: $name" }
    return index
}

private fun Cursor.stringOrNull161(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun Cursor.longOrNull161(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private data class GoalMapping161(
    val id: String,
    val sourceId: String,
    val subjectId: String,
)

private data class GoalPromotion161(
    val mappingId: String,
    val version: Long,
    val updatedAt: Long,
)

private const val GOAL_IDENTITY_CUTOVER_VERSION_161 = 3
private const val ORIENTATION_SUBJECT_TYPE_161 = "ORIENTATION"
private const val GOAL_ORIENTATION_KIND_161 = "GOAL"

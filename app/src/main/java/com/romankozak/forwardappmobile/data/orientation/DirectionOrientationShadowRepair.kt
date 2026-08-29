package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapIssueEntity
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyDirectionRowKind
import com.romankozak.forwardappmobile.shared.core.domain.workspace.classifyLegacyDirectionRow
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

internal data class DirectionOrientationShadowRepairPlan(
    val projectableRows: List<DirectionItemEntity>,
    val subjectChanges: List<ManagedSubjectEntity>,
    val mappingChanges: List<LegacySubjectMappingEntity>,
    val issues: List<OrientationBootstrapIssueEntity>,
)

/**
 * Keeps the legacy DIRECTION shadow reversible without assigning semantic
 * meaning to linked rows whose creation provenance was never persisted.
 */
internal fun planDirectionOrientationShadowRepair(
    rows: List<DirectionItemEntity>,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    now: Long,
    migrationVersion: Int,
): DirectionOrientationShadowRepairPlan {
    val mappingBySource =
        mappings
            .filter { it.sourceType == LegacyOrientationSourceType.DIRECTION.name }
            .associateBy { it.sourceId }
    val subjectById = subjects.associateBy { it.id }
    val orientationById = orientations.associateBy { it.subjectId }
    val projectableRows = mutableListOf<DirectionItemEntity>()
    val subjectChanges = mutableListOf<ManagedSubjectEntity>()
    val mappingChanges = mutableListOf<LegacySubjectMappingEntity>()
    val issues = mutableListOf<OrientationBootstrapIssueEntity>()

    rows.forEach { row ->
        val projection = row.toEffectiveOrientation(LegacySubjectUuid)
        val mapping = mappingBySource[row.id]
        val subject = mapping?.let { subjectById[it.subjectId] }
        val orientation = mapping?.let { orientationById[it.subjectId] }

        if (row.isDeleted) {
            if (mapping != null && subject != null) {
                subject.tombstoneIfNeeded(now)?.let(subjectChanges::add)
                mapping.deleteIfNeeded(now, migrationVersion)?.let(mappingChanges::add)
            }
            return@forEach
        }

        if (classifyLegacyDirectionRow(row.linkedContextId) == LegacyDirectionRowKind.LINKED_ENTRY_REQUIRES_REVIEW) {
            issues +=
                projection.issue(
                    code = LINKED_REVIEW_ISSUE,
                    detail =
                        "Linked Direction may be a Workspace shortcut or a semantic Direction; " +
                            "canonical shadow was quarantined without changing the legacy row",
                )
            when {
                mapping == null -> Unit
                mapping.subjectId != projection.subject.id ->
                    issues += projection.issue("DIRECTION_MAPPING_IDENTITY_MISMATCH", "Mapping points to ${mapping.subjectId}")
                mapping.state == LegacySubjectMappingState.CUT_OVER.name ->
                    issues += projection.issue("DIRECTION_LINKED_ROW_ALREADY_CUT_OVER", "Cut-over mapping cannot be quarantined")
                subject == null ->
                    issues += projection.issue("DIRECTION_SUBJECT_MISSING", "Mapped canonical subject is missing")
                orientation?.kind != OrientationKind.DIRECTION.name ->
                    issues += projection.issue("DIRECTION_KIND_MISMATCH", "Mapped canonical subject is not DIRECTION")
                else -> {
                    subject.tombstoneIfNeeded(now)?.let(subjectChanges::add)
                    mapping.quarantineIfNeeded(now, migrationVersion)?.let(mappingChanges::add)
                }
            }
            return@forEach
        }

        projectableRows += row
        if (mapping == null) return@forEach
        when {
            mapping.subjectId != projection.subject.id ->
                issues += projection.issue("DIRECTION_MAPPING_IDENTITY_MISMATCH", "Mapping points to ${mapping.subjectId}")
            mapping.state == LegacySubjectMappingState.CUT_OVER.name -> Unit
            subject == null ->
                issues += projection.issue("DIRECTION_SUBJECT_MISSING", "Mapped canonical subject is missing")
            orientation?.kind != OrientationKind.DIRECTION.name ->
                issues += projection.issue("DIRECTION_KIND_MISMATCH", "Mapped canonical subject is not DIRECTION")
            mapping.state == LegacySubjectMappingState.QUARANTINED.name &&
                mapping.migrationVersion != migrationVersion ->
                issues +=
                    projection.issue(
                        "DIRECTION_QUARANTINE_OWNER_MISMATCH",
                        "A quarantine from another migration cannot be restored automatically",
                    )
            else -> {
                subject.restoreProjectionIfNeeded(projection.subject.title, now)?.let(subjectChanges::add)
                mapping.restoreIfNeeded(now, migrationVersion)?.let(mappingChanges::add)
            }
        }
    }

    return DirectionOrientationShadowRepairPlan(
        projectableRows = projectableRows,
        subjectChanges = subjectChanges.distinctBy { it.id },
        mappingChanges = mappingChanges.distinctBy { it.id },
        issues = issues,
    )
}

private fun ManagedSubjectEntity.tombstoneIfNeeded(now: Long): ManagedSubjectEntity? =
    takeUnless { isDeleted }?.copy(
        updatedAt = now,
        syncedAt = null,
        isDeleted = true,
        version = version + 1L,
    )

private fun ManagedSubjectEntity.restoreProjectionIfNeeded(
    title: String,
    now: Long,
): ManagedSubjectEntity? =
    takeIf { isDeleted || this.title != title }?.copy(
        title = title,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = version + 1L,
    )

private fun LegacySubjectMappingEntity.quarantineIfNeeded(
    now: Long,
    migrationVersion: Int,
): LegacySubjectMappingEntity? =
    takeIf {
        state != LegacySubjectMappingState.QUARANTINED.name ||
            this.migrationVersion != migrationVersion ||
            isDeleted
    }?.copy(
        migrationVersion = migrationVersion,
        state = LegacySubjectMappingState.QUARANTINED.name,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = version + 1L,
    )

private fun LegacySubjectMappingEntity.restoreIfNeeded(
    now: Long,
    migrationVersion: Int,
): LegacySubjectMappingEntity? =
    takeIf { state == LegacySubjectMappingState.QUARANTINED.name || isDeleted }?.copy(
        migrationVersion = migrationVersion,
        state = LegacySubjectMappingState.MATERIALIZED.name,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = version + 1L,
    )

private fun LegacySubjectMappingEntity.deleteIfNeeded(
    now: Long,
    migrationVersion: Int,
): LegacySubjectMappingEntity? =
    takeUnless { isDeleted }?.copy(
        migrationVersion = migrationVersion,
        updatedAt = now,
        syncedAt = null,
        isDeleted = true,
        version = version + 1L,
    )

internal const val DIRECTION_SHADOW_REPAIR_VERSION: Int = 3
internal const val LINKED_REVIEW_ISSUE: String = "DIRECTION_LINKED_ROW_REQUIRES_REVIEW"

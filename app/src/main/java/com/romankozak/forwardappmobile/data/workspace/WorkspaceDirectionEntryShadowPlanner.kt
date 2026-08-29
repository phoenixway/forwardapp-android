package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.domain.workspace.LegacyDirectionRowKind
import com.romankozak.forwardappmobile.shared.core.domain.workspace.classifyLegacyDirectionRow
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

internal data class WorkspaceDirectionEntryShadowIssue(
    val sourceDirectionItemId: String,
    val code: String,
    val detail: String,
)

internal data class WorkspaceDirectionEntryShadowPlan(
    val changes: List<WorkspaceDirectionEntryEntity>,
    val issues: List<WorkspaceDirectionEntryShadowIssue>,
)

/**
 * Plans the Context-backed DIRECTION compatibility shadow from a complete
 * legacy direction_items collection.
 *
 * Legacy rows remain authoritative. The planner never changes them and never
 * mutates CANONICAL_ONLY Direction entries.
 */
internal fun planWorkspaceDirectionEntryShadow(
    rows: List<DirectionItemEntity>,
    workspaces: List<WorkspaceEntity>,
    capabilities: List<WorkspaceCapabilityInstanceEntity>,
    mappings: List<LegacySubjectMappingEntity>,
    subjects: List<ManagedSubjectEntity>,
    orientations: List<OrientationEntity>,
    existingEntries: List<WorkspaceDirectionEntryEntity>,
    now: Long,
): WorkspaceDirectionEntryShadowPlan {
    val issues = mutableListOf<WorkspaceDirectionEntryShadowIssue>()
    val changes = mutableListOf<WorkspaceDirectionEntryEntity>()

    val liveContextBackedWorkspaces =
        workspaces
            .filter {
                !it.isDeleted &&
                    it.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                    !it.sourceContextId.isNullOrBlank()
            }
            .groupBy { it.sourceContextId!! }

    val directionCapabilities =
        capabilities
            .filter {
                it.capabilityType == WorkspaceCapabilityType.DIRECTION.name &&
                    it.instanceKey == DEFAULT_DIRECTION_INSTANCE_KEY
            }
            .groupBy { it.workspaceId }

    val directionMappings =
        mappings
            .filter { it.sourceType == LegacyOrientationSourceType.DIRECTION.name }
            .groupBy { it.sourceId }

    val subjectById = subjects.associateBy { it.id }
    val orientationById = orientations.associateBy { it.subjectId }
    val existingById = existingEntries.associateBy { it.id }
    val legacyEntryById =
        existingEntries
            .filter {
                it.provenance ==
                    WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name
            }
            .associateBy { it.id }

    val sourceIds = rows.mapTo(hashSetOf()) { it.id }

    rows.forEach { row ->
        val idOwner = existingById[row.id]
        if (
            idOwner != null &&
            idOwner.provenance !=
                WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name
        ) {
            issues += row.issue(
                "DIRECTION_ENTRY_ID_COLLISION",
                "Entry id ${row.id} is already owned by another provenance or source",
            )
            return@forEach
        }

        val existing = legacyEntryById[row.id]

        if (row.isDeleted) {
            if (existing != null && !existing.isDeleted) {
                changes += existing.copy(
                    updatedAt = row.updatedAt ?: now,
                    syncedAt = null,
                    isDeleted = true,
                    version = existing.version + 1L,
                )
            }
            return@forEach
        }

        val owner =
            resolveContextBackedWorkspace(
                row = row,
                contextId = row.contextId,
                role = "OWNER",
                workspacesByContext = liveContextBackedWorkspaces,
                issues = issues,
            ) ?: run {
                tombstoneUnavailableLegacyShadow(existing, row, now, changes)
                return@forEach
            }

        val ownerCapabilities = directionCapabilities[owner.id].orEmpty()
        if (ownerCapabilities.size != 1) {
            issues += row.issue(
                "DIRECTION_ENTRY_CAPABILITY_UNRESOLVED",
                "Expected one stable default DIRECTION capability for Workspace ${owner.id}; " +
                    "found ${ownerCapabilities.size}",
            )
            tombstoneUnavailableLegacyShadow(existing, row, now, changes)
            return@forEach
        }
        val capability = ownerCapabilities.single()

        val orientationId: String?
        val targetWorkspaceId: String?
        val labelOverride: String?

        when (classifyLegacyDirectionRow(row.linkedContextId)) {
            LegacyDirectionRowKind.SEMANTIC_DIRECTION -> {
                val sourceMappings = directionMappings[row.id].orEmpty()
                if (sourceMappings.size != 1) {
                    issues += row.issue(
                        "DIRECTION_ENTRY_ORIENTATION_UNRESOLVED",
                        "Expected one durable DIRECTION mapping; found ${sourceMappings.size}",
                    )
                    tombstoneUnavailableLegacyShadow(existing, row, now, changes)
                    return@forEach
                }

                val mapping = sourceMappings.single()
                val subject = subjectById[mapping.subjectId]
                val orientation = orientationById[mapping.subjectId]

                if (
                    mapping.isDeleted ||
                    mapping.state == LegacySubjectMappingState.QUARANTINED.name ||
                    subject == null ||
                    subject.isDeleted ||
                    subject.subjectType != ManagedSubjectType.ORIENTATION.name ||
                    orientation?.kind != OrientationKind.DIRECTION.name
                ) {
                    issues += row.issue(
                        "DIRECTION_ENTRY_ORIENTATION_UNRESOLVED",
                        "Mapping does not resolve to a live non-quarantined DIRECTION Orientation",
                    )
                    tombstoneUnavailableLegacyShadow(existing, row, now, changes)
                    return@forEach
                }

                orientationId = mapping.subjectId
                targetWorkspaceId = null
                labelOverride = null
            }

            LegacyDirectionRowKind.LINKED_ENTRY_REQUIRES_REVIEW -> {
                val linkedContextId = requireNotNull(row.linkedContextId).trim()
                val target =
                    resolveContextBackedWorkspace(
                        row = row,
                        contextId = linkedContextId,
                        role = "TARGET",
                        workspacesByContext = liveContextBackedWorkspaces,
                        issues = issues,
                    ) ?: run {
                        tombstoneUnavailableLegacyShadow(existing, row, now, changes)
                        return@forEach
                    }

                // Do not infer semantic intent from a linked legacy row.
                orientationId = null
                targetWorkspaceId = target.id
                labelOverride = row.text
            }
        }

        val desired =
            WorkspaceDirectionEntryEntity(
                id = row.id,
                workspaceId = owner.id,
                capabilityInstanceId = capability.id,
                orientationId = orientationId,
                targetWorkspaceId = targetWorkspaceId,
                labelOverride = labelOverride,
                entryOrder = row.itemOrder.toLong(),
                provenance = WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
                createdAt = existing?.createdAt ?: row.updatedAt ?: 0L,
                updatedAt = row.updatedAt ?: now,
                syncedAt = null,
                isDeleted = false,
                version = existing?.version ?: 1L,
            )

        when {
            existing == null -> changes += desired
            existing.sameShadowProjection(desired) -> Unit
            else ->
                changes += desired.copy(
                    createdAt = existing.createdAt,
                    version = existing.version + 1L,
                )
        }
    }

    legacyEntryById.forEach { (sourceId, existing) ->
        if (sourceId in sourceIds) return@forEach
        if (!existing.isDeleted) {
            changes += existing.copy(
                updatedAt = now,
                syncedAt = null,
                isDeleted = true,
                version = existing.version + 1L,
            )
        }
    }

    return WorkspaceDirectionEntryShadowPlan(
        changes = changes.distinctBy { it.id },
        issues =
            issues.distinctBy {
                Triple(it.sourceDirectionItemId, it.code, it.detail)
            },
    )
}

private fun tombstoneUnavailableLegacyShadow(
    existing: WorkspaceDirectionEntryEntity?,
    row: DirectionItemEntity,
    now: Long,
    changes: MutableList<WorkspaceDirectionEntryEntity>,
) {
    if (existing == null || existing.isDeleted) return
    changes +=
        existing.copy(
            updatedAt = row.updatedAt ?: now,
            syncedAt = null,
            isDeleted = true,
            version = existing.version + 1L,
        )
}

private fun resolveContextBackedWorkspace(
    row: DirectionItemEntity,
    contextId: String,
    role: String,
    workspacesByContext: Map<String, List<WorkspaceEntity>>,
    issues: MutableList<WorkspaceDirectionEntryShadowIssue>,
): WorkspaceEntity? {
    val candidates =
        workspacesByContext[contextId].orEmpty().filter {
            it.id == contextId &&
                it.sourceContextId == contextId
        }
    if (candidates.size == 1) return candidates.single()

    issues += row.issue(
        "DIRECTION_ENTRY_${role}_WORKSPACE_UNRESOLVED",
        "Expected one proven live Context-backed Workspace for Context $contextId; " +
            "found ${candidates.size}",
    )
    return null
}

private fun DirectionItemEntity.issue(
    code: String,
    detail: String,
) = WorkspaceDirectionEntryShadowIssue(
    sourceDirectionItemId = id,
    code = code,
    detail = detail,
)

private fun WorkspaceDirectionEntryEntity.sameShadowProjection(
    other: WorkspaceDirectionEntryEntity,
): Boolean =
    workspaceId == other.workspaceId &&
        capabilityInstanceId == other.capabilityInstanceId &&
        orientationId == other.orientationId &&
        targetWorkspaceId == other.targetWorkspaceId &&
        labelOverride == other.labelOverride &&
        entryOrder == other.entryOrder &&
        provenance == other.provenance &&
        isDeleted == other.isDeleted

private const val DEFAULT_DIRECTION_INSTANCE_KEY = "default"

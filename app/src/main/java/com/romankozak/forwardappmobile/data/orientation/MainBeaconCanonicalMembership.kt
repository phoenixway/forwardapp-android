package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import java.util.UUID

internal fun planCanonicalMainBeaconMembershipChanges(
    legacyMembers: List<MainBeaconGroupMember>,
    mappings: List<LegacySubjectMappingEntity>,
    existingRelations: List<OrientationRelationEntity>,
    now: Long,
): List<OrientationRelationEntity> {
    val activeMappings =
        mappings.filter {
            !it.isDeleted && it.state == LegacySubjectMappingState.CUT_OVER.name
        }
    val beaconSubjects =
        activeMappings
            .filter { it.sourceType == LegacyOrientationSourceType.MAIN_BEACON.name }
            .associate { it.sourceId to it.subjectId }
    val groupSubjects =
        activeMappings
            .filter { it.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name }
            .associate { it.sourceId to it.subjectId }
    val beaconSourceIds = beaconSubjects.entries.associate { (sourceId, subjectId) -> subjectId to sourceId }
    val groupSourceIds = groupSubjects.entries.associate { (sourceId, subjectId) -> subjectId to sourceId }

    val relevantRelations =
        existingRelations.filter { relation ->
            relation.relationType == OrientationRelationType.PART_OF.name &&
                relation.fromOrientationId in beaconSourceIds &&
                relation.toOrientationId in groupSourceIds
        }
    val existingByEdge = relevantRelations.associateBy { it.fromOrientationId to it.toOrientationId }
    val desiredByEdge =
        legacyMembers.associate { member ->
            val beaconSubjectId = requireNotNull(beaconSubjects[member.beaconId]) {
                "Main Beacon ${member.beaconId} has no CUT_OVER canonical mapping"
            }
            val groupSubjectId = requireNotNull(groupSubjects[member.groupId]) {
                "Main Beacon Group ${member.groupId} has no CUT_OVER canonical mapping"
            }
            (beaconSubjectId to groupSubjectId) to member.order
        }

    val changed = mutableListOf<OrientationRelationEntity>()
    desiredByEdge.forEach { (edge, order) ->
        val existing = existingByEdge[edge]
        when {
            existing == null ->
                changed +=
                    OrientationRelationEntity(
                        id = mainBeaconMembershipRelationId(edge.first, edge.second),
                        fromOrientationId = edge.first,
                        toOrientationId = edge.second,
                        relationType = OrientationRelationType.PART_OF.name,
                        relationOrder = order,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
            existing.isDeleted || existing.relationOrder != order ->
                changed +=
                    existing.copy(
                        relationOrder = order,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = existing.version + 1L,
                    )
        }
    }
    relevantRelations
        .filter { !it.isDeleted && (it.fromOrientationId to it.toOrientationId) !in desiredByEdge }
        .forEach { existing ->
            changed +=
                existing.copy(
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = true,
                    version = existing.version + 1L,
                )
        }
    return changed
}

internal fun projectCanonicalMainBeaconMemberships(
    mappings: List<LegacySubjectMappingEntity>,
    relations: List<OrientationRelationEntity>,
): List<MainBeaconGroupMember> {
    val activeMappings =
        mappings.filter {
            !it.isDeleted && it.state == LegacySubjectMappingState.CUT_OVER.name
        }
    val beaconSourceIds =
        activeMappings
            .filter { it.sourceType == LegacyOrientationSourceType.MAIN_BEACON.name }
            .associate { it.subjectId to it.sourceId }
    val groupSourceIds =
        activeMappings
            .filter { it.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name }
            .associate { it.subjectId to it.sourceId }

    return relations
        .asSequence()
        .filter { !it.isDeleted && it.relationType == OrientationRelationType.PART_OF.name }
        .mapNotNull { relation ->
            val beaconId = beaconSourceIds[relation.fromOrientationId] ?: return@mapNotNull null
            val groupId = groupSourceIds[relation.toOrientationId] ?: return@mapNotNull null
            MainBeaconGroupMember(groupId = groupId, beaconId = beaconId, order = relation.relationOrder ?: 0L)
        }.sortedWith(compareBy<MainBeaconGroupMember> { it.groupId }.thenBy { it.order }.thenBy { it.beaconId })
        .toList()
}

private fun mainBeaconMembershipRelationId(
    beaconSubjectId: String,
    groupSubjectId: String,
): String =
    LegacySubjectUuid.uuidV5(
        UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
        "main-beacon-membership:$beaconSubjectId:$groupSubjectId",
    ).toString()

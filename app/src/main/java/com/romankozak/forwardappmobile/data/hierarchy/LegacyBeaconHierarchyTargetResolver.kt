package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * H4.0a compatibility resolver from a legacy Main Beacon id to the canonical
 * MANAGED_SUBJECT hierarchy target.
 *
 * This is intentionally one-way and fail-closed. It does not create mappings,
 * manufacture subjects, fall back to Context identity, or transfer hierarchy
 * authority from CURRENT V1.
 */
@Singleton
class LegacyBeaconHierarchyTargetResolver
    @Inject
    constructor(
        private val orientationDao: OrientationDao,
    ) {
        suspend fun resolve(legacyBeaconId: String): HierarchyTargetRef? {
            if (legacyBeaconId.isBlank()) return null

            val mapping =
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.MAIN_BEACON.name,
                    legacyBeaconId,
                ) ?: return null

            val subject =
                orientationDao.getManagedSubject(mapping.subjectId)
                    ?: return null

            return resolveLegacyBeaconHierarchyTarget(
                legacyBeaconId = legacyBeaconId,
                mapping = mapping,
                subject = subject,
            )
        }
    }

internal fun resolveLegacyBeaconHierarchyTarget(
    legacyBeaconId: String,
    mapping: LegacySubjectMappingEntity?,
    subject: ManagedSubjectEntity?,
): HierarchyTargetRef? {
    if (legacyBeaconId.isBlank()) return null
    if (mapping == null || subject == null) return null

    if (
        mapping.sourceType != LegacyOrientationSourceType.MAIN_BEACON.name ||
        mapping.sourceId != legacyBeaconId ||
        mapping.state != LegacySubjectMappingState.CUT_OVER.name ||
        mapping.isDeleted ||
        subject.id != mapping.subjectId ||
        subject.isDeleted
    ) {
        return null
    }

    return HierarchyTargetRef(
        type = HierarchyTargetType.MANAGED_SUBJECT,
        id = subject.id,
    )
}

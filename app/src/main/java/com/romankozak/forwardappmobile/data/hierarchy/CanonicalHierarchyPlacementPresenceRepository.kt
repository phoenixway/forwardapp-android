package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-only presence of canonical hierarchy targets.
 *
 * Both PRIMARY and LINK are placement occurrences.
 * Validated hierarchy topology guarantees live ancestor closure.
 */
@Singleton
class CanonicalHierarchyPlacementPresenceRepository
    @Inject
    constructor(
        private val dao: HierarchyPlacementDao,
    ) {
        fun observeVisiblePlacedTargets(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): Flow<Set<HierarchyTargetRef>> {
            require(hierarchyId == HierarchyId.GENERAL) {
                "Canonical hierarchy presence currently supports only GENERAL"
            }

            return dao.observeVisiblePlacedTargets(hierarchyId.value)
                .map { rows ->
                    rows.mapNotNull { row ->
                        val type =
                            when (row.targetType) {
                                HierarchyTargetType.MANAGED_SUBJECT.name ->
                                    HierarchyTargetType.MANAGED_SUBJECT

                                HierarchyTargetType.WORKSPACE.name ->
                                    HierarchyTargetType.WORKSPACE

                                else -> null
                            }

                        type?.let { HierarchyTargetRef(it, row.targetId) }
                    }.toSet()
                }
        }
    }

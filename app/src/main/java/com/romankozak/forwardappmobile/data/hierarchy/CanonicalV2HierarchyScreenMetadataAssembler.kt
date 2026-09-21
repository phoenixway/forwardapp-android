package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import javax.inject.Inject

/**
 * Display/operational metadata for the V2 hierarchy screen.
 *
 * This assembler is deliberately topology-blind to every legacy structural
 * source. Group membership/count derives only from the already-composed V2
 * presentation. Beacon owner links are operational badges only.
 */
class CanonicalV2HierarchyScreenMetadataAssembler
    @Inject
    constructor() {
        fun assemble(
            read: CanonicalV2ProductionHierarchyRead,
            workspacePresentations: Collection<HierarchyContextPresentationNode>,
            beacons: Collection<CanonicalV2HierarchyScreenOperationalBeaconMetadata>,
        ): CanonicalV2HierarchyScreenMetadata {
            val workspacePresentationsByTargetId =
                workspacePresentations.associateBy { it.id }
            require(
                workspacePresentationsByTargetId.size == workspacePresentations.size,
            ) {
                "V2 hierarchy screen metadata contains duplicate Workspace presentations"
            }

            val beaconsByPresentationId = beacons.associateBy { it.presentationId }
            require(beaconsByPresentationId.size == beacons.size) {
                "V2 hierarchy screen metadata contains duplicate Beacon presentation ids"
            }

            val linkedBeaconIdsByWorkspaceTargetId =
                buildMap<String, Set<String>> {
                    val mutable = linkedMapOf<String, MutableSet<String>>()
                    beacons.forEach { beacon ->
                        beacon.relatedOwnerIds.forEach { ownerId ->
                            if (ownerId in workspacePresentationsByTargetId) {
                                mutable.getOrPut(ownerId) { linkedSetOf() } +=
                                    beacon.presentationId
                            }
                        }
                    }
                    mutable.forEach { (ownerId, beaconIds) ->
                        put(ownerId, beaconIds.toSet())
                    }
                }

            val groupsBySyntheticScopeId =
                buildMap<String, CanonicalV2HierarchyScreenGroupMetadata> {
                    var activeGroupScopeId: String? = null
                    var activeGroupBeaconCount = 0

                    fun flushActiveGroup() {
                        val scopeId = activeGroupScopeId ?: return
                        put(
                            scopeId,
                            CanonicalV2HierarchyScreenGroupMetadata(
                                beaconCount = activeGroupBeaconCount,
                            ),
                        )
                        activeGroupScopeId = null
                        activeGroupBeaconCount = 0
                    }

                    read.presentation.entries.forEach { entry ->
                        when (entry) {
                            is CanonicalV2PresentedHierarchyEntry.SyntheticScope -> {
                                flushActiveGroup()
                                if (entry.kind == CanonicalV2SyntheticScopeKind.GROUP) {
                                    activeGroupScopeId = entry.id
                                }
                            }

                            is CanonicalV2PresentedHierarchyEntry.Occurrence -> {
                                if (
                                    activeGroupScopeId != null &&
                                    entry.target.type == HierarchyTargetType.MANAGED_SUBJECT
                                ) {
                                    activeGroupBeaconCount++
                                }
                            }
                        }
                    }
                    flushActiveGroup()
                }

            return CanonicalV2HierarchyScreenMetadata(
                workspacePresentationsByTargetId =
                    workspacePresentationsByTargetId,
                linkedBeaconIdsByWorkspaceTargetId =
                    linkedBeaconIdsByWorkspaceTargetId,
                beaconsByPresentationId =
                    beaconsByPresentationId.mapValues { (_, beacon) ->
                        CanonicalV2HierarchyScreenBeaconMetadata(
                            readinessStatus = beacon.readinessStatus,
                            relatedOwnerCount =
                                beacon.relatedOwnerIds.count {
                                    it in workspacePresentationsByTargetId
                                },
                        )
                    },
                groupsBySyntheticScopeId = groupsBySyntheticScopeId,
            )
        }
    }

data class CanonicalV2HierarchyScreenOperationalBeaconMetadata(
    val presentationId: String,
    val readinessStatus: MainBeaconReadinessStatus,
    val relatedOwnerIds: List<String>,
)

package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.orientation.buildProjection
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import javax.inject.Inject
import javax.inject.Singleton

class CanonicalV1HierarchySnapshotCaptureException(
    message: String,
) : IllegalStateException(message)

/**
 * Transactional read-only H2 ingress from classified CURRENT V1 authority.
 *
 * This class does not write V2, cut over readers, or become hierarchy
 * authority. It freezes one internally consistent V1 view for the pure H2
 * occurrence builder.
 */
@Singleton
class CanonicalV1HierarchySnapshotReader
    @Inject
    constructor(
        private val database: AppDatabase,
        private val presentationProjector: SystemWorkspacePresentationContextProjector,
        private val builder: CanonicalV1HierarchySnapshotBuilder,
    ) {
        suspend fun capture(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): CanonicalV1HierarchySnapshot =
            database.withTransaction {
                captureInCurrentTransaction(hierarchyId)
            }

        internal suspend fun captureInCurrentTransaction(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): CanonicalV1HierarchySnapshot {
            val contextDao = database.contextDao()
            val activeContexts =
                contextDao.getAllActiveOrdered()
                    .asSequence()
                    .map { context ->
                        val normalized =
                            context.parentId
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty() &&
                                        !it.equals("null", ignoreCase = true)
                                }
                        if (normalized == context.parentId) {
                            context
                        } else {
                            context.copy(parentId = normalized)
                        }
                    }
                    .toList()

            val presentations =
                presentationProjector.projectPresentationUniverse(activeContexts)
            requireDistinct(
                label = "V1 hierarchy presentation",
                ids = presentations.map { it.id },
            )

            val liveWorkspaces =
                database.workspaceDao()
                    .getAll()
                    .filterNot { it.isDeleted }
                    .associateBy { it.id }

            val workspaceInputs =
                presentations.mapIndexed { sourceOrdinal, presentation ->
                    val workspace =
                        liveWorkspaces[presentation.id]
                            ?: throw CanonicalV1HierarchySnapshotCaptureException(
                                "Visible V1 presentation ${presentation.id} has no live same-id Workspace target",
                            )

                    CanonicalV1WorkspaceSnapshotInput(
                        id = workspace.id,
                        name = presentation.name,
                        parentWorkspaceId = workspace.parentWorkspaceId,
                        order = workspace.workspaceOrder,
                        sourceOrdinal = sourceOrdinal,
                    )
                }

            val orientationDao = database.orientationDao()
            val commonProjection =
                buildProjection(
                    subjects = orientationDao.getAllManagedSubjects(),
                    mappings = orientationDao.getAllLegacyMappings(),
                )

            val mainBeaconDao = database.mainBeaconDao()
            val ownerRowsByBeacon =
                mainBeaconDao.getAllContextCrossRefsSync()
                    .groupBy { it.beaconId }

            ownerRowsByBeacon.forEach { (beaconId, rows) ->
                val duplicateOwner =
                    rows.groupingBy { it.contextId }
                        .eachCount()
                        .entries
                        .firstOrNull { it.value > 1 }
                if (duplicateOwner != null) {
                    throw CanonicalV1HierarchySnapshotCaptureException(
                        "Main Beacon $beaconId has duplicate operational-owner ref ${duplicateOwner.key}",
                    )
                }
            }

            val groupMembersByBeacon =
                mainBeaconDao.getAllGroupMembersSync()
                    .groupBy { it.beaconId }

            val beaconInputs =
                mainBeaconDao.getAllBeaconsSync()
                    .mapIndexed { sourceOrdinal, beacon ->
                        val canonical =
                            commonProjection.beaconsByLegacyId[beacon.id]
                                ?: throw CanonicalV1HierarchySnapshotCaptureException(
                                    "Visible Main Beacon ${beacon.id} has no live CUT_OVER ManagedSubject target",
                                )

                        val ownerRows =
                            ownerRowsByBeacon[beacon.id]
                                .orEmpty()
                                .sortedWith(
                                    compareBy({ it.order }, { it.contextId }),
                                )
                        val groupMembers =
                            groupMembersByBeacon[beacon.id]
                                .orEmpty()
                                .sortedWith(
                                    compareBy({ it.order }, { it.groupId }),
                                )

                        CanonicalV1BeaconSnapshotInput(
                            legacyBeaconId = beacon.id,
                            target =
                                HierarchyTargetRef(
                                    HierarchyTargetType.MANAGED_SUBJECT,
                                    canonical.id,
                                ),
                            title = canonical.title,
                            order = beacon.order,
                            parentBeaconId = beacon.parentBeaconId,
                            relatedOwnerIds = ownerRows.map { it.contextId },
                            groupIds = groupMembers.map { it.groupId },
                            groupOrders =
                                groupMembers.associate {
                                    it.groupId to it.order
                                },
                            sourceOrdinal = sourceOrdinal,
                        )
                    }

            val groupInputs =
                mainBeaconDao.getAllGroupsSync()
                    .mapIndexed { sourceOrdinal, group ->
                        val canonicalGroup =
                            commonProjection.groupsByLegacyId[group.id]
                                ?: throw CanonicalV1HierarchySnapshotCaptureException(
                                    "Visible Main Beacon Group ${group.id} has no live CUT_OVER canonical subject",
                                )

                        CanonicalV1BeaconGroupSnapshotInput(
                            id = group.id,
                            title = canonicalGroup.title,
                            order = group.order,
                            sourceOrdinal = sourceOrdinal,
                            canonicalSubjectId = canonicalGroup.id,
                        )
                    }

            val contextParentLinks =
                database.contextParentLinkDao()
                    .getActiveLinksOrdered()
                    .mapIndexed { sourceOrdinal, link ->
                        CanonicalV1ContextParentLinkSnapshotInput(
                            parentWorkspaceId = link.parentContextId,
                            childWorkspaceId = link.childContextId,
                            order = link.order,
                            sourceOrdinal = sourceOrdinal,
                        )
                    }

            val beaconParentLinks =
                mainBeaconDao.getAllParentLinksSync()
                    .mapIndexed { sourceOrdinal, link ->
                        CanonicalV1BeaconParentLinkSnapshotInput(
                            parentBeaconId = link.parentBeaconId,
                            childBeaconId = link.childBeaconId,
                            order = link.order,
                            sourceOrdinal = sourceOrdinal,
                        )
                    }

            return builder.build(
                input =
                    CanonicalV1HierarchySnapshotInput(
                        workspaces = workspaceInputs,
                        beacons = beaconInputs,
                        groups = groupInputs,
                        contextParentLinks = contextParentLinks,
                        beaconParentLinks = beaconParentLinks,
                    ),
                hierarchyId = hierarchyId,
            )
        }

        private fun requireDistinct(
            label: String,
            ids: List<String>,
        ) {
            val duplicate =
                ids.groupingBy { it }
                    .eachCount()
                    .entries
                    .firstOrNull { it.value > 1 }

            if (duplicate != null) {
                throw CanonicalV1HierarchySnapshotCaptureException(
                    "$label id ${duplicate.key} appears ${duplicate.value} times",
                )
            }
        }
    }

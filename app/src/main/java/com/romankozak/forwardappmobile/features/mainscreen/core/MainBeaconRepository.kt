package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementGroupScopeMutationCoordinator
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconSyncStatus
import com.romankozak.forwardappmobile.data.orientation.MainBeaconOrientationBridge
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainBeaconRepository
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val mainBeaconDao: MainBeaconDao,
        private val orientationBridge: MainBeaconOrientationBridge,
        private val hierarchyPlacementRepository: CanonicalHierarchyPlacementRepository,
        private val groupScopeCoordinator: HierarchyPlacementGroupScopeMutationCoordinator,
    ) {
        companion object {
            val DefaultLevels: List<MainBeaconLevelType> =
                listOf(
                    MainBeaconLevelType.MAIN_BEACON,
                    MainBeaconLevelType.REALIZATION_MODEL_OF_MAIN_BEACON,
                    MainBeaconLevelType.MANDATORY_CORE_OF_MAIN_BEACON,
                    MainBeaconLevelType.STRATEGIC_PROJECTING_OF_MAIN_BEACON,
                    MainBeaconLevelType.LONG_TERM_STRATEGY,
                    MainBeaconLevelType.MEDIUM_TERM_PROGRAM,
                    MainBeaconLevelType.WEEK,
                    MainBeaconLevelType.DAY,
                )

            fun defaultSyncStatus(levelType: MainBeaconLevelType): MainBeaconSyncStatus =
                if (levelType == MainBeaconLevelType.MAIN_BEACON) {
                    MainBeaconSyncStatus.UNSET
                } else {
                    MainBeaconSyncStatus.OUTDATED_BY_PARENT
                }
        }

        fun observeMainBeaconDetails(): Flow<List<MainBeaconWithRelations>> =
            combine(
                mainBeaconDao.observeMainBeaconRelations(),
                orientationBridge.observeCommonProjection(),
            ) { relationRows, commonProjection ->
                relationRows.map { row ->
                    val canonical = commonProjection.beaconsByLegacyId[row.beacon.id]
                    val beacon =
                        canonical?.let { row.beacon.copy(title = it.title, description = it.description) }
                            ?: row.beacon
                    val ensuredStatuses =
                        ensureAllLevelStatuses(
                            beacon.id,
                            row.levelStatuses.sortedBy { it.levelType },
                        )
                    val ownerOrders =
                        buildList {
                            row.contextCrossRefs.forEach { add(it.contextId to it.order) }
                            row.workspaceCrossRefs.forEach { add(it.workspaceId to it.order) }
                        }
                    require(ownerOrders.map { it.first }.distinct().size == ownerOrders.size) {
                        "Main Beacon ${row.beacon.id} has duplicate operational-owner refs"
                    }
                    val relatedOwnerIds =
                        ownerOrders
                            .sortedWith(compareBy<Pair<String, Long>> { it.second }.thenBy { it.first })
                            .map { it.first }
                    val relatedAttachments =
                        row.relatedAttachments.sortedWith(
                            compareByDescending<AttachmentEntity> { it.updatedAt }
                                .thenByDescending { it.createdAt },
                        )
                    val groupIds = row.groupMembers.sortedBy { it.order }.map { it.groupId }
                    val groupOrders = row.groupMembers.associate { it.groupId to it.order }

                    MainBeaconWithRelations(
                        beacon = beacon,
                        relatedOwnerIds = relatedOwnerIds,
                        relatedAttachments = relatedAttachments,
                        levelStatuses = ensuredStatuses,
                        groupIds = groupIds,
                        groupOrders = groupOrders,
                    )
                }
            }

        fun observeGroups(): Flow<List<MainBeaconGroup>> =
            combine(mainBeaconDao.observeGroups(), orientationBridge.observeCommonProjection()) { groups, projection ->
                groups.map { group ->
                    projection.groupsByLegacyId[group.id]
                        ?.let { group.copy(title = it.title, description = it.description) }
                        ?: group
                }
            }

        suspend fun getBeaconById(beaconId: String): MainBeacon? =
            mainBeaconDao.getBeaconById(beaconId)?.let { orientationBridge.project(it) }

        suspend fun getGroupById(groupId: String): MainBeaconGroup? =
            mainBeaconDao.getAllGroupsSync().firstOrNull { it.id == groupId }?.let { orientationBridge.project(it) }

        suspend fun createBeacon(
            beacon: MainBeacon,
            relatedOwnerIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
            parentPlacementId: PlacementId? = null,
            parentBeaconId: String? = null,
        ) {
            val nextOrder = mainBeaconDao.getMaxOrder() + 1L
            upsertBeacon(
                beacon = beacon.copy(order = nextOrder),
                relatedOwnerIds = relatedOwnerIds,
                relatedAttachmentIds = relatedAttachmentIds,
                groupIds = groupIds,
                levelStatuses = levelStatuses,
                exists = false,
                parentPlacementId = parentPlacementId,
                parentBeaconId = parentBeaconId,
            )
        }

        suspend fun updateBeacon(
            beacon: MainBeacon,
            relatedOwnerIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
        ) {
            upsertBeacon(beacon, relatedOwnerIds, relatedAttachmentIds, groupIds, levelStatuses, exists = true)
        }

        suspend fun deleteBeacon(beaconId: String) {
            orientationBridge.ensureCutOver()
            val now = System.currentTimeMillis()
            appDatabase.withTransaction {
                run {
                    val mapping = requireNotNull(
                        appDatabase.orientationDao().getLegacyMapping(
                            LegacyOrientationSourceType.MAIN_BEACON.name,
                            beaconId,
                        ),
                    ) { "V2 Beacon deletion requires a canonical mapping: $beaconId" }
                    require(!mapping.isDeleted && mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                        "V2 Beacon deletion requires an active CUT_OVER mapping: $beaconId"
                    }
                    val subject = requireNotNull(
                        appDatabase.orientationDao().getManagedSubject(mapping.subjectId),
                    ) { "V2 Beacon deletion requires its canonical subject: $beaconId" }
                    require(!subject.isDeleted) { "V2 Beacon target is already deleted: $beaconId" }
                    require(
                        appDatabase.orientationDao().getAllOrientations().any {
                            it.subjectId == subject.id && it.kind == OrientationKind.MAIN_BEACON.name
                        },
                    ) { "V2 Beacon mapping does not resolve to a Main Beacon: $beaconId" }
                    requireNotNull(mainBeaconDao.getBeaconById(beaconId)) {
                        "V2 Beacon deletion requires its legacy representation: $beaconId"
                    }
                }
                // Target lifecycle rejects live children under ANY PRIMARY or LINK
                // appearance. Its H1 and sidecar tombstones share this transaction
                // with canonical orientation and legacy representation retirement.
                orientationBridge.tombstone(LegacyOrientationSourceType.MAIN_BEACON, beaconId, now)
                mainBeaconDao.deleteBeacon(beaconId)
                orientationBridge.syncMembershipProjection(now)
            }
        }

        suspend fun setBeaconExpanded(
            beaconId: String,
            expanded: Boolean,
        ) {
            mainBeaconDao.updateBeaconExpanded(
                beaconId = beaconId,
                isExpanded = expanded,
                updatedAt = System.currentTimeMillis(),
            )
        }

        suspend fun createGroup(
            title: String,
            description: String? = null,
        ) {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) return
            val now = System.currentTimeMillis()
            val nextOrder = mainBeaconDao.getMaxGroupOrder() + 1L
            val group =
                MainBeaconGroup(
                    title = normalizedTitle,
                    description = description?.trim()?.ifBlank { null },
                    order = nextOrder,
                    updatedAt = now,
                    createdAt = now,
                )
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                mainBeaconDao.insertGroup(group)
                orientationBridge.writeCommon(group)
            }
        }

        suspend fun updateGroup(group: MainBeaconGroup) {
            val normalizedTitle = group.title.trim()
            if (normalizedTitle.isBlank()) return
            val updated =
                group.copy(
                    title = normalizedTitle,
                    description = group.description?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis(),
                )
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                run {
                    val mapping = requireNotNull(
                        appDatabase.orientationDao().getLegacyMapping(
                            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                            updated.id,
                        ),
                    ) { "V2 Group edit requires its canonical mapping: ${updated.id}" }
                    require(
                        !mapping.isDeleted &&
                            mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                            appDatabase.orientationDao().getManagedSubject(mapping.subjectId)?.isDeleted == false
                    ) { "V2 Group edit cannot revive a retired canonical Group: ${updated.id}" }
                }
                orientationBridge.writeCommon(updated)
                mainBeaconDao.updateGroup(updated)
            }
        }

        suspend fun deleteGroup(groupId: String) {
            orientationBridge.ensureCutOver()
            val now = System.currentTimeMillis()
            appDatabase.withTransaction {
                run {
                    val orientationDao = appDatabase.orientationDao()
                    val mapping = requireNotNull(
                        orientationDao.getLegacyMapping(
                            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                            groupId,
                        ),
                    ) { "V2 Group deletion requires a canonical mapping: $groupId" }
                    require(!mapping.isDeleted && mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                        "V2 Group deletion requires an active CUT_OVER mapping: $groupId"
                    }
                    require(orientationDao.getManagedSubject(mapping.subjectId)?.isDeleted == false) {
                        "V2 Group deletion requires a live canonical Group: $groupId"
                    }
                    require(orientationDao.getAllOrientations().any {
                        it.subjectId == mapping.subjectId && it.kind == OrientationKind.MAIN_BEACON_GROUP.name
                    }) { "V2 Group mapping does not resolve to a canonical Group: $groupId" }
                    require(mainBeaconDao.getAllGroupsSync().any { it.id == groupId }) {
                        "V2 Group deletion requires its legacy representation: $groupId"
                    }
                    groupScopeCoordinator.reconcileGroupRetirementLeafOnly(mapping.subjectId, now)
                }
                orientationBridge.tombstone(LegacyOrientationSourceType.MAIN_BEACON_GROUP, groupId, now)
                mainBeaconDao.deleteGroup(groupId)
                orientationBridge.syncMembershipProjection(now)
                run {
                    groupScopeCoordinator.validateAuthoritativeState()
                }
            }
        }

        suspend fun addRelatedContexts(
            beaconId: String,
            contextIds: Set<String>,
        ): Int {
            if (contextIds.isEmpty()) return 0

            val existingContextIds =
                mainBeaconDao
                    .getAllContextCrossRefsSync()
                    .asSequence()
                    .filter { it.beaconId == beaconId }
                    .mapTo(mutableSetOf()) { it.contextId }

            val newContextIds = contextIds.filterNot { it in existingContextIds }
            if (newContextIds.isEmpty()) return 0

            var nextOrder = mainBeaconDao.getMaxContextCrossRefOrder(beaconId) + 1L
            mainBeaconDao.insertContextCrossRefs(
                newContextIds.map { contextId ->
                    MainBeaconContextCrossRef(
                        beaconId = beaconId,
                        contextId = contextId,
                        order = nextOrder++,
                    )
                },
            )
            return newContextIds.size
        }

        suspend fun moveRelatedContextsToBeacon(
            beaconId: String,
            contextIds: Set<String>,
        ): Int {
            if (contextIds.isEmpty() || mainBeaconDao.getBeaconById(beaconId) == null) {
                return 0
            }

            return appDatabase.withTransaction {
                mainBeaconDao.deleteContextCrossRefsForContexts(contextIds)
                var nextOrder = mainBeaconDao.getMaxContextCrossRefOrder(beaconId) + 1L
                mainBeaconDao.insertContextCrossRefs(
                    contextIds.map { contextId ->
                        MainBeaconContextCrossRef(
                            beaconId = beaconId,
                            contextId = contextId,
                            order = nextOrder++,
                        )
                    },
                )
                contextIds.size
            }
        }

        suspend fun addRelatedWorkspaces(
            beaconId: String,
            workspaceIds: Set<String>,
        ): Int {
            if (workspaceIds.isEmpty()) return 0

            val existingOwnerIds =
                mainBeaconDao
                    .getAllContextCrossRefsSync()
                    .asSequence()
                    .filter { it.beaconId == beaconId }
                    .mapTo(mutableSetOf()) { it.contextId }

            val newWorkspaceIds = workspaceIds.filterNot { it in existingOwnerIds }
            if (newWorkspaceIds.isEmpty()) return 0

            val workspaces =
                newWorkspaceIds.map { workspaceId ->
                    requireNotNull(mainBeaconDao.getOperationalOwnerWorkspace(workspaceId)) {
                        "Main Beacon operational owner $workspaceId has no Workspace"
                    }
                }
            require(workspaces.all { workspace -> !workspace.isDeleted }) {
                "Main Beacon operational owner must be a live Workspace"
            }

            var nextOrder = mainBeaconDao.getMaxContextCrossRefOrder(beaconId) + 1L
            mainBeaconDao.insertWorkspaceCrossRefs(
                newWorkspaceIds.map { workspaceId ->
                    MainBeaconWorkspaceCrossRef(
                        beaconId = beaconId,
                        workspaceId = workspaceId,
                        order = nextOrder++,
                    )
                },
            )
            return newWorkspaceIds.size
        }

        suspend fun moveRelatedWorkspacesToBeacon(
            beaconId: String,
            workspaceIds: Set<String>,
        ): Int {
            if (workspaceIds.isEmpty() || mainBeaconDao.getBeaconById(beaconId) == null) {
                return 0
            }

            return appDatabase.withTransaction {
                val workspaces =
                    workspaceIds.map { workspaceId ->
                        requireNotNull(mainBeaconDao.getOperationalOwnerWorkspace(workspaceId)) {
                            "Main Beacon operational owner $workspaceId has no Workspace"
                        }
                    }
                require(workspaces.all { workspace -> !workspace.isDeleted }) {
                    "Main Beacon operational owner must be a live Workspace"
                }

                mainBeaconDao.deleteContextCrossRefsForContexts(workspaceIds)

                var nextOrder = mainBeaconDao.getMaxContextCrossRefOrder(beaconId) + 1L
                mainBeaconDao.insertWorkspaceCrossRefs(
                    workspaceIds.map { workspaceId ->
                        MainBeaconWorkspaceCrossRef(
                            beaconId = beaconId,
                            workspaceId = workspaceId,
                            order = nextOrder++,
                        )
                    },
                )
                workspaceIds.size
            }
        }

        suspend fun removeContextsFromAllBeacons(contextIds: Set<String>) {
            if (contextIds.isEmpty()) return
            mainBeaconDao.deleteContextCrossRefsForContexts(contextIds)
        }

        /**
         * Reorders synthetic Group presentation scopes.
         *
         * MainBeaconGroup.order is presentation metadata under V2 authority. It
         * does not own H1 topology, GroupScope provenance, PART_OF membership,
         * Beacon parentage, or parent-link topology.
         */
        suspend fun reorderGroups(groupIdsInOrder: List<String>) {

            appDatabase.withTransaction {
                run {
                    require(groupIdsInOrder.distinct().size == groupIdsInOrder.size) {
                        "V2 Group reorder contains duplicate Group ids"
                    }

                    val orientationDao = appDatabase.orientationDao()
                    val activeGroupMappings =
                        orientationDao.getAllLegacyMappings()
                            .filterNot { it.isDeleted }
                            .filter { it.state == LegacySubjectMappingState.CUT_OVER.name }
                            .filter {
                                it.sourceType ==
                                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name
                            }

                    require(
                        activeGroupMappings.map { it.sourceId }.distinct().size ==
                            activeGroupMappings.size,
                    ) {
                        "V2 Group reorder found duplicate active legacy Group mappings"
                    }
                    require(
                        activeGroupMappings.map { it.subjectId }.distinct().size ==
                            activeGroupMappings.size,
                    ) {
                        "V2 Group reorder found multiple legacy Groups for one canonical Group"
                    }

                    val orientationsBySubject =
                        orientationDao.getAllOrientations()
                            .groupBy { it.subjectId }

                    activeGroupMappings.forEach { mapping ->
                        val subject = requireNotNull(
                            orientationDao.getManagedSubject(mapping.subjectId),
                        ) {
                            "V2 Group reorder mapping ${mapping.sourceId} has no canonical subject"
                        }
                        require(!subject.isDeleted) {
                            "V2 Group reorder mapping ${mapping.sourceId} targets a deleted canonical Group"
                        }
                        require(
                            orientationsBySubject[mapping.subjectId]
                                .orEmpty()
                                .any { it.kind == OrientationKind.MAIN_BEACON_GROUP.name },
                        ) {
                            "V2 Group reorder mapping ${mapping.sourceId} is not a canonical Group"
                        }
                    }

                    val liveCanonicalGroupIds =
                        activeGroupMappings.mapTo(linkedSetOf()) { it.sourceId }

                    val persistedGroupIds =
                        mainBeaconDao.getAllGroupsSync()
                            .mapTo(linkedSetOf()) { it.id }

                    require(persistedGroupIds == liveCanonicalGroupIds) {
                        "V2 Group presentation rows disagree with active canonical Group mappings"
                    }
                    require(groupIdsInOrder.toSet() == liveCanonicalGroupIds) {
                        "V2 Group reorder requires the complete active Group sibling set"
                    }
                }

                groupIdsInOrder.forEachIndexed { index, groupId ->
                    mainBeaconDao.updateGroupOrder(groupId, index.toLong())
                }
            }
        }

        private suspend fun upsertBeacon(
            beacon: MainBeacon,
            relatedOwnerIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
            exists: Boolean,
            parentPlacementId: PlacementId? = null,
            parentBeaconId: String? = null,
        ) {
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                val stored = if (exists) {
                    requireNotNull(mainBeaconDao.getBeaconById(beacon.id)) {
                        "V2 metadata update requires an existing Beacon"
                    }
                } else {
                    null
                }
                if (exists) {
                    require(beacon.order == requireNotNull(stored).order) {
                        "V2 Beacon order changes require an exact occurrence command"
                    }
                    val currentGroups = mainBeaconDao.getAllGroupMembersSync()
                        .filter { it.beaconId == beacon.id }
                        .mapTo(linkedSetOf()) { it.groupId }
                    require(groupIds == currentGroups) {
                        "V2 Beacon Group changes require a fused occurrence command"
                    }
                }
                if (!exists) {
                    val existingGroupIds =
                        mainBeaconDao.getAllGroupsSync().mapTo(hashSetOf()) { it.id }
                    require(groupIds.all { it in existingGroupIds }) {
                        "V2 Beacon creation references a missing legacy Group"
                    }
                    require((parentBeaconId == null) == (parentPlacementId == null)) {
                        "V2 Beacon creation requires matching parent Beacon id and exact occurrence"
                    }
                    if (parentPlacementId != null) {
                        require(groupIds.isEmpty()) {
                            "Nested V2 Beacon creation cannot introduce root Group membership"
                        }
                        val parent = requireNotNull(
                            hierarchyPlacementRepository.getPlacement(parentPlacementId),
                        ) { "V2 Beacon parent occurrence is missing" }
                        require(
                            parent.hierarchyId == HierarchyId.GENERAL &&
                                parent.target.type == HierarchyTargetType.MANAGED_SUBJECT,
                        ) { "V2 Beacon parent must be a GENERAL ManagedSubject occurrence" }
                        val parentMapping = requireNotNull(
                            appDatabase.orientationDao().getLegacyMapping(
                                LegacyOrientationSourceType.MAIN_BEACON.name,
                                requireNotNull(parentBeaconId),
                            ),
                        ) { "V2 Beacon parent has no canonical mapping" }
                        require(
                            !parentMapping.isDeleted &&
                                parentMapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                                parent.target.id == parentMapping.subjectId &&
                                mainBeaconDao.getBeaconById(requireNotNull(parentBeaconId)) != null &&
                                appDatabase.orientationDao()
                                    .getManagedSubject(parentMapping.subjectId)?.isDeleted == false,
                        ) { "V2 Beacon parent target and occurrence disagree" }
                    }
                }
                run {
                    val priorMapping = appDatabase.orientationDao().getLegacyMapping(
                        LegacyOrientationSourceType.MAIN_BEACON.name,
                        beacon.id,
                    )
                    if (priorMapping != null) {
                        require(
                            !priorMapping.isDeleted &&
                                appDatabase.orientationDao()
                                    .getManagedSubject(priorMapping.subjectId)?.isDeleted == false,
                        ) { "V2 Beacon create/edit cannot revive a retired canonical target: ${beacon.id}" }
                    }
                }
                val existingOwnerOrders =
                    mainBeaconDao
                        .getAllContextCrossRefsSync()
                        .filter { it.beaconId == beacon.id }
                        .associate { it.contextId to it.order }
                var nextOwnerOrder = (existingOwnerOrders.values.maxOrNull() ?: -1L) + 1L

                val ownerWorkspaces =
                    relatedOwnerIds.associateWith { ownerId ->
                        requireNotNull(mainBeaconDao.getOperationalOwnerWorkspace(ownerId)) {
                            "Main Beacon operational owner $ownerId has no Workspace"
                        }
                    }
                require(ownerWorkspaces.values.all { workspace -> !workspace.isDeleted }) {
                    "Main Beacon operational owners must be live Workspaces"
                }

                if (exists) {
                    orientationBridge.writeCommon(beacon)
                    mainBeaconDao.updateBeacon(beacon)
                } else {
                    mainBeaconDao.insertBeacon(beacon)
                    orientationBridge.writeCommon(beacon)
                }

                mainBeaconDao.deleteContextCrossRefsForBeacon(beacon.id)
                mainBeaconDao.deleteAttachmentCrossRefsForBeacon(beacon.id)
                if (!exists) {
                    mainBeaconDao.deleteGroupMembersForBeacon(beacon.id)
                }

                if (relatedOwnerIds.isNotEmpty()) {
                    mainBeaconDao.insertWorkspaceCrossRefs(
                        relatedOwnerIds.map { ownerId ->
                            MainBeaconWorkspaceCrossRef(
                                beaconId = beacon.id,
                                workspaceId = ownerId,
                                order = existingOwnerOrders[ownerId] ?: nextOwnerOrder++,
                            )
                        },
                    )
                }

                if (relatedAttachmentIds.isNotEmpty()) {
                    mainBeaconDao.insertAttachmentCrossRefs(
                        relatedAttachmentIds.map { attachmentId ->
                            MainBeaconAttachmentCrossRef(beaconId = beacon.id, attachmentId = attachmentId)
                        },
                    )
                }

                if (!exists) {
                    if (groupIds.isNotEmpty()) {
                        mainBeaconDao.insertGroupMembers(
                            groupIds.mapIndexed { index, groupId ->
                                MainBeaconGroupMember(
                                    groupId = groupId,
                                    beaconId = beacon.id,
                                    order = index.toLong(),
                                )
                            },
                        )
                    }
                    orientationBridge.syncMembershipProjection(beacon.updatedAt)
                }

                if (!exists) {
                    val orientationDao = appDatabase.orientationDao()
                    val mapping = requireNotNull(
                        orientationDao.getLegacyMapping(
                            LegacyOrientationSourceType.MAIN_BEACON.name,
                            beacon.id,
                        ),
                    ) { "New Beacon has no canonical mapping" }
                    require(!mapping.isDeleted && mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                        "New Beacon canonical mapping is not active"
                    }
                    val subject = requireNotNull(orientationDao.getManagedSubject(mapping.subjectId))
                    require(!subject.isDeleted) { "New Beacon canonical subject is deleted" }
                    val target = HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, subject.id)

                    val canonicalGroupIds = groupIds.sorted().map { groupId ->
                        val groupMapping = requireNotNull(
                            orientationDao.getLegacyMapping(
                                LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                                groupId,
                            ),
                        ) { "Beacon Group has no canonical mapping: $groupId" }
                        require(
                            !groupMapping.isDeleted &&
                                groupMapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                                orientationDao.getManagedSubject(groupMapping.subjectId)?.isDeleted == false &&
                                orientationDao.getAllOrientations().any {
                                    it.subjectId == groupMapping.subjectId &&
                                        it.kind == OrientationKind.MAIN_BEACON_GROUP.name
                                },
                        ) { "Beacon Group mapping is not a live canonical Group: $groupId" }
                        groupMapping.subjectId
                    }
                    require(canonicalGroupIds.distinct().size == canonicalGroupIds.size) {
                        "Multiple legacy Groups map to the same canonical Group"
                    }

                    val primaryId =
                        hierarchyPlacementRepository.createPrimaryAppearanceInCurrentTransaction(
                            target = target,
                            parentPlacementId = parentPlacementId,
                            now = beacon.updatedAt,
                        )
                    if (parentPlacementId == null) {
                        groupScopeCoordinator.setRootScope(
                            placementId = primaryId,
                            groupSubjectId = canonicalGroupIds.firstOrNull(),
                            now = beacon.updatedAt,
                        )
                        canonicalGroupIds.drop(1).forEach { groupSubjectId ->
                            val linkId =
                                hierarchyPlacementRepository.createLinkAppearanceInCurrentTransaction(
                                    target = target,
                                    parentPlacementId = null,
                                    now = beacon.updatedAt,
                                )
                            groupScopeCoordinator.setRootScope(
                                placementId = linkId,
                                groupSubjectId = groupSubjectId,
                                now = beacon.updatedAt,
                            )
                        }
                    }
                    groupScopeCoordinator.validateAuthoritativeState()
                }

                mainBeaconDao.insertLevelStatuses(
                    ensureAllLevelStatuses(
                        beaconId = beacon.id,
                        existingStatuses = levelStatuses.map { it.copy(mainBeaconId = beacon.id) },
                    ),
                )
            }
        }

        private suspend fun ensureAllLevelStatuses(
            beaconId: String,
            existingStatuses: List<MainBeaconLevelStatus>,
        ): List<MainBeaconLevelStatus> {
            val byLevel = existingStatuses.associateBy { it.levelType }
            val ensured =
                DefaultLevels.map { levelType ->
                    byLevel[levelType]
                        ?: MainBeaconLevelStatus(
                            mainBeaconId = beaconId,
                            levelType = levelType,
                            generalStatus = MainBeaconReadinessStatus.BLOCKED,
                            syncStatus = defaultSyncStatus(levelType),
                        )
                }
            if (ensured.size != existingStatuses.size || existingStatuses.any { it.levelType !in DefaultLevels }) {
                mainBeaconDao.insertLevelStatuses(ensured)
            }
            return ensured
        }
    }

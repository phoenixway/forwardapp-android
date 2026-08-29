package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
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
                    val contextOrders = row.contextCrossRefs.associate { it.contextId to it.order }
                    val relatedContexts =
                        row.relatedContexts.sortedWith(
                            compareBy<Context> { contextOrders[it.id] ?: Long.MAX_VALUE }
                                .thenBy { it.name.lowercase() },
                        )
                    val relatedAttachments =
                        row.relatedAttachments.sortedWith(
                            compareByDescending<AttachmentEntity> { it.updatedAt }
                                .thenByDescending { it.createdAt },
                        )
                    val groupIds = row.groupMembers.sortedBy { it.order }.map { it.groupId }
                    val groupOrders = row.groupMembers.associate { it.groupId to it.order }

                    MainBeaconWithRelations(
                        beacon = beacon,
                        relatedContexts = relatedContexts,
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

        fun observeParentLinks(): Flow<List<MainBeaconParentLink>> = mainBeaconDao.observeParentLinks()

        suspend fun getBeaconById(beaconId: String): MainBeacon? =
            mainBeaconDao.getBeaconById(beaconId)?.let { orientationBridge.project(it) }

        suspend fun getGroupById(groupId: String): MainBeaconGroup? =
            mainBeaconDao.getAllGroupsSync().firstOrNull { it.id == groupId }?.let { orientationBridge.project(it) }

        suspend fun createBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
        ) {
            val nextOrder = mainBeaconDao.getMaxOrder() + 1L
            upsertBeacon(
                beacon = beacon.copy(order = nextOrder),
                relatedContextIds = relatedContextIds,
                relatedAttachmentIds = relatedAttachmentIds,
                groupIds = groupIds,
                levelStatuses = levelStatuses,
                exists = false,
            )
        }

        suspend fun updateBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
        ) {
            upsertBeacon(beacon, relatedContextIds, relatedAttachmentIds, groupIds, levelStatuses, exists = true)
        }

        suspend fun deleteBeacon(beaconId: String) {
            orientationBridge.ensureCutOver()
            val now = System.currentTimeMillis()
            appDatabase.withTransaction {
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
                orientationBridge.writeCommon(updated)
                mainBeaconDao.updateGroup(updated)
            }
        }

        suspend fun deleteGroup(groupId: String) {
            orientationBridge.ensureCutOver()
            val now = System.currentTimeMillis()
            appDatabase.withTransaction {
                orientationBridge.tombstone(LegacyOrientationSourceType.MAIN_BEACON_GROUP, groupId, now)
                mainBeaconDao.deleteGroup(groupId)
                orientationBridge.syncMembershipProjection(now)
            }
        }

        suspend fun addRelatedContexts(
            beaconId: String,
            contextIds: Set<String>,
        ): Int {
            if (contextIds.isEmpty()) {
                return 0
            }
            val existingContextIds =
                mainBeaconDao
                    .getAllContextCrossRefsSync()
                    .asSequence()
                    .filter { it.beaconId == beaconId }
                    .mapTo(mutableSetOf()) { it.contextId }
            val newContextIds = contextIds.filterNot { it in existingContextIds }
            return if (newContextIds.isEmpty()) {
                0
            } else {
                var nextOrder = mainBeaconDao.getMaxContextCrossRefOrder(beaconId) + 1L
                mainBeaconDao.insertContextCrossRefs(
                    newContextIds.map { contextId ->
                        MainBeaconContextCrossRef(beaconId = beaconId, contextId = contextId, order = nextOrder++)
                    },
                )
                newContextIds.size
            }
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
                        MainBeaconContextCrossRef(beaconId = beaconId, contextId = contextId, order = nextOrder++)
                    },
                )
                contextIds.size
            }
        }

        suspend fun removeContextsFromAllBeacons(contextIds: Set<String>) {
            if (contextIds.isEmpty()) return
            mainBeaconDao.deleteContextCrossRefsForContexts(contextIds)
        }

        suspend fun reorderBeacons(beaconIdsInOrder: List<String>) {
            if (beaconIdsInOrder.isEmpty()) return
            appDatabase.withTransaction {
                beaconIdsInOrder.forEachIndexed { index, beaconId ->
                    mainBeaconDao.updateBeaconOrder(beaconId, index.toLong())
                }
            }
        }

        suspend fun reorderGroups(groupIdsInOrder: List<String>) {
            if (groupIdsInOrder.isEmpty()) return
            appDatabase.withTransaction {
                groupIdsInOrder.forEachIndexed { index, groupId ->
                    mainBeaconDao.updateGroupOrder(groupId, index.toLong())
                }
            }
        }

        suspend fun reorderBeaconGroupMembers(
            groupId: String,
            beaconIdsInOrder: List<String>,
        ) {
            if (beaconIdsInOrder.isEmpty()) return
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                beaconIdsInOrder.forEachIndexed { index, beaconId ->
                    mainBeaconDao.updateGroupMemberOrder(
                        groupId = groupId,
                        beaconId = beaconId,
                        order = index.toLong(),
                    )
                }
                orientationBridge.syncMembershipProjection()
            }
        }

        suspend fun reorderBeaconParentChildren(
            parentBeaconId: String,
            beaconIdsInOrder: List<String>,
        ) {
            if (beaconIdsInOrder.isEmpty()) return
            val beaconsById = mainBeaconDao.getAllBeaconsSync().associateBy { it.id }
            val linkedChildIds =
                mainBeaconDao
                    .getAllParentLinksSync()
                    .filter { it.parentBeaconId == parentBeaconId }
                    .mapTo(hashSetOf()) { it.childBeaconId }
            val now = System.currentTimeMillis()
            appDatabase.withTransaction {
                beaconIdsInOrder.forEachIndexed { index, beaconId ->
                    if (beaconsById[beaconId]?.parentBeaconId == parentBeaconId) {
                        mainBeaconDao.updateBeaconOrder(beaconId, index.toLong())
                    } else if (beaconId in linkedChildIds) {
                        mainBeaconDao.updateParentLinkOrder(
                            parentBeaconId = parentBeaconId,
                            childBeaconId = beaconId,
                            order = index.toLong(),
                            updatedAt = now,
                        )
                    }
                }
            }
        }

        suspend fun reorderBeaconContexts(
            beaconId: String,
            contextIdsInOrder: List<String>,
        ) {
            if (contextIdsInOrder.isEmpty()) return
            val linkedContextIds =
                mainBeaconDao
                    .getAllContextCrossRefsSync()
                    .filter { it.beaconId == beaconId }
                    .mapTo(hashSetOf()) { it.contextId }
            appDatabase.withTransaction {
                contextIdsInOrder.forEachIndexed { index, contextId ->
                    if (contextId in linkedContextIds) {
                        mainBeaconDao.updateContextCrossRefOrder(
                            beaconId = beaconId,
                            contextId = contextId,
                            order = index.toLong(),
                        )
                    }
                }
            }
        }

        suspend fun moveBeaconToParent(
            beaconId: String,
            parentBeaconId: String?,
        ): Boolean {
            val canMove =
                beaconId != parentBeaconId &&
                    (parentBeaconId == null || !wouldCreateBeaconParentCycle(beaconId, parentBeaconId))
            if (canMove) {
                mainBeaconDao.updateBeaconParent(
                    beaconId = beaconId,
                    parentBeaconId = parentBeaconId,
                    updatedAt = System.currentTimeMillis(),
                )
            }
            return canMove
        }

        suspend fun moveBeaconToGroup(
            beaconId: String,
            groupId: String?,
        ) {
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                mainBeaconDao.updateBeaconParent(
                    beaconId = beaconId,
                    parentBeaconId = null,
                    updatedAt = System.currentTimeMillis(),
                )
                mainBeaconDao.deleteGroupMembersForBeacon(beaconId)
                groupId?.let {
                    mainBeaconDao.insertGroupMembers(
                        listOf(
                            MainBeaconGroupMember(
                                groupId = it,
                                beaconId = beaconId,
                                order = mainBeaconDao.getMaxOrder() + 1L,
                            ),
                        ),
                    )
                }
                orientationBridge.syncMembershipProjection()
            }
        }

        suspend fun addBeaconToGroup(
            beaconId: String,
            groupId: String?,
        ): Boolean {
            if (groupId == null || mainBeaconDao.getBeaconById(beaconId) == null) return false
            val alreadyInGroup =
                mainBeaconDao
                    .getAllGroupMembersSync()
                    .any { it.groupId == groupId && it.beaconId == beaconId }
            if (alreadyInGroup) return false
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                mainBeaconDao.insertGroupMembers(
                    listOf(
                        MainBeaconGroupMember(
                            groupId = groupId,
                            beaconId = beaconId,
                            order = mainBeaconDao.getMaxOrder() + 1L,
                        ),
                    ),
                )
                orientationBridge.syncMembershipProjection()
            }
            return true
        }

        suspend fun addBeaconParentLink(
            childBeaconId: String,
            parentBeaconId: String,
        ): Boolean {
            if (childBeaconId == parentBeaconId) return false
            val beaconsById = mainBeaconDao.getAllBeaconsSync().associateBy { it.id }
            if (childBeaconId !in beaconsById || parentBeaconId !in beaconsById) return false
            val existingLinks = mainBeaconDao.getAllParentLinksSync()
            if (beaconsById[childBeaconId]?.parentBeaconId == parentBeaconId) return false
            if (existingLinks.any { it.parentBeaconId == parentBeaconId && it.childBeaconId == childBeaconId }) return false
            if (wouldCreateBeaconParentCycle(childBeaconId, parentBeaconId, beaconsById.values.toList(), existingLinks)) {
                return false
            }

            val now = System.currentTimeMillis()
            return mainBeaconDao.insertParentLink(
                MainBeaconParentLink(
                    parentBeaconId = parentBeaconId,
                    childBeaconId = childBeaconId,
                    order = mainBeaconDao.getMaxParentLinkOrder(parentBeaconId) + 1L,
                    updatedAt = now,
                    createdAt = now,
                ),
            ) != -1L
        }

        suspend fun duplicateBeacon(
            sourceBeaconId: String,
            parentBeaconId: String?,
            groupId: String?,
        ): Boolean {
            val source = mainBeaconDao.getBeaconById(sourceBeaconId) ?: return false
            val now = System.currentTimeMillis()
            val targetId = java.util.UUID.randomUUID().toString()
            val sourceContexts = mainBeaconDao.getContextsForBeacon(sourceBeaconId).mapTo(linkedSetOf()) { it.id }
            val sourceAttachments =
                mainBeaconDao.getAttachmentsForBeacon(sourceBeaconId).mapTo(linkedSetOf()) { it.id }
            val sourceStatuses =
                mainBeaconDao.getLevelStatusesForBeacon(sourceBeaconId).map { status ->
                    status.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        mainBeaconId = targetId,
                        updatedAt = now,
                    )
                }
            val target =
                source.copy(
                    id = targetId,
                    title = "${source.title} copy".trim(),
                    parentBeaconId = parentBeaconId,
                    order = mainBeaconDao.getMaxOrder() + 1L,
                    updatedAt = now,
                    createdAt = now,
                )
            upsertBeacon(
                beacon = target,
                relatedContextIds = sourceContexts,
                relatedAttachmentIds = sourceAttachments,
                groupIds = groupId?.let { setOf(it) }.orEmpty(),
                levelStatuses = sourceStatuses,
                exists = false,
            )
            return true
        }

        private suspend fun wouldCreateBeaconParentCycle(
            beaconId: String,
            requestedParentId: String,
        ): Boolean {
            val beacons = mainBeaconDao.getAllBeaconsSync()
            return wouldCreateBeaconParentCycle(
                beaconId = beaconId,
                requestedParentId = requestedParentId,
                beacons = beacons,
                parentLinks = mainBeaconDao.getAllParentLinksSync(),
            )
        }

        private fun wouldCreateBeaconParentCycle(
            beaconId: String,
            requestedParentId: String,
            beacons: List<MainBeacon>,
            parentLinks: List<MainBeaconParentLink>,
        ): Boolean {
            val byId = beacons.associateBy { it.id }
            if (requestedParentId !in byId) return true

            val childrenByParentId = linkedMapOf<String, MutableList<String>>()
            beacons.forEach { beacon ->
                beacon.parentBeaconId?.let { parentId ->
                    childrenByParentId.getOrPut(parentId) { mutableListOf() } += beacon.id
                }
            }
            parentLinks.forEach { link ->
                childrenByParentId.getOrPut(link.parentBeaconId) { mutableListOf() } += link.childBeaconId
            }

            val pending = ArrayDeque<String>()
            pending += beaconId
            val visited = mutableSetOf<String>()
            while (pending.isNotEmpty()) {
                val cursor = pending.removeFirst()
                if (!visited.add(cursor)) continue
                if (cursor == requestedParentId) return true
                childrenByParentId[cursor].orEmpty().forEach(pending::add)
            }
            return false
        }

        private suspend fun upsertBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
            exists: Boolean,
        ) {
            orientationBridge.ensureCutOver()
            appDatabase.withTransaction {
                val existingContextOrders =
                    mainBeaconDao
                        .getAllContextCrossRefsSync()
                        .filter { it.beaconId == beacon.id }
                        .associate { it.contextId to it.order }
                var nextContextOrder = (existingContextOrders.values.maxOrNull() ?: -1L) + 1L

                if (exists) {
                    orientationBridge.writeCommon(beacon)
                    mainBeaconDao.updateBeacon(beacon)
                } else {
                    mainBeaconDao.insertBeacon(beacon)
                    orientationBridge.writeCommon(beacon)
                }

                mainBeaconDao.deleteContextCrossRefsForBeacon(beacon.id)
                mainBeaconDao.deleteAttachmentCrossRefsForBeacon(beacon.id)
                mainBeaconDao.deleteGroupMembersForBeacon(beacon.id)

                if (relatedContextIds.isNotEmpty()) {
                    mainBeaconDao.insertContextCrossRefs(
                        relatedContextIds.map { contextId ->
                            MainBeaconContextCrossRef(
                                beaconId = beacon.id,
                                contextId = contextId,
                                order = existingContextOrders[contextId] ?: nextContextOrder++,
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

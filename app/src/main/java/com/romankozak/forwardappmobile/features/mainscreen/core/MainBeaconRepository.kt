package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroupMember
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconSyncStatus
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainBeaconRepository
    @Inject
    constructor(
        private val appDatabase: AppDatabase,
        private val mainBeaconDao: MainBeaconDao,
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
            mainBeaconDao
                .observeMainBeaconRelations()
                .map { relationRows ->
                    relationRows.map { row ->
                        val beacon = row.beacon
                        val ensuredStatuses =
                            ensureAllLevelStatuses(
                                beacon.id,
                                row.levelStatuses.sortedBy { it.levelType },
                            )
                        val relatedContexts = row.relatedContexts.sortedBy { it.name.lowercase() }
                        val relatedAttachments =
                            row.relatedAttachments.sortedWith(
                                compareByDescending<AttachmentEntity> { it.updatedAt }
                                    .thenByDescending { it.createdAt },
                            )
                        val groupIds = row.groupMembers.sortedBy { it.order }.map { it.groupId }

                        MainBeaconWithRelations(
                            beacon = beacon,
                            relatedContexts = relatedContexts,
                            relatedAttachments = relatedAttachments,
                            levelStatuses = ensuredStatuses,
                            groupIds = groupIds,
                        )
                    }
                }

        fun observeGroups(): Flow<List<MainBeaconGroup>> = mainBeaconDao.observeGroups()

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
            mainBeaconDao.deleteBeacon(beaconId)
        }

        suspend fun createGroup(
            title: String,
            description: String? = null,
        ) {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) return
            val now = System.currentTimeMillis()
            val nextOrder = mainBeaconDao.getMaxGroupOrder() + 1L
            mainBeaconDao.insertGroup(
                MainBeaconGroup(
                    title = normalizedTitle,
                    description = description?.trim()?.ifBlank { null },
                    order = nextOrder,
                    updatedAt = now,
                    createdAt = now,
                ),
            )
        }

        suspend fun updateGroup(group: MainBeaconGroup) {
            val normalizedTitle = group.title.trim()
            if (normalizedTitle.isBlank()) return
            mainBeaconDao.updateGroup(
                group.copy(
                    title = normalizedTitle,
                    description = group.description?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        suspend fun deleteGroup(groupId: String) {
            mainBeaconDao.deleteGroup(groupId)
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
                mainBeaconDao.insertContextCrossRefs(
                    newContextIds.map { contextId ->
                        MainBeaconContextCrossRef(beaconId = beaconId, contextId = contextId)
                    },
                )
                newContextIds.size
            }
        }

        suspend fun addRelatedContextsToGroup(
            groupId: String,
            contextIds: Set<String>,
        ): Int {
            if (contextIds.isEmpty()) return 0
            val beaconIds =
                mainBeaconDao
                    .getAllGroupMembersSync()
                    .asSequence()
                    .filter { it.groupId == groupId }
                    .mapTo(linkedSetOf()) { it.beaconId }
            if (beaconIds.isEmpty()) return 0

            val existingPairs =
                mainBeaconDao
                    .getAllContextCrossRefsSync()
                    .asSequence()
                    .mapTo(hashSetOf()) { it.beaconId to it.contextId }
            val newCrossRefs =
                beaconIds.flatMap { beaconId ->
                    contextIds
                        .filterNot { contextId -> beaconId to contextId in existingPairs }
                        .map { contextId -> MainBeaconContextCrossRef(beaconId = beaconId, contextId = contextId) }
                }
            if (newCrossRefs.isEmpty()) return 0
            mainBeaconDao.insertContextCrossRefs(newCrossRefs)
            return newCrossRefs.size
        }

        suspend fun reorderBeacons(beaconIdsInOrder: List<String>) {
            if (beaconIdsInOrder.isEmpty()) return
            appDatabase.withTransaction {
                beaconIdsInOrder.forEachIndexed { index, beaconId ->
                    mainBeaconDao.updateBeaconOrder(beaconId, index.toLong())
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
            }
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
            val byId = mainBeaconDao.getAllBeaconsSync().associateBy { it.id }
            var cursor: String? = requestedParentId
            val visited = mutableSetOf<String>()
            while (cursor != null && visited.add(cursor)) {
                if (cursor == beaconId) return true
                cursor = byId[cursor]?.parentBeaconId
            }
            return requestedParentId !in byId
        }

        private suspend fun upsertBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            groupIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
            exists: Boolean,
        ) {
            appDatabase.withTransaction {
                if (exists) {
                    mainBeaconDao.updateBeacon(beacon)
                } else {
                    mainBeaconDao.insertBeacon(beacon)
                }

                mainBeaconDao.deleteContextCrossRefsForBeacon(beacon.id)
                mainBeaconDao.deleteAttachmentCrossRefsForBeacon(beacon.id)
                mainBeaconDao.deleteGroupMembersForBeacon(beacon.id)

                if (relatedContextIds.isNotEmpty()) {
                    mainBeaconDao.insertContextCrossRefs(
                        relatedContextIds.map { contextId ->
                            MainBeaconContextCrossRef(beaconId = beacon.id, contextId = contextId)
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

package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
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
            mainBeaconDao.observeMainBeacons().map { beacons ->
                beacons.map { beacon ->
                    val ensuredStatuses =
                        ensureAllLevelStatuses(
                            beacon.id,
                            mainBeaconDao.getLevelStatusesForBeacon(beacon.id),
                        )
                    MainBeaconWithRelations(
                        beacon = beacon,
                        relatedContexts = mainBeaconDao.getContextsForBeacon(beacon.id),
                        relatedAttachments = mainBeaconDao.getAttachmentsForBeacon(beacon.id),
                        levelStatuses = ensuredStatuses,
                    )
                }
            }

        suspend fun createBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
        ) {
            val nextOrder = mainBeaconDao.getMaxOrder() + 1L
            upsertBeacon(
                beacon = beacon.copy(order = nextOrder),
                relatedContextIds = relatedContextIds,
                relatedAttachmentIds = relatedAttachmentIds,
                levelStatuses = levelStatuses,
                exists = false,
            )
        }

        suspend fun updateBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
            levelStatuses: List<MainBeaconLevelStatus>,
        ) {
            upsertBeacon(beacon, relatedContextIds, relatedAttachmentIds, levelStatuses, exists = true)
        }

        suspend fun deleteBeacon(beaconId: String) {
            mainBeaconDao.deleteBeacon(beaconId)
        }

        suspend fun reorderBeacons(beaconIdsInOrder: List<String>) {
            if (beaconIdsInOrder.isEmpty()) return
            appDatabase.withTransaction {
                beaconIdsInOrder.forEachIndexed { index, beaconId ->
                    mainBeaconDao.updateBeaconOrder(beaconId, index.toLong())
                }
            }
        }

        private suspend fun upsertBeacon(
            beacon: MainBeacon,
            relatedContextIds: Set<String>,
            relatedAttachmentIds: Set<String>,
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

package com.romankozak.forwardappmobile.sync

import android.util.Log
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.sync.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.*
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.contexts.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for merging data from different sources,
 * creating sync reports, applying changes, and handling selective imports.
 */
@Singleton
class MergeRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val syncLocalService: SyncLocalService,
    private val logicHelper: SyncLogicHelper,
    private val goalDao: GoalDao,
    private val contextDao: ContextDao,
    private val listItemDao: ListItemDao,
    private val attachmentDao: AttachmentDao,
) {
    private val TAG = "MergeRepository"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    /**
     * Creates a sync report by comparing incoming backup data with local data
     * @param jsonString JSON string containing backup data
     * @return SyncReport with list of detected changes
     */
    suspend fun createSyncReport(jsonString: String): SyncReport {
        val backup = gson.fromJson(jsonString, FullAppBackup::class.java)
        val incomingDb = backup.database ?: return SyncReport(emptyList())

        val localProjects = contextDao.getAll().associateBy { it.id }
        val localGoals = goalDao.getAll().associateBy { it.id }
        val changes = mutableListOf<SyncChange>()

        incomingDb.goals.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeGoal(incomingRaw)
            val local = localGoals[incoming.id]?.let { SyncMapper.normalizeGoal(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Ціль", incoming.id, "Нова ціль: ${incoming.text}", entity = incoming))
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(SyncChange(ChangeType.Update, "Ціль", incoming.id, "Оновлено ціль: ${incoming.text}", entity = incoming))
            }
        }

        incomingDb.projects.filter { !SystemContexts.isSystem(ContextId(it.id)) }.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeProject(incomingRaw)
            val local = localProjects[incoming.id]?.let { SyncMapper.normalizeProject(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Список", incoming.id, "Новий список: ${incoming.name}", entity = incoming))
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(SyncChange(ChangeType.Update, "Список", incoming.id, "Оновлено список: ${incoming.name}", entity = incoming))
            }
        }

        val incomingGoalIds = incomingDb.goals.map { it.id }.toSet()
        localGoals.keys.minus(incomingGoalIds).forEach { id ->
            localGoals[id]?.let {
                changes.add(SyncChange(ChangeType.Delete, "Ціль", id, "Видалено ціль: ${it.text}", entity = it))
            }
        }

        return SyncReport(changes)
    }

    /**
     * Applies approved changes to the local database
     * @param approvedChanges List of changes to apply
     */
    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        appDatabase.withTransaction {
            approvedChanges.forEach { change ->
                when (change.type) {
                    ChangeType.Delete -> {
                        when (change.entityType) {
                            "Список" -> contextDao.deleteContextById(change.id)
                            "Ціль" -> goalDao.deleteGoalById(change.id)
                            "Привʼязка" -> listItemDao.deleteItemsByIds(listOf(change.id))
                        }
                    }
                    ChangeType.Update, ChangeType.Add, ChangeType.Move -> {
                        when (change.entity) {
                            is Context -> contextDao.insert(change.entity)
                            is Goal -> goalDao.insertGoal(change.entity)
                            is BacklogItem -> listItemDao.insertItem(change.entity)
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies server changes to local database with proper merging and conflict resolution
     * @param changes DatabaseContent with incoming changes
     * @return Result indicating success or failure
     */
    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            Log.d(TAG, "[applyServerChanges] Incoming items: projects=${changes.projects.size}, attachments=${changes.attachments.size}")

            appDatabase.withTransaction {
                val local = syncLocalService.loadLocalDatabaseContent()
                val allProjectIds = local.projects.map { it.id }.toSet()

                val idRedirects = mutableMapOf<String, String>()
                val localSystemProjects = local.projects.filter { SystemContexts.isSystem(ContextId(it.id)) }.associateBy { it.id }

                val correctedIncomingProjects = changes.projects.map { incoming ->
                    if (SystemContexts.isSystem(ContextId(incoming.id))) {
                        localSystemProjects[incoming.id]?.let { localSys ->
                            if (localSys.id != incoming.id) {
                                idRedirects[incoming.id] = localSys.id
                                return@map incoming.copy(id = localSys.id)
                            }
                        }
                    }
                    incoming
                }

                val mergedContexts = logicHelper.mergeAndMark(
                    incoming = correctedIncomingProjects.map { SyncMapper.normalizeProject(it) },
                    localMap = local.projects.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { p, s -> p.copy(syncedAt = s) },
                    syncedAt = ts,
                    isDeletedSelector = { it.isDeleted },
                )
                if (mergedContexts.isNotEmpty()) contextDao.insertContexts(mergedContexts)

                val mergedGoals = logicHelper.mergeAndMark(
                    incoming = changes.goals.map { SyncMapper.normalizeGoal(it) },
                    localMap = local.goals.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { g, s -> g.copy(syncedAt = s) },
                    syncedAt = ts,
                    isDeletedSelector = { it.isDeleted },
                )
                if (mergedGoals.isNotEmpty()) goalDao.insertGoals(mergedGoals)

                val contextIds = (allProjectIds + mergedContexts.map { it.id }).toSet()

                val processedAttachments = changes.attachments.map { att ->
                    val newOwnerId = att.ownerContextId?.let { idRedirects[it] ?: it }
                    att.copy(ownerContextId = newOwnerId)
                }.filter { it.ownerContextId == null || it.ownerContextId in contextIds }

                val incomingAttachments = logicHelper.mergeAndMark(
                    incoming = processedAttachments,
                    localMap = local.attachments.associateBy { it.id },
                    idSelector = { it.id },
                    versionSelector = { it.version },
                    updatedSelector = { it.updatedTs() },
                    markSynced = { at, s -> at.copy(syncedAt = s) },
                    syncedAt = ts,
                )

                val alreadySyncedIds = incomingAttachments.map { it.id }.toSet()
                val matchedExisting = processedAttachments
                    .filter { it.id !in alreadySyncedIds }
                    .mapNotNull { inc -> local.attachments.find { it.id == inc.id } }
                    .map { it.copy(syncedAt = ts) }

                attachmentDao.insertAttachments(incomingAttachments + matchedExisting)

                val synthesizedRefs = logicHelper.synthesizeMissingCrossRefs(processedAttachments, changes.contextAttachmentCrossRefs)
                val finalRefs = synthesizedRefs.map { ref ->
                    val newCtxId = idRedirects[ref.contextId] ?: ref.contextId
                    ref.copy(contextId = newCtxId, syncedAt = ts)
                }.filter { it.contextId in contextIds }

                attachmentDao.insertContextAttachmentLinks(finalRefs)

                val cleanedListItems = changes.backlogItems.map {
                    it.copy(
                        contextId = idRedirects[it.contextId] ?: it.contextId,
                        entityId = if (it.itemType == BacklogItemTypeValues.SUBLIST) idRedirects[it.entityId] ?: it.entityId else it.entityId,
                    )
                }
                listItemDao.insertItems(logicHelper.dedupListItems(cleanedListItems))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a diff between incoming and local data
     * @param incoming DatabaseContent from backup
     * @return BackupDiff with categorized differences
     */
    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff {
        val local = syncLocalService.loadLocalDatabaseContent()

        return BackupDiff(
            projects = logicHelper.diffEntities(
                incomingList = incoming.projects.map { SyncMapper.normalizeProject(it) },
                localList = local.projects,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            goals = logicHelper.diffEntities(
                incomingList = incoming.goals.map { SyncMapper.normalizeGoal(it) },
                localList = local.goals,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            backlogItems = logicHelper.diffEntities(
                incomingList = incoming.backlogItems,
                localList = local.backlogItems,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            documents = logicHelper.diffEntities(
                incomingList = incoming.documents,
                localList = local.documents,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            attachments = logicHelper.diffEntities(
                incomingList = incoming.attachments,
                localList = local.attachments,
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
            contextAttachmentCrossRefs = logicHelper.diffEntities(
                incomingList = incoming.contextAttachmentCrossRefs,
                localList = local.contextAttachmentCrossRefs,
                idSelector = { "${it.contextId}-${it.attachmentId}" },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                isDeletedSelector = { it.isDeleted },
            ),
        )
    }

    /**
     * Imports only selected data from backup, filtering by version and timestamp
     * @param selectedData DatabaseContent with selected items to import
     * @return Result with success message or error
     */
    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        val IMPORT_TAG = "MergeRepo_Selective"
        return try {
            val local = syncLocalService.loadLocalDatabaseContent()
            val ts = System.currentTimeMillis()

            fun <T> filterNewer(
                incoming: List<T>,
                localMap: Map<String, T>,
                idSelector: (T) -> String,
                versionSelector: (T) -> Long,
                updatedAtSelector: (T) -> Long,
            ): List<T> = incoming.filter { inc ->
                val loc = localMap[idSelector(inc)]
                if (loc == null) return@filter true
                val incVer = versionSelector(inc)
                val locVer = versionSelector(loc)
                if (incVer > locVer) return@filter true
                if (incVer < locVer) return@filter false
                updatedAtSelector(inc) > updatedAtSelector(loc)
            }

            appDatabase.withTransaction {
                Log.d(IMPORT_TAG, "Starting selective transaction...")

                val regularProjects = selectedData.projects.filter { !SystemContexts.isSystem(ContextId(it.id)) }
                val newerProjects = filterNewer(
                    regularProjects,
                    local.projects.associateBy { it.id },
                    { it.id },
                    { it.version },
                    { it.updatedTs() },
                )
                if (newerProjects.isNotEmpty()) {
                    contextDao.insertContexts(newerProjects.map { it.copy(syncedAt = ts) })
                }

                val newerGoals = filterNewer(
                    selectedData.goals,
                    local.goals.associateBy { it.id },
                    { it.id },
                    { it.version },
                    { it.updatedTs() },
                )
                if (newerGoals.isNotEmpty()) {
                    goalDao.insertGoals(newerGoals.map { it.copy(syncedAt = ts) })
                }

                val currentContextIds = (local.projects.map { it.id } + newerProjects.map { it.id }).toSet()
                val currentGoalIds = (local.goals.map { it.id } + newerGoals.map { it.id }).toSet()

                val validListItems = selectedData.backlogItems.filter {
                    it.contextId in currentContextIds || it.entityId in currentGoalIds
                }
                listItemDao.insertItems(validListItems.map { it.copy(syncedAt = ts) })

                if (selectedData.attachments.isNotEmpty()) {
                    val newerAttachments = filterNewer(
                        selectedData.attachments,
                        local.attachments.associateBy { it.id },
                        { it.id },
                        { it.version },
                        { it.updatedTs() },
                    )
                    attachmentDao.insertAttachments(newerAttachments.map { it.copy(syncedAt = ts) })
                }

                if (selectedData.contextAttachmentCrossRefs.isNotEmpty()) {
                    val validCrossRefs = selectedData.contextAttachmentCrossRefs.filter {
                        it.contextId in currentContextIds
                    }
                    attachmentDao.insertContextAttachmentLinks(validCrossRefs.map { it.copy(syncedAt = ts) })
                }
            }

            Result.success("Вибрані дані успішно імпортовано.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Critical error during selective import", e)
            Result.failure(e)
        }
    }
}
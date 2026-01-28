package com.romankozak.forwardappmobile.sync

import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.ChangeType
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange
import com.romankozak.forwardappmobile.core.data.models.sync.SyncReport
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for merging data from different sources,
 * creating sync reports, applying changes, and handling selective imports.
 * It contains the business logic for synchronization and delegates data access
 * to a local data source.
 */
@Singleton
class MergeRepository @Inject constructor(
    private val localDataSource: MergeLocalDataSource,
    private val logicHelper: SyncLogicHelper,
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

        val localProjects = localDataSource.getContexts().associateBy { it.id }
        val localGoals = localDataSource.getGoals().associateBy { it.id }
        val changes = mutableListOf<SyncChange>()

        incomingDb.goals.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeGoal(incomingRaw)
            val local = localGoals[incoming.id]?.let { SyncMapper.normalizeGoal(it) }

            if (local == null) {
                changes.add(
                    SyncChange(
                        ChangeType.Add,
                        "Ціль",
                        incoming.id,
                        "Нова ціль: ${incoming.text}",
                        entity = incoming
                    )
                )
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(
                    SyncChange(
                        ChangeType.Update,
                        "Ціль",
                        incoming.id,
                        "Оновлено ціль: ${incoming.text}",
                        entity = incoming
                    )
                )
            }
        }

        incomingDb.projects.filter { !SystemContexts.isSystem(ContextId(it.id)) }.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeProject(incomingRaw)
            val local = localProjects[incoming.id]?.let { SyncMapper.normalizeProject(it) }

            if (local == null) {
                changes.add(
                    SyncChange(
                        ChangeType.Add,
                        "Список",
                        incoming.id,
                        "Новий список: ${incoming.name}",
                        entity = incoming
                    )
                )
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(
                    SyncChange(
                        ChangeType.Update,
                        "Список",
                        incoming.id,
                        "Оновлено список: ${incoming.name}",
                        entity = incoming
                    )
                )
            }
        }

        val incomingGoalIds = incomingDb.goals.map { it.id }.toSet()
        localGoals.keys.minus(incomingGoalIds).forEach { id ->
            localGoals[id]?.let {
                changes.add(
                    SyncChange(
                        ChangeType.Delete,
                        "Ціль",
                        id,
                        "Видалено ціль: ${it.text}",
                        entity = it
                    )
                )
            }
        }

        return SyncReport(changes)
    }

    /**
     * Applies approved changes to the local database
     * @param approvedChanges List of changes to apply
     */
    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        localDataSource.applyChanges(approvedChanges)
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

            val local = localDataSource.getLocalDatabaseContent()
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
            if (mergedContexts.isNotEmpty()) localDataSource.insertContexts(mergedContexts)

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
            if (mergedGoals.isNotEmpty()) localDataSource.insertGoals(mergedGoals)

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

            localDataSource.insertAttachments(incomingAttachments + matchedExisting)

            val synthesizedRefs = logicHelper.synthesizeMissingCrossRefs(processedAttachments, changes.contextAttachmentCrossRefs)
            val finalRefs = synthesizedRefs.map { ref ->
                val newCtxId = idRedirects[ref.contextId] ?: ref.contextId
                ref.copy(contextId = newCtxId, syncedAt = ts)
            }.filter { it.contextId in contextIds }

            localDataSource.insertContextAttachmentLinks(finalRefs)

            val cleanedListItems = changes.backlogItems.map {
                it.copy(
                    contextId = idRedirects[it.contextId] ?: it.contextId,
                    entityId = if (it.itemType == BacklogItemTypeValues.SUBLIST) idRedirects[it.entityId] ?: it.entityId else it.entityId,
                )
            }
            localDataSource.insertListItems(logicHelper.dedupListItems(cleanedListItems))

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
        val local = localDataSource.getLocalDatabaseContent()

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
            val local = localDataSource.getLocalDatabaseContent()

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

            Log.d(IMPORT_TAG, "Starting selective transaction...")

            val regularProjects = selectedData.projects.filter { !SystemContexts.isSystem(ContextId(it.id)) }
            val newerProjects = filterNewer(
                regularProjects,
                local.projects.associateBy { it.id },
                { it.id },
                { it.version },
                { it.updatedTs() },
            )

            val newerGoals = filterNewer(
                selectedData.goals,
                local.goals.associateBy { it.id },
                { it.id },
                { it.version },
                { it.updatedTs() },
            )

            val currentContextIds = (local.projects.map { it.id } + newerProjects.map { it.id }).toSet()
            val currentGoalIds = (local.goals.map { it.id } + newerGoals.map { it.id }).toSet()

            val validListItems = selectedData.backlogItems.filter {
                it.contextId in currentContextIds || it.entityId in currentGoalIds
            }

            val newerAttachments = if (selectedData.attachments.isNotEmpty()) {
                filterNewer(
                    selectedData.attachments,
                    local.attachments.associateBy { it.id },
                    { it.id },
                    { it.version },
                    { it.updatedTs() },
                )
            } else emptyList()

            val validCrossRefs = if (selectedData.contextAttachmentCrossRefs.isNotEmpty()) {
                selectedData.contextAttachmentCrossRefs.filter {
                    it.contextId in currentContextIds
                }
            } else emptyList()

            localDataSource.importSelectedData(
                projects = newerProjects,
                goals = newerGoals,
                listItems = validListItems,
                attachments = newerAttachments,
                crossRefs = validCrossRefs
            )

            Result.success("Вибрані дані успішно імпортовано.")
        } catch (e: Exception) {
            Log.e(IMPORT_TAG, "Critical error during selective import", e)
            Result.failure(e)
        }
    }
}
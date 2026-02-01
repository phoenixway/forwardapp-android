// sync/src/main/java/com/romankozak/forwardappmobile/sync/MergeRepository.kt
package com.romankozak.forwardappmobile.sync

import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.*
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import com.romankozak.forwardappmobile.sync.SyncMapper.updatedTs // ВАЖЛИВО: імпорт extension-функції
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeRepository @Inject constructor(
    private val localDataSource: MergeLocalDataSource,
    private val logicHelper: SyncLogicHelper,
) {
    private val TAG = "MergeRepository"
    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

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
                changes.add(SyncChange(ChangeType.Add, "Ціль", incoming.id, "Нова ціль: ${incoming.text}", entity = incoming))
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(SyncChange(ChangeType.Update, "Ціль", incoming.id, "Оновлено ціль: ${incoming.text}", entity = incoming))
            }
        }

        incomingDb.projects.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeProject(incomingRaw)
            val local = localProjects[incoming.id]?.let { SyncMapper.normalizeProject(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Список", incoming.id, "Новий список: ${incoming.name}", entity = incoming))
            } else if (incoming.updatedTs() > local.updatedTs()) {
                changes.add(SyncChange(ChangeType.Update, "Список", incoming.id, "Оновлено список: ${incoming.name}", entity = incoming))
            }
        }

        return SyncReport(changes)
    }

    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            val local = localDataSource.getLocalDatabaseContent()
            val allProjectIds = local.projects.map { it.id }.toSet()

            // Мапінг для системних проектів (Inbox, Archive тощо), щоб не дублювати їх
            val idRedirects = mutableMapOf<String, String>()

            val mergedContexts = logicHelper.mergeAndMark(
                incoming = changes.projects.map { SyncMapper.normalizeProject(it) },
                localMap = local.projects.associateBy { it.id },
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                markSynced = { p, s -> p.copy(syncedAt = s) },
                syncedAt = ts,
                isDeletedSelector = { it.isDeleted }
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
                isDeletedSelector = { it.isDeleted }
            )
            if (mergedGoals.isNotEmpty()) localDataSource.insertGoals(mergedGoals)

            // Обробка вкладень
            val incomingAttachments = logicHelper.mergeAndMark(
                incoming = changes.attachments,
                localMap = local.attachments.associateBy { it.id },
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedTs() },
                markSynced = { at, s -> at.copy(syncedAt = s) },
                syncedAt = ts
            )
            localDataSource.insertAttachments(incomingAttachments)

            // Обробка зв'язків
            val synthesizedRefs = logicHelper.synthesizeMissingCrossRefs(changes.attachments, changes.contextAttachmentCrossRefs)
            localDataSource.insertContextAttachmentLinks(synthesizedRefs.map { it.copy(syncedAt = ts) })

            // Обробка елементів списків (дедуплікація)
            localDataSource.insertListItems(logicHelper.dedupListItems(changes.backlogItems))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes", e)
            Result.failure(e)
        }
    }

    suspend fun createBackupDiff(incoming: DatabaseContent): BackupDiff {
        val local = localDataSource.getLocalDatabaseContent()
        return BackupDiff(
            projects = logicHelper.diffEntities(incoming.projects.map { SyncMapper.normalizeProject(it) }, local.projects, { it.id }, { it.version }, { it.updatedTs() }),
            goals = logicHelper.diffEntities(incoming.goals.map { SyncMapper.normalizeGoal(it) }, local.goals, { it.id }, { it.version }, { it.updatedTs() }),
            backlogItems = logicHelper.diffEntities(incoming.backlogItems, local.backlogItems, { it.id }, { it.version }, { it.updatedTs() }),
            documents = logicHelper.diffEntities(incoming.documents, local.documents, { it.id }, { it.version }, { it.updatedTs() }),
            attachments = logicHelper.diffEntities(incoming.attachments, local.attachments, { it.id }, { it.version }, { it.updatedTs() }),
            contextAttachmentCrossRefs = logicHelper.diffEntities(incoming.contextAttachmentCrossRefs, local.contextAttachmentCrossRefs, { "${it.contextId}-${it.attachmentId}" }, { it.version }, { it.updatedTs() })
        )
    }
    // У MergeRepository.kt додайте ці "проксі" методи:

    /**
     * Застосовує схвалені зміни до локальної бази даних.
     */
    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        localDataSource.applyChanges(approvedChanges)
    }

    /**
     * Імпортує лише вибрані дані (наприклад, тільки певні проекти).
     */
    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        return try {
            localDataSource.importSelectedData(
                projects = selectedData.projects,
                goals = selectedData.goals,
                listItems = selectedData.backlogItems,
                attachments = selectedData.attachments,
                crossRefs = selectedData.contextAttachmentCrossRefs
            )
            Result.success("Вибрані дані успішно імпортовано")
        } catch (e: Exception) {
            Log.e(TAG, "Selective import failed", e)
            Result.failure(e)
        }
    }

    suspend fun applyServerChanges(bundle: com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            localDataSource.applySnapshotBundle(bundle)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes from snapshot", e)
            Result.failure(e)
        }
    }
}
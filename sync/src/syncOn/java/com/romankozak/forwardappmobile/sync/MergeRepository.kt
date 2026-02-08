// sync/src/main/java/com/romankozak/forwardappmobile/sync/MergeRepository.kt
package com.romankozak.forwardappmobile.sync

import android.util.Log
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MergeRepository @Inject constructor(
    private val mergeLocalDataSource: com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource,
    private val fullBackupLocalDataSource: FullBackupLocalDataSource,
    private val logicHelper: SyncLogicHelper,
) {
    private val TAG = "MergeRepository"
    private val gson = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongDeserializer())
        .create()

    suspend fun createSyncReport(jsonString: String): SyncReport {
        val backup = gson.fromJson(sanitizeIncomingBackupJson(jsonString), FullAppBackup::class.java)
        val incomingDb = backup.database ?: return SyncReport(emptyList())

        val localProjects = mergeLocalDataSource.getContexts().associateBy { it.id }
        val localGoals = mergeLocalDataSource.getGoals().associateBy { it.id }
        val changes = mutableListOf<SyncChange>()

        incomingDb.goals.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeGoal(incomingRaw)
            val local = localGoals[incoming.id]?.let { SyncMapper.normalizeGoal(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Ціль", incoming.id, "Нова ціль: ${incoming.text}", entity = incoming))
            } else if ((incoming.updatedAt ?: incoming.createdAt) > (local.updatedAt ?: local.createdAt)) {
                changes.add(SyncChange(ChangeType.Update, "Ціль", incoming.id, "Оновлено ціль: ${incoming.text}", entity = incoming))
            }
        }

        incomingDb.projects.forEach { incomingRaw ->
            val incoming = SyncMapper.normalizeProject(incomingRaw)
            val local = localProjects[incoming.id]?.let { SyncMapper.normalizeProject(it) }

            if (local == null) {
                changes.add(SyncChange(ChangeType.Add, "Список", incoming.id, "Новий список: ${incoming.name}", entity = incoming))
            } else if ((incoming.updatedAt ?: incoming.createdAt) > (local.updatedAt ?: local.createdAt)) {
                changes.add(SyncChange(ChangeType.Update, "Список", incoming.id, "Оновлено список: ${incoming.name}", entity = incoming))
            }
        }

        return SyncReport(changes)
    }

    suspend fun applyServerChanges(changes: DatabaseContent): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            val local = mergeLocalDataSource.getLocalDatabaseContent()
            val allProjectIds = local.projects.map { it.id }.toSet()

            // Мапінг для системних проектів (Inbox, Archive тощо), щоб не дублювати їх
            val idRedirects = mutableMapOf<String, String>()

            val mergedContexts = logicHelper.mergeAndMark(
                incoming = changes.projects.map { SyncMapper.normalizeProject(it) },
                localMap = local.projects.associateBy { it.id },
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedAt ?: it.createdAt },
                markSynced = { p, s -> p.copy(syncedAt = s) },
                syncedAt = ts,
                isDeletedSelector = { it.isDeleted }
            )
            if (mergedContexts.isNotEmpty()) mergeLocalDataSource.insertContexts(mergedContexts)

            val mergedGoals = logicHelper.mergeAndMark(
                incoming = changes.goals.map { SyncMapper.normalizeGoal(it) },
                localMap = local.goals.associateBy { it.id },
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedAt ?: it.createdAt },
                markSynced = { g, s -> g.copy(syncedAt = s) },
                syncedAt = ts,
                isDeletedSelector = { it.isDeleted }
            )
            if (mergedGoals.isNotEmpty()) mergeLocalDataSource.insertGoals(mergedGoals)

            // Обробка вкладень
            val incomingAttachments = logicHelper.mergeAndMark(
                incoming = changes.attachments,
                localMap = local.attachments.associateBy { it.id },
                idSelector = { it.id },
                versionSelector = { it.version },
                updatedSelector = { it.updatedAt },
                markSynced = { at, s -> at.copy(syncedAt = s) },
                syncedAt = ts
            )
            mergeLocalDataSource.insertAttachments(incomingAttachments)

            // Обробка зв'язків
            val synthesizedRefs = logicHelper.synthesizeMissingCrossRefs(changes.attachments, changes.contextAttachmentCrossRefs)
            mergeLocalDataSource.insertContextAttachmentLinks(synthesizedRefs.map { it.copy(syncedAt = ts) })

            // Обробка елементів списків (дедуплікація)
            mergeLocalDataSource.insertListItems(logicHelper.dedupListItems(changes.backlogItems))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes", e)
            Result.failure(e)
        }
    }

    suspend fun createBackupDiff(incoming: DatabaseContent): LegacyBackupDiff {
        val local = mergeLocalDataSource.getLocalDatabaseContent()
        return LegacyBackupDiff(
            projects = logicHelper.diffEntities(incoming.projects.map { SyncMapper.normalizeProject(it) }, local.projects, { it.id }, { it.version }, { it.updatedAt ?: 0L }),
            goals = logicHelper.diffEntities(incoming.goals.map { SyncMapper.normalizeGoal(it) }, local.goals, { it.id }, { it.version }, { it.updatedAt ?: 0L }),
            backlogItems = logicHelper.diffEntities(incoming.backlogItems, local.backlogItems, { it.id }, { it.version }, { it.updatedAt ?: 0L }),
            documents = logicHelper.diffEntities(incoming.documents, local.documents, { it.id }, { it.version }, { it.updatedAt }),
            attachments = logicHelper.diffEntities(incoming.attachments, local.attachments, { it.id }, { it.version }, { it.updatedAt }),
            contextAttachmentCrossRefs = logicHelper.diffEntities(incoming.contextAttachmentCrossRefs, local.contextAttachmentCrossRefs, { "${it.contextId}-${it.attachmentId}" }, { 0L }, { it.updatedAt ?: 0L })
        )
    }
    // У MergeRepository.kt додайте ці "проксі" методи:

    /**
     * Застосовує схвалені зміни до локальної бази даних.
     */
    suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        mergeLocalDataSource.applyChanges(approvedChanges)
    }

    /**
     * Імпортує лише вибрані дані (наприклад, тільки певні проекти).
     */
    suspend fun importSelectedData(selectedData: DatabaseContent): Result<String> {
        return try {
            mergeLocalDataSource.importSelectedData(
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

suspend fun createBackupDiff(incoming: SnapshotBundle): BackupDiff {
        val local = fullBackupLocalDataSource.loadFullSnapshotBundle()
        return BackupDiff(
            projects = logicHelper.diffEntities(incoming.contexts, local.contexts, { project -> project.id }, { project -> project.version }, { project -> project.updatedAt }),
            goals = logicHelper.diffEntities(incoming.goals, local.goals, { goal -> goal.id }, { goal -> goal.version }, { goal -> goal.updatedAt }),
            backlogItems = logicHelper.diffEntities(incoming.backlogItems, local.backlogItems, { item -> item.id }, { item -> item.version }, { item -> item.updatedAt }),
            documents = logicHelper.diffEntities(incoming.documents, local.documents, { doc -> doc.id }, { doc -> doc.version }, { doc -> doc.updatedAt }),
            attachments = logicHelper.diffEntities(incoming.attachments, local.attachments, { attachment -> attachment.id }, { attachment -> attachment.version }, { attachment -> attachment.updatedAt }),
            contextAttachmentCrossRefs = logicHelper.diffEntities(incoming.crossRefs, local.crossRefs, { crossRef -> "${crossRef.contextId}-${crossRef.attachmentId}" }, { 0L }, { crossRef -> crossRef.updatedAt })
        )
    }

suspend fun createSyncReport(bundle: SnapshotBundle): SyncReport {
        val localBundle = fullBackupLocalDataSource.loadFullSnapshotBundle()
        val changes = mutableListOf<SyncChange>()

        // Helper function to add changes
        fun <T> addChanges(incomingList: List<T>, localMap: Map<String, T>, idSelector: (T) -> String, nameSelector: (T) -> String, typeName: String, versionSelector: (T) -> Long, updatedSelector: (T) -> Long) {
            incomingList.forEach { incoming ->
                val local = localMap[idSelector(incoming)]
                if (local == null) {
                    changes.add(SyncChange(ChangeType.Add, typeName, idSelector(incoming), "Новий $typeName: ${nameSelector(incoming)}", entity = incoming as Any))
                } else if (versionSelector(incoming) > versionSelector(local) || (versionSelector(incoming) == versionSelector(local) && updatedSelector(incoming) > updatedSelector(local))) {
                    changes.add(SyncChange(ChangeType.Update, typeName, idSelector(incoming), "Оновлено $typeName: ${nameSelector(incoming)}", entity = incoming as Any))
                }
            }
        }

        addChanges(bundle.contexts, localBundle.contexts.associateBy { context -> context.id }, { context -> context.id }, { context -> context.name }, "Список", { context -> context.version }, { context -> context.updatedAt })
        addChanges(bundle.goals, localBundle.goals.associateBy { goal -> goal.id }, { goal -> goal.id }, { goal -> goal.text }, "Ціль", { goal -> goal.version }, { goal -> goal.updatedAt })

        return SyncReport(changes)
    }

    suspend fun applyServerChanges(bundle: SnapshotBundle): Result<Unit> {
        val ts = System.currentTimeMillis()
        return try {
            mergeLocalDataSource.applySnapshotBundle(bundle)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply server changes from snapshot", e)
            Result.failure(e)
        }
    }

    private fun sanitizeIncomingBackupJson(rawJson: String): String {
        return rawJson.replace(
            Regex("\"experimentalCapabilityIds\"\\s*:\\s*null"),
            "\"experimentalCapabilityIds\":[]",
        )
    }
}

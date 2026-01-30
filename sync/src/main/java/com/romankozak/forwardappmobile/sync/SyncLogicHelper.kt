//sync/src/main/java/com/romankozak/forwardappmobile/sync/SyncLogicHelper.kt
package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.sync.SyncMapper.updatedTs
import com.romankozak.forwardappmobile.core.data.models.sync.DiffResult
import com.romankozak.forwardappmobile.core.data.models.sync.UpdatedItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SyncLogicHelper
    @Inject
    constructor() {
        private val TAG = "SyncLogicHelper"

        /**
         * Основний алгоритм злиття даних (Merge).
         * Використовує стратегію LWW (Last-Write-Wins): виграє той, у кого вища версія
         * або новіша мітка часу оновлення.
         */
        inline fun <T> mergeAndMark(
            incoming: List<T>,
            localMap: Map<String, T>,
            crossinline idSelector: (T) -> String,
            crossinline versionSelector: (T) -> Long,
            crossinline updatedSelector: (T) -> Long,
            crossinline markSynced: (T, Long) -> T,
            syncedAt: Long,
            crossinline isDeletedSelector: (T) -> Boolean = { false },
            noinline logConsumer: ((T, T) -> Unit)? = null,
        ): List<T> {
            // 1. Дедуплікація вхідних даних (якщо сервер прислав дублі)
            val bestIncomingById =
                incoming
                    .groupBy { idSelector(it) }
                    .mapValues { entry ->
                        entry.value.maxWithOrNull(
                            compareBy<T> { versionSelector(it) }
                                .thenBy { updatedSelector(it) }
                                .thenBy { if (isDeletedSelector(it)) 1 else 0 },
                        )!!
                    }

            // 2. Порівняння з локальними даними
            return bestIncomingById.values.mapNotNull { inc ->
                val id = idSelector(inc)
                val local = localMap[id]
                if (local != null) logConsumer?.invoke(local, inc)

                val incVersion = versionSelector(inc)
                val localVersion = local?.let { versionSelector(it) } ?: Long.MIN_VALUE
                val incUpdated = updatedSelector(inc)
                val localUpdated = local?.let { updatedSelector(it) } ?: Long.MIN_VALUE

                val shouldTakeIncoming =
                    when {
                        local == null -> true
                        incVersion > localVersion -> true
                        incVersion < localVersion -> false
                        incUpdated > localUpdated -> true
                        incUpdated < localUpdated -> false
                        else -> true // При повній рівності серверні дані зазвичай мають пріоритет
                    }

                if (shouldTakeIncoming) markSynced(inc, syncedAt) else null
            }
        }

        /**
         * Розраховує різницю між двома наборами даних для звіту про синхронізацію.
         */
        fun <T> diffEntities(
            incomingList: List<T>,
            localList: List<T>,
            idSelector: (T) -> String,
            versionSelector: (T) -> Long,
            updatedSelector: (T) -> Long,
            isDeletedSelector: (T) -> Boolean = { false },
        ): DiffResult<T> {
            val localMap = localList.associateBy(idSelector)
            val incomingMap = incomingList.associateBy(idSelector)

            val added = incomingList.filter { idSelector(it) !in localMap && !isDeletedSelector(it) }

            val deleted =
                localList.mapNotNull { loc ->
                    val incomingMatch = incomingMap[idSelector(loc)]
                    if (incomingMatch != null && isDeletedSelector(incomingMatch)) loc else null
                }

            val updated =
                incomingList.mapNotNull { inc ->
                    if (isDeletedSelector(inc)) return@mapNotNull null
                    val localItem = localMap[idSelector(inc)] ?: return@mapNotNull null

                    val incVer = versionSelector(inc)
                    val locVer = versionSelector(localItem)
                    val incUpdated = updatedSelector(inc)
                    val locUpdated = updatedSelector(localItem)

                    val changed = incVer > locVer || (incVer == locVer && incUpdated > locUpdated) || inc != localItem
                    if (changed) UpdatedItem(local = localItem, incoming = inc) else null
                }

            return DiffResult(added = added, updated = updated, deleted = deleted)
        }

        /**
         * Дедуплікація елементів беклогу.
         */
        fun dedupListItems(items: List<BacklogItem>): List<BacklogItem> =
            items.groupBy { Triple(it.contextId, it.entityId, it.itemType) }
                .mapNotNull { (_, candidates) ->
                    candidates.maxWithOrNull(
                        compareBy<BacklogItem> { it.version }
                            .thenBy { it.updatedTs() }
                            .thenBy { if (it.isDeleted) 1 else 0 },
                    )
                }

        fun dedupBacklogOrders(items: List<BacklogOrder>): List<BacklogOrder> = BacklogOrderUtils.dedupBacklogOrders(items)

        /**
         * Створює відсутні крос-посилання для вкладень.
         */
        fun synthesizeMissingCrossRefs(
            attachments: List<AttachmentEntity>,
            existingCrossRefs: List<ContextAttachmentCrossRef>,
        ): List<ContextAttachmentCrossRef> {
            val existingKeys = existingCrossRefs.associateBy { "${it.contextId}-${it.attachmentId}" }

            val synthesized =
                attachments.mapNotNull { attachment ->
                    val owner = attachment.ownerContextId ?: return@mapNotNull null
                    val key = "$owner-${attachment.id}"
                    if (existingKeys.containsKey(key)) return@mapNotNull null

                    ContextAttachmentCrossRef(
                        contextId = owner,
                        attachmentId = attachment.id,
                        attachmentOrder = -attachment.updatedAt,
                        updatedAt = attachment.updatedAt,
                        syncedAt = attachment.syncedAt,
                        isDeleted = attachment.isDeleted,
                        version = attachment.version,
                    )
                }

            return (existingCrossRefs + synthesized).distinctBy { "${it.contextId}-${it.attachmentId}" }
        }

        /**
         * Перевірка, чи потребує об'єкт синхронізації (чи був він змінений після останньої відмітки).
         */
        fun <T> isUnsynced(
            item: T,
            syncedAtSelector: (T) -> Long?,
            updatedSelector: (T) -> Long,
            isDeletedSelector: (T) -> Boolean,
        ): Boolean {
            val syncedAt = syncedAtSelector(item) ?: 0L
            val updated = updatedSelector(item)
            return isDeletedSelector(item) || syncedAt == 0L || updated > syncedAt
        }
    }
